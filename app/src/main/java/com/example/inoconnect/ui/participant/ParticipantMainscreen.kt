package com.example.inoconnect.ui.participant

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inoconnect.data.FirebaseRepository

@Composable
fun ParticipantMainScreen(
    rootNavController: NavController, // Used for logging out & global navigation (Create Project)
    onEventClick: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val repository = remember { FirebaseRepository() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf("Home", "History", "Notify", "Profile")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.DateRange,
                    Icons.Default.Notifications,
                    Icons.Default.Person
                )

                // Simple index tracking for the selected tab
                var selectedItem by remember { mutableIntStateOf(0) }

                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            bottomNavController.navigate(item.lowercase()) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Home Tab
            composable("home") {
                ParticipantHome(
                    onEventClick = onEventClick,
                    onCreateProjectClick = {
                        // Navigate using the ROOT controller (because Create Project is a full screen)
                        rootNavController.navigate("create_project")
                    }
                )
            }

            // 2. History Tab (Placeholder)
            composable("history") {
                Text("History Events Page (Coming Soon)", modifier = Modifier.padding(16.dp))
            }

            // 3. Notify Tab (Placeholder)
            composable("notify") {
                Text("Notifications Page (Coming Soon)", modifier = Modifier.padding(16.dp))
            }

            // 4. Profile Tab (Logout Button)
            composable("profile") {
                Button(
                    onClick = {
                        repository.logout()
                        // Use root controller to go back to Login
                        rootNavController.navigate("login") {
                            popUpTo(0) // Clear back stack
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Logout")
                }
            }
        }
    }
}