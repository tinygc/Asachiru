package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.remote.NewsRssDataSource
import com.tinygc.asachiru.data.dto.NewsDto
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NewsRepositoryImplTest {

    private lateinit var newsRssDataSource: NewsRssDataSource
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: NewsRepositoryImpl

    @Before
    fun setUp() {
        newsRssDataSource = mock()
        settingsRepository = mock()
        
        // Mock settings to return RSS URL
        runTest {
            whenever(settingsRepository.getSettings()).thenReturn(
                Settings(
                    postalCode = "100-0001",
                    newsIntervalMinutes = 30,
                    rssUrl = "https://test-rss.com/feed",
                    enableTts = false,
                    rssPreset = null
                )
            )
        }
        
        repository = NewsRepositoryImpl(newsRssDataSource, settingsRepository)
    }

    @Test
    fun `getLatestNews should return Success with news list`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900"),
            NewsDto("Title 2", "Description 2", "https://example.com/2", "Mon, 01 Jan 2024 13:00:00 +0900"),
            NewsDto("Title 3", "Description 3", "https://example.com/3", "Mon, 01 Jan 2024 14:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = 3)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertEquals(3, newsList.size)
        assertEquals("Title 1", newsList[0].title)
        assertEquals("Description 1", newsList[0].description)
        // Note: link field was removed from News entity
    }

    @Test
    fun `getLatestNews should limit results by count parameter`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900"),
            NewsDto("Title 2", "Description 2", "https://example.com/2", "Mon, 01 Jan 2024 13:00:00 +0900"),
            NewsDto("Title 3", "Description 3", "https://example.com/3", "Mon, 01 Jan 2024 14:00:00 +0900"),
            NewsDto("Title 4", "Description 4", "https://example.com/4", "Mon, 01 Jan 2024 15:00:00 +0900"),
            NewsDto("Title 5", "Description 5", "https://example.com/5", "Mon, 01 Jan 2024 16:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = 2)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertEquals(2, newsList.size)
        assertEquals("Title 1", newsList[0].title)
        assertEquals("Title 2", newsList[1].title)
    }

    @Test
    fun `getLatestNews should return all items when count is larger than available`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900"),
            NewsDto("Title 2", "Description 2", "https://example.com/2", "Mon, 01 Jan 2024 13:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = 10)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertEquals(2, newsList.size)
    }

    @Test
    fun `getLatestNews should return empty list when no news available`() = runTest {
        // Given
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(emptyList())

        // When
        val result = repository.getLatestNews(count = 10)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertTrue(newsList.isEmpty())
    }

    @Test
    fun `getLatestNews should convert DTO to Entity correctly`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto(link = "https://example.com", 
                title = "Test Title",
                description = "Test Description",
                pubDate = "Mon, 01 Jan 2024 12:00:00 +0900"
            )
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = 1)

        // Then
        assertTrue(result is Result.Success)
        val news = (result as Result.Success).data.first()
        assertEquals("Test Title", news.title)
        assertEquals("Test Description", news.description)
        // Note: link field was removed from News entity
        assertTrue(news.publishedAt > 0L)
    }

    @Test
    fun `getLatestNews should return NetworkException on IOException`() = runTest {
        // Given
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenAnswer {
            throw IOException("Network error")
        }

        // When
        val result = repository.getLatestNews(count = 10)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.NetworkException)
        assertEquals("Network error", exception.message)
    }

    @Test
    fun `getLatestNews should return ParseException on generic Exception`() = runTest {
        // Given
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenThrow(RuntimeException("Parse error"))

        // When
        val result = repository.getLatestNews(count = 10)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.ParseException)
        assertEquals("Parse error", exception.message)
    }

    @Test
    fun `getLatestNews should handle count of zero`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = 0)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertTrue(newsList.isEmpty())
    }

    @Test
    fun `getLatestNews should handle negative count`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = -5)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertTrue(newsList.isEmpty())
    }

    @Test
    fun `getLatestNews should preserve news order from data source`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("First", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 15:00:00 +0900"),
            NewsDto("Second", "Description 2", "https://example.com/2", "Mon, 01 Jan 2024 14:00:00 +0900"),
            NewsDto("Third", "Description 3", "https://example.com/3", "Mon, 01 Jan 2024 13:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = 3)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertEquals("First", newsList[0].title)
        assertEquals("Second", newsList[1].title)
        assertEquals("Third", newsList[2].title)
    }

    @Test
    fun `getLatestNews should handle large count value`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        val result = repository.getLatestNews(count = Int.MAX_VALUE)

        // Then
        assertTrue(result is Result.Success)
        val newsList = (result as Result.Success).data
        assertEquals(1, newsList.size)
    }

    @Test
    fun `getLatestNews should call data source exactly once`() = runTest {
        // Given
        val mockNewsList = listOf(
            NewsDto("Title 1", "Description 1", "https://example.com/1", "Mon, 01 Jan 2024 12:00:00 +0900")
        )
        whenever(newsRssDataSource.fetchLatestNews(anyString())).thenReturn(mockNewsList)

        // When
        repository.getLatestNews(count = 1)

        // Then
        verify(newsRssDataSource, times(1)).fetchLatestNews(anyString())
    }
}
