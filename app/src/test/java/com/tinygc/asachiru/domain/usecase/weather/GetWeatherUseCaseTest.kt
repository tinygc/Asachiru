package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class GetWeatherUseCaseTest {

    @Mock
    private lateinit var weatherRepository: WeatherRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var useCase: GetWeatherUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GetWeatherUseCase(weatherRepository, settingsRepository)
    }

    @Test
    fun `invoke should return weather when repository returns success`() = runTest {
        // Arrange
        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)
        val expectedWeather = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 20,
            maxTemperature = 25,
            minTemperature = 15,
            precipitationProbability = 10
        )

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Success(expectedWeather))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedWeather, (result as Result.Success).data)
    }

    @Test
    fun `invoke should return error when repository returns error`() = runTest {
        // Arrange
        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)
        val exception = AppException.NetworkException("Network error")

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Error(exception))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `invoke should return error when settingsRepository throws exception`() = runTest {
        // Arrange
        val exception = RuntimeException("Failed to get settings")
        whenever(settingsRepository.getSettings()).thenThrow(exception)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `invoke should use postal code from settings`() = runTest {
        // Arrange
        val postalCode = "5300001"
        val settings = Settings(postalCode, 45)
        val expectedWeather = Weather(
            condition = WeatherCondition.RAINY,
            currentTemperature = 18,
            maxTemperature = 22,
            minTemperature = 16,
            precipitationProbability = 80
        )

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Success(expectedWeather))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedWeather, (result as Result.Success).data)
    }

    @Test
    fun `invoke should handle network exception from repository`() = runTest {
        // Arrange
        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)
        val networkException = AppException.NetworkException("Connection timeout")

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Error(networkException))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.NetworkException)
        assertEquals("Connection timeout", error.exception.message)
    }

    @Test
    fun `invoke should handle API exception from repository`() = runTest {
        // Arrange
        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)
        val apiException = AppException.ApiException(404, "City not found")

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Error(apiException))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.ApiException)
        assertEquals(404, (error.exception as AppException.ApiException).statusCode)
    }

    @Test
    fun `invoke should handle different weather conditions`() = runTest {
        // Arrange
        val conditions = listOf(
            WeatherCondition.SUNNY,
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY,
            WeatherCondition.SNOWY,
            WeatherCondition.OTHER
        )

        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)

        conditions.forEach { condition ->
            // Arrange
            val expectedWeather = Weather(
                condition = condition,
                currentTemperature = 20,
                maxTemperature = 25,
                minTemperature = 15,
                precipitationProbability = 30
            )

            whenever(settingsRepository.getSettings()).thenReturn(settings)
            whenever(weatherRepository.getWeather(postalCode))
                .thenReturn(Result.Success(expectedWeather))

            // Act
            val result = useCase()

            // Assert
            assertTrue("Result should be Success for $condition", result is Result.Success)
            assertEquals(condition, (result as Result.Success).data.condition)
        }
    }
}
