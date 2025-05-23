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
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.data.network.ollama.OllamaService
import com.example.tabletopcompanion.features.roommanagement.CreateRoomScreen
import com.example.tabletopcompanion.features.roommanagement.JoinRoomScreen
import com.example.tabletopcompanion.features.roommanagement.RoomScreen
import com.example.tabletopcompanion.features.roommanagement.RoomViewModelFactory
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
            // Instantiate TemplateRepository with application context
            CreateRoomScreen(navController, RoomViewModelFactory(application, TemplateRepository(application), OllamaService()))
        }
        composable("joinRoom") {
            // JoinRoomScreen also needs RoomViewModelFactory which needs TemplateRepository
            JoinRoomScreen(navController, RoomViewModelFactory(application, TemplateRepository(application), OllamaService()))
        }
        composable("templateManagement") { // Added route for TemplateManagementScreen
            TemplateManagementScreen()
        }
        composable(
            route = "roomScreen/{roomId}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId")
            // Pass the factory to RoomScreen
            RoomScreen(
                navController = navController,
                roomId = roomId ?: "ERROR_NO_ROOM_ID",
                // Instantiate TemplateRepository with application context
                roomViewModelFactory = RoomViewModelFactory(application, TemplateRepository(application), OllamaService())
            )
        }
    }
}
