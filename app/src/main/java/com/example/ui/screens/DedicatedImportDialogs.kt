package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProxyProtocol
import com.example.model.ServerConfig
import com.example.proxy.UniversalConfigParser

// ==========================================
// 1. Dedicated Clipboard Import Dialog
// ==========================================
@Composable
fun ClipboardImportDialog(
    availableGroups: List<String>,
    currentGroup: String,
    onDismiss: () -> Unit,
    onSaveConfig: (ServerConfig) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var rawText by remember { mutableStateOf("") }
    var parsedConfig by remember { mutableStateOf<ServerConfig?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var customName by remember { mutableStateOf("") }
    var targetGroup by remember { mutableStateOf(if (currentGroup != "All") currentGroup else "Default") }

    fun checkClipboard() {
        val clip = clipboardManager.getText()?.text?.trim() ?: ""
        rawText = clip
        errorText = null
        if (clip.isNotBlank()) {
            val res = UniversalConfigParser.parse(clip)
            if (res.isSuccess) {
                val cfg = res.getOrNull()!!
                parsedConfig = cfg
                customName = cfg.name
            } else {
                parsedConfig = null
                errorText = res.exceptionOrNull()?.localizedMessage ?: "No supported proxy link found in clipboard."
            }
        } else {
            parsedConfig = null
            errorText = "Clipboard is currently empty."
        }
    }

    LaunchedEffect(Unit) {
        checkClipboard()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Import from Clipboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Clipboard Auto-Detection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (parsedConfig != null) {
                    val cfg = parsedConfig!!
                    val badgeColor = when (cfg.proxyProtocol) {
                        ProxyProtocol.VLESS -> Color(0xFF059669)
                        ProxyProtocol.VMESS -> Color(0xFF2563EB)
                        ProxyProtocol.SHADOWSOCKS -> Color(0xFFD97706)
                        ProxyProtocol.TROJAN -> Color(0xFF7C3AED)
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = cfg.protocolBadgeText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Valid Config Detected",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = badgeColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Server: ${cfg.address}:${cfg.port}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Transport: ${cfg.transport.uppercase()} • Security: ${cfg.security.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Config Title / Name") },
                        modifier = Modifier.fillMaxWidth().testTag("clipboard_config_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetGroup,
                        onValueChange = { targetGroup = it },
                        label = { Text("Assign Group") },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorText ?: "No valid configuration found in clipboard.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { checkClipboard() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-read Clipboard")
                    }
                }
            }
        },
        confirmButton = {
            if (parsedConfig != null) {
                Button(
                    onClick = {
                        val finalCfg = parsedConfig!!.copy(
                            name = customName.ifBlank { parsedConfig!!.name },
                            groupName = targetGroup.ifBlank { "Default" }
                        )
                        onSaveConfig(finalCfg)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_clipboard_import_button")
                ) {
                    Text("Save Config")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 2. Dedicated Link / URL Import Dialog
// ==========================================
@Composable
fun LinkImportDialog(
    availableGroups: List<String>,
    currentGroup: String,
    onDismiss: () -> Unit,
    onSaveConfig: (ServerConfig) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var linkText by remember { mutableStateOf("") }
    var targetGroup by remember { mutableStateOf(if (currentGroup != "All") currentGroup else "Default") }
    var parsedPreview by remember { mutableStateOf<ServerConfig?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun parseInput(input: String) {
        linkText = input
        errorText = null
        if (input.isNotBlank()) {
            val res = UniversalConfigParser.parse(input)
            parsedPreview = res.getOrNull()
            if (res.isFailure) {
                errorText = res.exceptionOrNull()?.localizedMessage ?: "Invalid link format"
            }
        } else {
            parsedPreview = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Import via Link / URL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("VLESS, VMess, SS & Trojan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Paste your configuration link (e.g. vless://, vmess://, ss://, trojan://) below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = linkText,
                    onValueChange = { parseInput(it) },
                    label = { Text("Config Link") },
                    placeholder = { Text("vless://... or vmess://...") },
                    trailingIcon = {
                        Row {
                            if (linkText.isNotEmpty()) {
                                IconButton(onClick = { parseInput("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    parseInput(clip)
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_link_input_field"),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetGroup,
                    onValueChange = { targetGroup = it },
                    label = { Text("Target Group") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (parsedPreview != null) {
                    val preview = parsedPreview!!
                    val badgeColor = when (preview.proxyProtocol) {
                        ProxyProtocol.VLESS -> Color(0xFF059669)
                        ProxyProtocol.VMESS -> Color(0xFF2563EB)
                        ProxyProtocol.SHADOWSOCKS -> Color(0xFFD97706)
                        ProxyProtocol.TROJAN -> Color(0xFF7C3AED)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = preview.protocolBadgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preview.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${preview.address}:${preview.port} • ${preview.transport.uppercase()} (${preview.security})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cfg = parsedPreview ?: UniversalConfigParser.parse(linkText).getOrNull()
                    if (cfg != null) {
                        onSaveConfig(cfg.copy(groupName = targetGroup.ifBlank { "Default" }))
                        onDismiss()
                    }
                },
                enabled = linkText.isNotBlank() && errorText == null && parsedPreview != null,
                modifier = Modifier.testTag("save_link_config_button")
            ) {
                Text("Import Config")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 3. Dedicated Subscription Import Dialog
// ==========================================
@Composable
fun SubscriptionImportDialog(
    availableGroups: List<String>,
    currentGroup: String,
    onDismiss: () -> Unit,
    onImportSub: (subUrl: String, groupName: String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var subUrlText by remember { mutableStateOf("") }
    var targetGroup by remember { mutableStateOf(if (currentGroup != "All") currentGroup else "Subscription") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Import Subscription", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Multi-Node Auto Download", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Enter a subscription URL (HTTP/HTTPS) to download and auto-import all server nodes (VLESS, VMess, SS, Trojan) into a dedicated group.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subUrlText,
                    onValueChange = { subUrlText = it },
                    label = { Text("Subscription Link (URL)") },
                    placeholder = { Text("https://example.com/sub/...") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                subUrlText = clip
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_url_input_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetGroup,
                    onValueChange = { targetGroup = it },
                    label = { Text("Group Name for Subscription") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subUrlText.isNotBlank()) {
                        isLoading = true
                        onImportSub(subUrlText.trim(), targetGroup.ifBlank { "Subscription" })
                    }
                },
                enabled = subUrlText.isNotBlank() && !isLoading,
                modifier = Modifier.testTag("fetch_sub_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fetching...")
                } else {
                    Text("Fetch & Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 4. Dedicated Raw JSON Config Import Dialog
// ==========================================
@Composable
fun JsonConfigImportDialog(
    availableGroups: List<String>,
    currentGroup: String,
    onDismiss: () -> Unit,
    onSaveConfig: (ServerConfig) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var jsonText by remember { mutableStateOf("") }
    var targetGroup by remember { mutableStateOf(if (currentGroup != "All") currentGroup else "Default") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var parsedConfig by remember { mutableStateOf<ServerConfig?>(null) }

    fun validateAndParse(text: String) {
        jsonText = text
        errorText = null
        if (text.isNotBlank()) {
            val res = UniversalConfigParser.parseJson(text)
            if (res.isSuccess) {
                parsedConfig = res.getOrNull()
            } else {
                parsedConfig = null
                errorText = res.exceptionOrNull()?.localizedMessage ?: "Invalid JSON configuration format."
            }
        } else {
            parsedConfig = null
        }
    }

    val sampleVlessJson = """
    {
      "protocol": "vless",
      "tag": "VLESS JSON Server",
      "settings": {
        "vnext": [{
          "address": "vless.example.com",
          "port": 443,
          "users": [{ "id": "b831381d-6324-4d53-ad4f-8cda48b30811", "flow": "xtls-rprx-vision" }]
        }]
      },
      "streamSettings": {
        "network": "tcp",
        "security": "tls",
        "tlsSettings": { "serverName": "vless.example.com" }
      }
    }
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEC4899).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Custom / Raw JSON Config", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Xray & V2Ray Format", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Paste or edit your outbound / core JSON configuration:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { validateAndParse(sampleVlessJson) },
                        label = { Text("Load VLESS Template") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                validateAndParse(clip)
                            }
                        },
                        label = { Text("Paste Clipboard") },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { validateAndParse(it) },
                    label = { Text("JSON Outbound / Config") },
                    placeholder = { Text("{\n  \"protocol\": \"vless\", ...\n}") },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("raw_json_input_field"),
                    minLines = 6,
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetGroup,
                    onValueChange = { targetGroup = it },
                    label = { Text("Target Group") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (parsedConfig != null) {
                    val preview = parsedConfig!!
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Parsed: ${preview.protocol.uppercase()} -> ${preview.address}:${preview.port}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedConfig != null) {
                        onSaveConfig(parsedConfig!!.copy(groupName = targetGroup.ifBlank { "Default" }))
                        onDismiss()
                    }
                },
                enabled = parsedConfig != null,
                modifier = Modifier.testTag("import_json_config_button")
            ) {
                Text("Import Config")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 5. Dedicated Create Group Dialog
// ==========================================
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreateGroup: (groupName: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }

    val presetSuggestions = listOf("VIP Servers", "Gaming & Low Ping", "Tehran Direct", "Sub-01", "Backup Nodes")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D9488).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Create New Group", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Server Categorization", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Enter a name for the new category tab:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Tehran Fast") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetSuggestions.forEach { suggestion ->
                        FilterChip(
                            selected = groupName == suggestion,
                            onClick = { groupName = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreateGroup(groupName.trim())
                        onDismiss()
                    }
                },
                enabled = groupName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_group_button")
            ) {
                Text("Create Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
