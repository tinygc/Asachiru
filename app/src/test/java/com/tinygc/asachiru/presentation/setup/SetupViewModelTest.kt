package com.tinygc.asachiru.presentation.setup

import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.usecase.settings.SaveSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SetupViewModelのテスト
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @Mock
    private lateinit var saveSettingsUseCase: SaveSettingsUseCase

    private lateinit var viewModel: SetupViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SetupViewModel(saveSettingsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() {
        // Given & When
        val state = viewModel.uiState.value

        // Then
        assertEquals("", state.postalCode)
        assertEquals(30, state.newsInterval)
        assertTrue(state.isPostalCodeValid)
        assertTrue(state.isNewsIntervalValid)
        assertFalse(state.isSaving)
        assertNull(state.saveError)
        assertFalse(state.isComplete)
    }

    @Test
    fun `updatePostalCode should update postal code and validation`() {
        // Given
        val postalCode = "1000001"

        // When
        viewModel.updatePostalCode(postalCode)

        // Then
        val state = viewModel.uiState.value
        assertEquals(postalCode, state.postalCode)
        assertTrue(state.isPostalCodeValid)
    }

    @Test
    fun `updatePostalCode with invalid code should set validation to false`() {
        // Given
        val postalCode = "123" // 7桁未満

        // When
        viewModel.updatePostalCode(postalCode)

        // Then
        val state = viewModel.uiState.value
        assertEquals(postalCode, state.postalCode)
        assertFalse(state.isPostalCodeValid)
    }

    @Test
    fun `updatePostalCode with non-digit should set validation to false`() {
        // Given
        val postalCode = "100000A" // 数字以外を含む

        // When
        viewModel.updatePostalCode(postalCode)

        // Then
        val state = viewModel.uiState.value
        assertEquals(postalCode, state.postalCode)
        assertFalse(state.isPostalCodeValid)
    }

    @Test
    fun `updateNewsInterval should update news interval and validation`() {
        // Given
        val interval = 45

        // When
        viewModel.updateNewsInterval(interval)

        // Then
        val state = viewModel.uiState.value
        assertEquals(interval, state.newsInterval)
        assertTrue(state.isNewsIntervalValid)
    }

    @Test
    fun `updateNewsInterval with value less than 1 should set validation to false`() {
        // Given
        val interval = 0

        // When
        viewModel.updateNewsInterval(interval)

        // Then
        val state = viewModel.uiState.value
        assertEquals(interval, state.newsInterval)
        assertFalse(state.isNewsIntervalValid)
    }

    @Test
    fun `updateNewsInterval with value greater than 60 should set validation to false`() {
        // Given
        val interval = 61

        // When
        viewModel.updateNewsInterval(interval)

        // Then
        val state = viewModel.uiState.value
        assertEquals(interval, state.newsInterval)
        assertFalse(state.isNewsIntervalValid)
    }

    @Test
    fun `saveSettings should save valid settings successfully`() = runTest {
        // Given
        viewModel.updatePostalCode("1000001")
        viewModel.updateNewsInterval(30)
        whenever(saveSettingsUseCase(any())).thenReturn(Result.Success(Unit))

        // When
        viewModel.saveSettings()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.saveError)
        assertTrue(state.isComplete)
        verify(saveSettingsUseCase).invoke(Settings("1000001", 30))
    }

    @Test
    fun `saveSettings should set error when save fails`() = runTest {
        // Given
        viewModel.updatePostalCode("1000001")
        viewModel.updateNewsInterval(30)
        val errorMessage = "Failed to save settings"
        whenever(saveSettingsUseCase(any())).thenReturn(
            Result.Error(AppException.SettingsException(errorMessage))
        )

        // When
        viewModel.saveSettings()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(errorMessage, state.saveError)
        assertFalse(state.isComplete)
    }

    @Test
    fun `saveSettings should not save when postal code is invalid`() = runTest {
        // Given
        viewModel.updatePostalCode("123") // 不正な郵便番号
        viewModel.updateNewsInterval(30)

        // When
        viewModel.saveSettings()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isComplete)
    }

    @Test
    fun `saveSettings should not save when news interval is invalid`() = runTest {
        // Given
        viewModel.updatePostalCode("1000001")
        viewModel.updateNewsInterval(0) // 不正な間隔

        // When
        viewModel.saveSettings()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isComplete)
    }

    @Test
    fun `saveSettings should set isSaving to true during save`() = runTest {
        // Given
        viewModel.updatePostalCode("1000001")
        viewModel.updateNewsInterval(30)
        whenever(saveSettingsUseCase(any())).thenReturn(Result.Success(Unit))

        // When
        viewModel.saveSettings()

        // Then (保存処理開始直後)
        assertTrue(viewModel.uiState.value.isSaving)

        // Then (保存処理完了後)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `updatePostalCode with empty string should set validation to false`() {
        // When
        viewModel.updatePostalCode("")

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.postalCode)
        assertFalse(state.isPostalCodeValid)
    }

    @Test
    fun `updatePostalCode with 8 digits should set validation to false`() {
        // When
        viewModel.updatePostalCode("10000011") // 8桁

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isPostalCodeValid)
    }

    @Test
    fun `updateNewsInterval with boundary value 1 should be valid`() {
        // When
        viewModel.updateNewsInterval(1)

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.newsInterval)
        assertTrue(state.isNewsIntervalValid)
    }

    @Test
    fun `updateNewsInterval with boundary value 60 should be valid`() {
        // When
        viewModel.updateNewsInterval(60)

        // Then
        val state = viewModel.uiState.value
        assertEquals(60, state.newsInterval)
        assertTrue(state.isNewsIntervalValid)
    }

    @Test
    fun `updateNewsInterval with negative value should set validation to false`() {
        // When
        viewModel.updateNewsInterval(-1)

        // Then
        val state = viewModel.uiState.value
        assertEquals(-1, state.newsInterval)
        assertFalse(state.isNewsIntervalValid)
    }

    @Test
    fun `multiple updatePostalCode calls should update state correctly`() {
        // When
        viewModel.updatePostalCode("123")
        viewModel.updatePostalCode("1234567")

        // Then
        val state = viewModel.uiState.value
        assertEquals("1234567", state.postalCode)
        assertTrue(state.isPostalCodeValid)
    }

    @Test
    fun `multiple updateNewsInterval calls should update state correctly`() {
        // When
        viewModel.updateNewsInterval(10)
        viewModel.updateNewsInterval(50)

        // Then
        val state = viewModel.uiState.value
        assertEquals(50, state.newsInterval)
        assertTrue(state.isNewsIntervalValid)
    }
}
