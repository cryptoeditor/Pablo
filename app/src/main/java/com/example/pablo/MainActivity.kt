package com.example.pablo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
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

@PreviewScreenSizes
@Composable
fun PabloApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CONTROL) }

    // Shared state, "hoisted" here so every screen sees the same values.
    var radioAddress by rememberSaveable { mutableStateOf("192.168.1.100") }
    var isConnected by rememberSaveable { mutableStateOf(false) }

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
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.CONTROL -> ControlScreen(
                    isConnected = isConnected,
                    modifier = screenModifier
                )

                AppDestinations.MONITOR -> MonitorScreen(
                    isConnected = isConnected,
                    radioAddress = radioAddress,
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
