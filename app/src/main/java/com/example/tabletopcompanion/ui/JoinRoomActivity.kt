package com.example.tabletopcompanion.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopcompanion.R
import com.example.tabletopcompanion.ui.RoomActivity

class JoinRoomActivity : AppCompatActivity() {

    private lateinit var joinRoomNameEditText: EditText
    private lateinit var confirmJoinRoomButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_room)

        joinRoomNameEditText = findViewById(R.id.joinRoomNameEditText)
        confirmJoinRoomButton = findViewById(R.id.confirmJoinRoomButton)

        confirmJoinRoomButton.setOnClickListener {
            val roomName = joinRoomNameEditText.text.toString().trim()
            if (roomName.isNotEmpty()) {
                val intent = Intent(this, RoomActivity::class.java)
                intent.putExtra("roomName", roomName)
                startActivity(intent)
                finish() // Finish JoinRoomActivity after launching RoomActivity
            } else {
                Toast.makeText(this, "Room name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
