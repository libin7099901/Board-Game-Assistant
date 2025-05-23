package com.example.tabletopcompanion.features.roommanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tabletopcompanion.data.UserProfileRepository
import com.example.tabletopcompanion.data.model.template.UIDefinition
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.runtime.saveable.rememberSaveable // For input fields
import androidx.compose.runtime.snapshots.SnapshotStateMap // For input fields
import androidx.compose.foundation.text.KeyboardOptions // For input fields
import androidx.compose.ui.text.input.KeyboardType // For input fields


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    navController: NavController,
    roomId: String, // Expecting this to be passed, though VM might not use it directly if state is already set
    roomViewModelFactory: RoomViewModelFactory
) {
    val roomViewModel: RoomViewModel = viewModel(factory = roomViewModelFactory)
    val roomState by roomViewModel.roomState.collectAsState()
    val loadedGameTemplate by roomViewModel.loadedGameTemplate.collectAsState()
    val llmQueryActive by roomViewModel.llmQueryActive.collectAsState()
    val llmResponse by roomViewModel.llmResponse.collectAsState()
    val llmError by roomViewModel.llmError.collectAsState()

    // Game State observables
    val currentPlayerName by roomViewModel.currentPlayerName.collectAsState()
    val currentPhaseName by roomViewModel.currentPhaseName.collectAsState()
    val gameMessage by roomViewModel.gameMessage.collectAsState()

    val context = LocalContext.current
    val userProfileRepository = remember { UserProfileRepository(context) }
    val currentUsername = userProfileRepository.getUsername()

    var newPlayerName by remember { mutableStateOf("") }
    var llmQuestionText by remember { mutableStateOf("") }

    if (roomState == null) {
        // Room state might not be immediately available if navigated directly
        // or if ViewModel is re-created.
        // A real implementation might need roomViewModel.loadRoom(roomId) here.
        // For this subtask, we assume CreateRoomScreen has set the state in a shared VM.
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Loading Room Details...")
            // Potentially add a back button or timeout logic
        }
        return
    }

    val room = roomState!! // Safe by now due to the check above

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Room: ${room.roomName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Game: ${room.gameTemplateName ?: "Not Selected"}", style = MaterialTheme.typography.titleMedium)
        Text("Host: ${room.host.username} ${if (room.host.username == currentUsername) "(You)" else ""}", style = MaterialTheme.typography.titleSmall)
        Text("Status: ${room.currentGameState}", style = MaterialTheme.typography.titleSmall)

        Spacer(modifier = Modifier.height(8.dp))

        // Player List (remains as is)
        Text("Players:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)) { // Limit height if needed
            items(room.players) { player ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${player.username} ${if (player.isHost) "[Host]" else ""} ${if (player.username == currentUsername && !player.isHost) "(You)" else ""}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Divider()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))


        // Game State and Controls Section
        if (room.currentGameState == "PLAYING") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("Game State", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Current Player: ${currentPlayerName ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                    Text("Current Phase: ${currentPhaseName ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                    Text("Message: ${gameMessage ?: ""}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { roomViewModel.requestNextPlayer() }) { Text("Next Player") }
                        Button(onClick = { roomViewModel.requestNextPhase() }) { Text("Next Phase") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic UI Controls based on Game Template
            DynamicGameControlsUI(
                uiDefinition = loadedGameTemplate?.uiDefinition,
                roomViewModel = roomViewModel
            )
        }

        // Add Player Section (Host only, during SETUP)
        if (room.host.username == currentUsername && room.currentGameState == "SETUP") {
            OutlinedTextField(
                value = newPlayerName,
                onValueChange = { newPlayerName = it },
                label = { Text("New Player Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (newPlayerName.isNotBlank()) {
                        roomViewModel.addPlayer(newPlayerName)
                        newPlayerName = "" // Clear field
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = newPlayerName.isNotBlank()
            ) {
                Text("Add Player (Test)")
            }
        }

        // Start Game Button (Host only, during SETUP)
        if (room.host.username == currentUsername && room.currentGameState == "SETUP") {
            Button(
                onClick = { roomViewModel.startGame() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = room.currentGameState == "SETUP" // Redundant with outer check, but good for clarity
            ) {
                Text("Start Game")
            }
        }


        // LLM Assistant Section - Only visible if a template is loaded and game is PLAYING
        if (room.currentGameState == "PLAYING") {
            loadedGameTemplate?.let { template ->
                Card(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) { // Allow LLM to take remaining space
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ask the Assistant", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

                        OutlinedTextField(
                            value = llmQuestionText,
                            onValueChange = { llmQuestionText = it },
                            label = { Text("Your question about '${template.gameInfo.name}'") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            enabled = !llmQueryActive
                        )
                        Button(
                            onClick = { roomViewModel.askLlm(llmQuestionText) },
                            enabled = !llmQueryActive && llmQuestionText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ask")
                        }

                        if (llmQueryActive) {
                            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                        }

                        llmError?.let { error ->
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        llmResponse?.let { response ->
                            Text(
                                text = "Assistant: $response",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicGameControlsUI(uiDefinition: UIDefinition?, roomViewModel: RoomViewModel) {
    val indicatorValues by roomViewModel.indicatorValues.collectAsState()
    val inputFieldValues = remember { mutableStateMapOf<String, String>() }

    if (uiDefinition == null) {
        Text("No UI definition loaded or game template not fully processed yet.")
        return
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Indicators Section
    if (uiDefinition.indicators.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Game Indicators", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                uiDefinition.indicators.forEach { indicator ->
                    val displayValue = indicatorValues[indicator.id] ?: indicator.initialValue
                    Text(text = "${indicator.label}: $displayValue", modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }

    // Input Fields Section
    if (uiDefinition.inputFields.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Inputs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                uiDefinition.inputFields.forEach { fieldConfig ->
                    OutlinedTextField(
                        value = inputFieldValues[fieldConfig.id] ?: fieldConfig.defaultValue ?: "",
                        onValueChange = { inputFieldValues[fieldConfig.id] = it },
                        label = { Text(fieldConfig.label) },
                        keyboardOptions = if (fieldConfig.type == "number") KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Buttons Section
    if (uiDefinition.buttons.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Available Actions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    uiDefinition.buttons.forEach { button ->
                        Button(
                            onClick = {
                                val params = mutableMapOf<String, String>()
                                uiDefinition.inputFields.forEach { fieldConfig ->
                                    val key = fieldConfig.targetActionParameterKey ?: fieldConfig.id
                                    inputFieldValues[fieldConfig.id]?.let { value ->
                                        params[key] = value
                                    }
                                }
                                roomViewModel.requestPlayerAction(button.actionId, params.ifEmpty { null })
                                inputFieldValues.clear() // Clear inputs after action
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(button.text)
                        }
                    }
                }
            }
        }
    }
}
