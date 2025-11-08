package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.remote.WeatherApiDataSource
import com.tinygc.asachiru.data.dto.ForecastDto
import com.tinygc.asachiru.data.dto.TemperatureDto
import com.tinygc.asachiru.data.dto.TemperatureValueDto
import com.tinygc.asachiru.data.dto.WeatherApiResponse
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.WeatherCondition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private lateinit var weatherApiDataSource: WeatherApiDataSource
    private lateinit var repository: WeatherRepositoryImpl

    @Before
    fun setUp() {
        weatherApiDataSource = mock()
        repository = WeatherRepositoryImpl(weatherApiDataSource)
    }

    @Test
    fun `getWeather should return Success with valid postal code`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "晴れ",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "25"),
                        min = TemperatureValueDto(celsius = "15")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(WeatherCondition.SUNNY, weather.condition)
        assertEquals(25, weather.currentTemperature)
        assertEquals(25, weather.maxTemperature)
        assertEquals(15, weather.minTemperature)
        assertEquals(0, weather.precipitationProbability)
    }

    @Test
    fun `getWeather should convert cloudy weather condition correctly`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "曇り",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "20"),
                        min = TemperatureValueDto(celsius = "10")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(WeatherCondition.CLOUDY, weather.condition)
    }

    @Test
    fun `getWeather should convert rainy weather condition correctly`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "雨",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "18"),
                        min = TemperatureValueDto(celsius = "12")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(WeatherCondition.RAINY, weather.condition)
    }

    @Test
    fun `getWeather should convert snowy weather condition correctly`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "雪",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "5"),
                        min = TemperatureValueDto(celsius = "-2")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(WeatherCondition.SNOWY, weather.condition)
    }

    @Test
    fun `getWeather should handle null temperature values`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "晴れ",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = null),
                        min = TemperatureValueDto(celsius = null)
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(0, weather.currentTemperature)
        assertEquals(0, weather.maxTemperature)
        assertEquals(0, weather.minTemperature)
    }

    @Test
    fun `getWeather should return NetworkException on IOException`() = runTest {
        // Given
        val postalCode = "1000001"
        whenever(weatherApiDataSource.fetchWeather(any())).thenThrow(IOException("Network error"))

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.NetworkException)
        assertEquals("Network error", exception.message)
    }

    @Test
    fun `getWeather should return ApiException on invalid postal code`() = runTest {
        // Given
        val invalidPostalCode = "0000000"

        // When
        val result = repository.getWeather(invalidPostalCode)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.ApiException)
    }

    @Test
    fun `getWeather should return NetworkException when no forecast data available`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(forecasts = emptyList())
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.NetworkException)
    }

    @Test
    fun `getWeather should return ParseException on generic Exception`() = runTest {
        // Given
        val postalCode = "1000001"
        whenever(weatherApiDataSource.fetchWeather(any())).thenThrow(RuntimeException("Parse error"))

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.ParseException)
        assertEquals("Parse error", exception.message)
    }

    @Test
    fun `getWeather should convert postal code to area code correctly`() = runTest {
        // Given
        val postalCode = "0600000"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "晴れ",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "22"),
                        min = TemperatureValueDto(celsius = "12")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("016000")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        verify(weatherApiDataSource).fetchWeather("016000")
    }

    @Test
    fun `getWeather should use first forecast from response`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "晴れ",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "25"),
                        min = TemperatureValueDto(celsius = "15")
                    )
                ),
                ForecastDto(
                    telop = "雨",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "20"),
                        min = TemperatureValueDto(celsius = "10")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(WeatherCondition.SUNNY, weather.condition) // Should use first forecast
    }

    @Test
    fun `getWeather should handle invalid temperature format`() = runTest {
        // Given
        val postalCode = "1000001"
        val mockResponse = WeatherApiResponse(
            forecasts = listOf(
                ForecastDto(
                    telop = "晴れ",
                    temperature = TemperatureDto(
                        max = TemperatureValueDto(celsius = "invalid"),
                        min = TemperatureValueDto(celsius = "also invalid")
                    )
                )
            )
        )
        whenever(weatherApiDataSource.fetchWeather("130010")).thenReturn(mockResponse)

        // When
        val result = repository.getWeather(postalCode)

        // Then
        assertTrue(result is Result.Success)
        val weather = (result as Result.Success).data
        assertEquals(0, weather.currentTemperature)
        assertEquals(0, weather.maxTemperature)
        assertEquals(0, weather.minTemperature)
    }
}
