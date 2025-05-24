package com.example.tabletopcompanion.data

import android.content.Context
import android.content.SharedPreferences

class RoomSettingsRepository(private val context: Context) {

    private val prefsName = "RoomSettingsPrefs"
    private val keySelectedTemplateId = "selectedTemplateId"

    private fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun saveSelectedTemplateId(templateId: String) {
        val editor = getSharedPreferences().edit()
        editor.putString(keySelectedTemplateId, templateId)
        editor.apply()
    }

    fun getSelectedTemplateId(): String? {
        val prefs = getSharedPreferences()
        return prefs.getString(keySelectedTemplateId, null)
    }

    fun clearSelectedTemplateId() {
        val editor = getSharedPreferences().edit()
        editor.remove(keySelectedTemplateId)
        editor.apply()
    }
}
