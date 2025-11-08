package com.tinygc.asachiru.presentation.splash

import com.tinygc.asachiru.domain.usecase.settings.CheckSettingsExistUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SplashViewModelのテスト
 */
class SplashViewModelTest {

    @Mock
    private lateinit var checkSettingsExistUseCase: CheckSettingsExistUseCase

    private lateinit var viewModel: SplashViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = SplashViewModel(checkSettingsExistUseCase)
    }

    @Test
    fun `checkSettings should return true when settings exist`() = runTest {
        // Given
        whenever(checkSettingsExistUseCase()).thenReturn(true)

        // When
        val result = viewModel.checkSettings()

        // Then
        assertTrue(result)
        verify(checkSettingsExistUseCase).invoke()
    }

    @Test
    fun `checkSettings should return false when settings do not exist`() = runTest {
        // Given
        whenever(checkSettingsExistUseCase()).thenReturn(false)

        // When
        val result = viewModel.checkSettings()

        // Then
        assertFalse(result)
        verify(checkSettingsExistUseCase).invoke()
    }

    @Test
    fun `checkSettings should call use case`() = runTest {
        // Given
        whenever(checkSettingsExistUseCase()).thenReturn(true)

        // When
        viewModel.checkSettings()

        // Then
        verify(checkSettingsExistUseCase).invoke()
    }

    @Test
    fun `multiple checkSettings calls should work correctly`() = runTest {
        // Given
        whenever(checkSettingsExistUseCase()).thenReturn(true)

        // When
        val result1 = viewModel.checkSettings()
        val result2 = viewModel.checkSettings()

        // Then
        assertTrue(result1)
        assertTrue(result2)
    }

    @Test
    fun `checkSettings should return different results based on use case`() = runTest {
        // Given
        whenever(checkSettingsExistUseCase()).thenReturn(false).thenReturn(true)

        // When
        val result1 = viewModel.checkSettings()
        val result2 = viewModel.checkSettings()

        // Then
        assertFalse(result1)
        assertTrue(result2)
    }
}
