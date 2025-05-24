package com.example.tabletopcompanion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.tabletopcompanion.R // For string resources
import com.example.tabletopcompanion.data.RoomSettingsRepository
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.model.GameTemplateInfo
import com.example.tabletopcompanion.model.game.GameIndicator
import com.example.tabletopcompanion.model.game.GameState

class RoomViewModel(application: Application) : AndroidViewModel(application) {

    private val templateRepository: TemplateRepository = TemplateRepository(application)
    private val roomSettingsRepository: RoomSettingsRepository = RoomSettingsRepository(application)

    val gameState: MutableLiveData<GameState?> = MutableLiveData(null)
    val gameIndicators: MutableLiveData<List<GameIndicator>> = MutableLiveData(emptyList())
    val currentPhaseNameDisplay: MutableLiveData<String> = MutableLiveData(application.getString(R.string.room_no_template_loaded))

    private var loadedPhases: List<String> = emptyList()
    private var currentPhaseIndex: Int = -1

    fun loadInitialRoomState() {
        val selectedTemplateId = roomSettingsRepository.getSelectedTemplateId()
        if (selectedTemplateId == null) {
            currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_no_template_selected_error))
            gameState.postValue(null)
            gameIndicators.postValue(emptyList())
            return
        }
        loadTemplateAndInitGame(selectedTemplateId)
    }

    fun loadTemplateAndInitGame(templateId: String?) { // Renamed from templateToLoadId to templateId for clarity
        if (templateId == null) {
            currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_no_template_id_provided_error))
            gameState.postValue(null)
            gameIndicators.postValue(emptyList())
            return
        }

        val gameTemplateInfo: GameTemplateInfo? = templateRepository.getTemplateById(templateId)

        if (gameTemplateInfo == null) {
            currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_template_not_found_error, templateId))
            gameState.postValue(null)
            gameIndicators.postValue(emptyList())
            return
        }

        // Initialize Phases
        loadedPhases = gameTemplateInfo.phases
        currentPhaseIndex = if (loadedPhases.isNotEmpty()) 0 else -1

        // Initialize Indicators
        val initialIndicators = gameTemplateInfo.initialIndicators.map { parsedIndicator ->
            GameIndicator(
                name = parsedIndicator.name,
                value = parsedIndicator.initialValue,
                type = parsedIndicator.type
            )
        }
        gameIndicators.postValue(initialIndicators)

        // Initialize GameState
        val initialPhaseName = if (currentPhaseIndex != -1) loadedPhases[currentPhaseIndex] else "N/A"
        val newGameState = GameState(
            activeTemplateId = gameTemplateInfo.id,
            currentRound = 1, // Default, can be an indicator later
            currentPhaseName = initialPhaseName,
            currentPlayerId = null // Default
        )
        gameState.postValue(newGameState)
        currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_phase_display_prefix, initialPhaseName))
    }

    fun nextPhase() {
        if (loadedPhases.isEmpty()) {
            currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_no_phases_in_template_error))
            return
        }

        currentPhaseIndex++
        if (currentPhaseIndex >= loadedPhases.size) {
            currentPhaseIndex = 0 // Loop back to the first phase or handle game end
        }

        val newPhaseName = loadedPhases[currentPhaseIndex]
        val currentGameState = gameState.value
        if (currentGameState != null) {
            gameState.postValue(currentGameState.copy(currentPhaseName = newPhaseName))
            currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_phase_display_prefix, newPhaseName))
        } else {
            // This case should ideally not happen if a game is loaded
            currentPhaseNameDisplay.postValue(getApplication<Application>().getString(R.string.room_game_state_not_initialized_error))
        }
    }
}
