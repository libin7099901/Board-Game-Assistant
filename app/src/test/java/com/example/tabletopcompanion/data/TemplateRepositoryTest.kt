package com.example.tabletopcompanion.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tabletopcompanion.model.GameTemplateInfo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK]) // Using an older SDK to avoid issues with latest
class TemplateRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: TemplateRepository
    private val prefsName = "GameTemplatePrefs" // Same as in TemplateRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = TemplateRepository(context)
        // Clear SharedPreferences before each test
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun createDummyTemplate(id: String, name: String): GameTemplateInfo {
        return GameTemplateInfo(
            id = id,
            name = name,
            description = "Description for $name",
            version = "1.0",
            unzippedPath = "/path/to/$name",
            originalZipName = "$name.zip"
        )
    }

    @Test
    fun addAndGetTemplates_singleTemplate_returnsCorrectList() {
        val template1 = createDummyTemplate("id1", "Template1")
        repository.addTemplate(template1)

        val templates = repository.getTemplates()
        assertEquals(1, templates.size)
        assertEquals(template1, templates[0])
    }

    @Test
    fun addAndGetTemplates_multipleTemplates_returnsCorrectList() {
        val template1 = createDummyTemplate("id1", "Template1")
        val template2 = createDummyTemplate("id2", "Template2")
        repository.addTemplate(template1)
        repository.addTemplate(template2)

        val templates = repository.getTemplates()
        assertEquals(2, templates.size)
        assertTrue(templates.contains(template1))
        assertTrue(templates.contains(template2))
    }

    @Test
    fun getTemplates_noTemplates_returnsEmptyList() {
        val templates = repository.getTemplates()
        assertTrue(templates.isEmpty())
    }

    @Test
    fun deleteTemplate_templateExists_removesTemplate() {
        val template1 = createDummyTemplate("id1", "Template1")
        val template2 = createDummyTemplate("id2", "Template2")
        val template3 = createDummyTemplate("id3", "Template3")
        repository.addTemplate(template1)
        repository.addTemplate(template2)
        repository.addTemplate(template3)

        repository.deleteTemplate("id2")

        val templates = repository.getTemplates()
        assertEquals(2, templates.size)
        assertTrue(templates.contains(template1))
        assertFalse(templates.contains(template2))
        assertTrue(templates.contains(template3))
    }

    @Test
    fun deleteTemplate_templateNotExists_listUnchanged() {
        val template1 = createDummyTemplate("id1", "Template1")
        val template2 = createDummyTemplate("id2", "Template2")
        repository.addTemplate(template1)
        repository.addTemplate(template2)

        repository.deleteTemplate("id_non_existent")

        val templates = repository.getTemplates()
        assertEquals(2, templates.size)
        assertTrue(templates.contains(template1))
        assertTrue(templates.contains(template2))
    }

    @Test
    fun addTemplate_checkIdGeneration_ifIdExistsIsReplaced() {
        val initialId = "fixedId"
        val template1 = createDummyTemplate(initialId, "Template1")
        val template2 = createDummyTemplate(initialId, "Template2") // Same ID

        repository.addTemplate(template1)
        repository.addTemplate(template2) // This should trigger ID regeneration for template2

        val templates = repository.getTemplates()
        assertEquals(2, templates.size)

        val t1 = templates.find { it.name == "Template1" }
        val t2 = templates.find { it.name == "Template2" }

        assertNotNull(t1)
        assertNotNull(t2)
        assertEquals(initialId, t1!!.id) // First template keeps its ID
        assertNotEquals(initialId, t2!!.id) // Second template should have a new ID
        assertNotNull(UUID.fromString(t2.id)) // Check if the new ID is a valid UUID
    }
    
    @Test
    fun getTemplateById_templateExists_returnsTemplate() {
        val template1 = createDummyTemplate("id1", "Template1")
        repository.addTemplate(template1)

        val foundTemplate = repository.getTemplateById("id1")
        assertNotNull(foundTemplate)
        assertEquals(template1, foundTemplate)
    }

    @Test
    fun getTemplateById_templateNotExists_returnsNull() {
        val template1 = createDummyTemplate("id1", "Template1")
        repository.addTemplate(template1)

        val foundTemplate = repository.getTemplateById("id_non_existent")
        assertNull(foundTemplate)
    }


    @After
    fun tearDown() {
        // Clear SharedPreferences after each test
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
