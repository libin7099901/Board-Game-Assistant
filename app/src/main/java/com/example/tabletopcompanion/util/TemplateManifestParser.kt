package com.example.tabletopcompanion.util

import com.example.tabletopcompanion.model.GameTemplateInfo
import org.json.JSONException
import org.json.JSONObject

object TemplateManifestParser {

    // This class can be used to wrap parsed data before forming the full GameTemplateInfo
    data class ParsedManifestData(
        val name: String,
        val description: String?,
        val version: String?
    )

    @Throws(JSONException::class, ManifestParsingException::class)
    fun parseManifest(jsonString: String): ParsedManifestData {
        try {
            val json = JSONObject(jsonString)

            if (!json.has("name")) {
                throw ManifestParsingException("Required field 'name' is missing from game_info.json.")
            }
            val name = json.getString("name")
            if (name.isNullOrEmpty()){
                throw ManifestParsingException("Required field 'name' cannot be null or empty in game_info.json.")
            }

            val description = json.optString("description", null)
            val version = json.optString("version", null)

            return ParsedManifestData(name, description, version)
        } catch (e: JSONException) {
            // Re-throw JSONException if it's a syntax issue, or wrap if it's a missing field handled by optString
            throw ManifestParsingException("Error parsing game_info.json: ${e.message}", e)
        }
    }
}

class ManifestParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)
