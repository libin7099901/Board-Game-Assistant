package com.example.tabletopcompanion.features.templatemanagement

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TemplateViewModelTest {

    private lateinit var viewModel: TemplateViewModel

    @Before
    fun setUp() {
        viewModel = TemplateViewModel()
    }

    @Test
    fun testInitialTemplatesList() {
        val expectedInitialTemplates = listOf("Template A", "Template B", "Template C (Default)")
        assertEquals("Initial templates list should match the default", expectedInitialTemplates, viewModel.templates)
    }

    @Test
    fun testImportTemplate_addNew() {
        val newTemplateName = "Template D"
        viewModel.importTemplate(newTemplateName)
        assertTrue("New template should be added to the list", viewModel.templates.contains(newTemplateName))
        assertEquals("List size should increase by 1", 4, viewModel.templates.size)
    }

    @Test
    fun testImportTemplate_addExisting() {
        val existingTemplateName = "Template A"
        val initialSize = viewModel.templates.size
        viewModel.importTemplate(existingTemplateName)
        assertTrue("Existing template should still be in the list", viewModel.templates.contains(existingTemplateName))
        assertEquals("List size should not change when adding existing template", initialSize, viewModel.templates.size)
    }

    @Test
    fun testDeleteTemplate_existing() {
        val templateToDelete = "Template B"
        val initialSize = viewModel.templates.size
        assertTrue("Template to delete should initially be in the list", viewModel.templates.contains(templateToDelete))

        viewModel.deleteTemplate(templateToDelete)

        assertFalse("Deleted template should no longer be in the list", viewModel.templates.contains(templateToDelete))
        assertEquals("List size should decrease by 1", initialSize - 1, viewModel.templates.size)
    }

    @Test
    fun testDeleteTemplate_nonExisting() {
        val nonExistingTemplate = "Template X"
        val initialSize = viewModel.templates.size
        assertFalse("Non-existing template should not be in the list initially", viewModel.templates.contains(nonExistingTemplate))

        viewModel.deleteTemplate(nonExistingTemplate)

        assertEquals("List size should not change when trying to delete non-existing template", initialSize, viewModel.templates.size)
    }
}
