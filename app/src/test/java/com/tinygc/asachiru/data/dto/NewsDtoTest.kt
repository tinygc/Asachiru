package com.tinygc.asachiru.data.dto

import org.junit.Assert.*
import org.junit.Test

class NewsDtoTest {

    @Test
    fun `toEntity should convert NewsDto to News entity`() {
        // Arrange
        val newsDto = NewsDto(
            title = "テストニュース",
            description = "テスト説明文",
            link = "https://example.com/news/123",
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals("テストニュース", news.title)
        assertEquals("テスト説明文", news.description)
        assertEquals("https://example.com/news/123", news.id)
        assertTrue(news.publishedAt > 0)
    }

    @Test
    fun `toEntity should use link as ID`() {
        // Arrange
        val newsDto = NewsDto(
            title = "タイトル",
            description = "説明",
            link = "https://unique-link.com/article/456",
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals("https://unique-link.com/article/456", news.id)
    }

    @Test
    fun `toEntity should parse valid RFC 822 date`() {
        // Arrange
        val newsDto = NewsDto(
            title = "タイトル",
            description = "説明",
            link = "https://example.com",
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertTrue(news.publishedAt > 0)
    }

    @Test
    fun `toEntity should handle invalid date format gracefully`() {
        // Arrange
        val newsDto = NewsDto(
            title = "タイトル",
            description = "説明",
            link = "https://example.com",
            pubDate = "invalid date format"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals(0L, news.publishedAt)
    }

    @Test
    fun `toEntity should handle empty date string`() {
        // Arrange
        val newsDto = NewsDto(
            title = "タイトル",
            description = "説明",
            link = "https://example.com",
            pubDate = ""
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals(0L, news.publishedAt)
    }

    @Test
    fun `toEntity should preserve title and description exactly`() {
        // Arrange
        val title = "特殊文字を含む「タイトル」・test"
        val description = "改行\nタブ\t含む説明文"
        val newsDto = NewsDto(
            title = title,
            description = description,
            link = "https://example.com",
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals(title, news.title)
        assertEquals(description, news.description)
    }

    @Test
    fun `toEntity should handle different RFC 822 date variations`() {
        // Arrange
        val dates = listOf(
            "Mon, 01 Jan 2024 00:00:00 +0900",
            "Tue, 15 Feb 2024 14:30:00 +0900",
            "Wed, 31 Dec 2025 23:59:59 +0900"
        )

        dates.forEach { pubDate ->
            val newsDto = NewsDto(
                title = "タイトル",
                description = "説明",
                link = "https://example.com",
                pubDate = pubDate
            )

            // Act
            val news = newsDto.toEntity()

            // Assert
            assertTrue("Date should be parsed: $pubDate", news.publishedAt > 0)
        }
    }

    @Test
    fun `toEntity should handle very long title and description`() {
        // Arrange
        val longTitle = "あ".repeat(500)
        val longDescription = "い".repeat(1000)
        val newsDto = NewsDto(
            title = longTitle,
            description = longDescription,
            link = "https://example.com",
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals(longTitle, news.title)
        assertEquals(longDescription, news.description)
    }

    @Test
    fun `toEntity should handle URL with query parameters`() {
        // Arrange
        val linkWithParams = "https://example.com/news?id=123&category=sports"
        val newsDto = NewsDto(
            title = "タイトル",
            description = "説明",
            link = linkWithParams,
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals(linkWithParams, news.id)
    }

    @Test
    fun `toEntity should create News with correct structure`() {
        // Arrange
        val newsDto = NewsDto(
            title = "ニュースタイトル",
            description = "ニュース説明",
            link = "https://www3.nhk.or.jp/news/html/20251106/k10012345678.html",
            pubDate = "Wed, 06 Nov 2025 15:30:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertNotNull(news)
        assertNotNull(news.id)
        assertNotNull(news.title)
        assertNotNull(news.description)
        assertTrue(news.publishedAt >= 0)
    }

    @Test
    fun `toEntity should handle empty title and description`() {
        // Arrange
        val newsDto = NewsDto(
            title = "",
            description = "",
            link = "https://example.com",
            pubDate = "Wed, 06 Nov 2025 12:00:00 +0900"
        )

        // Act
        val news = newsDto.toEntity()

        // Assert
        assertEquals("", news.title)
        assertEquals("", news.description)
        assertEquals("https://example.com", news.id)
    }
}
