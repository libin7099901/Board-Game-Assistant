package com.example.tabletopcompanion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopcompanion.ui.CreateRoomActivity
import com.example.tabletopcompanion.ui.ProfileSetupActivity
import com.example.tabletopcompanion.ui.RoomActivity

class MainActivity : AppCompatActivity() {

    private lateinit var createRoomButton: Button
    private lateinit var joinRoomButton: Button
    private lateinit var manageTemplatesButton: Button
    private lateinit var userProfileButton: Button

    private val createRoomLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val roomName = data?.getStringExtra("roomName")
            if (roomName != null && roomName.isNotEmpty()) {
                val intent = Intent(this, RoomActivity::class.java)
                intent.putExtra("roomName", roomName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Failed to get room name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createRoomButton = findViewById(R.id.createRoomButton)
        joinRoomButton = findViewById(R.id.joinRoomButton)
        manageTemplatesButton = findViewById(R.id.manageTemplatesButton)
        userProfileButton = findViewById(R.id.userProfileButton)

        createRoomButton.setOnClickListener {
            val intent = Intent(this, CreateRoomActivity::class.java)
            createRoomLauncher.launch(intent)
        }

        joinRoomButton.setOnClickListener {
            val intent = Intent(this, com.example.tabletopcompanion.ui.JoinRoomActivity::class.java)
            startActivity(intent)
        }

        manageTemplatesButton.setOnClickListener {
            val intent = Intent(this, com.example.tabletopcompanion.ui.TemplateManagerActivity::class.java)
            startActivity(intent)
        }

        userProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java)
            startActivity(intent)
        }
    }
}
