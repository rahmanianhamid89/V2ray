package com.example.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.example.logger.AppLogger
import com.example.network.DnsManager
import com.example.storage.AppSettingsManager
import java.io.FileInputStream
import java.io.FileOutputStream

class TunManager(private val service: VpnService) {

    private var tunDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    fun establishTun(
        virtualIp: String = "10.0.0.2",
        prefixLength: Int = 24,
        mtu: Int = 1500,
        dnsServers: List<String> = DnsManager.getDnsServers()
    ): Result<ParcelFileDescriptor> {
        return try {
            val builder = service.Builder()
                .setSession("V2RayVpnService")
                .setMtu(mtu)
                .addAddress(virtualIp, prefixLength)
                .addRoute("0.0.0.0", 0)

            dnsServers.forEach { dns ->
                try {
                    builder.addDnsServer(dns)
                } catch (e: Exception) {
                    AppLogger.w("TunManager", "Failed to add DNS server $dns: ${e.localizedMessage}")
                }
            }

            // Exclude current app from VPN loop to prevent socket loops
            try {
                builder.addDisallowedApplication(service.packageName)
            } catch (e: Exception) {
                AppLogger.w("TunManager", "Could not add disallowed application: ${e.localizedMessage}")
            }

            // Exclude user-selected bypassed apps (Split Tunneling)
            val bypassedApps = AppSettingsManager.getBypassedApps()
            var bypassedCount = 0
            bypassedApps.forEach { pkg ->
                if (pkg != service.packageName) {
                    try {
                        builder.addDisallowedApplication(pkg)
                        bypassedCount++
                    } catch (e: Exception) {
                        AppLogger.w("TunManager", "Failed to bypass app $pkg: ${e.localizedMessage}")
                    }
                }
            }
            if (bypassedCount > 0) {
                AppLogger.i("TunManager", "Bypassed $bypassedCount selected applications from VPN tunnel.")
            }

            val pfd = builder.establish()
                ?: return Result.failure(IllegalStateException("VpnService.Builder.establish() returned null. Check VPN permission."))

            tunDescriptor = pfd
            inputStream = FileInputStream(pfd.fileDescriptor)
            outputStream = FileOutputStream(pfd.fileDescriptor)

            AppLogger.i("TunManager", "TUN virtual interface established successfully ($virtualIp/$prefixLength, MTU $mtu)")
            Result.success(pfd)
        } catch (e: Exception) {
            AppLogger.e("TunManager", "Error establishing TUN interface: ${e.localizedMessage}", e)
            closeTun()
            Result.failure(e)
        }
    }

    fun getInputStream(): FileInputStream? = inputStream
    fun getOutputStream(): FileOutputStream? = outputStream

    fun closeTun() {
        try { inputStream?.close() } catch (ignored: Exception) {}
        try { outputStream?.close() } catch (ignored: Exception) {}
        try { tunDescriptor?.close() } catch (ignored: Exception) {}
        inputStream = null
        outputStream = null
        tunDescriptor = null
        AppLogger.i("TunManager", "TUN virtual interface closed.")
    }
}
