package com.example.tabletopcompanion.util

import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test

class TemplateManifestParserTest {

    @Test
    fun parseManifest_validJson_returnsCorrectData() {
        val jsonString = """
            {
                "name": "Test Game",
                "description": "A fun game.",
                "version": "1.0.1"
            }
        """.trimIndent()
        val parsedData = TemplateManifestParser.parseManifest(jsonString)
        assertEquals("Test Game", parsedData.name)
        assertEquals("A fun game.", parsedData.description)
        assertEquals("1.0.1", parsedData.version)
    }

    @Test(expected = ManifestParsingException::class)
    fun parseManifest_missingName_throwsException() {
        val jsonString = """
            {
                "description": "A game without a name.",
                "version": "1.0.0"
            }
        """.trimIndent()
        TemplateManifestParser.parseManifest(jsonString)
    }
    
    @Test(expected = ManifestParsingException::class)
    fun parseManifest_emptyName_throwsException() {
        val jsonString = """
            {
                "name": "",
                "description": "A game with an empty name.",
                "version": "1.0.0"
            }
        """.trimIndent()
        TemplateManifestParser.parseManifest(jsonString)
    }

    @Test(expected = ManifestParsingException::class)
    fun parseManifest_emptyJson_throwsException() {
        val jsonString = "{}"
        TemplateManifestParser.parseManifest(jsonString) // Should throw due to missing 'name'
    }

    @Test(expected = ManifestParsingException::class)
    fun parseManifest_malformedJson_throwsException() {
        val jsonString = """
            {
                "name": "Test Game",
                "description": "A fun game." 
                "version": "1.0.0" // Missing comma
            }
        """.trimIndent()
        TemplateManifestParser.parseManifest(jsonString)
    }

    @Test
    fun parseManifest_optionalFieldsMissing_parsesSuccessfully() {
        val jsonString = """
            {
                "name": "Minimal Game"
            }
        """.trimIndent()
        val parsedData = TemplateManifestParser.parseManifest(jsonString)
        assertEquals("Minimal Game", parsedData.name)
        assertNull(parsedData.description)
        assertNull(parsedData.version)
    }

    @Test
    fun parseManifest_descriptionNull_parsesSuccessfully() {
        val jsonString = """
            {
                "name": "Game With Null Desc",
                "description": null,
                "version": "1.1"
            }
        """.trimIndent()
        // org.json treats explicit null as a special JSONObject.NULL,
        // optString will convert this to null string if not found, or "null" string if value is JSONObject.NULL
        // We expect our parser to return null for the description field.
        val parsedData = TemplateManifestParser.parseManifest(jsonString)
        assertEquals("Game With Null Desc", parsedData.name)
        assertNull(parsedData.description) // optString should handle JSONObject.NULL and return null
        assertEquals("1.1", parsedData.version)
    }

    @Test
    fun parseManifest_versionNull_parsesSuccessfully() {
        val jsonString = """
            {
                "name": "Game With Null Ver",
                "description": "Desc here",
                "version": null
            }
        """.trimIndent()
        val parsedData = TemplateManifestParser.parseManifest(jsonString)
        assertEquals("Game With Null Ver", parsedData.name)
        assertEquals("Desc here", parsedData.description)
        assertNull(parsedData.version) // optString should handle JSONObject.NULL and return null
    }
}
