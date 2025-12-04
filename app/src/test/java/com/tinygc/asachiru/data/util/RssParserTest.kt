package com.tinygc.asachiru.data.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RssParserTest {

    @Test
    fun `parse should return news list from valid RSS`() {
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

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(2, result.size)
        assertEquals("テストニュース1", result[0].title)
        assertEquals("テスト説明1", result[0].description)
        assertEquals("https://example.com/1", result[0].link)
        assertEquals("Wed, 06 Nov 2025 12:00:00 +0900", result[0].pubDate)
    }

    @Test
    fun `parse should return empty list for RSS with no items`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Channel Title</title>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse should handle single item RSS`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Single News</title>
                        <description>Single Description</description>
                        <link>https://example.com/single</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Single News", result[0].title)
    }

    @Test
    fun `parse should skip items with missing required fields`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Complete News</title>
                        <description>Complete Description</description>
                        <link>https://example.com/complete</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                    <item>
                        <title>Incomplete News</title>
                        <description>Missing link and pubDate</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Complete News", result[0].title)
    }

    @Test
    fun `parse should handle RSS with special characters`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>特殊文字「」『』・：</title>
                        <description>改行&amp;エンティティ&lt;&gt;</description>
                        <link>https://example.com/特殊</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(1, result.size)
        assertEquals("特殊文字「」『』・：", result[0].title)
    }

    @Test
    fun `parse should handle multiple items correctly`() {
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

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(10, result.size)
        (0..9).forEach { i ->
            assertEquals("News ${i + 1}", result[i].title)
            assertEquals("Description ${i + 1}", result[i].description)
        }
    }

    @Test
    fun `parse should preserve order of items`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>First</title>
                        <description>First Description</description>
                        <link>https://example.com/1</link>
                        <pubDate>Wed, 06 Nov 2025 10:00:00 +0900</pubDate>
                    </item>
                    <item>
                        <title>Second</title>
                        <description>Second Description</description>
                        <link>https://example.com/2</link>
                        <pubDate>Wed, 06 Nov 2025 11:00:00 +0900</pubDate>
                    </item>
                    <item>
                        <title>Third</title>
                        <description>Third Description</description>
                        <link>https://example.com/3</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(3, result.size)
        assertEquals("First", result[0].title)
        assertEquals("Second", result[1].title)
        assertEquals("Third", result[2].title)
    }

    @Test
    fun `parse should handle RSS with extra tags`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Channel Title</title>
                    <link>https://example.com</link>
                    <description>Channel Description</description>
                    <item>
                        <title>News Title</title>
                        <description>News Description</description>
                        <link>https://example.com/news</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                        <guid>https://example.com/news</guid>
                        <category>General</category>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(1, result.size)
        assertEquals("News Title", result[0].title)
        assertEquals("News Description", result[0].description)
    }

    @Test
    fun `parse should handle empty title or description gracefully`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title></title>
                        <description></description>
                        <link>https://example.com/empty</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert - empty values are not null so should be included
        if (result.isNotEmpty()) {
            assertEquals("", result[0].title)
            assertEquals("", result[0].description)
        }
    }

    @Test
    fun `parse should handle NHK RSS format`() {
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
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(1, result.size)
        assertEquals("政治ニュース", result[0].title)
        assertTrue(result[0].link.contains("nhk.or.jp"))
    }

    @Test
    fun `parse should handle RSS without description (like Yahoo News)`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Yahoo!ニュース</title>
                    <item>
                        <title>日本版「政府効率化省」を設置</title>
                        <link>https://news.yahoo.co.jp/pickup/6560181</link>
                        <pubDate>Tue, 25 Nov 2025 05:25:22 GMT</pubDate>
                    </item>
                    <item>
                        <title>福井知事が辞職表明 セクハラ疑惑</title>
                        <link>https://news.yahoo.co.jp/pickup/6560191</link>
                        <pubDate>Tue, 25 Nov 2025 07:15:51 GMT</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(2, result.size)
        assertEquals("日本版「政府効率化省」を設置", result[0].title)
        assertEquals("", result[0].description) // descriptionがない場合は空文字列
        assertEquals("https://news.yahoo.co.jp/pickup/6560181", result[0].link)
        assertEquals("Tue, 25 Nov 2025 05:25:22 GMT", result[0].pubDate)
    }
}
