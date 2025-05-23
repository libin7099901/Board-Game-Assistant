package com.example.tabletopcompanion.features.roommanagement

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tabletopcompanion.data.UserProfileRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(navController: NavController) {
    val context = LocalContext.current
    val userProfileRepository = remember { UserProfileRepository(context) }
    var roomName by remember { mutableStateOf("") }
    val currentUsername = userProfileRepository.getUsername() ?: "Player"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create a New Room",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            label = { Text("Enter Room Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                if (roomName.isNotBlank()) {
                    val encodedRoomName = URLEncoder.encode(roomName, StandardCharsets.UTF_8.toString())
                    val encodedPlayerName = URLEncoder.encode("Host: $currentUsername", StandardCharsets.UTF_8.toString())
                    // Navigate to RoomScreen, passing room name and host
                    // For now, players list will just contain the host
                    navController.navigate("roomScreen/$encodedRoomName/$encodedPlayerName")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = roomName.isNotBlank()
        ) {
            Text("Create Room")
        }
    }
}
