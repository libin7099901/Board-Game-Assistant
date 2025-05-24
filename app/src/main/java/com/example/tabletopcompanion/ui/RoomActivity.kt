package com.example.tabletopcompanion.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopcompanion.R

class RoomActivity : AppCompatActivity() {

    private lateinit var roomNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        roomNameTextView = findViewById(R.id.roomNameTextView)

        val roomName = intent.getStringExtra("roomName")

        if (roomName != null && roomName.isNotEmpty()) {
            roomNameTextView.text = "Room: $roomName"
        } else {
            roomNameTextView.text = "Room: Error - Name not found"
            // Optionally, finish the activity or show an error dialog
            // Toast.makeText(this, "Error: Room name not provided", Toast.LENGTH_LONG).show()
            // finish()
        }
    }
}
