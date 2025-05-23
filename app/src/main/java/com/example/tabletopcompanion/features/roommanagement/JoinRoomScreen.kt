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
fun JoinRoomScreen(navController: NavController) {
    val context = LocalContext.current
    val userProfileRepository = remember { UserProfileRepository(context) }
    var roomNameToJoin by remember { mutableStateOf("") }
    val currentUsername = userProfileRepository.getUsername() ?: "Player"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Join an Existing Room",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = roomNameToJoin,
            onValueChange = { roomNameToJoin = it },
            label = { Text("Enter Room Name/ID") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                if (roomNameToJoin.isNotBlank()) {
                    val encodedRoomName = URLEncoder.encode(roomNameToJoin, StandardCharsets.UTF_8.toString())
                    // Simulate joining: pass room name, a dummy host, and current user
                    val dummyHost = URLEncoder.encode("Host: DummyHost", StandardCharsets.UTF_8.toString())
                    val currentUserEncoded = URLEncoder.encode("Player: $currentUsername", StandardCharsets.UTF_8.toString())
                    val players = listOf(dummyHost, currentUserEncoded).joinToString(",")
                    navController.navigate("roomScreen/$encodedRoomName/$players")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = roomNameToJoin.isNotBlank()
        ) {
            Text("Join Room")
        }
    }
}
