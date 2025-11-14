package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class WeatherTest {

    @Test
    fun `Weather should hold all weather information`() {
        // Arrange & Act
        val weather = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 20,
            maxTemperature = 25,
            minTemperature = 15,
            precipitationProbability = 10,
            dateLabel = "今日",
            iconText = "☀"
        )

        // Assert
        assertEquals(WeatherCondition.SUNNY, weather.condition)
        assertEquals(20, weather.currentTemperature)
        assertEquals(25, weather.maxTemperature)
        assertEquals(15, weather.minTemperature)
        assertEquals(10, weather.precipitationProbability)
    }

    @Test
    fun `EMPTY should have default values`() {
        // Act
        val empty = Weather.EMPTY

        // Assert
        assertEquals(WeatherCondition.OTHER, empty.condition)
        assertEquals(0, empty.currentTemperature)
        assertNull(empty.maxTemperature) // データなしの場合はnull
        assertNull(empty.minTemperature) // データなしの場合はnull
        assertEquals(0, empty.precipitationProbability)
    }

    @Test
    fun `Weather should be data class with correct equality`() {
        // Arrange
        val weather1 = Weather(
            condition = WeatherCondition.RAINY,
            currentTemperature = 18,
            maxTemperature = 20,
            minTemperature = 16,
            precipitationProbability = 80,
            dateLabel = "今日",
            iconText = "☂"
        )
        val weather2 = Weather(
            condition = WeatherCondition.RAINY,
            currentTemperature = 18,
            maxTemperature = 20,
            minTemperature = 16,
            precipitationProbability = 80,
            dateLabel = "今日",
            iconText = "☂"
        )
        val weather3 = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 25,
            maxTemperature = 28,
            minTemperature = 22,
            precipitationProbability = 0,
            dateLabel = "今日",
            iconText = "☀"
        )

        // Assert
        assertEquals(weather1, weather2)
        assertNotEquals(weather1, weather3)
    }

    @Test
    fun `Weather should accept negative temperatures`() {
        // Arrange & Act
        val weather = Weather(
            condition = WeatherCondition.SNOWY,
            currentTemperature = -5,
            maxTemperature = 0,
            minTemperature = -10,
            precipitationProbability = 100,
            dateLabel = "今日",
            iconText = "⛄"
        )

        // Assert
        assertEquals(-5, weather.currentTemperature)
        assertEquals(0, weather.maxTemperature)
        assertEquals(-10, weather.minTemperature)
    }

    @Test
    fun `Weather should handle all weather conditions`() {
        // Act
        val conditions = listOf(
            WeatherCondition.SUNNY,
            WeatherCondition.CLOUDY,
            WeatherCondition.RAINY,
            WeatherCondition.SNOWY,
            WeatherCondition.OTHER
        )

        // Assert
        conditions.forEach { condition ->
            val weather = Weather(
                condition = condition,
                currentTemperature = 20,
                maxTemperature = 25,
                minTemperature = 15,
                precipitationProbability = 30,
                dateLabel = "今日",
                iconText = "☀"
            )
            assertEquals(condition, weather.condition)
        }
    }
}
