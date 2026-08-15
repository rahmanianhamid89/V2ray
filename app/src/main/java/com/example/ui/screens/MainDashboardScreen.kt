package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.ProxyProtocol
import com.example.model.ServerConfig
import com.example.model.VpnStats

@Composable
fun MainDashboardScreen(
    connectionState: ConnectionState,
    vpnStats: VpnStats,
    activeConfig: ServerConfig?,
    pingResult: String?,
    isTestingPing: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onImportClick: () -> Unit,
    onPingClick: () -> Unit,
    onSelectServerClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting
    val isDisconnecting = connectionState is ConnectionState.Disconnecting
    val isError = connectionState is ConnectionState.Error

    val cardBgColor by animateColorAsState(
        targetValue = when {
            isConnected -> Color(0xFF064E3B)
            isConnecting || isDisconnecting -> Color(0xFF1E293B)
            isError -> Color(0xFF450A0A)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "cardBgColor"
    )

    val accentColor by animateColorAsState(
        targetValue = when {
            isConnected -> Color(0xFF10B981)
            isConnecting -> Color(0xFF3B82F6)
            isDisconnecting -> Color(0xFFF59E0B)
            isError -> Color(0xFFEF4444)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "accentColor"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = when {
            isConnected -> Color(0xFFDC2626)
            isConnecting -> Color(0xFF2563EB)
            isDisconnecting -> Color(0xFFB91C1C)
            isError -> Color(0xFFDC2626)
            else -> Color(0xFF059669)
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "buttonBgColor"
    )

    val buttonElevation by animateDpAsState(
        targetValue = if (isConnected || isConnecting) 8.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonElevation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonGlowAlpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Connection Status Hero Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Connection Pulse Ring & Animated Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isConnecting || isConnected) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(
                            width = if (isConnected || isConnecting) 2.5.dp else 1.5.dp,
                            color = accentColor.copy(alpha = if (isConnected) 0.85f else 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    ) {
                        AnimatedContent(
                            targetState = connectionState,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.75f))
                                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.75f))
                            },
                            label = "heroIconAnimation"
                        ) { state ->
                            when (state) {
                                is ConnectionState.Connecting, is ConnectionState.Disconnecting -> {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                                is ConnectionState.Connected -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Connected",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                is ConnectionState.Error -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                is ConnectionState.Disconnected -> {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Ready to Connect",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Text with AnimatedContent
                AnimatedContent(
                    targetState = connectionState,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.9f))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                    },
                    label = "statusTextAnimation"
                ) { state ->
                    Text(
                        text = when (state) {
                            is ConnectionState.Disconnected -> "DISCONNECTED"
                            is ConnectionState.Connecting -> "CONNECTING..."
                            is ConnectionState.Connected -> "CONNECTED"
                            is ConnectionState.Disconnecting -> "DISCONNECTING..."
                            is ConnectionState.Error -> "CONNECTION ERROR"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isConnected) {
                    val conn = connectionState as ConnectionState.Connected
                    Text(
                        text = "Protocol: ${conn.protocol} | Ping: ${conn.latencyMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    if (conn.publicIp != null) {
                        Text(
                            text = "Public IP: ${conn.publicIp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                } else if (isConnecting) {
                    val conn = connectionState as ConnectionState.Connecting
                    Text(
                        text = conn.stage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else if (isError) {
                    val conn = connectionState as ConnectionState.Error
                    Text(
                        text = conn.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (conn.detail != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = conn.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "Secure proxy tunnel ready (VLESS, VMess, SS, Trojan)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Smoothly Animated Main Connect / Disconnect Action Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Animated glow aura behind the button when connected or connecting
                    if (isConnected || isConnecting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.87f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    buttonBgColor.copy(
                                        alpha = if (isConnected) buttonGlowAlpha * 0.4f else 0.25f
                                    )
                                )
                        )
                    }

                    Button(
                        onClick = {
                            if (isConnected) {
                                onDisconnectClick()
                            } else {
                                onConnectClick()
                            }
                        },
                        enabled = !isConnecting && !isDisconnecting && (isConnected || activeConfig != null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBgColor,
                            disabledContainerColor = buttonBgColor.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = buttonElevation,
                            pressedElevation = 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(54.dp)
                            .testTag(if (isConnected) "disconnect_vpn_button" else "connect_vpn_button")
                    ) {
                        AnimatedContent(
                            targetState = connectionState,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.85f))
                                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f))
                            },
                            label = "buttonContentAnimation"
                        ) { state ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when (state) {
                                    is ConnectionState.Connecting -> {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "CONNECTING...",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    is ConnectionState.Disconnecting -> {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "DISCONNECTING...",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    is ConnectionState.Connected -> {
                                        Icon(
                                            imageVector = Icons.Default.StopCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "DISCONNECT VPN",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CONNECT VPN",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Server Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectServerClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Server",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Node",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeConfig?.getDisplayTitle() ?: "No server selected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (activeConfig != null) {
                        val subDetail = when (activeConfig.proxyProtocol) {
                            ProxyProtocol.VLESS -> "${activeConfig.address}:${activeConfig.port} • VLESS/${activeConfig.transport.uppercase()} (${activeConfig.security})"
                            ProxyProtocol.VMESS -> "${activeConfig.address}:${activeConfig.port} • VMESS/${activeConfig.transport.uppercase()} (${activeConfig.security})"
                            ProxyProtocol.SHADOWSOCKS -> "${activeConfig.address}:${activeConfig.port} • SS (${activeConfig.method})"
                            ProxyProtocol.TROJAN -> "${activeConfig.address}:${activeConfig.port} • TROJAN/TLS"
                        }
                        Text(
                            text = subDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Total Used: ${activeConfig.trafficDisplayText}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSelectServerClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Switch")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Realtime Stats Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Traffic & Statistics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatTile(
                        icon = Icons.Default.ArrowUpward,
                        label = "Upload",
                        value = vpnStats.formatTx(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatTile(
                        icon = Icons.Default.ArrowDownward,
                        label = "Download",
                        value = vpnStats.formatRx(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatTile(
                        icon = Icons.Default.Speed,
                        label = "Duration",
                        value = vpnStats.formatDuration(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions (Import Config & Test Ping)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onImportClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_config_dashboard_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import Config")
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(
                onClick = onPingClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("test_ping_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTestingPing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Ping")
            }
        }

        if (pingResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = pingResult,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
