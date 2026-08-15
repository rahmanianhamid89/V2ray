package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storage.AppSettingsManager

@Composable
fun SettingsScreen(
    onNavigateToBypassApps: () -> Unit
) {
    val bypassedApps by AppSettingsManager.bypassedAppsFlow.collectAsStateWithLifecycle()
    val currentPingUrl by AppSettingsManager.pingUrlFlow.collectAsStateWithLifecycle()
    val currentPrimaryDns by AppSettingsManager.primaryDnsFlow.collectAsStateWithLifecycle()
    val currentSecondaryDns by AppSettingsManager.secondaryDnsFlow.collectAsStateWithLifecycle()

    var pingUrlInput by remember(currentPingUrl) { mutableStateOf(currentPingUrl) }
    var pingSaveMsg by remember { mutableStateOf<String?>(null) }

    var primaryDnsInput by remember(currentPrimaryDns) { mutableStateOf(currentPrimaryDns) }
    var secondaryDnsInput by remember(currentSecondaryDns) { mutableStateOf(currentSecondaryDns) }
    var dnsSaveMsg by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Application Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Bypass Apps / Split Tunneling Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBypassApps() }
                    .testTag("bypass_apps_setting_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bypass Apps (Split Tunneling)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (bypassedApps.isEmpty()) "No apps bypassed (all routed through VPN)" else "${bypassedApps.size} apps bypassed from VPN tunnel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Configure",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Ping Test URL Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ping & Latency Test URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The endpoint used for HTTP connectivity checks and latency testing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pingUrlInput,
                        onValueChange = {
                            pingUrlInput = it
                            pingSaveMsg = null
                        },
                        label = { Text("Ping Test URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ping_url_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips for ping URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = pingUrlInput == "https://www.gstatic.com/generate_204",
                            onClick = {
                                pingUrlInput = "https://www.gstatic.com/generate_204"
                                pingSaveMsg = null
                            },
                            label = { Text("Google 204") }
                        )
                        FilterChip(
                            selected = pingUrlInput == "https://cp.cloudflare.com/generate_204",
                            onClick = {
                                pingUrlInput = "https://cp.cloudflare.com/generate_204"
                                pingSaveMsg = null
                            },
                            label = { Text("Cloudflare 204") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            AppSettingsManager.setPingUrl(pingUrlInput)
                            pingSaveMsg = "Ping Test URL saved."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_ping_url_button")
                    ) {
                        Text("Save URL")
                    }

                    if (pingSaveMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = pingSaveMsg!!,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Custom DNS Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom DNS Servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Specify DNS resolvers used for resolving domain names inside the VPN tunnel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // DNS Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = primaryDnsInput == "1.1.1.1" && secondaryDnsInput == "1.0.0.1",
                            onClick = {
                                primaryDnsInput = "1.1.1.1"
                                secondaryDnsInput = "1.0.0.1"
                                dnsSaveMsg = null
                            },
                            label = { Text("Cloudflare") }
                        )
                        FilterChip(
                            selected = primaryDnsInput == "8.8.8.8" && secondaryDnsInput == "8.8.4.4",
                            onClick = {
                                primaryDnsInput = "8.8.8.8"
                                secondaryDnsInput = "8.8.4.4"
                                dnsSaveMsg = null
                            },
                            label = { Text("Google") }
                        )
                        FilterChip(
                            selected = primaryDnsInput == "9.9.9.9" && secondaryDnsInput == "149.112.112.112",
                            onClick = {
                                primaryDnsInput = "9.9.9.9"
                                secondaryDnsInput = "149.112.112.112"
                                dnsSaveMsg = null
                            },
                            label = { Text("Quad9") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = primaryDnsInput,
                        onValueChange = {
                            primaryDnsInput = it
                            dnsSaveMsg = null
                        },
                        label = { Text("Primary DNS") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = secondaryDnsInput,
                        onValueChange = {
                            secondaryDnsInput = it
                            dnsSaveMsg = null
                        },
                        label = { Text("Secondary DNS") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            AppSettingsManager.setDns(primaryDnsInput, secondaryDnsInput)
                            dnsSaveMsg = "DNS servers updated successfully."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_dns_button")
                    ) {
                        Text("Save DNS")
                    }

                    if (dnsSaveMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = dnsSaveMsg!!,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
