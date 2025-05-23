package com.example.tabletopcompanion.features.roommanagement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.data.model.GameTemplateMetadata
import com.example.tabletopcompanion.data.model.Player
import com.example.tabletopcompanion.data.model.Room
import com.example.tabletopcompanion.data.UserProfileRepository
import com.example.tabletopcompanion.data.model.template.GameTemplate
import com.example.tabletopcompanion.data.network.ollama.OllamaService
import com.example.tabletopcompanion.network.model.* // Import all P2P messages
import com.example.tabletopcompanion.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RoomViewModel(
    application: Application,
    private val templateRepository: TemplateRepository,
    private val ollamaService: OllamaService // Added OllamaService
) : AndroidViewModel(application) {

    private val _roomState = MutableStateFlow<Room?>(null)
    val roomState: StateFlow<Room?> = _roomState.asStateFlow()

    private val _availableTemplates = MutableStateFlow<List<GameTemplateMetadata>>(emptyList())
    val availableTemplates: StateFlow<List<GameTemplateMetadata>> = _availableTemplates.asStateFlow()

    private val userProfileRepository = UserProfileRepository(application)

    // StateFlows for LLM interaction
    private val _llmQueryActive = MutableStateFlow(false)
    val llmQueryActive: StateFlow<Boolean> = _llmQueryActive.asStateFlow()

    private val _llmResponse = MutableStateFlow<String?>(null)
    val llmResponse: StateFlow<String?> = _llmResponse.asStateFlow()

    private val _llmError = MutableStateFlow<String?>(null)
    val llmError: StateFlow<String?> = _llmError.asStateFlow()

    private val _loadedGameTemplate = MutableStateFlow<GameTemplate?>(null)
    val loadedGameTemplate: StateFlow<GameTemplate?> = _loadedGameTemplate.asStateFlow()

    // Game Engine related states
    private var gameEngine: GameEngine? = null

    private val _currentPlayerName = MutableStateFlow<String?>(null)
    val currentPlayerName: StateFlow<String?> = _currentPlayerName.asStateFlow()

    private val _currentPhaseName = MutableStateFlow<String?>(null)
    val currentPhaseName: StateFlow<String?> = _currentPhaseName.asStateFlow()

    private val _gameMessage = MutableStateFlow<String?>(null)
    val gameMessage: StateFlow<String?> = _gameMessage.asStateFlow()

    private val _indicatorValues = MutableStateFlow<Map<String, String>>(emptyMap())
    val indicatorValues: StateFlow<Map<String, String>> = _indicatorValues.asStateFlow()

    // NSD related
    private lateinit var nsdHelper: NsdHelper
    val discoveredServices: StateFlow<List<android.net.nsd.NsdServiceInfo>> // Directly expose from NsdHelper

    // P2P Manager
    private val p2pManager = P2PCommunicationManager()
    private val connectedClients = java.util.concurrent.ConcurrentHashMap<String, java.io.ObjectOutputStream>() // clientId to stream
    private var objectInputStreamSelfAsClient: java.io.ObjectInputStream? = null
    private val clientPlayerMap = java.util.concurrent.ConcurrentHashMap<String, Player>() // P2P clientId to Player

    private val _isP2PConnected = MutableStateFlow(false)
    val isP2PConnected: StateFlow<Boolean> = _isP2PConnected.asStateFlow()


    companion object {
        const val HOST_PORT = 8888
    }

    init {
        loadAvailableTemplates()
        nsdHelper = NsdHelper(application)
        discoveredServices = nsdHelper.discoveredServices // Assign StateFlow
    }

    override fun onCleared() {
        super.onCleared()
        stopNsdRegistration()
        stopNsdDiscovery() // Ensure discovery is also stopped
        p2pManager.shutdown() // Shutdown P2P manager
        gameEngine?.onGameStateUpdate?.value = null
    }

    fun getCurrentUserId(): String = userProfileRepository.getCurrentUserId()

    fun loadAvailableTemplates() {
        viewModelScope.launch {
            _availableTemplates.value = templateRepository.getTemplateMetadataList()
        }
    }

    fun createRoom(roomName: String, templateId: String) {
        viewModelScope.launch {
            val hostUsername = userProfileRepository.getUsername() ?: "Host" // Default if no username set
            val hostAvatarUrl = userProfileRepository.getAvatarUrl() // Get avatar
            val hostPlayer = Player(
                id = UUID.randomUUID().toString(),
                username = hostUsername,
                isHost = true,
                avatarUrl = hostAvatarUrl
            )
            val roomId = UUID.randomUUID().toString()
            val selectedTemplate = _availableTemplates.value.find { it.templateId == templateId }

            val newRoom = Room(
                roomId = roomId,
                roomName = roomName,
                host = hostPlayer,
                players = listOf(hostPlayer), // Initial player list
                selectedTemplateId = templateId,
                gameTemplateName = selectedTemplate?.name,
                currentGameState = "SETUP"
            )
            _roomState.value = newRoom

            // Register service for NSD and Start P2P Host Server
            newRoom.let { room ->
                val attributes = mapOf(
                    "roomId" to room.roomId,
                    "roomName" to room.roomName,
                    "hostName" to room.host.username,
                    "templateId" to (room.selectedTemplateId ?: ""),
                    "gameState" to room.currentGameState
                )
                nsdHelper.registerService(room.roomName, HOST_PORT, attributes)
                startP2PHostServer(room)
            }

            // Load the full game template data
            newRoom.selectedTemplateId?.let { newTemplateId ->
                viewModelScope.launch {
                    when (val result = templateRepository.loadGameTemplate(newTemplateId)) {
                        is Result.Success -> _loadedGameTemplate.value = result.data
                        is Result.Error -> {
                            _llmError.value = "Failed to load game template: ${result.exception.message}"
                        }
                    }
                }
            }
        }
    }

    fun stopNsdRegistration() {
        nsdHelper.unregisterService()
    }

    fun startNsdDiscovery() {
        nsdHelper.startDiscovery()
    }

    fun stopNsdDiscovery() {
        nsdHelper.stopDiscovery()
    }

    private fun startP2PHostServer(initialRoom: Room) {
        p2pManager.startHostServer(
            port = HOST_PORT,
            onClientConnected = { outputStream, clientId ->
                connectedClients[clientId] = outputStream
                _roomState.value?.let {
                    p2pManager.sendMessageToClient(clientId, RoomStateUpdate(it))
                }
                _gameMessage.value = "Client $clientId connected."
            },
            onClientDisconnected = { clientId ->
                connectedClients.remove(clientId)
                val playerWhoLeft = clientPlayerMap.remove(clientId)
                playerWhoLeft?.let {
                    _roomState.value?.let { currentRoom ->
                        val updatedPlayers = currentRoom.players.filterNot { p -> p.id == it.id }
                        _roomState.value = currentRoom.copy(players = updatedPlayers)
                        p2pManager.broadcastMessageToAll(PlayerLeftMsg(it.id))
                        broadcastRoomState()
                    }
                }
                _gameMessage.value = "Client ${playerWhoLeft?.username ?: clientId} disconnected."
            },
            onMessageReceived = { clientId, message ->
                viewModelScope.launch {
                    handleP2PMessageFromClient(clientId, message)
                }
            },
            onError = { error ->
                _gameMessage.value = "Host Server Error: $error"
                // Potentially stop NSD registration if server fails critically
            }
        )
    }

    private fun handleP2PMessageFromClient(clientId: String, message: P2PMessage) {
        when (message) {
            is JoinRequest -> {
                val newPlayer = Player(id = message.userId, username = message.username, isHost = false)
                clientPlayerMap[clientId] = newPlayer
                _roomState.value?.let { currentRoom ->
                    val updatedPlayers = currentRoom.players.toMutableList().apply { add(newPlayer) }
                    _roomState.value = currentRoom.copy(players = updatedPlayers)
                    broadcastRoomState()
                }
                _gameMessage.value = "Player ${message.username} joined."
            }
            is PlayerActionIntentMsg -> {
                val actingPlayer = _roomState.value?.players?.find { it.id == message.playerId }
                if (actingPlayer != null) {
                    android.util.Log.d("RoomViewModel_Host", "Received PlayerActionIntentMsg for action '${message.actionId}' from player ${actingPlayer.username} (${message.playerId})")
                    // Optional: Add validation here - e.g., is it this player's turn according to gameEngine?
                    // For now, host executes the action on behalf of the player.
                    // gameEngine.performAction will emit new game state, which is then broadcast.
                    gameEngine?.performAction(actionId = message.actionId, actingPlayerId = message.playerId, parameters = message.parameters)
                } else {
                    android.util.Log.e("RoomViewModel_Host", "Player not found for PlayerActionIntentMsg: ID ${message.playerId}")
                    // Optionally send an error message back to the specific client who sent this
                    // p2pManager.sendMessageToClient(clientId, ErrorMessageMsg("Invalid player ID in your action request."))
                }
            }
            // Placeholder for other message types like GameActionMsg, ChatMsg etc.
            else -> _gameMessage.value = "Host received unhandled message: $message from $clientId"
        }
    }

    private fun broadcastRoomState() {
        _roomState.value?.let {
            p2pManager.broadcastMessageToAll(RoomStateUpdate(it))
        }
    }


    fun addPlayer(username: String) { // Primarily for local host testing
        if (_roomState.value?.host?.id != userProfileRepository.getCurrentUserId()) {
             _gameMessage.value = "Only host can add players locally for testing."
             return
        }
        _roomState.value?.let { currentRoom ->
            val newPlayer = Player(
                id = UUID.randomUUID().toString(),
                username = username,
                isHost = false
            )
            val updatedPlayers = currentRoom.players.toMutableList().apply { add(newPlayer) }
            _roomState.value = currentRoom.copy(players = updatedPlayers)
            broadcastRoomState() // Broadcast if host
        }
    }

    fun startGame() {
        val currentRoom = _roomState.value
        val currentTemplate = _loadedGameTemplate.value

        if (currentRoom == null || currentTemplate == null) {
            _gameMessage.value = "Error: Room or game template not loaded."
            return
        }
        if (currentRoom.host.id != userProfileRepository.getCurrentUserId()) {
            _gameMessage.value = "Only the host can start the game."
            return
        }

        if (currentTemplate.gameLogic == null || currentTemplate.gameLogic.phasesTurns.isEmpty()) {
            _gameMessage.value = "Error: Game logic or phases missing in template."
            return
        }

        gameEngine = GameEngine(currentTemplate.gameLogic, currentRoom.players, currentTemplate.uiDefinition)

        viewModelScope.launch {
            var previousMessage: String? = null
            var previousIndicatorValues: Map<String, String>? = null

            gameEngine?.onGameStateUpdate?.collect { gameStateBundle ->
                gameStateBundle?.let {
                    _currentPlayerName.value = it.currentPlayer?.username
                    _currentPhaseName.value = it.currentPhaseTurnName
                    _gameMessage.value = it.message
                    _indicatorValues.value = it.indicatorValues

                    // If host, broadcast GameStateUpdateMsg if relevant data changed
                    if (currentRoom.host.id == getCurrentUserId()) {
                        val messageChanged = it.message != previousMessage && it.message != null
                        val indicatorsChanged = it.indicatorValues != previousIndicatorValues

                        if (messageChanged || indicatorsChanged) {
                            p2pManager.broadcastMessageToAll(
                                GameStateUpdateMsg(
                                    it.currentPlayer?.username,
                                    it.currentPhaseTurnName,
                                    it.message,
                                    it.indicatorValues
                                )
                            )
                            previousMessage = it.message
                            previousIndicatorValues = it.indicatorValues
                        }
                    }
                }
            }
        }
        gameEngine?.initializeGame()
        _roomState.value = currentRoom.copy(currentGameState = "PLAYING")
        broadcastRoomState() // Update clients about the "PLAYING" state
        p2pManager.broadcastMessageToAll(StartGameMsg(_roomState.value!!)) // Send StartGameMsg

        _llmResponse.value = null
        _llmError.value = null
    }

    fun requestNextPlayer() {
        // If host, tell engine and broadcast. If client, send request to host.
        if (_roomState.value?.host?.id == userProfileRepository.getCurrentUserId()) {
            viewModelScope.launch {
                gameEngine?.advanceToNextPlayer()
                // GameStateUpdateMsg is sent via gameEngine.onGameStateUpdate collector
            }
        } else {
            // TODO: Send NextPlayerRequestMsg to host
            _gameMessage.value = "Client action: RequestNextPlayer (not implemented yet)"
        }
    }

    fun requestNextPhase() {
        // If host, tell engine and broadcast. If client, send request to host.
        if (_roomState.value?.host?.id == userProfileRepository.getCurrentUserId()) {
            viewModelScope.launch {
                gameEngine?.advanceToNextPhaseOrTurn()
                // GameStateUpdateMsg is sent via gameEngine.onGameStateUpdate collector
            }
        } else {
            // TODO: Send NextPhaseRequestMsg to host
            _gameMessage.value = "Client action: RequestNextPhase (not implemented yet)"
        }
    }

    fun requestPlayerAction(actionId: String) {
        viewModelScope.launch { // Ensure it's launched in a coroutine if not already
            val currentUserId = getCurrentUserId() // Assuming this method exists and works
            val room = _roomState.value ?: return@launch // Exit if room state is null

            if (room.host.id == currentUserId) { // User is the HOST
                android.util.Log.d("RoomViewModel", "Host performing action: $actionId with params: $parameters")
                gameEngine?.performAction(actionId, currentUserId, parameters)
                // performAction in GameEngine should call emitCurrentGameState,
                // which in turn triggers host's broadcast of GameStateUpdateMsg.
            } else { // User is a CLIENT
                android.util.Log.d("RoomViewModel", "Client requesting action: $actionId")
                p2pManager.sendMessageToServer(
                    PlayerActionIntentMsg(
                        playerId = currentUserId,
                        actionId = actionId,
                        parameters = null // No parameters from UI yet
                    )
                )
            }
        }
    }

    fun joinRoomRemote(hostAddress: String, port: Int, currentUserName: String, currentUserId: String) {
        p2pManager.connectToHost(
            hostAddress = hostAddress,
            port = port,
            onConnectionSuccess = { outputStream, inputStream ->
                this.objectInputStreamSelfAsClient = inputStream
                p2pManager.sendMessageToServer(JoinRequest(currentUserName, currentUserId))
                _isP2PConnected.value = true // Used to trigger navigation
                _gameMessage.value = "Connected to host. Sent join request."
            },
            onMessageReceived = { message ->
                viewModelScope.launch {
                    handleP2PMessageFromServer(message)
                }
            },
            onDisconnected = {
                _gameMessage.value = "Disconnected from host."
                _roomState.value = null // Clear room state
                _isP2PConnected.value = false
                gameEngine = null // Clear game engine
                // Reset relevant game state flows
                _currentPlayerName.value = null
                _currentPhaseName.value = null
                _loadedGameTemplate.value = null

            },
            onError = { error ->
                _gameMessage.value = "Client Error: $error"
                _isP2PConnected.value = false
            }
        )
    }

    private fun handleP2PMessageFromServer(message: P2PMessage) {
        when (message) {
            is RoomStateUpdate -> {
                val oldTemplateId = _roomState.value?.selectedTemplateId
                val wasPlaying = _roomState.value?.currentGameState == "PLAYING"
                _roomState.value = message.room
                if (oldTemplateId != message.room.selectedTemplateId || _loadedGameTemplate.value == null) {
                    loadFullTemplateAndPotentiallyStartGame(message.room, message.room.currentGameState == "PLAYING" && !wasPlaying)
                } else if (message.room.currentGameState == "PLAYING" && gameEngine == null) {
                    loadFullTemplateAndPotentiallyStartGame(message.room, true)
                }
            }
            is PlayerJoinedMsg -> { // This might be redundant if RoomStateUpdate is comprehensive
                _roomState.value?.let { currentRoom ->
                    if (currentRoom.players.none { it.id == message.player.id }) {
                        val updatedPlayers = currentRoom.players.toMutableList().apply { add(message.player) }
                        _roomState.value = currentRoom.copy(players = updatedPlayers)
                    }
                }
            }
            is PlayerLeftMsg -> {
                _roomState.value?.let { currentRoom ->
                    val updatedPlayers = currentRoom.players.filterNot { it.id == message.playerId }
                    _roomState.value = currentRoom.copy(players = updatedPlayers)
                }
            }
            is GameStateUpdateMsg -> {
                _currentPlayerName.value = message.currentPlayerName
                _currentPhaseName.value = message.currentPhaseName
                _gameMessage.value = message.gameMessage
                _indicatorValues.value = message.indicatorValues ?: emptyMap()
            }
            is StartGameMsg -> {
                // Host has started the game, client needs to initialize its game engine
                loadFullTemplateAndPotentiallyStartGame(message.initialRoomState, true)
            }
            is ErrorMessageMsg -> {
                _gameMessage.value = "Error from host: ${message.error}"
            }
            else -> _gameMessage.value = "Client received unhandled message: $message"
        }
    }

    private fun loadFullTemplateAndPotentiallyStartGame(room: Room, startGameAfterLoad: Boolean = false) {
        _roomState.value = room // Ensure local room state is up-to-date
        viewModelScope.launch {
            room.selectedTemplateId?.let { templateId ->
                when (val result = templateRepository.loadGameTemplate(templateId)) {
                    is Result.Success -> {
                        _loadedGameTemplate.value = result.data
                        if (startGameAfterLoad && room.currentGameState == "PLAYING") {
                             // Client-side game engine initialization
                            if (gameEngine == null && _loadedGameTemplate.value != null && _roomState.value != null) {
                                gameEngine = GameEngine(
                                    _loadedGameTemplate.value!!.gameLogic,
                                    _roomState.value!!.players,
                                    _loadedGameTemplate.value!!.uiDefinition
                                )
                                // Client doesn't collect its own engine's updates for broadcasting,
                                // it relies on GameStateUpdateMsg from host.
                                // But it needs to initialize its engine to follow along.
                                gameEngine?.initializeGame() // Initialize to the start state
                                _gameMessage.value = "Game engine initialized as client."
                                // Initial indicator values will be set by the first GameStateUpdateMsg or StartGameMsg
                            }
                        }
                    }
                    is Result.Error -> {
                        _gameMessage.value = "Client: Failed to load game template: ${result.exception.message}"
                    }
                }
            } ?: run {
                _gameMessage.value = "Client: No template ID selected in room."
            }
        }
    }


    fun requestPlayerAction(actionId: String) {
        val currentRoom = _roomState.value ?: return
        val engine = gameEngine ?: return
        // Get current player ID using the index from GameEngine against the Room's player list
        val actingPlayerId = currentRoom.players.getOrNull(engine.currentPlayerIndex)?.id ?: return

        viewModelScope.launch {
            val result = engine.performAction(actionId, actingPlayerId)
            if (result is Result.Error) {
                _gameMessage.value = "Action Error: ${result.exception.message}"
            }
            // Success messages are handled by onGameStateUpdate
        }
    }

    fun askLlm(question: String) {
        viewModelScope.launch {
            _llmQueryActive.value = true
            _llmResponse.value = null
            _llmError.value = null

            if (_loadedGameTemplate.value == null) {
                _llmError.value = "Game template not loaded."
                _llmQueryActive.value = false
                return@launch
            }

            val gameTemplate = _loadedGameTemplate.value!!
            val rules = gameTemplate.llmRulesDocument
            // Assuming llmPrompts and userQueryTemplate are non-null based on typical template structure
            // A more robust solution would check for nulls here or ensure data integrity upon template import.
            val systemPrompt = gameTemplate.llmPrompts.systemPrompt
            val userQueryTemplate = gameTemplate.llmPrompts.userQueryTemplate

            val modelName = gameTemplate.llmPrompts.modelName ?: "mistral" // Default model

            // Construct the full prompt, ensuring userQueryTemplate is handled if it could be null
            val fullPrompt = userQueryTemplate.replace("{question}", question) +
                    "\n\nGame Rules for Context:\n$rules"


            when (val result = ollamaService.generateResponse(
                model = modelName,
                prompt = fullPrompt,
                systemPrompt = systemPrompt
            )) {
                is Result.Success -> {
                    _llmResponse.value = result.data.response
                }
                is Result.Error -> {
                    _llmError.value = "LLM Error: ${result.exception.message}"
                }
            }
            _llmQueryActive.value = false
        }
    }
}

// Factory class is now in its own file: RoomViewModelFactory.kt
// Removing it from here to avoid duplication if it was accidentally left.
