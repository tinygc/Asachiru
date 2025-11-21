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
            ),
            chanceOfRain = ChanceOfRainDto(
                t00_06 = "10%",
                t06_12 = "20%",
                t12_18 = "30%",
                t18_24 = "15%"
            )
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(WeatherCondition.SUNNY, weather.condition)
        assertEquals(20, weather.currentTemperature) // (25 + 15) / 2
        assertEquals(25, weather.maxTemperature)
        assertEquals(15, weather.minTemperature)
        assertEquals(30, weather.precipitationProbability) // max of t06_12(20%) and t12_18(30%)
    }

    @Test
    fun `toEntity should parse sunny weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "晴れ",
            temperature = TemperatureDto(null, null),
            chanceOfRain = null
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(WeatherCondition.SUNNY, weather.condition)
    }

    @Test
    fun `toEntity should parse cloudy weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "曇り",
            temperature = TemperatureDto(null, null),
            chanceOfRain = null
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(WeatherCondition.CLOUDY, weather.condition)
    }

    @Test
    fun `toEntity should parse rainy weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "雨",
            temperature = TemperatureDto(null, null),
            chanceOfRain = null
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(WeatherCondition.RAINY, weather.condition)
    }

    @Test
    fun `toEntity should parse snowy weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "雪",
            temperature = TemperatureDto(null, null),
            chanceOfRain = null
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(WeatherCondition.SNOWY, weather.condition)
    }

    @Test
    fun `toEntity should parse other weather condition`() {
        // Arrange
        val forecastDto = ForecastDto(
            date = "2025-11-07",
            telop = "その他の天気",
            temperature = TemperatureDto(null, null),
            chanceOfRain = null
        )

        // Act
        val weather = forecastDto.toEntity()

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
            ),
            chanceOfRain = ChanceOfRainDto(
                t00_06 = "10%",
                t06_12 = "20%",
                t12_18 = "30%",
                t18_24 = "15%"
            )
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertNull(weather.minTemperature) // データなしの場合はnull
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
            ),
            chanceOfRain = ChanceOfRainDto(
                t00_06 = "10%",
                t06_12 = "20%",
                t12_18 = "30%",
                t18_24 = "15%"
            )
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(15, weather.minTemperature)
        assertNull(weather.maxTemperature) // データなしの場合はnull
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
            ),
            chanceOfRain = null
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertNull(weather.minTemperature) // パース失敗の場合はnull
        assertNull(weather.maxTemperature) // パース失敗の場合はnull
    }

    @Test
    fun `toEntity should handle mixed weather conditions with priority`() {
        // Arrange
        // 最初の天気を優先（「のち」の前の天気）
        val conditions = listOf(
            "晴れのち曇り" to WeatherCondition.SUNNY,
            "曇りのち雨" to WeatherCondition.CLOUDY,
            "雨のち雪" to WeatherCondition.RAINY,
            "雪のち晴れ" to WeatherCondition.SNOWY
        )

        conditions.forEach { (telop, expectedCondition) ->
            val forecastDto = ForecastDto(
                date = "2025-11-07",
                telop = telop,
                temperature = TemperatureDto(null, null),
                chanceOfRain = null
            )

            // Act
            val weather = forecastDto.toEntity()

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
            ),
            chanceOfRain = ChanceOfRainDto(
                t00_06 = "40%",
                t06_12 = "70%",
                t12_18 = "80%",
                t18_24 = "60%"
            )
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(20, weather.currentTemperature) // (10 + 30) / 2
        assertEquals(80, weather.precipitationProbability) // max of t06_12(70%) and t12_18(80%)
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
            ),
            chanceOfRain = ChanceOfRainDto(
                t00_06 = "80%",
                t06_12 = "90%",
                t12_18 = "85%",
                t18_24 = "75%"
            )
        )

        // Act
        val weather = forecastDto.toEntity()

        // Assert
        assertEquals(-5, weather.minTemperature)
        assertEquals(2, weather.maxTemperature)
        assertEquals(-1, weather.currentTemperature) // (-5 + 2) / 2 = -1
        assertEquals(90, weather.precipitationProbability) // max of t06_12(90%) and t12_18(85%)
    }
}
