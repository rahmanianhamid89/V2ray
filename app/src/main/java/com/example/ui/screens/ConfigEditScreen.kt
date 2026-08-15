package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ProxyProtocol
import com.example.model.ServerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditScreen(
    initialConfig: ServerConfig?,
    initialProtocol: String? = null,
    onSaveConfig: (ServerConfig) -> Unit,
    onBack: () -> Unit
) {
    var protocol by remember { mutableStateOf(initialConfig?.protocol ?: initialProtocol ?: "vless") }
    var name by remember { mutableStateOf(initialConfig?.name ?: "") }
    var groupName by remember { mutableStateOf(initialConfig?.groupName ?: "Default") }
    var address by remember { mutableStateOf(initialConfig?.address ?: "") }
    var portText by remember {
        mutableStateOf(
            initialConfig?.port?.toString() ?: when (protocol) {
                "shadowsocks" -> "8388"
                "trojan" -> "443"
                else -> "443"
            }
        )
    }

    // Auth fields
    var uuid by remember { mutableStateOf(initialConfig?.uuid ?: "") }
    var password by remember { mutableStateOf(initialConfig?.password ?: "") }
    var method by remember { mutableStateOf(initialConfig?.method ?: "aes-256-gcm") }
    var alterIdText by remember { mutableStateOf(initialConfig?.alterId?.toString() ?: "0") }
    var encryption by remember { mutableStateOf(initialConfig?.encryption ?: "none") }

    // Stream & Security fields
    var transport by remember { mutableStateOf(initialConfig?.transport ?: "tcp") }
    var security by remember { mutableStateOf(initialConfig?.security ?: if (protocol == "shadowsocks") "none" else "tls") }
    var sni by remember { mutableStateOf(initialConfig?.sni ?: "") }
    var alpn by remember { mutableStateOf(initialConfig?.alpn ?: "") }
    var fingerprint by remember { mutableStateOf(initialConfig?.fingerprint ?: "chrome") }
    var wsPath by remember { mutableStateOf(initialConfig?.wsPath ?: "/") }
    var wsHost by remember { mutableStateOf(initialConfig?.wsHost ?: "") }
    var publicKey by remember { mutableStateOf(initialConfig?.publicKey ?: "") }
    var shortId by remember { mutableStateOf(initialConfig?.shortId ?: "") }
    var spiderX by remember { mutableStateOf(initialConfig?.spiderX ?: "") }
    var flow by remember { mutableStateOf(initialConfig?.flow ?: "") }

    val scrollState = rememberScrollState()

    val isFormValid = when (protocol) {
        "vless", "vmess" -> address.isNotBlank() && uuid.isNotBlank()
        "trojan", "shadowsocks" -> address.isNotBlank() && password.isNotBlank()
        else -> address.isNotBlank()
    }

    fun buildConfig(): ServerConfig {
        val portInt = portText.toIntOrNull() ?: 443
        val alterIdInt = alterIdText.toIntOrNull() ?: 0
        return (initialConfig ?: ServerConfig()).copy(
            protocol = protocol,
            name = name.ifBlank { "$address:$portInt" },
            groupName = groupName.ifBlank { "Default" },
            address = address.trim(),
            port = portInt,
            uuid = uuid.trim(),
            password = password.trim(),
            method = method.trim(),
            alterId = alterIdInt,
            encryption = encryption,
            transport = transport,
            security = security,
            sni = sni.trim(),
            alpn = alpn.trim(),
            fingerprint = fingerprint.trim(),
            wsPath = wsPath.trim(),
            wsHost = wsHost.trim(),
            publicKey = publicKey.trim(),
            shortId = shortId.trim(),
            spiderX = spiderX.trim(),
            flow = flow.trim()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialConfig == null) "New ${protocol.uppercase()} Config" else "Edit ${protocol.uppercase()} Config"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isFormValid) {
                                onSaveConfig(buildConfig())
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.testTag("save_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Protocol Selection Chips (VLESS, VMess, Shadowsocks, Trojan)
            Text(
                text = "Select Protocol",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = protocol == "vless",
                    onClick = {
                        protocol = "vless"
                        if (security == "none") security = "tls"
                    },
                    label = { Text("VLESS") },
                    modifier = Modifier.testTag("protocol_chip_vless")
                )
                FilterChip(
                    selected = protocol == "vmess",
                    onClick = {
                        protocol = "vmess"
                        if (security == "reality") security = "tls"
                    },
                    label = { Text("VMess") },
                    modifier = Modifier.testTag("protocol_chip_vmess")
                )
                FilterChip(
                    selected = protocol == "shadowsocks",
                    onClick = {
                        protocol = "shadowsocks"
                        transport = "tcp"
                        security = "none"
                    },
                    label = { Text("Shadowsocks") },
                    modifier = Modifier.testTag("protocol_chip_shadowsocks")
                )
                FilterChip(
                    selected = protocol == "trojan",
                    onClick = {
                        protocol = "trojan"
                        security = "tls"
                    },
                    label = { Text("Trojan") },
                    modifier = Modifier.testTag("protocol_chip_trojan")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile & Group
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name (Alias)") },
                placeholder = { Text("e.g. Frankfurt Fast, Sub Node 01") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Server & Port
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Server Address *") },
                    placeholder = { Text("192.168.1.1 or example.com") },
                    modifier = Modifier.weight(2f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Protocol-Specific Credentials
            when (protocol) {
                "vless" -> {
                    Text("VLESS Authentication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text("User UUID *") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = flow,
                        onValueChange = { flow = it },
                        label = { Text("Flow (Optional, e.g. xtls-rprx-vision)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "vmess" -> {
                    Text("VMess Authentication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text("User ID (UUID) *") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = alterIdText,
                            onValueChange = { alterIdText = it },
                            label = { Text("AlterId") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = encryption,
                            onValueChange = { encryption = it },
                            label = { Text("Security / Cipher") },
                            placeholder = { Text("auto, aes-128-gcm, zero") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }

                "shadowsocks" -> {
                    Text("Shadowsocks Authentication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password / Pre-Shared Key *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Encryption Method", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = method == "aes-256-gcm",
                            onClick = { method = "aes-256-gcm" },
                            label = { Text("AES-256-GCM") }
                        )
                        FilterChip(
                            selected = method == "aes-128-gcm",
                            onClick = { method = "aes-128-gcm" },
                            label = { Text("AES-128-GCM") }
                        )
                        FilterChip(
                            selected = method == "chacha20-ietf-poly1305",
                            onClick = { method = "chacha20-ietf-poly1305" },
                            label = { Text("ChaCha20") }
                        )
                    }
                }

                "trojan" -> {
                    Text("Trojan Authentication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transport Protocol Selection (For VLESS, VMess, Trojan)
            if (protocol != "shadowsocks") {
                Text("Transport Protocol", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = transport == "tcp",
                        onClick = { transport = "tcp" },
                        label = { Text("TCP") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = transport == "ws",
                        onClick = { transport = "ws" },
                        label = { Text("WebSocket (WS)") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Security Layer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = security == "none",
                        onClick = { security = "none" },
                        label = { Text("None") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = security == "tls",
                        onClick = { security = "tls" },
                        label = { Text("TLS") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (protocol == "vless") {
                        FilterChip(
                            selected = security == "reality",
                            onClick = { security = "reality" },
                            label = { Text("REALITY") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (security == "tls" || security == "reality") {
                    OutlinedTextField(
                        value = sni,
                        onValueChange = { sni = it },
                        label = { Text("SNI / Server Name (TLS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fingerprint,
                        onValueChange = { fingerprint = it },
                        label = { Text("Fingerprint (e.g. chrome, firefox)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (security == "reality" && protocol == "vless") {
                    Text("REALITY Parameters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = publicKey,
                        onValueChange = { publicKey = it },
                        label = { Text("Public Key (pbk)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shortId,
                        onValueChange = { shortId = it },
                        label = { Text("Short ID (sid)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spiderX,
                        onValueChange = { spiderX = it },
                        label = { Text("SpiderX (spx)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (transport == "ws") {
                    Text("WebSocket Parameters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wsPath,
                        onValueChange = { wsPath = it },
                        label = { Text("WS Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wsHost,
                        onValueChange = { wsHost = it },
                        label = { Text("WS Host Header") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        onSaveConfig(buildConfig())
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_config_bottom_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save ${protocol.uppercase()} Configuration")
            }
        }
    }
}
