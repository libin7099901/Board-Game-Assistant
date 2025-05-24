package com.example.tabletopcompanion.ui

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.tabletopcompanion.R
import com.example.tabletopcompanion.data.RoomSettingsRepository
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.model.GameTemplateInfo
import com.example.tabletopcompanion.model.game.GameIndicator
import com.example.tabletopcompanion.model.game.GameState
import com.example.tabletopcompanion.model.template.ParsedIndicatorInfo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK]) // Using a consistent SDK for Robolectric
class RoomViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockApplication: Application
    @Mock
    private lateinit var mockTemplateRepository: TemplateRepository
    @Mock
    private lateinit var mockRoomSettingsRepository: RoomSettingsRepository

    private lateinit var viewModel: RoomViewModel

    // Observers
    @Mock private lateinit var gameStateObserver: Observer<GameState?>
    @Mock private lateinit var gameIndicatorsObserver: Observer<List<GameIndicator>>
    @Mock private lateinit var currentPhaseNameDisplayObserver: Observer<String>


    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock application context and string resources
        `when`(mockApplication.applicationContext).thenReturn(mock(Context::class.java))
        `when`(mockApplication.getString(R.string.room_no_template_loaded)).thenReturn("No template loaded.")
        `when`(mockApplication.getString(R.string.room_no_template_selected_error)).thenReturn("No template selected. Please select one from Manage Templates.")
        `when`(mockApplication.getString(R.string.room_no_template_id_provided_error)).thenReturn("Error: No template ID provided to load.")
        `when`(mockApplication.getString(R.string.room_template_not_found_error, anyString())).thenAnswer { "Error: Template not found (ID: ${it.arguments[1]})." }
        `when`(mockApplication.getString(R.string.room_phase_display_prefix, anyString())).thenAnswer { "Phase: ${it.arguments[1]}" }
        `when`(mockApplication.getString(R.string.room_no_phases_in_template_error)).thenReturn("No phases in current template.")
        `when`(mockApplication.getString(R.string.room_game_state_not_initialized_error)).thenReturn("Error: Game state not initialized.")


        viewModel = RoomViewModel(mockApplication)
        // Manually inject mocks. This is a common pattern if DI is not used for ViewModels.
        // This requires reflection or making fields internal/public for test purposes.
        // For this example, let's assume we can modify RoomViewModel to allow this or use a TestViewModelFactory.
        // As a simple workaround for now, we'll re-initialize repositories inside ViewModel in this test context if they were private.
        // However, the provided RoomViewModel structure instantiates them directly.
        // So, we need to ensure the ViewModel uses *our* mocked instances.
        // This is typically done via constructor injection or a public setter in ViewModel.
        // Since RoomViewModel(application) initializes them, we'll rely on its internal structure
        // and mock the behavior of those *real* repositories if they are accessed via Application context.
        // For this exercise, I'll assume the constructor of RoomViewModel is:
        // class RoomViewModel(application: Application, private val templateRepository: TemplateRepository, private val roomSettingsRepository: RoomSettingsRepository)
        // If not, the test setup would be different (more integration-like).
        // Re-adjusting based on the actual RoomViewModel: it initializes repos internally.
        // This means we can't directly inject mocked repos without changing RoomViewModel.
        // The test will then be more of an integration test for the ViewModel with its actual repo instances.
        // To make it a unit test, RoomViewModel's design would need to change to accept repo instances.
        // For now, I'll proceed as if we *could* inject them, using a hypothetical modified constructor/setter.
        // The following lines would only work if RoomViewModel was refactored for DI:
        // viewModel.templateRepository = mockTemplateRepository
        // viewModel.roomSettingsRepository = mockRoomSettingsRepository
        // Since we can't do that without editing RoomViewModel, the mocks for repos will be used to define behavior,
        // and we trust that if RoomViewModel used these (via DI), the test would be valid.
        // The alternative is to use Robolectric to control SharedPreferences for the *actual* repo instances.

        // For the purpose of this exercise, we will mock the repositories' methods as if they were injected.
        // This is a common approach in testing even if full DI isn't implemented, by preparing the environment.
        val templateRepositoryField = RoomViewModel::class.java.getDeclaredField("templateRepository")
        templateRepositoryField.isAccessible = true
        templateRepositoryField.set(viewModel, mockTemplateRepository)

        val roomSettingsRepositoryField = RoomViewModel::class.java.getDeclaredField("roomSettingsRepository")
        roomSettingsRepositoryField.isAccessible = true
        roomSettingsRepositoryField.set(viewModel, mockRoomSettingsRepository)

        // Setup observers
        viewModel.gameState.observeForever(gameStateObserver)
        viewModel.gameIndicators.observeForever(gameIndicatorsObserver)
        viewModel.currentPhaseNameDisplay.observeForever(currentPhaseNameDisplayObserver)
    }

    private fun createSampleGameTemplateInfo(id: String, nameSuffix: String = "Test Template", phases: List<String>, indicators: List<ParsedIndicatorInfo>): GameTemplateInfo {
        return GameTemplateInfo(id = id, name = "$nameSuffix $id", phases = phases, initialIndicators = indicators, unzippedPath = "/path/$id", originalZipName = "$id.zip")
    }

    @Test
    fun loadInitialRoomState_selectedIdExists_loadsTemplate() {
        val templateId = "test_id_1"
        val phases = listOf("Phase1", "Phase2")
        val indicators = listOf(ParsedIndicatorInfo("Indi1", "Val1", "text"))
        val sampleTemplate = createSampleGameTemplateInfo(templateId, "InitialLoad", phases, indicators)

        `when`(mockRoomSettingsRepository.getSelectedTemplateId()).thenReturn(templateId)
        `when`(mockTemplateRepository.getTemplateById(templateId)).thenReturn(sampleTemplate)

        viewModel.loadInitialRoomState()

        // Verify LiveData updates
        verify(currentPhaseNameDisplayObserver).onChanged("Phase: Phase1")
        verify(gameStateObserver).onChanged(
            GameState(
                activeTemplateId = templateId,
                currentRound = 1,
                currentPhaseName = "Phase1",
                currentPlayerId = null
            )
        )
        verify(gameIndicatorsObserver).onChanged(
            listOf(GameIndicator("Indi1", "Val1", "text"))
        )

        assertEquals(templateId, viewModel.gameState.value?.activeTemplateId)
        assertEquals("Phase1", viewModel.gameState.value?.currentPhaseName)
        assertEquals(1, viewModel.gameIndicators.value?.size)
        assertEquals("Indi1", viewModel.gameIndicators.value?.get(0)?.name)
    }

    @Test
    fun loadInitialRoomState_noSelectedId_setsErrorState() {
        `when`(mockRoomSettingsRepository.getSelectedTemplateId()).thenReturn(null)

        viewModel.loadInitialRoomState()

        verify(currentPhaseNameDisplayObserver).onChanged("No template selected. Please select one from Manage Templates.")
        verify(gameStateObserver).onChanged(null)
        verify(gameIndicatorsObserver).onChanged(emptyList())
        assertNull(viewModel.gameState.value)
    }

    @Test
    fun loadTemplateAndInitGame_nullId_setsErrorState() {
        viewModel.loadTemplateAndInitGame(null)

        verify(currentPhaseNameDisplayObserver).onChanged("Error: No template ID provided to load.")
        verify(gameStateObserver).onChanged(null)
        verify(gameIndicatorsObserver).onChanged(emptyList())
        assertNull(viewModel.gameState.value)
    }

    @Test
    fun loadTemplateAndInitGame_validId_templateNotFound_setsErrorState() {
        val templateId = "bad_id"
        `when`(mockTemplateRepository.getTemplateById(templateId)).thenReturn(null)

        viewModel.loadTemplateAndInitGame(templateId)

        verify(currentPhaseNameDisplayObserver).onChanged("Error: Template not found (ID: $templateId).")
        verify(gameStateObserver).onChanged(null)
        verify(gameIndicatorsObserver).onChanged(emptyList())
        assertNull(viewModel.gameState.value)
    }

    @Test
    fun loadTemplateAndInitGame_templateNoPhases_initializesCorrectly() {
        val templateId = "id_no_phases"
        val sampleTemplate = createSampleGameTemplateInfo(templateId, "NoPhases", emptyList(), emptyList())
        `when`(mockTemplateRepository.getTemplateById(templateId)).thenReturn(sampleTemplate)

        viewModel.loadTemplateAndInitGame(templateId)
        
        verify(currentPhaseNameDisplayObserver).onChanged("Phase: N/A")
        assertEquals("N/A", viewModel.gameState.value?.currentPhaseName)
        assertTrue(viewModel.gameIndicators.value?.isEmpty() == true)
    }

    @Test
    fun loadTemplateAndInitGame_templateNoIndicators_initializesCorrectly() {
        val templateId = "id_no_indis"
        val sampleTemplate = createSampleGameTemplateInfo(templateId, "NoIndicators", listOf("P1"), emptyList())
        `when`(mockTemplateRepository.getTemplateById(templateId)).thenReturn(sampleTemplate)

        viewModel.loadTemplateAndInitGame(templateId)

        verify(gameIndicatorsObserver).onChanged(emptyList())
        assertTrue(viewModel.gameIndicators.value?.isEmpty() == true)
        assertEquals("Phase: P1", viewModel.currentPhaseNameDisplay.value) // Check phase name from observer
    }

    @Test
    fun nextPhase_noTemplateLoaded_stateUnchanged() {
        // Initial state set by constructor mock
        `when`(mockApplication.getString(R.string.room_no_template_loaded)).thenReturn("Initial: No template loaded.")
        val initialViewModel = RoomViewModel(mockApplication) // Re-init to get constructor-set value
        initialViewModel.currentPhaseNameDisplay.observeForever(currentPhaseNameDisplayObserver)

        // Ensure it starts in an error/unloaded state for this specific test
        `when`(mockRoomSettingsRepository.getSelectedTemplateId()).thenReturn(null)
        viewModel.loadInitialRoomState() // This will set it to "No template selected..."

        val initialDisplayValue = viewModel.currentPhaseNameDisplay.value
        viewModel.nextPhase() // Attempt to advance phase

        assertEquals(initialDisplayValue, viewModel.currentPhaseNameDisplay.value) // Should remain "No template selected..."
        verify(currentPhaseNameDisplayObserver, times(1)).onChanged("No template selected. Please select one from Manage Templates.")
        // times(1) because loadInitialRoomState sets it, nextPhase shouldn't change it if no phases.
    }

    @Test
    fun nextPhase_templateNoPhases_stateUnchanged() {
        val templateId = "no_phases_template"
        val sampleTemplate = createSampleGameTemplateInfo(templateId, "NoPhasesCycle", emptyList(), emptyList())
        `when`(mockTemplateRepository.getTemplateById(templateId)).thenReturn(sampleTemplate)

        viewModel.loadTemplateAndInitGame(templateId) // Loads template with no phases
        val initialPhaseDisplay = viewModel.currentPhaseNameDisplay.value // Should be "Phase: N/A"

        viewModel.nextPhase() // Attempt to advance phase

        assertEquals(initialPhaseDisplay, viewModel.currentPhaseNameDisplay.value)
        verify(currentPhaseNameDisplayObserver).onChanged("Phase: N/A") // From load
        verify(currentPhaseNameDisplayObserver).onChanged("No phases in current template.") // From nextPhase
    }


    @Test
    fun nextPhase_cyclesThroughPhasesCorrectly() {
        val templateId = "cycle_phases_template"
        val phases = listOf("Phase A", "Phase B", "Phase C")
        val sampleTemplate = createSampleGameTemplateInfo(templateId, "Cycle", phases, emptyList())
        `when`(mockTemplateRepository.getTemplateById(templateId)).thenReturn(sampleTemplate)

        viewModel.loadTemplateAndInitGame(templateId)
        assertEquals("Phase: Phase A", viewModel.currentPhaseNameDisplay.value)
        assertEquals("Phase A", viewModel.gameState.value?.currentPhaseName)

        viewModel.nextPhase()
        assertEquals("Phase: Phase B", viewModel.currentPhaseNameDisplay.value)
        assertEquals("Phase B", viewModel.gameState.value?.currentPhaseName)

        viewModel.nextPhase()
        assertEquals("Phase: Phase C", viewModel.currentPhaseNameDisplay.value)
        assertEquals("Phase C", viewModel.gameState.value?.currentPhaseName)

        viewModel.nextPhase() // Cycle back to A
        assertEquals("Phase: Phase A", viewModel.currentPhaseNameDisplay.value)
        assertEquals("Phase A", viewModel.gameState.value?.currentPhaseName)
    }
}
