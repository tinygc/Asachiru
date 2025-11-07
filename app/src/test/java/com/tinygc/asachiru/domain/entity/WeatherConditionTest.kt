package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun `All weather conditions should have display names`() {
        // Assert
        assertEquals("晴れ", WeatherCondition.SUNNY.displayName)
        assertEquals("曇り", WeatherCondition.CLOUDY.displayName)
        assertEquals("雨", WeatherCondition.RAINY.displayName)
        assertEquals("雪", WeatherCondition.SNOWY.displayName)
        assertEquals("その他", WeatherCondition.OTHER.displayName)
    }

    @Test
    fun `getIconResourceId should return valid resource ID`() {
        // Act & Assert
        val conditions = WeatherCondition.values()

        conditions.forEach { condition ->
            val resourceId = condition.getIconResourceId()
            assertTrue("Resource ID should be positive", resourceId > 0)
        }
    }

    @Test
    fun `WeatherCondition values should return all conditions`() {
        // Act
        val allConditions = WeatherCondition.values()

        // Assert
        assertEquals(5, allConditions.size)
        assertTrue(allConditions.contains(WeatherCondition.SUNNY))
        assertTrue(allConditions.contains(WeatherCondition.CLOUDY))
        assertTrue(allConditions.contains(WeatherCondition.RAINY))
        assertTrue(allConditions.contains(WeatherCondition.SNOWY))
        assertTrue(allConditions.contains(WeatherCondition.OTHER))
    }

    @Test
    fun `Each condition should have unique display name`() {
        // Arrange
        val displayNames = WeatherCondition.values().map { it.displayName }

        // Assert
        assertEquals(displayNames.size, displayNames.toSet().size)
    }
}
