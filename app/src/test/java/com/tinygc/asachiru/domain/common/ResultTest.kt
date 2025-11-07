package com.tinygc.asachiru.domain.common

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `Success should hold data`() {
        // Arrange
        val expectedData = "test data"

        // Act
        val result = Result.Success(expectedData)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedData, result.data)
    }

    @Test
    fun `Error should hold exception`() {
        // Arrange
        val expectedException = Exception("test error")

        // Act
        val result = Result.Error(expectedException)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(expectedException, result.exception)
    }

    @Test
    fun `Success and Error should be different types`() {
        // Arrange
        val success = Result.Success("data")
        val error = Result.Error(Exception("error"))

        // Assert
        assertTrue(success is Result.Success)
        assertTrue(error is Result.Error)
        assertNotEquals(success::class, error::class)
    }

    @Test
    fun `Success should work with different data types`() {
        // Act
        val stringResult = Result.Success("string")
        val intResult = Result.Success(123)
        val boolResult = Result.Success(true)

        // Assert
        assertTrue(stringResult is Result.Success)
        assertEquals("string", stringResult.data)
        assertTrue(intResult is Result.Success)
        assertEquals(123, intResult.data)
        assertTrue(boolResult is Result.Success)
        assertEquals(true, boolResult.data)
    }
}
