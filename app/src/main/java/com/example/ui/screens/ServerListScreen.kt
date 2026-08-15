package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfigSortOption
import com.example.model.ProxyProtocol
import com.example.model.ServerConfig
import com.example.network.PingTester
import com.example.proxy.UniversalConfigParser
import com.example.proxy.XrayConfigJsonBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    servers: List<ServerConfig>,
    activeConfig: ServerConfig?,
    groups: List<String>,
    selectedGroup: String,
    sortOption: ConfigSortOption,
    isGroupPingTesting: Boolean,
    groupPingProgress: Float,
    onSelectGroup: (String) -> Unit,
    onSelectSortOption: (ConfigSortOption) -> Unit,
    onSelectServer: (ServerConfig) -> Unit,
    onEditServer: (ServerConfig) -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    onTestSinglePing: (ServerConfig) -> Unit,
    onTestGroupPing: (String) -> Unit,
    onResetTraffic: (ServerConfig) -> Unit,
    onResetAllTraffic: () -> Unit,
    onAddServerClick: () -> Unit,
    onImportUrlClick: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDeleteAllConfigs: () -> Unit = {}
) {
    var shareTargetConfig by remember { mutableStateOf<ServerConfig?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteGroupConfirmDialog by remember { mutableStateOf<String?>(null) }
    var showResetAllTrafficDialog by remember { mutableStateOf(false) }
    var showResetTrafficForConfig by remember { mutableStateOf<ServerConfig?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val groupScrollState = rememberScrollState()

    // Filter by group and sort by selected sort option (Default: PING)
    val filteredConfigs = remember(servers, selectedGroup, sortOption) {
        val groupFiltered = if (selectedGroup == "All") {
            servers
        } else {
            servers.filter { it.groupName.equals(selectedGroup, ignoreCase = true) }
        }

        when (sortOption) {
            ConfigSortOption.PING -> PingTester.sortByLowestPing(groupFiltered)
            ConfigSortOption.TRAFFIC_DESC -> groupFiltered.sortedByDescending { it.totalTrafficBytes }
            ConfigSortOption.TRAFFIC_ASC -> groupFiltered.sortedBy { it.totalTrafficBytes }
            ConfigSortOption.NAME -> groupFiltered.sortedBy { it.getDisplayTitle().lowercase() }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServerClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_server_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Configuration")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(groupScrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" tab
                val allCount = servers.size
                FilterChip(
                    selected = selectedGroup == "All",
                    onClick = { onSelectGroup("All") },
                    label = { Text("All ($allCount)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("group_tab_all")
                )

                // Individual Groups
                groups.forEach { groupName ->
                    val groupCount = servers.count { it.groupName.equals(groupName, ignoreCase = true) }
                    FilterChip(
                        selected = selectedGroup == groupName,
                        onClick = { onSelectGroup(groupName) },
                        label = { Text("$groupName ($groupCount)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("group_tab_${groupName.lowercase().replace(" ", "_")}")
                    )
                }

                // Add Group Action Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImportUrlClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Group",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Group / Sub",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Group Header Bar with Sort, Batch Ping, Delete All & Group management actions
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedGroup == "All") "All Configurations" else "Group: $selectedGroup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${filteredConfigs.size} nodes",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Sorted by: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = sortOption.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Sort Dropdown Selector Button
                            Box {
                                OutlinedButton(
                                    onClick = { sortMenuExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("sort_configs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort Configurations",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sortOption.shortLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    ConfigSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = option.displayName,
                                                        fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (sortOption == option) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                when (option) {
                                                    ConfigSortOption.PING -> Icon(Icons.Default.Speed, contentDescription = null)
                                                    ConfigSortOption.TRAFFIC_DESC -> Icon(Icons.Default.DataUsage, contentDescription = null)
                                                    ConfigSortOption.TRAFFIC_ASC -> Icon(Icons.Default.DataUsage, contentDescription = null)
                                                    ConfigSortOption.NAME -> Icon(Icons.Default.Sort, contentDescription = null)
                                                }
                                            },
                                            onClick = {
                                                onSelectSortOption(option)
                                                sortMenuExpanded = false
                                            },
                                            modifier = Modifier.testTag("sort_option_${option.name.lowercase()}")
                                        )
                                    }
                                }
                            }

                            // Test Group Ping Button
                            OutlinedButton(
                                onClick = { onTestGroupPing(selectedGroup) },
                                enabled = !isGroupPingTesting && filteredConfigs.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("group_ping_test_button")
                            ) {
                                if (isGroupPingTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isGroupPingTesting) "Testing..." else "Ping",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Header More Options / Delete All Menu
                            if (servers.isNotEmpty()) {
                                var moreMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { moreMenuExpanded = true },
                                        modifier = Modifier.testTag("configs_more_options_button")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                    }
                                    DropdownMenu(
                                        expanded = moreMenuExpanded,
                                        onDismissRequest = { moreMenuExpanded = false }
                                    ) {
                                        // Delete All in Current Selected Group
                                        if (selectedGroup != "All") {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text("Delete All in '$selectedGroup'", fontWeight = FontWeight.SemiBold)
                                                        Text("Remove ${filteredConfigs.size} configs in this group", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.DeleteSweep,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    moreMenuExpanded = false
                                                    showDeleteGroupConfirmDialog = selectedGroup
                                                },
                                                modifier = Modifier.testTag("menu_delete_all_in_group")
                                            )
                                            HorizontalDivider()
                                        }

                                        // Delete All Configurations in App
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Delete All Configurations", fontWeight = FontWeight.SemiBold)
                                                    Text("Clear all ${servers.size} configs across all groups", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                moreMenuExpanded = false
                                                showDeleteAllConfirmDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_delete_all_configs")
                                        )

                                        HorizontalDivider()

                                        DropdownMenuItem(
                                            text = { Text("Reset All Traffic Data") },
                                            leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                moreMenuExpanded = false
                                                showResetAllTrafficDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_reset_all_traffic")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isGroupPingTesting) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { groupPingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // Server Cards List with Swipe-to-Right to Delete
            if (filteredConfigs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedGroup == "All") "No Configurations" else "No configs in '$selectedGroup'",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add or import a configuration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredConfigs, key = { it.id }) { server ->
                        val isActive = activeConfig?.id == server.id
                        SwipeableServerConfigCard(
                            server = server,
                            isActive = isActive,
                            onSelect = { onSelectServer(server) },
                            onEdit = { onEditServer(server) },
                            onShare = { shareTargetConfig = server },
                            onDelete = { onDeleteServer(server) },
                            onTestPing = { onTestSinglePing(server) },
                            onResetTraffic = { showResetTrafficForConfig = server }
                        )
                    }
                }
            }
        }

        // Share Dialog with Copy URL & Copy JSON actions
        shareTargetConfig?.let { target ->
            ShareConfigDialog(
                server = target,
                onDismiss = { shareTargetConfig = null },
                onEdit = {
                    shareTargetConfig = null
                    onEditServer(target)
                },
                onTestPing = {
                    onTestSinglePing(target)
                }
            )
        }

        // Reset Traffic Confirmation Dialog for single config
        showResetTrafficForConfig?.let { configToReset ->
            AlertDialog(
                onDismissRequest = { showResetTrafficForConfig = null },
                title = { Text("Reset Traffic Usage?") },
                text = {
                    Text("Are you sure you want to reset traffic usage for '${configToReset.getDisplayTitle()}'? Current recorded total is ${configToReset.trafficDisplayText}.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onResetTraffic(configToReset)
                            showResetTrafficForConfig = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_reset_single_traffic_button")
                    ) {
                        Text("Reset Traffic")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetTrafficForConfig = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Reset All Traffic Confirmation Dialog
        if (showResetAllTrafficDialog) {
            AlertDialog(
                onDismissRequest = { showResetAllTrafficDialog = false },
                title = { Text("Reset All Traffic Usage?") },
                text = { Text("Are you sure you want to clear cumulative traffic stats for all server configurations? Total used bytes will be reset to 0 B.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetAllTrafficDialog = false
                            onResetAllTraffic()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_reset_all_traffic_button")
                    ) {
                        Text("Reset All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetAllTrafficDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Group Confirmation Dialog
        showDeleteGroupConfirmDialog?.let { targetGroupName ->
            val countInGroup = servers.count { it.groupName.equals(targetGroupName, ignoreCase = true) }
            AlertDialog(
                onDismissRequest = { showDeleteGroupConfirmDialog = null },
                title = { Text("Delete All in '$targetGroupName'?") },
                text = { Text("Are you sure you want to delete all $countInGroup configuration(s) in group '$targetGroupName'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteGroupConfirmDialog = null
                            onDeleteGroup(targetGroupName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_group_configs_button")
                    ) {
                        Text("Delete All in Group")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteGroupConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete All Configs Confirmation Dialog
        if (showDeleteAllConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirmDialog = false },
                title = { Text("Delete All Configurations?") },
                text = { Text("Are you sure you want to delete all proxy configurations? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAllConfirmDialog = false
                            onDeleteAllConfigs()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_all_button")
                    ) {
                        Text("Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Wraps ServerConfigCard with SwipeToDismissBox (swiping right reveals red delete background & triggers delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableServerConfigCard(
    server: ServerConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onTestPing: () -> Unit,
    onResetTraffic: () -> Unit
) {
    var isDeleted by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                isDeleted = true
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    AnimatedVisibility(
        visible = !isDeleted,
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = false, // Only swipe right (StartToEnd) to reveal delete
            enableDismissFromStartToEnd = true,
            backgroundContent = {
                // Background revealed when swiping to the right
                val progress = dismissState.progress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Configuration",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Delete Config",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            content = {
                ServerConfigCard(
                    server = server,
                    isActive = isActive,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onShare = onShare,
                    onDelete = onDelete,
                    onTestPing = onTestPing,
                    onResetTraffic = onResetTraffic
                )
            }
        )
    }
}

@Composable
fun ServerConfigCard(
    server: ServerConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onTestPing: () -> Unit,
    onResetTraffic: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }
    var shareExportMenuExpanded by remember { mutableStateOf(false) }

    // Ping status color
    val pingColor = when {
        server.ping > 0 && server.ping < 180 -> Color(0xFF10B981) // Fast Emerald
        server.ping >= 180 && server.ping < 350 -> Color(0xFFF59E0B) // Amber
        server.ping >= 350 -> Color(0xFFEF4444) // Slow
        server.ping == -1L -> Color(0xFFEF4444) // Timeout
        else -> MaterialTheme.colorScheme.outline // Untested
    }

    // Protocol badge color
    val protocolBadgeColor = when (server.proxyProtocol) {
        ProxyProtocol.VLESS -> Color(0xFF059669) // Emerald
        ProxyProtocol.VMESS -> Color(0xFF2563EB) // Blue
        ProxyProtocol.SHADOWSOCKS -> Color(0xFFD97706) // Amber / Orange
        ProxyProtocol.TROJAN -> Color(0xFF7C3AED) // Purple / Violet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("server_card_${server.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status / Protocol Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else protocolBadgeColor.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = server.protocolBadgeText.take(2),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = protocolBadgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Server Information
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.getDisplayTitle(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Protocol Badge
                    Surface(
                        color = protocolBadgeColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = server.protocolBadgeText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color.White
                        )
                    }
                    if (isActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val detailText = when (server.proxyProtocol) {
                        ProxyProtocol.VLESS -> "${server.transport.uppercase()} • ${server.security.uppercase()}"
                        ProxyProtocol.VMESS -> "${server.transport.uppercase()} • ${server.security.uppercase()} • ${server.encryption}"
                        ProxyProtocol.SHADOWSOCKS -> server.method.uppercase()
                        ProxyProtocol.TROJAN -> "${server.transport.uppercase()} • TLS"
                    }
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (server.groupName.isNotBlank() && server.groupName != "Default") {
                        Text(
                            text = " • 📁 ${server.groupName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badges Column: Traffic Usage & Ping latency & Direct Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Persistent Traffic Consumption Badge in front of config
                Surface(
                    color = if (server.totalTrafficBytes > 0) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("traffic_badge_${server.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataUsage,
                            contentDescription = "Traffic consumed",
                            tint = if (server.totalTrafficBytes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = server.trafficDisplayText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = if (server.totalTrafficBytes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ping Latency Badge
                Surface(
                    color = pingColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onTestPing() }
                        .testTag("ping_badge_${server.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = server.pingDisplayText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = pingColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: Edit, Direct Share/Export Menu (URL & JSON), More Options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("edit_server_${server.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Config",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Direct Sharing / Export Menu Button (URL & JSON options)
                    Box {
                        IconButton(
                            onClick = { shareExportMenuExpanded = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("share_server_${server.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export & Share Config (URL / JSON)",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = shareExportMenuExpanded,
                            onDismissRequest = { shareExportMenuExpanded = false }
                        ) {
                            // Export as URL
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "Copy ${server.proxyProtocol.displayName} URL",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Export standard config link",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    val shareUrl = UniversalConfigParser.toShareUrl(server)
                                    if (shareUrl.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(shareUrl))
                                    }
                                    shareExportMenuExpanded = false
                                },
                                modifier = Modifier.testTag("export_url_server_${server.id}")
                            )

                            // Export as JSON
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "Copy Xray JSON",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Export outbound JSON config",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                onClick = {
                                    val json = XrayConfigJsonBuilder.buildXrayConfigJson(server)
                                    clipboardManager.setText(AnnotatedString(json))
                                    shareExportMenuExpanded = false
                                },
                                modifier = Modifier.testTag("export_json_server_${server.id}")
                            )

                            HorizontalDivider()

                            // Share via Android Apps
                            DropdownMenuItem(
                                text = {
                                    Text("Share via Apps...")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    val shareUrl = UniversalConfigParser.toShareUrl(server)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        putExtra(Intent.EXTRA_TITLE, server.name)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share ${server.proxyProtocol.displayName} Config")
                                    context.startActivity(shareIntent)
                                    shareExportMenuExpanded = false
                                },
                                modifier = Modifier.testTag("share_apps_server_${server.id}")
                            )

                            // Full Share & Export Dialog
                            DropdownMenuItem(
                                text = {
                                    Text("More Sharing Options...")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                },
                                onClick = {
                                    shareExportMenuExpanded = false
                                    onShare()
                                }
                            )
                        }
                    }

                    // More Options (Reset Traffic, Test Ping, Delete)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("more_options_server_${server.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reset Traffic (${server.trafficDisplayText})") },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    onResetTraffic()
                                },
                                modifier = Modifier.testTag("menu_reset_traffic_${server.id}")
                            )
                            DropdownMenuItem(
                                text = { Text("Test Ping") },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                onClick = {
                                    onTestPing()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy ${server.proxyProtocol.displayName} URL") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    val shareUrl = UniversalConfigParser.toShareUrl(server)
                                    if (shareUrl.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(shareUrl))
                                    }
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Xray JSON") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    val json = XrayConfigJsonBuilder.buildXrayConfigJson(server)
                                    clipboardManager.setText(AnnotatedString(json))
                                    menuExpanded = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Configuration") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDelete()
                                    menuExpanded = false
                                },
                                modifier = Modifier.testTag("menu_delete_server_${server.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
