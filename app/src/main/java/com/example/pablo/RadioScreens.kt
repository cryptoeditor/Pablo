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
 * Right now every value lives only in this screen (local "state") and the
 * buttons don't talk to any hardware yet. Later this becomes a ViewModel that
 * sends commands to the real SDR.
 */
@Composable
fun ControlScreen(modifier: Modifier = Modifier) {
    var frequency by remember { mutableStateOf("145.500") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Control", style = MaterialTheme.typography.headlineMedium)

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
            Button(onClick = { /* TODO: send the message to the radio */ }) {
                Text("Send")
            }
            OutlinedButton(onClick = { /* TODO: start voice input */ }) {
                Text("Voice")
            }
        }
    }
}

/**
 * MONITOR screen — a read-only view of the radio and network status.
 *
 * These values are hard-coded placeholders for now. Later they update live
 * from the radio while it's operating.
 */
@Composable
fun MonitorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Monitor", style = MaterialTheme.typography.headlineMedium)
        StatusRow(label = "Radio", value = "Disconnected")
        StatusRow(label = "Network", value = "—")
        StatusRow(label = "Signal", value = "0 dBm")
        StatusRow(label = "Mode", value = "Idle")
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
 * This is the ONLY screen whose values we will later SAVE (so they survive
 * closing the app). The radio itself stays passive and stores no data.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var address by remember { mutableStateOf("192.168.1.100") }
    var autoConnect by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Radio address") },
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

        Button(onClick = { /* TODO: connect to the radio */ }) {
            Text("Connect")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ControlScreenPreview() {
    PabloTheme { ControlScreen() }
}

@Preview(showBackground = true)
@Composable
private fun MonitorScreenPreview() {
    PabloTheme { MonitorScreen() }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PabloTheme { SettingsScreen() }
}
