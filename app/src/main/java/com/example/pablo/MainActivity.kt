package com.example.pablo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.pablo.ui.theme.PabloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PabloTheme {
                PabloApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun PabloApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CONTROL) }

    // Shared state, "hoisted" here so every screen sees the same values.
    var radioAddress by rememberSaveable { mutableStateOf("192.168.1.100") }
    var isConnected by rememberSaveable { mutableStateOf(false) }
    var scanIntervalSeconds by rememberSaveable { mutableStateOf(15) }
    var contacts by remember { mutableStateOf(emptyList<RadioContact>()) }

    // Background sampling loop: while connected, take a signal "snapshot" every
    // few seconds. Restarts automatically when connection or interval changes.
    LaunchedEffect(isConnected, scanIntervalSeconds) {
        if (isConnected) {
            while (true) {
                contacts = sampleNearbyRadios()
                delay(scanIntervalSeconds * 1000L)
            }
        } else {
            contacts = emptyList()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Pablo SDR") },
                    actions = {
                        ConnectionPill(isConnected = isConnected)
                        Spacer(Modifier.width(12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.CONTROL -> ControlScreen(
                    isConnected = isConnected,
                    scanIntervalSeconds = scanIntervalSeconds,
                    onScanIntervalChange = { scanIntervalSeconds = it },
                    modifier = screenModifier
                )

                AppDestinations.MONITOR -> MonitorScreen(
                    isConnected = isConnected,
                    radioAddress = radioAddress,
                    contacts = contacts,
                    scanIntervalSeconds = scanIntervalSeconds,
                    modifier = screenModifier
                )

                AppDestinations.SETTINGS -> SettingsScreen(
                    radioAddress = radioAddress,
                    onAddressChange = { radioAddress = it },
                    isConnected = isConnected,
                    onToggleConnection = { isConnected = !isConnected },
                    modifier = screenModifier
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    CONTROL("Control", R.drawable.ic_home),
    MONITOR("Monitor", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_account_box),
}
