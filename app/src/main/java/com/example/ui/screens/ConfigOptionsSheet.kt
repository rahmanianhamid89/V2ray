package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ConfigOptionCategory {
    IMPORT_METHODS,
    MANUAL_PROTOCOLS,
    ORGANIZATION
}

enum class ConfigActionOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val category: ConfigOptionCategory,
    val testTag: String
) {
    IMPORT_CLIPBOARD(
        title = "Import from Clipboard",
        description = "Auto-detect and load proxy link or JSON from clipboard",
        icon = Icons.Default.ContentPaste,
        iconTint = Color(0xFF10B981),
        category = ConfigOptionCategory.IMPORT_METHODS,
        testTag = "option_import_clipboard"
    ),
    IMPORT_LINK(
        title = "Import via Link / URL",
        description = "Import vless://, vmess://, ss://, or trojan:// URI",
        icon = Icons.Default.Link,
        iconTint = Color(0xFF3B82F6),
        category = ConfigOptionCategory.IMPORT_METHODS,
        testTag = "option_import_link"
    ),
    IMPORT_SUBSCRIPTION(
        title = "Import Subscription (Sub)",
        description = "Download and auto-update multi-node subscription package",
        icon = Icons.Default.CloudDownload,
        iconTint = Color(0xFF6366F1),
        category = ConfigOptionCategory.IMPORT_METHODS,
        testTag = "option_import_subscription"
    ),
    IMPORT_RAW_JSON(
        title = "Custom / Raw JSON Config",
        description = "Import, validate, or edit raw Xray/V2Ray JSON configuration",
        icon = Icons.Default.Code,
        iconTint = Color(0xFFEC4899),
        category = ConfigOptionCategory.IMPORT_METHODS,
        testTag = "option_import_raw_json"
    ),
    MANUAL_VLESS(
        title = "VLESS Configuration",
        description = "Manual VLESS setup with TLS / REALITY / Flow settings",
        icon = Icons.Default.Bolt,
        iconTint = Color(0xFF059669),
        category = ConfigOptionCategory.MANUAL_PROTOCOLS,
        testTag = "option_manual_vless"
    ),
    MANUAL_VMESS(
        title = "VMess Configuration",
        description = "Manual VMess setup with AlterID / Security / WebSocket settings",
        icon = Icons.Default.RocketLaunch,
        iconTint = Color(0xFF2563EB),
        category = ConfigOptionCategory.MANUAL_PROTOCOLS,
        testTag = "option_manual_vmess"
    ),
    MANUAL_SHADOWSOCKS(
        title = "Shadowsocks Configuration",
        description = "Manual Shadowsocks setup with Cipher / Key encryption settings",
        icon = Icons.Default.Shield,
        iconTint = Color(0xFFD97706),
        category = ConfigOptionCategory.MANUAL_PROTOCOLS,
        testTag = "option_manual_shadowsocks"
    ),
    MANUAL_TROJAN(
        title = "Trojan Configuration",
        description = "Manual Trojan setup with Password, TLS & SNI settings",
        icon = Icons.Default.Lock,
        iconTint = Color(0xFF7C3AED),
        category = ConfigOptionCategory.MANUAL_PROTOCOLS,
        testTag = "option_manual_trojan"
    ),
    CREATE_GROUP(
        title = "Create New Group",
        description = "Create a custom category tab to organize server nodes",
        icon = Icons.Default.CreateNewFolder,
        iconTint = Color(0xFF0D9488),
        category = ConfigOptionCategory.ORGANIZATION,
        testTag = "option_create_group"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigOptionsSheet(
    onDismiss: () -> Unit,
    onSelectOption: (ConfigActionOption) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Add Configuration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose an option to open its dedicated setup screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            // Options List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section: Import Methods
                item {
                    SectionHeader(title = "Import & Subscriptions")
                }
                items(ConfigActionOption.entries.filter { it.category == ConfigOptionCategory.IMPORT_METHODS }) { option ->
                    ConfigOptionItem(
                        option = option,
                        onClick = {
                            onDismiss()
                            onSelectOption(option)
                        }
                    )
                }

                // Section: Manual Protocols
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "Manual Protocol Setup")
                }
                items(ConfigActionOption.entries.filter { it.category == ConfigOptionCategory.MANUAL_PROTOCOLS }) { option ->
                    ConfigOptionItem(
                        option = option,
                        onClick = {
                            onDismiss()
                            onSelectOption(option)
                        }
                    )
                }

                // Section: Organization
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "Organization")
                }
                items(ConfigActionOption.entries.filter { it.category == ConfigOptionCategory.ORGANIZATION }) { option ->
                    ConfigOptionItem(
                        option = option,
                        onClick = {
                            onDismiss()
                            onSelectOption(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun ConfigOptionItem(
    option: ConfigActionOption,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(option.testTag),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(option.iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = option.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
