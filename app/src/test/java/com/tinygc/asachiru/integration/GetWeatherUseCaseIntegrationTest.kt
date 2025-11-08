package com.tinygc.asachiru.integration

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.repository.WeatherRepository
import com.tinygc.asachiru.domain.usecase.weather.GetWeatherUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * GetWeatherUseCaseとRepositoryの統合テスト
 *
 * UseCase + Repository層の連携を検証します。
 * Repositoryはモックを使用し、UseCaseのロジックと
 * リポジトリとの統合を確認します。
 */
class GetWeatherUseCaseIntegrationTest {

    @Mock
    private lateinit var weatherRepository: WeatherRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var getWeatherUseCase: GetWeatherUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getWeatherUseCase = GetWeatherUseCase(weatherRepository, settingsRepository)
    }

    @Test
    fun `invoke should return weather when repositories return success`() = runTest {
        // Given
        val settings = Settings("1000001", 30)
        val weather = Weather(
            condition = com.tinygc.asachiru.domain.entity.WeatherCondition.SUNNY,
            temperature = 20,
            precipitation = 10
        )

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather("1000001")).thenReturn(Result.Success(weather))

        // When
        val result = getWeatherUseCase()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(weather, (result as Result.Success).data)
    }

    @Test
    fun `invoke should return error when settings repository fails`() = runTest {
        // Given
        val errorSettings = Settings("", 0) // Invalid settings

        whenever(settingsRepository.getSettings()).thenReturn(errorSettings)

        // When
        val result = getWeatherUseCase()

        // Then
        assertTrue(result is Result.Error)
    }

    @Test
    fun `invoke should return error when weather repository fails`() = runTest {
        // Given
        val settings = Settings("1000001", 30)
        val error = com.tinygc.asachiru.domain.common.AppException.NetworkException("Network error")

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather("1000001")).thenReturn(Result.Error(error))

        // When
        val result = getWeatherUseCase()

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Network error", (result as Result.Error).exception.message)
    }

    @Test
    fun `invoke should use postal code from settings`() = runTest {
        // Given
        val settings = Settings("9999999", 30)
        val weather = Weather(
            condition = com.tinygc.asachiru.domain.entity.WeatherCondition.RAINY,
            temperature = 15,
            precipitation = 80
        )

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather("9999999")).thenReturn(Result.Success(weather))

        // When
        val result = getWeatherUseCase()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(weather, (result as Result.Success).data)
        assertEquals(com.tinygc.asachiru.domain.entity.WeatherCondition.RAINY, weather.condition)
    }

    @Test
    fun `invoke should handle empty postal code from settings`() = runTest {
        // Given
        val settings = Settings("", 30)

        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // When
        val result = getWeatherUseCase()

        // Then
        assertTrue(result is Result.Error)
    }
}
