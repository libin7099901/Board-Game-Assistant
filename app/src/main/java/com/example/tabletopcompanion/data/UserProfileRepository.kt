package com.example.tabletopcompanion.data

import android.content.Context
import android.content.SharedPreferences

class UserProfileRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id" // Added user_id key
    }

    fun saveUsername(username: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    // Updated getUsername to provide a default value as per previous subtasks
    fun getUsername(): String = sharedPreferences.getString(KEY_USERNAME, null) ?: "User_${UUID.randomUUID().toString().substring(0, 4)}" ?: "User_Anon"


    fun getCurrentUserId(): String {
        var userId = sharedPreferences.getString(KEY_USER_ID, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            with(sharedPreferences.edit()) {
                putString(KEY_USER_ID, userId)
                apply()
            }
        }
        return userId
    }
}
