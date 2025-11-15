package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CheckSettingsExistUseCaseTest {

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var useCase: CheckSettingsExistUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = CheckSettingsExistUseCase(settingsRepository)
    }

    @Test
    fun `invoke should return true when settings exist`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings()).thenReturn(true)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result)
        verify(settingsRepository).hasSettings()
    }

    @Test
    fun `invoke should return false when settings do not exist`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings()).thenReturn(false)

        // Act
        val result = useCase()

        // Assert
        assertFalse(result)
        verify(settingsRepository).hasSettings()
    }

    @Test
    fun `invoke should call repository hasSettings method`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings()).thenReturn(true)

        // Act
        useCase()

        // Assert
        verify(settingsRepository).hasSettings()
    }

    @Test
    fun `invoke should directly delegate to repository`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings()).thenReturn(false)

        // Act
        val result = useCase()

        // Assert
        assertFalse(result)
        verify(settingsRepository).hasSettings()
    }

    @Test
    fun `invoke should handle consecutive calls correctly`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings())
            .thenReturn(false)  // First call: no settings
            .thenReturn(true)   // Second call: settings exist

        // Act
        val result1 = useCase()
        val result2 = useCase()

        // Assert
        assertFalse(result1)
        assertTrue(result2)
    }

    @Test
    fun `invoke should return consistent result from repository`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings()).thenReturn(true)

        // Act
        val result1 = useCase()
        val result2 = useCase()
        val result3 = useCase()

        // Assert
        assertTrue(result1)
        assertTrue(result2)
        assertTrue(result3)
    }

    @Test
    fun `invoke should properly handle false result`() = runTest {
        // Arrange
        whenever(settingsRepository.hasSettings()).thenReturn(false)

        // Act
        val result = useCase()

        // Assert
        assertFalse(result)
        verify(settingsRepository).hasSettings()
    }

    @Test
    fun `invoke should work in different scenarios`() = runTest {
        // Test multiple scenarios
        val scenarios = listOf(true, false, true, true, false)

        scenarios.forEach { expectedResult ->
            // Arrange
            whenever(settingsRepository.hasSettings()).thenReturn(expectedResult)

            // Act
            val result = useCase()

            // Assert
            assertEquals(expectedResult, result)
        }
    }
}
