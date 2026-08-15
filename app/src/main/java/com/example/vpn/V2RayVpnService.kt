package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.logger.AppLogger
import com.example.model.ConnectionState
import com.example.model.ServerConfig
import com.example.network.ConnectivityTester
import com.example.proxy.VlessConfigJsonBuilder
import com.example.proxy.VlessProxyEngine
import com.example.proxy.XrayConfigJsonBuilder
import com.example.proxy.XrayCoreBridge
import com.example.storage.ConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class V2RayVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunManager: TunManager? = null
    private var proxyEngine: VlessProxyEngine? = null

    private var packetLoopJob: Job? = null
    private var statsJob: Job? = null

    private var connectedStartTime = 0L
    private var currentActiveServerId = 0L
    private var txBytesCount = 0L
    private var rxBytesCount = 0L
    private var txPacketsCount = 0L
    private var rxPacketsCount = 0L
    private var lastFlushedTxBytes = 0L
    private var lastFlushedRxBytes = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        AppLogger.i(TAG, "V2RayVpnService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        AppLogger.i(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_CONNECT -> {
                val serverId = intent.getLongExtra(EXTRA_SERVER_ID, -1L)
                startVpnTunnel(serverId)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
            }
            else -> {
                if (VpnController.connectionState.value is ConnectionState.Disconnected) {
                    val serverId = intent?.getLongExtra(EXTRA_SERVER_ID, -1L) ?: -1L
                    startVpnTunnel(serverId)
                }
            }
        }

        return START_STICKY
    }

    private fun startVpnTunnel(serverId: Long) {
        serviceScope.launch {
            try {
                VpnController.updateConnectionState(ConnectionState.Connecting("Loading configuration..."))
                startForeground(NOTIFICATION_ID, buildNotification("Connecting to proxy server..."))

                val repository = ConfigRepository.getInstance(applicationContext)
                val config = if (serverId > 0) {
                    val list = withContext(Dispatchers.IO) { repository.getActiveConfig() }
                    list
                } else {
                    repository.getActiveConfig()
                }

                if (config == null) {
                    val errMsg = "No active server configuration found."
                    AppLogger.e(TAG, errMsg)
                    VpnController.updateConnectionState(ConnectionState.Error("Missing Config", errMsg))
                    stopSelf()
                    return@launch
                }

                currentActiveServerId = config.id
                lastFlushedTxBytes = 0L
                lastFlushedRxBytes = 0L

                val protoName = config.protocol.uppercase()
                AppLogger.i(TAG, "Preparing VPN for $protoName server: ${config.getDisplayTitle()} (${config.address}:${config.port})")
                VpnController.updateConnectionState(ConnectionState.Connecting("Connecting $protoName core..."))

                // Step 1: Start Proxy Engine / Core
                val configJson = XrayConfigJsonBuilder.buildXrayConfigJson(config)
                val coreResult = XrayCoreBridge.startCore(config, configJson)
                if (coreResult.isFailure) {
                    val err = coreResult.exceptionOrNull()?.localizedMessage ?: "Failed to start proxy core"
                    AppLogger.e(TAG, err)
                    VpnController.updateConnectionState(ConnectionState.Error("Proxy Core Error", err))
                    stopSelf()
                    return@launch
                }

                // Connect socket level proxy
                val engine = VlessProxyEngine(config)
                val connectRes = engine.connect()
                if (connectRes.isFailure) {
                    val err = connectRes.exceptionOrNull()?.localizedMessage ?: "$protoName Socket Connection failed"
                    AppLogger.e(TAG, "Socket connect failed: $err")
                    VpnController.updateConnectionState(ConnectionState.Error("Connection Failed", "Unable to connect to ${config.address}:${config.port} - $err"))
                    XrayCoreBridge.stopCore()
                    stopSelf()
                    return@launch
                }
                proxyEngine = engine

                // Step 2: Establish TUN Virtual Interface
                VpnController.updateConnectionState(ConnectionState.Connecting("Configuring TUN interface..."))
                val manager = TunManager(this@V2RayVpnService)
                val tunResult = manager.establishTun()
                if (tunResult.isFailure) {
                    val err = tunResult.exceptionOrNull()?.localizedMessage ?: "Failed to create TUN interface"
                    AppLogger.e(TAG, err)
                    VpnController.updateConnectionState(ConnectionState.Error("TUN Error", err))
                    engine.close()
                    XrayCoreBridge.stopCore()
                    stopSelf()
                    return@launch
                }
                tunManager = manager

                // Step 3: Launch Packet Processing Loop
                startPacketProcessingLoop(manager.getInputStream(), manager.getOutputStream(), engine)

                // Step 4: Perform Real Connectivity Test before setting State to Connected
                VpnController.updateConnectionState(ConnectionState.Connecting("Verifying tunnel internet access..."))
                val testResult = ConnectivityTester.testConnection()

                if (testResult.isSuccessful) {
                    connectedStartTime = System.currentTimeMillis()
                    VpnController.resetStats()
                    startStatsTimer()

                    val protoDetail = when (config.protocol.lowercase()) {
                        "vless" -> "VLESS (${config.transport.uppercase()})"
                        "vmess" -> "VMess (${config.transport.uppercase()})"
                        "shadowsocks", "ss" -> "Shadowsocks (${config.method})"
                        "trojan" -> "Trojan (${config.transport.uppercase()})"
                        else -> config.protocol.uppercase()
                    }

                    val connectedState = ConnectionState.Connected(
                        serverAddress = config.address,
                        protocol = protoDetail,
                        latencyMs = testResult.latencyMs,
                        publicIp = testResult.publicIp,
                        connectedSince = connectedStartTime
                    )
                    VpnController.updateConnectionState(connectedState)

                    val statusText = "Connected to ${config.getDisplayTitle()} | ${testResult.latencyMs}ms"
                    updateNotification(statusText)
                    AppLogger.i(TAG, "VPN Tunnel fully established & verified ($protoDetail)!")
                } else {
                    val testErr = testResult.errorMessage ?: "Tunnel established but no Internet response."
                    AppLogger.e(TAG, "Connectivity test failed: $testErr")
                    VpnController.updateConnectionState(ConnectionState.Error("Tunnel Connectivity Failed", testErr))
                    stopVpnInternal()
                }

            } catch (e: Exception) {
                AppLogger.e(TAG, "Unexpected error starting VPN", e)
                VpnController.updateConnectionState(ConnectionState.Error("System Error", e.localizedMessage))
                stopVpnInternal()
            }
        }
    }

    private fun startPacketProcessingLoop(
        inp: FileInputStream?,
        out: FileOutputStream?,
        engine: VlessProxyEngine
    ) {
        if (inp == null || out == null) return

        packetLoopJob = serviceScope.launch(Dispatchers.IO) {
            val buffer = ByteBuffer.allocate(32768)
            val packetBytes = ByteArray(32768)

            AppLogger.i(TAG, "Started TUN IP packet forwarding loop.")

            while (isActive) {
                try {
                    val length = inp.read(packetBytes)
                    if (length > 0) {
                        txBytesCount += length
                        txPacketsCount++

                        // Route IP packet to proxy socket stream
                        engine.getOutputStream()?.apply {
                            write(packetBytes, 0, length)
                            flush()
                        }
                    } else if (length < 0) {
                        break
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        AppLogger.w(TAG, "Packet read exception: ${e.localizedMessage}")
                        delay(10)
                    }
                }
            }
        }
    }

    private fun startStatsTimer() {
        statsJob?.cancel()
        val repository = ConfigRepository.getInstance(applicationContext)
        statsJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val elapsedSec = (System.currentTimeMillis() - connectedStartTime) / 1000
                VpnController.updateStats(
                    txBytes = txBytesCount,
                    rxBytes = rxBytesCount,
                    txPackets = txPacketsCount,
                    rxPackets = rxPacketsCount,
                    durationSec = elapsedSec
                )

                // Incrementally persist traffic every 3 seconds or on notable data exchange
                val deltaTx = txBytesCount - lastFlushedTxBytes
                val deltaRx = rxBytesCount - lastFlushedRxBytes
                if (currentActiveServerId > 0 && (deltaTx > 0 || deltaRx > 0)) {
                    lastFlushedTxBytes = txBytesCount
                    lastFlushedRxBytes = rxBytesCount
                    repository.addTraffic(currentActiveServerId, deltaTx, deltaRx)
                }
            }
        }
    }

    private fun stopVpnTunnel() {
        serviceScope.launch {
            VpnController.updateConnectionState(ConnectionState.Disconnecting)
            // Final flush of remaining traffic before stopping
            val deltaTx = txBytesCount - lastFlushedTxBytes
            val deltaRx = rxBytesCount - lastFlushedRxBytes
            if (currentActiveServerId > 0 && (deltaTx > 0 || deltaRx > 0)) {
                val repository = ConfigRepository.getInstance(applicationContext)
                repository.addTraffic(currentActiveServerId, deltaTx, deltaRx)
            }
            stopVpnInternal()
            VpnController.updateConnectionState(ConnectionState.Disconnected)
            stopSelf()
        }
    }

    private fun stopVpnInternal() {
        packetLoopJob?.cancel()
        statsJob?.cancel()
        packetLoopJob = null
        statsJob = null

        tunManager?.closeTun()
        tunManager = null

        proxyEngine?.close()
        proxyEngine = null

        XrayCoreBridge.stopCore()

        stopForeground(STOP_FOREGROUND_REMOVE)
        AppLogger.i(TAG, "VPN service disconnected and resources cleaned up.")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnInternal()
        serviceScope.cancel()
        AppLogger.i(TAG, "V2RayVpnService destroyed.")
    }

    override fun onRevoke() {
        super.onRevoke()
        AppLogger.w(TAG, "VPN Permission was revoked by user or system!")
        stopVpnTunnel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "V2Ray VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active VPN connection status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, V2RayVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("V2Ray VPN")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Disconnect", disconnectPendingIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val TAG = "V2RayVpnService"
        const val CHANNEL_ID = "v2ray_vpn_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_SERVER_ID = "extra_server_id"
    }
}
