package com.example.tabletopcompanion.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopcompanion.R
import com.example.tabletopcompanion.data.UserProfileRepository
import com.example.tabletopcompanion.model.UserProfile

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var avatarImageView: ImageView
    private lateinit var selectAvatarButton: Button
    private lateinit var saveProfileButton: Button

    private lateinit var userProfileRepository: UserProfileRepository
    private var selectedAvatarUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it
            avatarImageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        usernameEditText = findViewById(R.id.usernameEditText)
        avatarImageView = findViewById(R.id.avatarImageView)
        selectAvatarButton = findViewById(R.id.selectAvatarButton)
        saveProfileButton = findViewById(R.id.saveProfileButton)

        userProfileRepository = UserProfileRepository(this)

        loadProfile()

        selectAvatarButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        saveProfileButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfile() {
        val userProfile = userProfileRepository.loadUserProfile()
        usernameEditText.setText(userProfile.username)
        userProfile.avatarUri?.let {
            if (it.isNotEmpty()) {
                try {
                    val uri = Uri.parse(it)
                    avatarImageView.setImageURI(uri)
                    selectedAvatarUri = uri // Keep track of loaded URI
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading avatar: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Optionally, set a default avatar if loading fails
                    avatarImageView.setImageResource(R.drawable.ic_launcher_background) // Replace with your placeholder
                }
            } else {
                 avatarImageView.setImageResource(R.drawable.ic_launcher_background) // Replace with your placeholder
            }
        } ?: run {
            // Set a default placeholder if avatarUri is null
            avatarImageView.setImageResource(R.drawable.ic_launcher_background) // Replace with your placeholder
        }
    }

    private fun saveProfile() {
        val username = usernameEditText.text.toString().trim()
        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        val userProfile = UserProfile(username, selectedAvatarUri?.toString())
        userProfileRepository.saveUserProfile(userProfile)
        Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show()
        // Optionally, finish the activity
        // finish()
    }
}
