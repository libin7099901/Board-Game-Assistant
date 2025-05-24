package com.example.tabletopcompanion.util

import com.example.tabletopcompanion.model.GameTemplateInfo
import org.json.JSONException
import org.json.JSONObject

object TemplateManifestParser {

    // This class can be used to wrap parsed data before forming the full GameTemplateInfo
    data class ParsedManifestData(
        val name: String,
        val description: String?,
        val version: String?,
        val phases: List<String>,
        val initialIndicators: List<com.example.tabletopcompanion.model.template.ParsedIndicatorInfo>
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

            // Parse Phases
            val phasesList = mutableListOf<String>()
            val phasesArray = json.optJSONArray("phases")
            if (phasesArray != null) {
                for (i in 0 until phasesArray.length()) {
                    try {
                        val phase = phasesArray.getString(i)
                        if (!phase.isNullOrEmpty()) { // Ensure phase is not null or empty
                           phasesList.add(phase)
                        }
                        // else: skip null/empty phase names silently or log
                    } catch (e: JSONException) {
                        // Log or handle type error for a phase entry, e.g., skip
                    }
                }
            }

            // Parse Initial Indicators
            val indicatorsList = mutableListOf<com.example.tabletopcompanion.model.template.ParsedIndicatorInfo>()
            val indicatorsArray = json.optJSONArray("initialIndicators")
            if (indicatorsArray != null) {
                for (i in 0 until indicatorsArray.length()) {
                    try {
                        val indicatorObject = indicatorsArray.getJSONObject(i)
                        val indicatorName = indicatorObject.optString("name")
                        val indicatorValue = indicatorObject.optString("initialValue")
                        val indicatorType = indicatorObject.optString("type")

                        if (indicatorName.isNotEmpty() && indicatorValue.isNotEmpty() && indicatorType.isNotEmpty()) {
                            indicatorsList.add(
                                com.example.tabletopcompanion.model.template.ParsedIndicatorInfo(
                                    indicatorName,
                                    indicatorValue,
                                    indicatorType
                                )
                            )
                        }
                        // else: skip malformed indicator (missing required fields)
                    } catch (e: JSONException) {
                        // Log or handle type error for an indicator entry, e.g., skip
                    }
                }
            }

            return ParsedManifestData(name, description, version, phasesList, indicatorsList)
        } catch (e: JSONException) {
            // Re-throw JSONException for overall syntax issues
            throw ManifestParsingException("Error parsing game_info.json: ${e.message}", e)
        }
    }
}

class ManifestParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)
