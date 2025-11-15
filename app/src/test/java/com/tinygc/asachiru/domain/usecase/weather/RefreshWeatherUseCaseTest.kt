package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class RefreshWeatherUseCaseTest {

    @Mock
    private lateinit var getWeatherUseCase: GetWeatherUseCase

    private lateinit var useCase: RefreshWeatherUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = RefreshWeatherUseCase(getWeatherUseCase)
    }

    @Test
    fun `invoke should return weather when getWeatherUseCase returns success`() = runTest {
        // Arrange
        val expectedWeather = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 20,
            maxTemperature = 25,
            minTemperature = 15,
            precipitationProbability = 10,
            dateLabel = "今日",
            iconText = "☀"
        )

        whenever(getWeatherUseCase()).thenReturn(Result.Success(expectedWeather))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedWeather, (result as Result.Success).data)
    }

    @Test
    fun `invoke should return error when getWeatherUseCase returns error`() = runTest {
        // Arrange
        val exception = AppException.NetworkException("Network error")
        whenever(getWeatherUseCase()).thenReturn(Result.Error(exception))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `invoke should delegate to getWeatherUseCase`() = runTest {
        // Arrange
        val weather = Weather(
            condition = WeatherCondition.CLOUDY,
            currentTemperature = 18,
            maxTemperature = 22,
            minTemperature = 14,
            precipitationProbability = 30,
            dateLabel = "今日",
            iconText = "☁"
        )
        whenever(getWeatherUseCase()).thenReturn(Result.Success(weather))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(weather, (result as Result.Success).data)
    }

    @Test
    fun `invoke should handle network exception from getWeatherUseCase`() = runTest {
        // Arrange
        val networkException = AppException.NetworkException("Connection timeout")
        whenever(getWeatherUseCase()).thenReturn(Result.Error(networkException))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.NetworkException)
        assertEquals("Connection timeout", error.exception.message)
    }

    @Test
    fun `invoke should handle API exception from getWeatherUseCase`() = runTest {
        // Arrange
        val apiException = AppException.ApiException(500, "Internal server error")
        whenever(getWeatherUseCase()).thenReturn(Result.Error(apiException))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.ApiException)
        assertEquals(500, (error.exception as AppException.ApiException).statusCode)
    }

    @Test
    fun `invoke should handle different weather conditions from getWeatherUseCase`() = runTest {
        // Arrange
        val conditions = listOf(
            WeatherCondition.SUNNY,
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY,
            WeatherCondition.SNOWY,
            WeatherCondition.OTHER
        )

        conditions.forEach { condition ->
            // Arrange
            val weather = Weather(
                condition = condition,
                currentTemperature = 20,
                maxTemperature = 25,
                minTemperature = 15,
                precipitationProbability = 20,
                dateLabel = "今日",
                iconText = "☀"
            )
            whenever(getWeatherUseCase()).thenReturn(Result.Success(weather))

            // Act
            val result = useCase()

            // Assert
            assertTrue("Result should be Success for $condition", result is Result.Success)
            assertEquals(condition, (result as Result.Success).data.condition)
        }
    }

    @Test
    fun `invoke should handle consecutive calls correctly`() = runTest {
        // Arrange
        val weather1 = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 25,
            maxTemperature = 30,
            minTemperature = 20,
            precipitationProbability = 0,
            dateLabel = "今日",
            iconText = "☀"
        )
        val weather2 = Weather(
            condition = WeatherCondition.RAINY,
            currentTemperature = 18,
            maxTemperature = 20,
            minTemperature = 15,
            precipitationProbability = 80,
            dateLabel = "今日",
            iconText = "☂"
        )

        whenever(getWeatherUseCase())
            .thenReturn(Result.Success(weather1))
            .thenReturn(Result.Success(weather2))

        // Act
        val result1 = useCase()
        val result2 = useCase()

        // Assert
        assertTrue(result1 is Result.Success)
        assertEquals(weather1, (result1 as Result.Success).data)

        assertTrue(result2 is Result.Success)
        assertEquals(weather2, (result2 as Result.Success).data)
    }
}
