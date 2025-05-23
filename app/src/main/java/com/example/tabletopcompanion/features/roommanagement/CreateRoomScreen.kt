package com.example.tabletopcompanion.features.roommanagement

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.data.model.GameTemplateMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    navController: NavController,
    roomViewModelFactory: RoomViewModelFactory
) {
    val roomViewModel: RoomViewModel = viewModel(factory = roomViewModelFactory)
    val availableTemplates by roomViewModel.availableTemplates.collectAsState()
    val roomState by roomViewModel.roomState.collectAsState()

    var roomName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<GameTemplateMetadata?>(null) }

    LaunchedEffect(roomState) {
        roomState?.roomId?.let {
            navController.navigate("roomScreen/$it") {
                // Pop up to mainScreen to avoid back stack buildup to CreateRoomScreen
                popUpTo("mainScreen") 
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create a New Room",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            label = { Text("Enter Room Name") },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTemplate?.name ?: "Select a Game Template",
                onValueChange = {},
                readOnly = true,
                label = { Text("Game Template") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableTemplates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text("${template.name} (${template.playerCountDescription})") },
                        onClick = {
                            selectedTemplate = template
                            expanded = false
                        }
                    )
                }
                if (availableTemplates.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No templates available. Import first.") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                }
            }
        }

        Button(
            onClick = {
                selectedTemplate?.let { template ->
                    if (roomName.isNotBlank()) {
                        roomViewModel.createRoom(roomName, template.templateId)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = roomName.isNotBlank() && selectedTemplate != null
        ) {
            Text("Create Room")
        }

        // Optional: Indicate room is being advertised
        val currentRoomState = roomState // Capture for stable composition
        if (currentRoomState != null && currentRoomState.currentGameState == "SETUP") {
            Text(
                text = "Room '${currentRoomState.roomName}' is being advertised on the local network...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
