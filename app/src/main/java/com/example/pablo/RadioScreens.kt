package com.example.pablo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pablo.ui.theme.PabloTheme

/**
 * CONTROL screen — adjust the radio and enter data to send.
 *
 * `isConnected` is passed IN from the app shell (state hoisting): this screen
 * doesn't own that fact, it just reacts to it. Frequency/message are local to
 * this screen because nothing else needs them.
 */
@Composable
fun ControlScreen(
    isConnected: Boolean,
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
        Text("Control", style = MaterialTheme.typography.headlineMedium)

        if (!isConnected) {
            Text(
                "Not connected — open Settings and tap Connect first.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

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
                // Disabled until connected and there's something to send.
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

        // Shows the result of the last button press, if any.
        lastAction?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * MONITOR screen — a read-only view of the radio and network status.
 *
 * It reflects the shared connection state passed in from the app shell.
 * The signal/mode values are still placeholders until real hardware is wired up.
 */
@Composable
fun MonitorScreen(
    isConnected: Boolean,
    radioAddress: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Monitor", style = MaterialTheme.typography.headlineMedium)
        StatusRow(label = "Radio", value = if (isConnected) "Connected" else "Disconnected")
        StatusRow(label = "Network", value = if (isConnected) radioAddress else "—")
        StatusRow(label = "Signal", value = if (isConnected) "-72 dBm" else "0 dBm")
        StatusRow(label = "Mode", value = if (isConnected) "Listening" else "Idle")
    }
}

/** One "Label ............ value" row used by the Monitor screen. */
@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * SETTINGS screen — connection settings.
 *
 * The address and connection state are HOISTED (owned by the app shell and
 * passed in), so the Monitor screen sees the same values. This screen reports
 * user actions back up via the `onAddressChange` / `onToggleConnection` callbacks.
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
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = radioAddress,
            onValueChange = onAddressChange,
            label = { Text("Radio address") },
            // Don't allow editing the address while a connection is live.
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

        Button(onClick = onToggleConnection) {
            Text(if (isConnected) "Disconnect" else "Connect")
        }

        Text(
            text = if (isConnected) "Connected to $radioAddress" else "Not connected",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ControlScreenPreview() {
    PabloTheme { ControlScreen(isConnected = false) }
}

@Preview(showBackground = true)
@Composable
private fun MonitorScreenPreview() {
    PabloTheme { MonitorScreen(isConnected = true, radioAddress = "192.168.1.100") }
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
