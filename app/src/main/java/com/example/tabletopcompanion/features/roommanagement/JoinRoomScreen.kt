package com.example.tabletopcompanion.features.roommanagement

import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    navController: NavController,
    // Assuming RoomViewModelFactory is provided by the navigation graph setup in MainActivity
    // If not, it needs to be passed or a default instance created (less ideal for testing/DI)
    roomViewModelFactory: RoomViewModelFactory
) {
    val roomViewModel: RoomViewModel = viewModel(factory = roomViewModelFactory)
    val discoveredServices by roomViewModel.discoveredServices.collectAsState()
    val isP2PConnected by roomViewModel.isP2PConnected.collectAsState()
    val roomState by roomViewModel.roomState.collectAsState() // To get roomId after connection

    val context = LocalContext.current // For UserProfileRepository
    val userProfileRepository = remember { UserProfileRepository(context) }

    LaunchedEffect(Unit) {
        roomViewModel.startNsdDiscovery()
    }

    DisposableEffect(Unit) {
        onDispose {
            roomViewModel.stopNsdDiscovery()
        }
    }

    // Navigate to RoomScreen when isP2PConnected is true and we have a room ID
    LaunchedEffect(isP2PConnected, roomState) {
        if (isP2PConnected) {
            roomState?.roomId?.let { roomId ->
                navController.navigate("roomScreen/$roomId") {
                    popUpTo("mainScreen") // Avoid back stack to JoinRoomScreen
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Available Rooms",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (discoveredServices.isEmpty()) {
            Text("Searching for rooms on the local network...")
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(discoveredServices) { service ->
                    DiscoveredServiceItem(serviceInfo = service) {
                        val currentUsername = userProfileRepository.getUsername()
                        val currentUserId = userProfileRepository.getCurrentUserId()
                        val hostAddress = service.host?.hostAddress
                        val port = service.port

                        if (hostAddress != null) {
                            Log.d(
                                "JoinRoomScreen",
                                "Joining room: ${service.serviceName} at $hostAddress:$port as $currentUsername ($currentUserId)"
                            )
                            roomViewModel.joinRoomRemote(hostAddress, port, currentUsername, currentUserId)
                        } else {
                            Log.e("JoinRoomScreen", "Host address is null for service: ${service.serviceName}")
                            // Optionally show a toast or message to the user
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredServiceItem(
    serviceInfo: NsdServiceInfo,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = serviceInfo.attributes["roomName"]?.toString(StandardCharsets.UTF_8)
                    ?: serviceInfo.serviceName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Host: ${serviceInfo.attributes["hostName"]?.toString(StandardCharsets.UTF_8) ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Game: ${serviceInfo.attributes["templateId"]?.toString(StandardCharsets.UTF_8)?.take(20).let { if (it != null && it.length == 20) "$it..." else it } ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "IP: ${serviceInfo.host?.hostAddress ?: "N/A"}:${serviceInfo.port}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Status: ${serviceInfo.attributes["gameState"]?.toString(StandardCharsets.UTF_8) ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onJoin,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
                enabled = serviceInfo.host?.hostAddress != null // Disable join if host address not resolved
            ) {
                Text("Join")
            }
        }
    }
}
