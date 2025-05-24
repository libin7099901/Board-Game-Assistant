package com.example.tabletopcompanion.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopcompanion.R

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var roomNameEditText: EditText
    private lateinit var confirmCreateRoomButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_room)

        roomNameEditText = findViewById(R.id.roomNameEditText)
        confirmCreateRoomButton = findViewById(R.id.confirmCreateRoomButton)

        confirmCreateRoomButton.setOnClickListener {
            val roomName = roomNameEditText.text.toString().trim()
            if (roomName.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("roomName", roomName)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Room name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
