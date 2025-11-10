package com.tinygc.asachiru.data.dto

import com.tinygc.asachiru.domain.entity.WeatherCondition
import org.junit.Assert.*
import org.junit.Test

class WeatherDtoTest {

    @Test
    fun `toEntity should convert ForecastDto to Weather entity`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(
                min = TemperatureValueDto("15"),
                max = TemperatureValueDto("25")
            )
        )

        // Act
        val weather = forecastDto.toEntity(currentTemp = 20, precipProb = 10)

        // Assert
        assertEquals(WeatherCondition.SUNNY, weather.condition)
        assertEquals(20, weather.currentTemperature)
        assertEquals(25, weather.maxTemperature)
        assertEquals(15, weather.minTemperature)
        assertEquals(10, weather.precipitationProbability)
    }

    @Test
    fun `toEntity should parse sunny weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(null, null)
        )

        // Act
        val weather = forecastDto.toEntity(0, 0)

        // Assert
        assertEquals(WeatherCondition.SUNNY, weather.condition)
    }

    @Test
    fun `toEntity should parse cloudy weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "曇り",
            temperature = TemperatureDto(null, null)
        )

        // Act
        val weather = forecastDto.toEntity(0, 0)

        // Assert
        assertEquals(WeatherCondition.CLOUDY, weather.condition)
    }

    @Test
    fun `toEntity should parse rainy weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "雨",
            temperature = TemperatureDto(null, null)
        )

        // Act
        val weather = forecastDto.toEntity(0, 0)

        // Assert
        assertEquals(WeatherCondition.RAINY, weather.condition)
    }

    @Test
    fun `toEntity should parse snowy weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "雪",
            temperature = TemperatureDto(null, null)
        )

        // Act
        val weather = forecastDto.toEntity(0, 0)

        // Assert
        assertEquals(WeatherCondition.SNOWY, weather.condition)
    }

    @Test
    fun `toEntity should parse other weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "その他の天気",
            temperature = TemperatureDto(null, null)
        )

        // Act
        val weather = forecastDto.toEntity(0, 0)

        // Assert
        assertEquals(WeatherCondition.OTHER, weather.condition)
    }

    @Test
    fun `toEntity should handle null min temperature`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(
                min = null,
                max = TemperatureValueDto("25")
            )
        )

        // Act
        val weather = forecastDto.toEntity(20, 10)

        // Assert
        assertEquals(0, weather.minTemperature)
        assertEquals(25, weather.maxTemperature)
    }

    @Test
    fun `toEntity should handle null max temperature`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(
                min = TemperatureValueDto("15"),
                max = null
            )
        )

        // Act
        val weather = forecastDto.toEntity(20, 10)

        // Assert
        assertEquals(15, weather.minTemperature)
        assertEquals(0, weather.maxTemperature)
    }

    @Test
    fun `toEntity should handle invalid temperature strings`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(
                min = TemperatureValueDto("invalid"),
                max = TemperatureValueDto("not_a_number")
            )
        )

        // Act
        val weather = forecastDto.toEntity(20, 10)

        // Assert
        assertEquals(0, weather.minTemperature)
        assertEquals(0, weather.maxTemperature)
    }

    @Test
    fun `toEntity should handle mixed weather conditions with priority`() {
        // Arrange
        // 悪い天気を優先（雪 > 雨 > 曇り > 晴れ）
        val conditions = listOf(
            "晴れのち曇り" to WeatherCondition.CLOUDY,
            "曇りのち雨" to WeatherCondition.RAINY,
            "雨のち雪" to WeatherCondition.SNOWY,
            "雪のち晴れ" to WeatherCondition.SNOWY
        )

        conditions.forEach { (telop, expectedCondition) ->
            val forecastDto = ForecastDto(
                date = "2025-11-07",
                telop = telop,
                temperature = TemperatureDto(null, null)
            )

            // Act
            val weather = forecastDto.toEntity(0, 0)

            // Assert
            assertEquals("Telop: $telop", expectedCondition, weather.condition)
        }
    }

    @Test
    fun `toEntity should use provided current temperature and precipitation probability`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(
                min = TemperatureValueDto("10"),
                max = TemperatureValueDto("30")
            )
        )

        // Act
        val weather = forecastDto.toEntity(currentTemp = 22, precipProb = 80)

        // Assert
        assertEquals(22, weather.currentTemperature)
        assertEquals(80, weather.precipitationProbability)
    }

    @Test
    fun `toEntity should handle negative temperatures`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "雪",
            temperature = TemperatureDto(
                min = TemperatureValueDto("-5"),
                max = TemperatureValueDto("2")
            )
        )

        // Act
        val weather = forecastDto.toEntity(currentTemp = -2, precipProb = 90)

        // Assert
        assertEquals(-5, weather.minTemperature)
        assertEquals(2, weather.maxTemperature)
        assertEquals(-2, weather.currentTemperature)
    }
}
