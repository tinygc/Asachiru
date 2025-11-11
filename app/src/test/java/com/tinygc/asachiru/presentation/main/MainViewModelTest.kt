package com.tinygc.asachiru.presentation.main

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.*
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.usecase.clock.GetCurrentDateTimeUseCase
import com.tinygc.asachiru.domain.usecase.music.GetCurrentTrackUseCase
import com.tinygc.asachiru.domain.usecase.music.PlayMusicUseCase
import com.tinygc.asachiru.domain.usecase.news.GetLatestNewsUseCase
import com.tinygc.asachiru.domain.usecase.news.ReadNewsUseCase
import com.tinygc.asachiru.domain.usecase.weather.GetWeatherUseCase
import com.tinygc.asachiru.domain.usecase.weather.RefreshWeatherUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@org.junit.Ignore("TODO: MainViewModel uses infinite loops (while isActive) in init block. Need to refactor MainViewModel to make it testable. See TESTING.md for details.")
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentDateTimeUseCase: GetCurrentDateTimeUseCase
    private lateinit var getWeatherUseCase: GetWeatherUseCase
    private lateinit var refreshWeatherUseCase: RefreshWeatherUseCase
    private lateinit var getLatestNewsUseCase: GetLatestNewsUseCase
    private lateinit var readNewsUseCase: ReadNewsUseCase
    private lateinit var playMusicUseCase: PlayMusicUseCase
    private lateinit var getCurrentTrackUseCase: GetCurrentTrackUseCase
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var viewModel: MainViewModel

    private val testDateTime = DateTime(2025, 11, 8, 14, 30, 0, DayOfWeek.FRIDAY)
    private val testWeather = Weather(
        condition = WeatherCondition.SUNNY,
        currentTemperature = 25,
        maxTemperature = 28,
        minTemperature = 18,
        precipitationProbability = 10
    )
    private val testSettings = Settings("1000001", 30)
    private val testMusic = Music("1", "Test Track", "Test Artist", 1, 180_000L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getCurrentDateTimeUseCase = mock()
        getWeatherUseCase = mock()
        refreshWeatherUseCase = mock()
        getLatestNewsUseCase = mock()
        readNewsUseCase = mock()
        playMusicUseCase = mock()
        getCurrentTrackUseCase = mock()
        settingsRepository = mock()

        whenever(getCurrentDateTimeUseCase.invoke()).thenReturn(testDateTime)
        runBlocking {
            whenever(settingsRepository.getSettings()).thenReturn(testSettings)
        }
        whenever(getCurrentTrackUseCase.invoke()).thenReturn(testMusic)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `MainViewModel should be created successfully`() = runTest {
        // When
        viewModel = createViewModel()

        // Then
        assertNotNull(viewModel)
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `uiState should have initial state`() = runTest {
        // When
        viewModel = createViewModel()

        // Then - Turbineを使ってFlowの最初の値を確認
        viewModel.uiState.test {
            val initial = awaitItem()

            // 時計は即座に更新される
            assertEquals(testDateTime, initial.dateTime)
            // 音楽トラック情報は即座に更新される
            assertEquals(testMusic, initial.currentTrack)
            assertFalse(initial.isMusicPlaying)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init should call playMusicUseCase`() = runTest {
        // When
        viewModel = createViewModel()

        // Then - Turbineで少し待つ
        viewModel.uiState.test {
            awaitItem() // 最初の値を取得

            verify(playMusicUseCase, atLeastOnce()).invoke()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init should set isMusicPlaying to true`() = runTest {
        // When
        viewModel = createViewModel()

        // Then - Turbineで最初の2つの更新を確認
        viewModel.uiState.test {
            skipItems(1) // 最初の初期状態をスキップ

            val updated = awaitItem()
            assertTrue(updated.isMusicPlaying)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init should call getWeatherUseCase`() = runTest {
        // Given
        whenever(getWeatherUseCase.invoke()).thenReturn(Result.Success(testWeather))

        // When
        viewModel = createViewModel()
        advanceTimeBy(100)

        // Then
        verify(getWeatherUseCase, atLeastOnce()).invoke()
    }

    @Test
    fun `loadWeather should update weather when success`() = runTest {
        // Given
        whenever(getWeatherUseCase.invoke()).thenReturn(Result.Success(testWeather))

        // When
        viewModel = createViewModel()
        advanceTimeBy(100)

        // Then
        assertEquals(testWeather, viewModel.uiState.value.weather)
        assertFalse(viewModel.uiState.value.isWeatherLoading)
        assertNull(viewModel.uiState.value.weatherError)
    }

    @Test
    fun `loadWeather should update error when failure`() = runTest {
        // Given
        val error = Exception("Network error")
        whenever(getWeatherUseCase.invoke()).thenReturn(Result.Error(error))

        // When
        viewModel = createViewModel()
        advanceTimeBy(100)

        // Then
        assertNull(viewModel.uiState.value.weather)
        assertFalse(viewModel.uiState.value.isWeatherLoading)
        assertEquals("Network error", viewModel.uiState.value.weatherError)
    }

    @Test
    fun `refreshWeather should call refreshWeatherUseCase`() = runTest {
        // Given
        whenever(refreshWeatherUseCase.invoke()).thenReturn(Result.Success(testWeather))
        viewModel = createViewModel()

        // When
        viewModel.refreshWeather()
        advanceTimeBy(100)

        // Then
        verify(refreshWeatherUseCase, atLeastOnce()).invoke()
    }

    @Test
    fun `refreshWeather should update weather when success`() = runTest {
        // Given
        whenever(refreshWeatherUseCase.invoke()).thenReturn(Result.Success(testWeather))
        viewModel = createViewModel()

        // When
        viewModel.refreshWeather()
        advanceTimeBy(100)

        // Then
        assertEquals(testWeather, viewModel.uiState.value.weather)
        assertFalse(viewModel.uiState.value.isWeatherLoading)
        assertNull(viewModel.uiState.value.weatherError)
    }

    @Test
    fun `onResume should call refreshWeather`() = runTest {
        // Given
        whenever(refreshWeatherUseCase.invoke()).thenReturn(Result.Success(testWeather))
        viewModel = createViewModel()

        // When
        viewModel.onResume()
        advanceTimeBy(100)

        // Then
        verify(refreshWeatherUseCase, atLeastOnce()).invoke()
    }

    @Test
    fun `clock update should call getCurrentDateTimeUseCase repeatedly`() = runTest {
        // Given
        viewModel = createViewModel()

        // When - Turbineで複数の更新を確認
        viewModel.uiState.test {
            // 最初の3つの値を取得 (初期値 + 2回の更新)
            awaitItem() // 1回目
            awaitItem() // 2回目
            awaitItem() // 3回目

            // Then - 少なくとも2回呼ばれている
            verify(getCurrentDateTimeUseCase, atLeast(2)).invoke()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `track info update should call getCurrentTrackUseCase repeatedly`() = runTest {
        // Given
        viewModel = createViewModel()

        // When - Turbineで複数の更新を確認
        viewModel.uiState.test {
            awaitItem() // 1回目
            awaitItem() // 2回目

            // Then
            verify(getCurrentTrackUseCase, atLeast(2)).invoke()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentTrack should be updated`() = runTest {
        // Given
        viewModel = createViewModel()

        // When - Turbineで最初の値を取得
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertEquals(testMusic, state.currentTrack)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `news reading should wait 10 seconds before first read`() = runTest {
        // Given
        whenever(getLatestNewsUseCase.invoke(10)).thenReturn(Result.Success(emptyList()))
        viewModel = createViewModel()

        // When - 少しの間だけ監視
        viewModel.uiState.test {
            // 最初の数回の更新では、ニュース取得が呼ばれていないことを確認
            repeat(3) {
                awaitItem()
            }

            // Then - まだニュースは取得されていない（10秒待つため）
            verify(getLatestNewsUseCase, never()).invoke(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `MainUiState should have correct default values`() {
        // When
        val state = MainUiState()

        // Then
        assertEquals(DateTime.EMPTY, state.dateTime)
        assertNull(state.weather)
        assertFalse(state.isWeatherLoading)
        assertNull(state.weatherError)
        assertNull(state.currentNews)
        assertFalse(state.isNewsLoading)
        assertNull(state.newsError)
        assertNull(state.currentTrack)
        assertFalse(state.isMusicPlaying)
    }

    @Test
    fun `MainUiState copy should work correctly`() {
        // Given
        val state = MainUiState()

        // When
        val newState = state.copy(dateTime = testDateTime, weather = testWeather)

        // Then
        assertEquals(testDateTime, newState.dateTime)
        assertEquals(testWeather, newState.weather)
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(
            getCurrentDateTimeUseCase = getCurrentDateTimeUseCase,
            getWeatherUseCase = getWeatherUseCase,
            refreshWeatherUseCase = refreshWeatherUseCase,
            getLatestNewsUseCase = getLatestNewsUseCase,
            readNewsUseCase = readNewsUseCase,
            playMusicUseCase = playMusicUseCase,
            getCurrentTrackUseCase = getCurrentTrackUseCase,
            settingsRepository = settingsRepository
        )
    }
}
