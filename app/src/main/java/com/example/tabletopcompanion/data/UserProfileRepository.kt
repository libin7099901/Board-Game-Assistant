package com.example.tabletopcompanion.data

import android.content.Context
import android.content.SharedPreferences

class UserProfileRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "username"
    }

    fun saveUsername(username: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }
}
