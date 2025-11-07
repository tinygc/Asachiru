package com.tinygc.asachiru.domain.common

import org.junit.Assert.*
import org.junit.Test

class AppExceptionTest {

    @Test
    fun `NetworkException should hold message`() {
        // Arrange
        val expectedMessage = "Network error occurred"

        // Act
        val exception = AppException.NetworkException(expectedMessage)

        // Assert
        assertTrue(exception is AppException.NetworkException)
        assertEquals(expectedMessage, exception.message)
        assertTrue(exception is Exception)
    }

    @Test
    fun `ApiException should hold statusCode and message`() {
        // Arrange
        val expectedStatusCode = 404
        val expectedMessage = "Not Found"

        // Act
        val exception = AppException.ApiException(expectedStatusCode, expectedMessage)

        // Assert
        assertTrue(exception is AppException.ApiException)
        assertEquals(expectedStatusCode, exception.statusCode)
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `ParseException should hold message`() {
        // Arrange
        val expectedMessage = "Failed to parse JSON"

        // Act
        val exception = AppException.ParseException(expectedMessage)

        // Assert
        assertTrue(exception is AppException.ParseException)
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `SettingsException should hold message`() {
        // Arrange
        val expectedMessage = "Invalid settings"

        // Act
        val exception = AppException.SettingsException(expectedMessage)

        // Assert
        assertTrue(exception is AppException.SettingsException)
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `All AppExceptions should be different types`() {
        // Arrange
        val network = AppException.NetworkException("network")
        val api = AppException.ApiException(500, "api")
        val parse = AppException.ParseException("parse")
        val settings = AppException.SettingsException("settings")

        // Assert
        assertTrue(network is AppException.NetworkException)
        assertTrue(api is AppException.ApiException)
        assertTrue(parse is AppException.ParseException)
        assertTrue(settings is AppException.SettingsException)

        assertNotEquals(network::class, api::class)
        assertNotEquals(api::class, parse::class)
        assertNotEquals(parse::class, settings::class)
    }

    @Test
    fun `All AppExceptions should be AppException type`() {
        // Arrange
        val exceptions = listOf(
            AppException.NetworkException("network"),
            AppException.ApiException(500, "api"),
            AppException.ParseException("parse"),
            AppException.SettingsException("settings")
        )

        // Assert
        exceptions.forEach { exception ->
            assertTrue(exception is AppException)
            assertTrue(exception is Exception)
        }
    }
}
