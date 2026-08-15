package com.example

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logger.AppLogger
import com.example.model.ServerConfig
import com.example.storage.AppSettingsManager
import com.example.ui.MainViewModel
import com.example.ui.screens.BypassAppsScreen
import com.example.ui.screens.ClipboardImportDialog
import com.example.ui.screens.ConfigActionOption
import com.example.ui.screens.ConfigEditScreen
import com.example.ui.screens.ConfigOptionsSheet
import com.example.ui.screens.CreateGroupDialog
import com.example.ui.screens.JsonConfigImportDialog
import com.example.ui.screens.LinkImportDialog
import com.example.ui.screens.LogScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.ServerListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionImportDialog
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class NavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Shield),
    CONFIGS("Configs", Icons.Default.Dns),
    LOGS("Logs", Icons.Default.Terminal),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            AppLogger.i("MainActivity", "User granted VPN permission. Triggering connection...")
            viewModel.startVpnDirect(this)
        } else {
            AppLogger.w("MainActivity", "User rejected VPN permission request!")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppLogger.i("MainActivity", "Notification permission granted: $isGranted")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppSettingsManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
                var showConfigOptionsSheet by remember { mutableStateOf(false) }
                var activeDedicatedOption by remember { mutableStateOf<ConfigActionOption?>(null) }
                var editingConfig by remember { mutableStateOf<ServerConfig?>(null) }
                var editingProtocol by remember { mutableStateOf("vless") }
                var isEditingScreenActive by remember { mutableStateOf(false) }
                var isBypassAppsScreenActive by remember { mutableStateOf(false) }

                val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
                val vpnStats by viewModel.vpnStats.collectAsStateWithLifecycle()
                val activeConfig by viewModel.activeConfig.collectAsStateWithLifecycle()
                val allConfigs by viewModel.allConfigs.collectAsStateWithLifecycle()
                val groups by viewModel.groups.collectAsStateWithLifecycle()
                val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
                val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
                val isGroupPingTesting by viewModel.isGroupPingTesting.collectAsStateWithLifecycle()
                val groupPingProgress by viewModel.groupPingProgress.collectAsStateWithLifecycle()
                val logs by viewModel.logs.collectAsStateWithLifecycle()
                val pingResult by viewModel.pingResult.collectAsStateWithLifecycle()
                val isTestingPing by viewModel.isTestingPing.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val hideTopAndBottomBars = isEditingScreenActive || isBypassAppsScreenActive

                Scaffold(
                    topBar = {
                        if (!hideTopAndBottomBars) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = currentTab.title,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors()
                            )
                        }
                    },
                    bottomBar = {
                        if (!hideTopAndBottomBars) {
                            NavigationBar {
                                NavTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) },
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (hideTopAndBottomBars) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
                    ) {
                        if (isEditingScreenActive) {
                            ConfigEditScreen(
                                initialConfig = editingConfig,
                                initialProtocol = editingProtocol,
                                onSaveConfig = { savedConfig ->
                                    viewModel.saveConfig(savedConfig)
                                    isEditingScreenActive = false
                                    editingConfig = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Configuration saved successfully (${savedConfig.protocol.uppercase()}).")
                                    }
                                },
                                onBack = {
                                    isEditingScreenActive = false
                                    editingConfig = null
                                }
                            )
                        } else if (isBypassAppsScreenActive) {
                            BypassAppsScreen(
                                onBack = { isBypassAppsScreenActive = false }
                            )
                        } else {
                            when (currentTab) {
                                NavTab.DASHBOARD -> {
                                    MainDashboardScreen(
                                        connectionState = connectionState,
                                        vpnStats = vpnStats,
                                        activeConfig = activeConfig,
                                        pingResult = pingResult,
                                        isTestingPing = isTestingPing,
                                        onConnectClick = {
                                            viewModel.prepareVpnAndConnect(this@MainActivity) {
                                                val intent = VpnService.prepare(this@MainActivity)
                                                if (intent != null) {
                                                    vpnPermissionLauncher.launch(intent)
                                                }
                                            }
                                        },
                                        onDisconnectClick = {
                                            viewModel.stopVpn(this@MainActivity)
                                        },
                                        onImportClick = { showConfigOptionsSheet = true },
                                        onPingClick = { viewModel.runPingTest() },
                                        onSelectServerClick = { currentTab = NavTab.CONFIGS }
                                    )
                                }
                                NavTab.CONFIGS -> {
                                    ServerListScreen(
                                        servers = allConfigs,
                                        activeConfig = activeConfig,
                                        groups = groups,
                                        selectedGroup = selectedGroup,
                                        sortOption = sortOption,
                                        isGroupPingTesting = isGroupPingTesting,
                                        groupPingProgress = groupPingProgress,
                                        onSelectGroup = { group -> viewModel.setSelectedGroup(group) },
                                        onSelectSortOption = { option -> viewModel.setSortOption(option) },
                                        onSelectServer = { config ->
                                            viewModel.setActiveConfig(config)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Active server: ${config.getDisplayTitle()} (${config.protocol.uppercase()})")
                                            }
                                        },
                                        onEditServer = { config ->
                                            editingConfig = config
                                            editingProtocol = config.protocol
                                            isEditingScreenActive = true
                                        },
                                        onDeleteServer = { config ->
                                            viewModel.deleteConfig(config)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Deleted server configuration.")
                                            }
                                        },
                                        onTestSinglePing = { config ->
                                            viewModel.testSingleConfigPing(config)
                                        },
                                        onTestGroupPing = { group ->
                                            viewModel.testGroupPing(group)
                                        },
                                        onResetTraffic = { config ->
                                            viewModel.resetTraffic(config)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Reset traffic for ${config.getDisplayTitle()}")
                                            }
                                        },
                                        onResetAllTraffic = {
                                            viewModel.resetAllTraffic()
                                            scope.launch {
                                                snackbarHostState.showSnackbar("All server traffic usage reset to 0 B.")
                                            }
                                        },
                                        onAddServerClick = {
                                            showConfigOptionsSheet = true
                                        },
                                        onImportUrlClick = {
                                            showConfigOptionsSheet = true
                                        },
                                        onDeleteGroup = { group ->
                                            viewModel.deleteGroup(group)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Deleted group '$group'.")
                                            }
                                        },
                                        onDeleteAllConfigs = {
                                            viewModel.deleteAllConfigs()
                                            scope.launch {
                                                snackbarHostState.showSnackbar("All configurations deleted.")
                                            }
                                        }
                                    )
                                }
                                NavTab.LOGS -> {
                                    LogScreen(
                                        logs = logs,
                                        onClearLogs = { viewModel.clearLogs() }
                                    )
                                }
                                NavTab.SETTINGS -> {
                                    SettingsScreen(
                                        onNavigateToBypassApps = { isBypassAppsScreenActive = true }
                                    )
                                }
                            }
                        }

                        // Options sheet modal showing list of different titles
                        if (showConfigOptionsSheet) {
                            ConfigOptionsSheet(
                                onDismiss = { showConfigOptionsSheet = false },
                                onSelectOption = { option ->
                                    showConfigOptionsSheet = false
                                    when (option) {
                                        ConfigActionOption.MANUAL_VLESS -> {
                                            editingConfig = null
                                            editingProtocol = "vless"
                                            isEditingScreenActive = true
                                        }
                                        ConfigActionOption.MANUAL_VMESS -> {
                                            editingConfig = null
                                            editingProtocol = "vmess"
                                            isEditingScreenActive = true
                                        }
                                        ConfigActionOption.MANUAL_SHADOWSOCKS -> {
                                            editingConfig = null
                                            editingProtocol = "shadowsocks"
                                            isEditingScreenActive = true
                                        }
                                        ConfigActionOption.MANUAL_TROJAN -> {
                                            editingConfig = null
                                            editingProtocol = "trojan"
                                            isEditingScreenActive = true
                                        }
                                        else -> {
                                            activeDedicatedOption = option
                                        }
                                    }
                                }
                            )
                        }

                        // Dedicated dialogs for each respective title
                        when (activeDedicatedOption) {
                            ConfigActionOption.IMPORT_CLIPBOARD -> {
                                ClipboardImportDialog(
                                    availableGroups = groups,
                                    currentGroup = selectedGroup,
                                    onDismiss = { activeDedicatedOption = null },
                                    onSaveConfig = { config ->
                                        viewModel.saveConfig(config)
                                        activeDedicatedOption = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Imported ${config.protocol.uppercase()}: ${config.getDisplayTitle()}")
                                        }
                                    }
                                )
                            }
                            ConfigActionOption.IMPORT_LINK -> {
                                LinkImportDialog(
                                    availableGroups = groups,
                                    currentGroup = selectedGroup,
                                    onDismiss = { activeDedicatedOption = null },
                                    onSaveConfig = { config ->
                                        viewModel.saveConfig(config)
                                        activeDedicatedOption = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Imported ${config.protocol.uppercase()}: ${config.getDisplayTitle()}")
                                        }
                                    }
                                )
                            }
                            ConfigActionOption.IMPORT_SUBSCRIPTION -> {
                                SubscriptionImportDialog(
                                    availableGroups = groups,
                                    currentGroup = selectedGroup,
                                    onDismiss = { activeDedicatedOption = null },
                                    onImportSub = { subUrl, groupName ->
                                        viewModel.importSubscription(subUrl, groupName) { success, msg, count ->
                                            activeDedicatedOption = null
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (success) "Subscription imported: $count nodes in '$groupName'" else "Sub error: $msg"
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            ConfigActionOption.IMPORT_RAW_JSON -> {
                                JsonConfigImportDialog(
                                    availableGroups = groups,
                                    currentGroup = selectedGroup,
                                    onDismiss = { activeDedicatedOption = null },
                                    onSaveConfig = { config ->
                                        viewModel.saveConfig(config)
                                        activeDedicatedOption = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Imported JSON: ${config.getDisplayTitle()}")
                                        }
                                    }
                                )
                            }
                            ConfigActionOption.CREATE_GROUP -> {
                                CreateGroupDialog(
                                    onDismiss = { activeDedicatedOption = null },
                                    onCreateGroup = { groupName ->
                                        viewModel.createGroup(groupName)
                                        activeDedicatedOption = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Created group '$groupName'")
                                        }
                                    }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
