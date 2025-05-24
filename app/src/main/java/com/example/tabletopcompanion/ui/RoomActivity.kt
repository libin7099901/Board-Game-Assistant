package com.example.tabletopcompanion.ui

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels // For by viewModels()
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopcompanion.R

class RoomActivity : AppCompatActivity() {

    private lateinit var roomNameTextView: TextView // Will display room name and phase
    private lateinit var gameStateTextView: TextView
    private lateinit var gameIndicatorsTextView: TextView

    private val roomViewModel: RoomViewModel by viewModels()
    private var originalRoomName: String? = null // Store the original room name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        roomNameTextView = findViewById(R.id.roomNameTextView)
        gameStateTextView = findViewById(R.id.gameStateTextView)
        gameIndicatorsTextView = findViewById(R.id.gameIndicatorsTextView)

        originalRoomName = intent.getStringExtra("roomName")
        val templateId = intent.getStringExtra("templateId")

        if (originalRoomName.isNullOrEmpty()) {
            // Handle missing room name, e.g., set a default or show error
            roomNameTextView.text = "Error: Room name not provided"
        }

        // Observe LiveData
        roomViewModel.gameState.observe(this) { gameState ->
            Log.d("RoomActivity", "GameState updated: $gameState")
            val gameStateText = gameState?.let {
                "Round: ${it.currentRound}\n" +
                "Player: ${it.currentPlayerId ?: "N/A"}\n" +
                "Template: ${it.activeTemplateId ?: "N/A"}"
            } ?: "Game state not available."
            gameStateTextView.text = gameStateText
        }

        roomViewModel.gameIndicators.observe(this) { indicators ->
            Log.d("RoomActivity", "GameIndicators updated: $indicators")
            val indicatorsText = indicators.joinToString("\n") { indicator ->
                "${indicator.name}: ${indicator.value} (${indicator.type})"
            }
            gameIndicatorsTextView.text = if (indicatorsText.isEmpty()) "No indicators." else indicatorsText
        }

        roomViewModel.currentPhaseNameDisplay.observe(this) { phaseDisplayText ->
            Log.d("RoomActivity", "CurrentPhaseNameDisplay updated: $phaseDisplayText")
            val currentRoomName = originalRoomName ?: "Room"
            roomNameTextView.text = "Room: $currentRoomName - $phaseDisplayText"
        }

        // Load template and initialize game
        roomViewModel.loadInitialRoomState() // Changed from loadTemplateAndInitGame(templateId)

        // Example of how to trigger nextPhase (e.g., from a button click)
        val nextPhaseButton: android.widget.Button = findViewById(R.id.nextPhaseButton)
        nextPhaseButton.setOnClickListener {
            roomViewModel.nextPhase()
        }
    }
}
