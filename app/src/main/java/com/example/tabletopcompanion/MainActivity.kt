package com.example.tabletopcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tabletopcompanion.features.roommanagement.CreateRoomScreen
import com.example.tabletopcompanion.features.roommanagement.JoinRoomScreen
import com.example.tabletopcompanion.features.roommanagement.RoomScreen
import com.example.tabletopcompanion.features.templatemanagement.TemplateManagementScreen
import com.example.tabletopcompanion.features.userprofile.UserProfileScreen
import com.example.tabletopcompanion.ui.MainScreen
import com.example.tabletopcompanion.ui.theme.TabletopCompanionTheme
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TabletopCompanionTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") {
            MainScreen(navController)
        }
        composable("userProfile") {
            UserProfileScreen()
        }
        composable("createRoom") {
            CreateRoomScreen(navController)
        }
        composable("joinRoom") {
            JoinRoomScreen(navController)
        }
        composable("templateManagement") { // Added route for TemplateManagementScreen
            TemplateManagementScreen()
        }
        composable(
            route = "roomScreen/{roomName}/{players}",
            arguments = listOf(
                navArgument("roomName") { type = NavType.StringType },
                navArgument("players") { type = NavType.StringType } // Comma-separated list
            )
        ) { backStackEntry ->
            val roomName = backStackEntry.arguments?.getString("roomName")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            }
            val playersString = backStackEntry.arguments?.getString("players")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            }
            val playersList = playersString?.split(",") ?: emptyList()
            RoomScreen(roomName = roomName, players = playersList)
        }
    }
}
