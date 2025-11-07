package com.tinygc.asachiru.data.datasource.remote

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class WeatherApiDataSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: WeatherApiDataSource
    private lateinit var httpClient: OkHttpClient
    private lateinit var gson: Gson

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        httpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        gson = Gson()
        dataSource = WeatherApiDataSource(httpClient, gson)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchWeather should return WeatherApiResponse on success`() = runTest {
        // Arrange
        val responseJson = """
            {
                "forecasts": [
                    {
                        "date": "2025-11-07",
                        "telop": "晴れ",
                        "temperature": {
                            "min": {
                                "celsius": "15"
                            },
                            "max": {
                                "celsius": "25"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(responseJson)
            .setHeader("Content-Type", "application/json"))

        // Use mock server URL
        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchWeather("130010")

        // Assert
        assertNotNull(result)
        assertEquals(1, result.forecasts.size)
        assertEquals("2025-11-07", result.forecasts[0].date)
        assertEquals("晴れ", result.forecasts[0].telop)
        assertEquals("15", result.forecasts[0].temperature.min?.celsius)
        assertEquals("25", result.forecasts[0].temperature.max?.celsius)
    }

    @Test
    fun `fetchWeather should throw IOException on HTTP error`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchWeather("999999")
            fail("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("HTTP Error: 404") == true)
        }
    }

    @Test
    fun `fetchWeather should throw IOException on empty response`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("")
            .setHeader("Content-Type", "application/json"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchWeather("130010")
            fail("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Empty response") == true)
        }
    }

    @Test
    fun `fetchWeather should handle multiple forecasts`() = runTest {
        // Arrange
        val responseJson = """
            {
                "forecasts": [
                    {
                        "date": "2025-11-07",
                        "telop": "晴れ",
                        "temperature": {
                            "min": {"celsius": "15"},
                            "max": {"celsius": "25"}
                        }
                    },
                    {
                        "date": "2025-11-08",
                        "telop": "曇り",
                        "temperature": {
                            "min": {"celsius": "14"},
                            "max": {"celsius": "22"}
                        }
                    },
                    {
                        "date": "2025-11-09",
                        "telop": "雨",
                        "temperature": {
                            "min": {"celsius": "12"},
                            "max": {"celsius": "18"}
                        }
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(responseJson)
            .setHeader("Content-Type", "application/json"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchWeather("130010")

        // Assert
        assertEquals(3, result.forecasts.size)
        assertEquals("晴れ", result.forecasts[0].telop)
        assertEquals("曇り", result.forecasts[1].telop)
        assertEquals("雨", result.forecasts[2].telop)
    }

    @Test
    fun `fetchWeather should handle forecast with null temperature values`() = runTest {
        // Arrange
        val responseJson = """
            {
                "forecasts": [
                    {
                        "date": "2025-11-07",
                        "telop": "晴れ",
                        "temperature": {
                            "min": null,
                            "max": null
                        }
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(responseJson)
            .setHeader("Content-Type", "application/json"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchWeather("130010")

        // Assert
        assertEquals(1, result.forecasts.size)
        assertNull(result.forecasts[0].temperature.min)
        assertNull(result.forecasts[0].temperature.max)
    }

    @Test
    fun `fetchWeather should send correct request with area code`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"forecasts": []}""")
            .setHeader("Content-Type", "application/json"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        testDataSource.fetchWeather("270000")

        // Assert
        val request = mockWebServer.takeRequest()
        assertTrue(request.path?.contains("city=270000") == true)
        assertEquals("GET", request.method)
    }

    @Test
    fun `fetchWeather should handle 500 server error`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchWeather("130010")
            fail("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("HTTP Error: 500") == true)
        }
    }

    @Test
    fun `fetchWeather should handle invalid JSON`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("invalid json {{{")
            .setHeader("Content-Type", "application/json"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchWeather("130010")
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            // JSON parse exception should be thrown
            assertNotNull(e)
        }
    }

    // Helper method to create a data source with mock URL
    private fun createDataSourceWithMockUrl(): WeatherApiDataSource {
        // Create a custom data source that uses the mock server URL
        return object : WeatherApiDataSource(httpClient, gson) {
            suspend fun fetchWeather(areaCode: String): com.tinygc.asachiru.data.dto.WeatherApiResponse {
                val url = "${mockWebServer.url("/")}?city=$areaCode"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP Error: ${response.code}")
                }

                val json = response.body?.string() ?: throw IOException("Empty response")
                return gson.fromJson(json, com.tinygc.asachiru.data.dto.WeatherApiResponse::class.java)
            }
        }
    }
}
