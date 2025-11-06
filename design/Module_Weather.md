# モジュール設計書 - 天気機能

## 1. 概要

天気機能は、ユーザーが設定した郵便番号に基づいて天気情報を取得し、画面右側に表示する機能。

---

## 2. 機能要件（再掲）

### 2.1 表示項目
- 天気アイコン（晴れ、曇り、雨、雪など）
- 現在気温
- 最高気温
- 最低気温
- 降水確率

### 2.2 更新タイミング
- アプリがForegroundになったとき
- 30分経過時

### 2.3 配置
- 画面右側

### 2.4 エラー処理
- API接続エラー時: 該当箇所にエラーメッセージを表示
- 他の機能は正常動作を継続

---

## 3. アーキテクチャ設計

### 3.1 レイヤー構成

```
[Presentation Layer]
  - WeatherView (Custom View)
  - MainViewModel

[Domain Layer]
  - GetWeatherUseCase
  - RefreshWeatherUseCase
  - Weather (Entity)
  - WeatherCondition (Enum)
  - WeatherRepository (Interface)

[Data Layer]
  - WeatherRepositoryImpl
  - WeatherApiDataSource
  - WeatherDto
  - PostalCodeConverter
```

---

## 4. クラス設計

### 4.1 Domain Layer

#### 4.1.1 Weather Entity

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 天気情報を表すエンティティ
 */
data class Weather(
    val condition: WeatherCondition,
    val currentTemperature: Int,
    val maxTemperature: Int,
    val minTemperature: Int,
    val precipitationProbability: Int
) {
    companion object {
        /**
         * 空のWeather（初期値用）
         */
        val EMPTY = Weather(
            condition = WeatherCondition.OTHER,
            currentTemperature = 0,
            maxTemperature = 0,
            minTemperature = 0,
            precipitationProbability = 0
        )
    }
}
```

#### 4.1.2 WeatherCondition Enum

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 天気状態を表すEnum
 */
enum class WeatherCondition(val displayName: String) {
    SUNNY("晴れ"),
    CLOUDY("曇り"),
    RAINY("雨"),
    SNOWY("雪"),
    OTHER("その他");

    /**
     * アイコンリソースIDを取得
     */
    fun getIconResourceId(): Int {
        return when (this) {
            SUNNY -> R.drawable.ic_weather_sunny
            CLOUDY -> R.drawable.ic_weather_cloudy
            RAINY -> R.drawable.ic_weather_rainy
            SNOWY -> R.drawable.ic_weather_snowy
            OTHER -> R.drawable.ic_weather_other
        }
    }
}
```

#### 4.1.3 WeatherRepository Interface

```kotlin
package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.common.Result

/**
 * 天気情報を取得するリポジトリのインターフェース
 */
interface WeatherRepository {
    /**
     * 指定された郵便番号の天気情報を取得
     * @param postalCode 郵便番号（7桁）
     * @return 天気情報（Result型）
     */
    suspend fun getWeather(postalCode: String): Result<Weather>
}
```

#### 4.1.4 GetWeatherUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.repository.WeatherRepository
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.common.Result

/**
 * 天気情報を取得するユースケース
 */
class GetWeatherUseCase(
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定された郵便番号の天気情報を取得
     * @return 天気情報（Result型）
     */
    suspend operator fun invoke(): Result<Weather> {
        return try {
            val settings = settingsRepository.getSettings()
            weatherRepository.getWeather(settings.postalCode)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

#### 4.1.5 RefreshWeatherUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.common.Result

/**
 * 天気情報を強制的に再取得するユースケース
 */
class RefreshWeatherUseCase(
    private val getWeatherUseCase: GetWeatherUseCase
) {
    /**
     * 天気情報を強制的に再取得
     * @return 天気情報（Result型）
     */
    suspend operator fun invoke(): Result<Weather> {
        return getWeatherUseCase()
    }
}
```

### 4.2 Data Layer

#### 4.2.1 WeatherDto

```kotlin
package com.tinygc.asachiru.data.dto

import com.google.gson.annotations.SerializedName
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition

/**
 * 天気APIのレスポンスDTO
 */
data class WeatherApiResponse(
    @SerializedName("forecasts")
    val forecasts: List<ForecastDto>
)

data class ForecastDto(
    @SerializedName("date")
    val date: String,

    @SerializedName("telop")
    val telop: String, // "晴れ", "曇り", "雨"など

    @SerializedName("temperature")
    val temperature: TemperatureDto
)

data class TemperatureDto(
    @SerializedName("min")
    val min: TemperatureValueDto?,

    @SerializedName("max")
    val max: TemperatureValueDto?
)

data class TemperatureValueDto(
    @SerializedName("celsius")
    val celsius: String
)

/**
 * WeatherDtoからWeatherエンティティへ変換
 */
fun ForecastDto.toEntity(currentTemp: Int, precipProb: Int): Weather {
    val condition = parseWeatherCondition(telop)

    return Weather(
        condition = condition,
        currentTemperature = currentTemp,
        maxTemperature = temperature.max?.celsius?.toIntOrNull() ?: 0,
        minTemperature = temperature.min?.celsius?.toIntOrNull() ?: 0,
        precipitationProbability = precipProb
    )
}

/**
 * 天気文字列をWeatherConditionに変換
 */
private fun parseWeatherCondition(telop: String): WeatherCondition {
    return when {
        telop.contains("晴") -> WeatherCondition.SUNNY
        telop.contains("曇") -> WeatherCondition.CLOUDY
        telop.contains("雨") -> WeatherCondition.RAINY
        telop.contains("雪") -> WeatherCondition.SNOWY
        else -> WeatherCondition.OTHER
    }
}
```

#### 4.2.2 WeatherApiDataSource

```kotlin
package com.tinygc.asachiru.data.datasource.remote

import com.google.gson.Gson
import com.tinygc.asachiru.data.dto.WeatherApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 天気APIからデータを取得するデータソース
 */
class WeatherApiDataSource(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        // 天気予報API（livedoor天気互換）
        private const val BASE_URL = "https://weather.tsukumijima.net/api/forecast"
    }

    /**
     * 天気情報を取得
     * @param areaCode 地域コード
     * @return WeatherApiResponse
     * @throws IOException ネットワークエラー
     */
    suspend fun fetchWeather(areaCode: String): WeatherApiResponse = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?city=$areaCode"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP Error: ${response.code}")
        }

        val json = response.body?.string() ?: throw IOException("Empty response")
        gson.fromJson(json, WeatherApiResponse::class.java)
    }
}
```

#### 4.2.3 PostalCodeConverter

```kotlin
package com.tinygc.asachiru.data.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 郵便番号を地域コードに変換するユーティリティ
 *
 * 実装方針:
 * - 郵便番号の上位3桁をキーとして地域コードにマッピング
 * - マッピングデータはassets/postal_code_mapping.jsonから読み込み
 * - 全国の郵便番号に対応（約1,000エントリ）
 */
object PostalCodeConverter {

    // マッピングテーブル（遅延初期化）
    private var postalCodeToAreaCodeMap: Map<String, String>? = null

    /**
     * マッピングテーブルを初期化
     * @param context Androidコンテキスト
     */
    fun initialize(context: Context) {
        if (postalCodeToAreaCodeMap != null) return

        // assets/postal_code_mapping.jsonから読み込み
        val json = context.assets.open("postal_code_mapping.json").bufferedReader().use {
            it.readText()
        }

        val type = object : TypeToken<Map<String, String>>() {}.type
        postalCodeToAreaCodeMap = Gson().fromJson(json, type)
    }

    /**
     * 郵便番号を地域コードに変換
     * @param postalCode 郵便番号（7桁）
     * @return 地域コード
     * @throws IllegalArgumentException 郵便番号が不正、または変換できない
     * @throws IllegalStateException initialize()が呼ばれていない
     */
    fun convertToAreaCode(postalCode: String): String {
        require(postalCode.length == 7) {
            "郵便番号は7桁で入力してください（例: 1000001）"
        }
        require(postalCode.all { it.isDigit() }) {
            "郵便番号は数字のみで入力してください"
        }

        val map = postalCodeToAreaCodeMap
            ?: throw IllegalStateException("PostalCodeConverter is not initialized")

        // 上位3桁でマッピング
        val prefix = postalCode.substring(0, 3)
        return map[prefix]
            ?: throw IllegalArgumentException(
                "郵便番号「$postalCode」に対応する地域が見つかりませんでした。\n" +
                "郵便番号が正しいかご確認ください。"
            )
    }

    /**
     * サンプルのマッピングテーブル（開発用）
     * 実際の実装では、assets/postal_code_mapping.jsonに以下の形式で格納:
     * {
     *   "001": "016010",  // 北海道札幌市
     *   "002": "016010",
     *   ...
     *   "100": "130010",  // 東京都千代田区
     *   "101": "130010",
     *   ...
     *   "530": "270000",  // 大阪府大阪市
     *   ...
     * }
     *
     * 全国の郵便番号→地域コードマッピングは、以下のリソースを参考に作成:
     * - 日本郵便の郵便番号データ
     * - 気象庁の地域コード一覧
     */
    fun getSampleMapping(): Map<String, String> = mapOf(
        // 北海道
        "001" to "016010", "002" to "016010", "003" to "016010", "004" to "016010",
        "060" to "016010", "061" to "016010", "062" to "016010", "063" to "016010",

        // 東京都
        "100" to "130010", "101" to "130010", "102" to "130010", "103" to "130010",
        "104" to "130010", "105" to "130010", "106" to "130010", "107" to "130010",
        "108" to "130010", "109" to "130010", "110" to "130010", "111" to "130010",

        // 大阪府
        "530" to "270000", "531" to "270000", "532" to "270000", "533" to "270000",
        "534" to "270000", "535" to "270000", "536" to "270000", "540" to "270000",

        // その他主要都市は実装時に追加
        // ... 全国約1,000エントリ
    )
}
```

**注意事項:**
実装時には、`assets/postal_code_mapping.json`ファイルを作成し、全国の郵便番号→地域コードマッピングを格納すること。
Application.onCreate()でPostalCodeConverter.initialize(context)を呼び出すこと。
```

#### 4.2.4 WeatherRepositoryImpl

```kotlin
package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.remote.WeatherApiDataSource
import com.tinygc.asachiru.data.dto.toEntity
import com.tinygc.asachiru.data.util.PostalCodeConverter
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.repository.WeatherRepository
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.common.AppException
import java.io.IOException

/**
 * WeatherRepositoryの実装
 */
class WeatherRepositoryImpl(
    private val weatherApiDataSource: WeatherApiDataSource
) : WeatherRepository {

    override suspend fun getWeather(postalCode: String): Result<Weather> {
        return try {
            // 郵便番号を地域コードに変換
            val areaCode = PostalCodeConverter.convertToAreaCode(postalCode)

            // APIからデータ取得
            val apiResponse = weatherApiDataSource.fetchWeather(areaCode)

            // 今日の予報を取得（forecasts[0]）
            val todayForecast = apiResponse.forecasts.firstOrNull()
                ?: throw IOException("No forecast data available")

            // DTOをEntityに変換
            // 注: 現在気温と降水確率はAPIレスポンスに含まれない場合があるため、
            //     ここでは簡易的に最高気温を現在気温、降水確率を0%としている
            val currentTemp = todayForecast.temperature.max?.celsius?.toIntOrNull() ?: 0
            val weather = todayForecast.toEntity(currentTemp, 0)

            Result.Success(weather)
        } catch (e: IOException) {
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: IllegalArgumentException) {
            Result.Error(AppException.ApiException(400, e.message ?: "Invalid postal code"))
        } catch (e: Exception) {
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse weather data"))
        }
    }
}
```

### 4.3 Presentation Layer

#### 4.3.1 MainViewModel（天気部分のみ抜粋）

```kotlin
package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.usecase.weather.GetWeatherUseCase
import com.tinygc.asachiru.domain.usecase.weather.RefreshWeatherUseCase
import com.tinygc.asachiru.domain.common.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val refreshWeatherUseCase: RefreshWeatherUseCase,
    // ... 他のUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadWeather()
        startWeatherAutoRefresh()
        // ... 他の初期化処理
    }

    /**
     * 天気情報を取得
     */
    private fun loadWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }

            when (val result = getWeatherUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            weather = result.data,
                            isWeatherLoading = false,
                            weatherError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isWeatherLoading = false,
                            weatherError = result.exception.message
                        )
                    }
                }
            }
        }
    }

    /**
     * 天気情報の自動更新を開始（30分ごと）
     */
    private fun startWeatherAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30 * 60 * 1000L) // 30分
                refreshWeather()
            }
        }
    }

    /**
     * 天気情報を手動で再取得
     */
    fun refreshWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }

            when (val result = refreshWeatherUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            weather = result.data,
                            isWeatherLoading = false,
                            weatherError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isWeatherLoading = false,
                            weatherError = result.exception.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Foreground復帰時の処理
     */
    fun onResume() {
        refreshWeather()
    }
}
```

#### 4.3.2 WeatherView

```kotlin
package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.Weather

/**
 * 天気情報を表示するカスタムビュー
 */
class WeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weather: Weather? = null
    private var errorMessage: String? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.WHITE
    }

    /**
     * 天気情報を更新
     */
    fun updateWeather(weather: Weather?) {
        this.weather = weather
        this.errorMessage = null
        invalidate()
    }

    /**
     * エラーメッセージを表示
     */
    fun showError(message: String) {
        this.errorMessage = message
        this.weather = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (errorMessage != null) {
            drawError(canvas)
            return
        }

        weather?.let {
            drawWeatherIcon(canvas, it)
            drawTemperature(canvas, it)
            drawPrecipitation(canvas, it)
        }
    }

    private fun drawError(canvas: Canvas) {
        textPaint.color = Color.RED
        canvas.drawText(
            "Error: $errorMessage",
            50f, 50f, textPaint
        )
    }

    private fun drawWeatherIcon(canvas: Canvas, weather: Weather) {
        // アイコン描画（ビットマップ使用）
        // 実装はリソースから読み込んで描画
    }

    private fun drawTemperature(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.WHITE
        canvas.drawText(
            "現在: ${weather.currentTemperature}°C",
            50f, 100f, textPaint
        )
        canvas.drawText(
            "最高: ${weather.maxTemperature}°C",
            50f, 130f, textPaint
        )
        canvas.drawText(
            "最低: ${weather.minTemperature}°C",
            50f, 160f, textPaint
        )
    }

    private fun drawPrecipitation(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.CYAN
        canvas.drawText(
            "降水確率: ${weather.precipitationProbability}%",
            50f, 190f, textPaint
        )
    }
}
```

---

## 5. テスト設計

### 5.1 単体テスト

#### 5.1.1 GetWeatherUseCaseTest

```kotlin
package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.repository.WeatherRepository
import com.tinygc.asachiru.domain.common.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class GetWeatherUseCaseTest {

    @Mock
    private lateinit var weatherRepository: WeatherRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var useCase: GetWeatherUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GetWeatherUseCase(weatherRepository, settingsRepository)
    }

    @Test
    fun `invoke should return weather when repository returns success`() = runBlocking {
        // Arrange
        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)
        val expectedWeather = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 20,
            maxTemperature = 25,
            minTemperature = 15,
            precipitationProbability = 10
        )

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Success(expectedWeather))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedWeather, (result as Result.Success).data)
    }

    @Test
    fun `invoke should return error when repository returns error`() = runBlocking {
        // Arrange
        val postalCode = "1000001"
        val settings = Settings(postalCode, 30)
        val exception = Exception("Network error")

        whenever(settingsRepository.getSettings()).thenReturn(settings)
        whenever(weatherRepository.getWeather(postalCode))
            .thenReturn(Result.Error(exception))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result is Result.Error)
    }
}
```

#### 5.1.2 PostalCodeConverterTest

```kotlin
package com.tinygc.asachiru.data.util

import org.junit.Assert.*
import org.junit.Test

class PostalCodeConverterTest {

    @Test
    fun `convertToAreaCode should return correct area code for Tokyo`() {
        // Arrange
        val postalCode = "1000001" // 千代田区

        // Act
        val areaCode = PostalCodeConverter.convertToAreaCode(postalCode)

        // Assert
        assertEquals("130010", areaCode)
    }

    @Test
    fun `convertToAreaCode should throw exception for invalid length`() {
        // Arrange
        val postalCode = "123" // 不正な桁数

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            PostalCodeConverter.convertToAreaCode(postalCode)
        }
    }

    @Test
    fun `convertToAreaCode should throw exception for non-digit`() {
        // Arrange
        val postalCode = "12345ab" // 数字以外を含む

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            PostalCodeConverter.convertToAreaCode(postalCode)
        }
    }
}
```

---

## 6. シーケンス図

```
[MainActivity] [MainViewModel] [GetWeatherUseCase] [SettingsRepo] [WeatherRepo] [WeatherApiDataSource] [API]
     |              |                  |                 |              |               |              |
     |--onResume()->|                  |                 |              |               |              |
     |              |--invoke()------->|                 |              |               |              |
     |              |                  |--getSettings()->|              |               |              |
     |              |                  |<--Settings------|              |               |              |
     |              |                  |--getWeather(postalCode)------->|               |              |
     |              |                  |                 |              |--fetchWeather()->            |
     |              |                  |                 |              |               |--HTTP GET-->|
     |              |                  |                 |              |               |<--Response--|
     |              |                  |                 |              |<--WeatherDto--|              |
     |              |                  |<--Result<Weather>--------------|               |              |
     |              |<--Result<Weather>|                 |              |               |              |
     |              |--update uiState->|                 |              |               |              |
     |<--StateFlow--|                  |                 |              |               |              |
     |--updateWeather()->[WeatherView] |                 |              |               |              |
```

---

## 7. ファイル構成

```
domain/
├── entity/
│   ├── Weather.kt
│   └── WeatherCondition.kt
├── repository/
│   └── WeatherRepository.kt
└── usecase/
    └── weather/
        ├── GetWeatherUseCase.kt
        └── RefreshWeatherUseCase.kt

data/
├── repository/
│   └── WeatherRepositoryImpl.kt
├── datasource/
│   └── remote/
│       └── WeatherApiDataSource.kt
├── dto/
│   └── WeatherDto.kt
└── util/
    └── PostalCodeConverter.kt

presentation/
└── main/
    ├── MainViewModel.kt
    └── views/
        └── WeatherView.kt
```

---

## 8. 外部API仕様

### 8.1 天気予報API

**エンドポイント:**
```
GET https://weather.tsukumijima.net/api/forecast?city={areaCode}
```

**パラメータ:**
- `city`: 地域コード（例: 130010 = 東京）

**レスポンス例:**
```json
{
  "forecasts": [
    {
      "date": "2025-11-06",
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
```

---

## 9. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**次のステップ:**
ニュース機能のモジュール設計書を作成する。
