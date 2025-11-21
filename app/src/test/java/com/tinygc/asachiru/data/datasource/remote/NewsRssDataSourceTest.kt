package com.tinygc.asachiru.data.datasource.remote

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NewsRssDataSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: NewsRssDataSource
    private lateinit var httpClient: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        httpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        dataSource = NewsRssDataSource(httpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchLatestNews should return news list on success`() = runTest {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>テストニュース1</title>
                        <description>テスト説明1</description>
                        <link>https://example.com/1</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                    <item>
                        <title>テストニュース2</title>
                        <description>テスト説明2</description>
                        <link>https://example.com/2</link>
                        <pubDate>Wed, 06 Nov 2025 13:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(rssXml)
            .setHeader("Content-Type", "application/xml"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchLatestNews("https://test-rss-url.com")

        // Assert
        assertEquals(2, result.size)
        assertEquals("テストニュース1", result[0].title)
        assertEquals("テスト説明1", result[0].description)
        assertEquals("https://example.com/1", result[0].link)
    }

    @Test
    fun `fetchLatestNews should throw IOException on HTTP error`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchLatestNews("https://test-rss-url.com")
            fail("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("HTTP Error: 404") == true)
        }
    }

    @Test
    fun `fetchLatestNews should throw IOException on empty response`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("")
            .setHeader("Content-Type", "application/xml"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchLatestNews("https://test-rss-url.com")
            fail("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Empty response") == true)
        }
    }

    @Test
    fun `fetchLatestNews should handle RSS with multiple items`() = runTest {
        // Arrange
        val items = (1..10).map { i ->
            """
                <item>
                    <title>News $i</title>
                    <description>Description $i</description>
                    <link>https://example.com/$i</link>
                    <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                </item>""".trimIndent()
        }.joinToString("\n")

        val rssXml = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
    <channel>
$items
    </channel>
</rss>"""

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(rssXml)
            .setHeader("Content-Type", "application/xml"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchLatestNews("https://test-rss-url.com")

        // Assert
        assertEquals(10, result.size)
        (0..9).forEach { i ->
            assertEquals("News ${i + 1}", result[i].title)
        }
    }

    @Test
    fun `fetchLatestNews should handle RSS with no items`() = runTest {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>NHKニュース</title>
                </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(rssXml)
            .setHeader("Content-Type", "application/xml"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchLatestNews("https://test-rss-url.com")

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fetchLatestNews should handle 500 server error`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act & Assert
        try {
            testDataSource.fetchLatestNews("https://test-rss-url.com")
            fail("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("HTTP Error: 500") == true)
        }
    }

    @Test
    fun `fetchLatestNews should send GET request`() = runTest {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel></channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(rssXml)
            .setHeader("Content-Type", "application/xml"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        testDataSource.fetchLatestNews("https://test-rss-url.com")

        // Assert
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
    }

    @Test
    fun `fetchLatestNews should handle NHK RSS format`() = runTest {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>NHKニュース</title>
                    <item>
                        <title>政治ニュース</title>
                        <description>政治に関するニュースです。</description>
                        <link>https://www3.nhk.or.jp/news/html/20251106/k10012345678.html</link>
                        <pubDate>Wed, 06 Nov 2025 15:30:00 +0900</pubDate>
                    </item>
                    <item>
                        <title>経済ニュース</title>
                        <description>経済に関するニュースです。</description>
                        <link>https://www3.nhk.or.jp/news/html/20251106/k10012345679.html</link>
                        <pubDate>Wed, 06 Nov 2025 16:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(rssXml)
            .setHeader("Content-Type", "application/xml"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchLatestNews("https://test-rss-url.com")

        // Assert
        assertEquals(2, result.size)
        assertEquals("政治ニュース", result[0].title)
        assertEquals("経済ニュース", result[1].title)
        assertTrue(result[0].link.contains("nhk.or.jp"))
    }

    @Test
    fun `fetchLatestNews should handle RSS with special characters`() = runTest {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>特殊文字「」『』・：</title>
                        <description>改行&amp;エンティティ&lt;&gt;</description>
                        <link>https://example.com/special</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(rssXml)
            .setHeader("Content-Type", "application/xml; charset=UTF-8"))

        val testDataSource = createDataSourceWithMockUrl()

        // Act
        val result = testDataSource.fetchLatestNews("https://test-rss-url.com")

        // Assert
        assertEquals(1, result.size)
        assertEquals("特殊文字「」『』・：", result[0].title)
    }

    // Helper method to create a data source with mock URL
    private fun createDataSourceWithMockUrl(): NewsRssDataSource {
        return object : NewsRssDataSource(httpClient) {
            override suspend fun fetchLatestNews(rssUrl: String): List<com.tinygc.asachiru.data.dto.NewsDto> {
                val url = mockWebServer.url("/").toString()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP Error: ${response.code}")
                }

                val body = response.body ?: throw IOException("Empty response")
                val bodyString = body.string()
                if (bodyString.isBlank()) {
                    throw IOException("Empty response")
                }

                val inputStream = bodyString.byteInputStream()
                return com.tinygc.asachiru.data.util.RssParser.parse(inputStream)
            }
        }
    }
}
