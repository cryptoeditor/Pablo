package com.example.pablo

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pablo.ui.theme.PabloTheme
import kotlin.math.roundToInt

// Status colors reused across the app.
private val ConnectedGreen = Color(0xFF2E9E5B)
private val DisconnectedRed = Color(0xFFC2453F)

/**
 * A small rounded "Online / Offline" badge with a colored dot.
 * Shown in the top bar so the connection state is always visible.
 */
@Composable
fun ConnectionPill(isConnected: Boolean) {
    val dotColor = if (isConnected) ConnectedGreen else DisconnectedRed
    val label = if (isConnected) "Online" else "Offline"

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(dotColor, CircleShape)
            )
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * CONTROL screen — adjust the radio and enter data to send.
 */
@Composable
fun ControlScreen(
    isConnected: Boolean,
    scanIntervalSeconds: Int,
    onScanIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var frequency by remember { mutableStateOf("145.500") }
    var message by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("Control")

        if (!isConnected) {
            Text(
                "Not connected — open Settings and tap Connect first.",
                color = DisconnectedRed,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency (MHz)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message to send") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = isConnected && message.isNotBlank(),
                        onClick = {
                            lastAction = "Sent \"$message\" on $frequency MHz"
                            message = ""
                            // TODO: actually transmit this to the radio.
                        }
                    ) {
                        Text("Send")
                    }
                    OutlinedButton(
                        onClick = { lastAction = "Voice input isn't wired up yet." }
                        // TODO: start Android voice input here.
                    ) {
                        Text("Voice")
                    }
                }

                lastAction?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Controls how often the Monitor screen samples nearby signals.
        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Scan interval: $scanIntervalSeconds s",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = scanIntervalSeconds.toFloat(),
                    onValueChange = { onScanIntervalChange(it.roundToInt()) },
                    valueRange = 5f..60f
                )
                Text(
                    "How often the Monitor map samples nearby radios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * MONITOR screen — a read-only dashboard of the radio and network status.
 */
@Composable
fun MonitorScreen(
    isConnected: Boolean,
    radioAddress: String,
    contacts: List<RadioContact>,
    scanIntervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("Monitor")

        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatusRow(
                    label = "Radio",
                    value = if (isConnected) "Connected" else "Disconnected",
                    valueColor = if (isConnected) ConnectedGreen else DisconnectedRed
                )
                StatusRow(label = "Network", value = if (isConnected) radioAddress else "—")
                StatusRow(label = "Mode", value = if (isConnected) "Listening" else "Idle")

                // Signal strength shown as a bar instead of just a number.
                Text("Signal", style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(
                    progress = { if (isConnected) 0.72f else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isConnected) "-72 dBm" else "No signal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The RF map: nearby radios plotted around the user.
        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nearby radios", style = MaterialTheme.typography.titleMedium)
                if (isConnected) {
                    SignalRadar(contacts = contacts, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Sampling every $scanIntervalSeconds s · simulated data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Connect to scan for nearby radios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * SETTINGS screen — connection settings (the only values we'll later save).
 */
@Composable
fun SettingsScreen(
    radioAddress: String,
    onAddressChange: (String) -> Unit,
    isConnected: Boolean,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoConnect by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("Settings")

        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = radioAddress,
                    onValueChange = onAddressChange,
                    label = { Text("Radio address") },
                    enabled = !isConnected,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-connect on startup")
                    Switch(checked = autoConnect, onCheckedChange = { autoConnect = it })
                }

                Button(
                    onClick = onToggleConnection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }

                Text(
                    text = if (isConnected) "Connected to $radioAddress" else "Not connected",
                    color = if (isConnected) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** Shared big heading at the top of each screen. */
@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
}

/** One "Label ............ value" row, with an optional colored value. */
@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ControlScreenPreview() {
    PabloTheme {
        ControlScreen(
            isConnected = false,
            scanIntervalSeconds = 15,
            onScanIntervalChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonitorScreenPreview() {
    PabloTheme {
        MonitorScreen(
            isConnected = true,
            radioAddress = "192.168.1.100",
            contacts = sampleNearbyRadios(),
            scanIntervalSeconds = 15
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PabloTheme {
        SettingsScreen(
            radioAddress = "192.168.1.100",
            onAddressChange = {},
            isConnected = false,
            onToggleConnection = {}
        )
    }
}
