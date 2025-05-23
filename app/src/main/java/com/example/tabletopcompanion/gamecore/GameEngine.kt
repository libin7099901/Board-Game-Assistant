package com.example.tabletopcompanion.gamecore

import com.example.tabletopcompanion.data.model.Player
import com.example.tabletopcompanion.data.model.template.GameLogic
import com.example.tabletopcompanion.data.model.template.PhaseTurnDefinition
import com.example.tabletopcompanion.data.model.template.UIDefinition
import com.example.tabletopcompanion.util.Result // Your existing Result class
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameEngine(
    private val gameLogic: GameLogic,
    private val players: List<Player>,
    private val uiDefinition: UIDefinition? // Added uiDefinition
) {
    data class GameStateBundle(
        val currentPlayer: Player?,
        val currentPhaseTurnId: String?,
        val currentPhaseTurnName: String?,
        val message: String?,
        val indicatorValues: Map<String, String> // Added indicatorValues
    )

    val onGameStateUpdate: MutableStateFlow<GameStateBundle?> = MutableStateFlow(null)
    private val gameVariables = mutableMapOf<String, Any>() // Added gameVariables

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
        // Initialize variables
        gameLogic.variables?.values?.forEach { varDef ->
            val initialValue = parseVariableValue(varDef.initialValue, varDef.type)
            gameVariables[varDef.id] = initialValue ?: varDef.initialValue // Store as string if parsing fails
        }

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

    private fun parseVariableValue(value: String, type: String): Any? {
        return when (type.lowercase()) {
            "integer" -> value.toIntOrNull()
            "string" -> value
            // Add other types like "boolean", "list<string>", etc. as needed
            else -> value // Default to storing as string if type is unknown or parsing fails
        }
    }

    private fun resolveValue(expression: String, currentVarValue: Any?, parameters: Map<String, String>?): Any? {
        val paramRegex = "\\{PARAM:(\\w+)\\}".toRegex() // Corrected regex
        paramRegex.matchEntire(expression)?.let { matchResult ->
            val key = matchResult.groupValues[1]
            // Try to parse parameter as Int if it looks like a number, otherwise return as string
            return parameters?.get(key)?.let { it.toIntOrNull() ?: it }
        }

        if (expression.toIntOrNull() != null) return expression.toInt()
        // {SELF} should be checked before {SELF} + 1
        if (expression == "{SELF}" && currentVarValue != null) return currentVarValue
        if (expression == "{SELF} + 1" && currentVarValue is Int) return currentVarValue + 1
        if (expression.startsWith("\"") && expression.endsWith("\"")) return expression.drop(1).dropLast(1)
        
        // Check if the expression itself is a variable ID (for direct variable assignment)
        if (gameVariables.containsKey(expression)) {
            return gameVariables[expression]
        }
        
        return expression // Default to returning the expression as a string
    }

    fun getVariableValue(variableId: String): Any? = gameVariables[variableId]

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

    fun performAction(actionId: String, actingPlayerId: String, parameters: Map<String, String>? = null): Result<String> {
        val actionDef = gameLogic.actions.find { it.id == actionId }
        if (actionDef == null) {
            val errorMsg = "Action $actionId not found in game logic."
            emitCurrentGameState(errorMsg) // Emit before returning error
            return Result.Error(IllegalArgumentException(errorMsg))
        }

        // Basic checks (can be expanded with actionDef.triggerConditions, etc.)
        if (actingPlayerId != getCurrentPlayer()?.id) {
            val errorMsg = "Player $actingPlayerId is not the current player (${getCurrentPlayer()?.username})."
            // emitCurrentGameState(errorMsg) // Optionally emit, or let action fail silently for non-current player
            // return Result.Error(IllegalStateException(errorMsg)) // Or just log and proceed for now
        }

        var messageForStateUpdate = "Action ${actionDef.name ?: actionId} by ${getCurrentPlayer()?.username}."

        actionDef.effects.forEach { effect ->
            when (effect.type) {
                "SHOW_MESSAGE" -> {
                    val resolvedMessage = resolveValue(effect.valueExpression ?: "", null, parameters)?.toString() ?: ""
                    messageForStateUpdate += " $resolvedMessage"
                }
                "MODIFY_VARIABLE" -> {
                    val targetVarId = effect.target ?: run {
                        messageForStateUpdate += " Error: Missing target for MODIFY_VARIABLE effect in action $actionId."
                        return@forEach // Continue to next effect
                    }
                    val expression = effect.valueExpression ?: ""
                    val currentVarVal = gameVariables[targetVarId]
                    
                    val resolvedValue = resolveValue(expression, currentVarVal, parameters)

                    if (resolvedValue != null) {
                        var finalValueToSet = resolvedValue
                        // Ensure type consistency if variable already exists and has a parsed type
                        val existingVarDef = gameLogic.variables?.get(targetVarId)
                        if (existingVarDef != null && currentVarVal != null) { // And existing var value is not null
                            when (existingVarDef.type.lowercase()) {
                                "integer" -> if (finalValueToSet !is Int) finalValueToSet = finalValueToSet.toString().toIntOrNull() ?: finalValueToSet
                                // Add more type checks as needed for boolean, etc.
                            }
                        } else if (existingVarDef != null && existingVarDef.type.lowercase() == "integer" && finalValueToSet !is Int) {
                            // If it's a new variable defined as integer, try to parse
                            finalValueToSet = finalValueToSet.toString().toIntOrNull() ?: finalValueToSet
                        }

                        gameVariables[targetVarId] = finalValueToSet
                        messageForStateUpdate += " Var $targetVarId set to $finalValueToSet."
                    } else {
                        messageForStateUpdate += " Failed to resolve expression '$expression' for $targetVarId."
                    }
                }
                // Add other effect types like "NEXT_TURN", "MOVE_PLAYER", etc.
            }
        }

        emitCurrentGameState(messageForStateUpdate)
        return Result.Success(messageForStateUpdate)
    }

    private fun emitCurrentGameState(message: String?) {
        val currentIndicatorValues = mutableMapOf<String, String>()
        uiDefinition?.indicators?.forEach { indicatorConfig ->
            if (indicatorConfig.valueSourceType == "variable" && indicatorConfig.valueSourceId != null) {
                val varValue = gameVariables[indicatorConfig.valueSourceId]?.toString() ?: indicatorConfig.initialValue
                currentIndicatorValues[indicatorConfig.id] = varValue
            } else {
                currentIndicatorValues[indicatorConfig.id] = indicatorConfig.initialValue
            }
        }

        val bundle = GameStateBundle(
            getCurrentPlayer(),
            currentPhaseOrTurnId,
            getCurrentPhaseOrTurn()?.name,
            message,
            currentIndicatorValues
        )
        onGameStateUpdate.value = bundle
    }
}
