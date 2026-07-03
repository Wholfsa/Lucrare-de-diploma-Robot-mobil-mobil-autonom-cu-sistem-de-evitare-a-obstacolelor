package com.example.esp32bluetoothcontrol
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            //------------------ TEMA MOV ------------------
            val purpleDark = darkColorScheme(
                background = Color(0xFF12001F),
                surface = Color(0xFF1A002B),
                primary = Color(0xFF9C27B0),
                secondary = Color(0xFFE040FB),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = purpleDark) {

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {

                    //------------------ HOME SCREEN ------------------
                    composable("home") {
                        HomeScreen(navController)
                    }

                    //------------------ CONTROL SCREEN (Manual + Auto) ------------------
                    composable("control") {

                        val sensorViewModel: SensorViewModel = viewModel()
                        val bluetoothController = remember { BluetoothController() }

                        var connectionStatus by remember { mutableStateOf("Disconnected") }
                        var selectedTab by remember { mutableStateOf(0) }

                        val scope = rememberCoroutineScope()

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1E88E5),   //Albastru modern
                                            Color(0xFF6A1B9A)    //Mov elegant
                                        )
                                    )
                                ),
                            bottomBar = {
                                NavigationBar(
                                    containerColor = Color(0xFF1A002B)
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        label = { Text("Manual") },
                                        icon = {},
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            indicatorColor = Color(0xFF9C27B0)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        label = { Text("Auto") },
                                        icon = {},
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            indicatorColor = Color(0xFF9C27B0)
                                        )
                                    )
                                }
                            }
                        ) { padding ->

                            val screenModifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1E88E5),   //Albastru modern
                                            Color(0xFF6A1B9A)    //Mov elegant
                                        )
                                    )
                                )

                            when (selectedTab) {

                                //------------------ MANUAL MODE ------------------
                                0 -> ManualModeScreen(
                                    connectionStatus = connectionStatus,
                                    onConnect = {
                                        scope.launch {
                                            val ok = bluetoothController.connect()
                                            connectionStatus = if (ok) "Connected" else "Failed"
                                        }
                                    },
                                    bluetoothController = bluetoothController,
                                    sensorViewModel = sensorViewModel,
                                    modifier = screenModifier
                                )

                                //------------------ AUTO MODE ------------------
                                1 -> AutoModeScreen(
                                    connectionStatus = connectionStatus,
                                    onConnect = {
                                        scope.launch {
                                            val ok = bluetoothController.connect()
                                            connectionStatus = if (ok) "Connected" else "Failed"
                                        }
                                    },
                                    bluetoothController = bluetoothController,
                                    sensorViewModel = sensorViewModel,
                                    modifier = screenModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}