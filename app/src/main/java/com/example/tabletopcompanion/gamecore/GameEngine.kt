package com.example.tabletopcompanion.gamecore

import com.example.tabletopcompanion.data.model.Player
import com.example.tabletopcompanion.data.model.template.GameLogic
import com.example.tabletopcompanion.data.model.template.PhaseTurnDefinition
import com.example.tabletopcompanion.util.Result // Your existing Result class
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameEngine(
    private val gameLogic: GameLogic,
    private val players: List<Player>
) {
    data class GameStateBundle(
        val currentPlayer: Player?,
        val currentPhaseTurnId: String?,
        val currentPhaseTurnName: String?,
        val message: String?
    )

    val onGameStateUpdate: MutableStateFlow<GameStateBundle?> = MutableStateFlow(null)

    private var currentPlayerIndexInternal: Int = 0
    val currentPlayerIndex: Int
        get() = currentPlayerIndexInternal

    private var currentPhaseOrTurnId: String? = null

    init {
        if (players.isEmpty()) {
            throw IllegalArgumentException("Player list cannot be empty for GameEngine.")
        }
    }

    fun initializeGame() {
        if (gameLogic.phasesTurns.isEmpty()) {
            emitCurrentGameState("Error: No phases or turns defined in the game logic. Cannot initialize game.")
            return
        }
        currentPlayerIndexInternal = 0
        currentPhaseOrTurnId = gameLogic.phasesTurns.minByOrNull { it.order }?.id
        if (currentPhaseOrTurnId == null) {
            emitCurrentGameState("Error: Could not determine the starting phase/turn.")
            return
        }
        emitCurrentGameState("Game Initialized. Starting with ${getCurrentPlayer()?.username} in phase ${getCurrentPhaseOrTurn()?.name}.")
    }

    fun getCurrentPlayer(): Player? {
        return players.getOrNull(currentPlayerIndexInternal)
    }

    fun getCurrentPhaseOrTurn(): PhaseTurnDefinition? {
        return gameLogic.phasesTurns.find { it.id == currentPhaseOrTurnId }
    }

    fun advanceToNextPlayer() {
        if (players.isNotEmpty()) {
            currentPlayerIndexInternal = (currentPlayerIndexInternal + 1) % players.size
            emitCurrentGameState("Next player: ${getCurrentPlayer()?.username}.")
        } else {
            emitCurrentGameState("Error: No players to advance.")
        }
    }

    fun advanceToNextPhaseOrTurn(targetId: String? = null) {
        val currentPhase = getCurrentPhaseOrTurn()
        var nextPhase: PhaseTurnDefinition? = null

        if (targetId != null) {
            nextPhase = gameLogic.phasesTurns.find { it.id == targetId }
        } else if (currentPhase != null) {
            // Find next phase by order
            nextPhase = gameLogic.phasesTurns
                .filter { it.order > currentPhase.order }
                .minByOrNull { it.order }
            // If no phase with greater order, loop back to the first (or handle end of game)
            if (nextPhase == null) {
                nextPhase = gameLogic.phasesTurns.minByOrNull { it.order }
            }
        } else if (gameLogic.phasesTurns.isNotEmpty()) {
            // If currentPhase is null (e.g., not initialized properly), start from the first phase
            nextPhase = gameLogic.phasesTurns.minByOrNull { it.order }
        }


        if (nextPhase != null) {
            currentPhaseOrTurnId = nextPhase.id
            currentPlayerIndexInternal = 0 // Reset to the first player for the new phase/turn
            emitCurrentGameState("Advanced to phase: ${nextPhase.name}. Player: ${getCurrentPlayer()?.username}.")
        } else {
            emitCurrentGameState("Error: Could not determine the next phase or turn.")
        }
    }

    fun performAction(actionId: String, actingPlayerId: String): Result<String> {
        val actionDef = gameLogic.actions.find { it.id == actionId }
        if (actionDef == null) {
            val errorMsg = "Action $actionId not found in game logic."
            emitCurrentGameState(errorMsg)
            return Result.Error(IllegalArgumentException(errorMsg))
        }

        // Basic checks (can be expanded with actionDef.triggerConditions, etc.)
        if (actingPlayerId != getCurrentPlayer()?.id) {
            val errorMsg = "Player $actingPlayerId is not the current player (${getCurrentPlayer()?.username})."
            emitCurrentGameState(errorMsg)
            // return Result.Error(IllegalStateException(errorMsg)) // Or just log and proceed for now
        }

        // Placeholder for actual effect execution
        // actionDef.effects.forEach { effect -> processEffect(effect) }

        val message = "Player ${getCurrentPlayer()?.username} performed action: ${actionDef.name ?: actionId}."
        emitCurrentGameState(message)
        return Result.Success("Action ${actionDef.name ?: actionId} performed by player $actingPlayerId.")
    }

    private fun emitCurrentGameState(message: String?) {
        val bundle = GameStateBundle(
            getCurrentPlayer(),
            currentPhaseOrTurnId,
            getCurrentPhaseOrTurn()?.name,
            message
        )
        onGameStateUpdate.value = bundle
    }
}
