# レイヤー構成詳細定義書

## 1. 概要

本ドキュメントはClean Architectureの各レイヤーの詳細な責務・ルール・実装ガイドラインを定義する。

---

## 2. Presentation Layer

### 2.1 責務
- UIの表示とユーザー入力の処理
- ViewModelからの状態変更の監視とUI反映
- ライフサイクル管理

### 2.2 構成要素

#### 2.2.1 Activity
**役割:**
- 画面の表示制御
- ViewModelのインスタンス化
- Viewの初期化と配置

**実装ルール:**
- ビジネスロジックを含めない
- データ加工は行わない
- ViewModelの状態を監視し、UIを更新するのみ

**例:**
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var clockView: ClockView
    private lateinit var weatherView: WeatherView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelFactory())
            .get(MainViewModel::class.java)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        clockView = findViewById(R.id.clock_view)
        weatherView = findViewById(R.id.weather_view)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: MainUiState) {
        clockView.updateDateTime(state.dateTime)
        weatherView.updateWeather(state.weather)
        // エラー表示など
    }
}
```

#### 2.2.2 ViewModel
**役割:**
- UI状態の管理
- UseCaseの実行
- エラーハンドリング

**実装ルール:**
- Android SDKへの依存を最小限に（ViewModelクラス以外）
- UI状態はStateFlowで公開
- UseCaseを通じてのみデータ取得

**例:**
```kotlin
class MainViewModel(
    private val getCurrentDateTimeUseCase: GetCurrentDateTimeUseCase,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val getLatestNewsUseCase: GetLatestNewsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startClockUpdate()
        loadWeather()
        startNewsReading()
    }

    private fun startClockUpdate() {
        viewModelScope.launch {
            while (isActive) {
                val dateTime = getCurrentDateTimeUseCase()
                _uiState.update { it.copy(dateTime = dateTime) }
                delay(1000) // 1秒ごとに更新
            }
        }
    }

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

    fun refreshWeather() {
        loadWeather()
    }
}
```

#### 2.2.3 Custom View
**役割:**
- 特定のUI要素の描画とアニメーション
- 独立したUI部品としての機能提供

**実装ルール:**
- データの取得・加工は行わない
- 受け取ったデータをそのまま表示
- アニメーション処理のみ内包

**例:**
```kotlin
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentDateTime: DateTime = DateTime.EMPTY
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun updateDateTime(dateTime: DateTime) {
        this.currentDateTime = dateTime
        invalidate() // 再描画
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTime(canvas)
        drawDate(canvas)
    }

    private fun drawTime(canvas: Canvas) {
        paint.textSize = 80f
        paint.color = Color.WHITE
        canvas.drawText(
            currentDateTime.timeString,
            50f, 100f, paint
        )
    }

    private fun drawDate(canvas: Canvas) {
        paint.textSize = 32f
        val dayColor = when (currentDateTime.dayOfWeek) {
            DayOfWeek.SUNDAY -> Color.RED
            DayOfWeek.SATURDAY -> Color.BLUE
            else -> Color.BLACK
        }
        paint.color = dayColor
        canvas.drawText(
            currentDateTime.dateString,
            50f, 150f, paint
        )
    }
}
```

### 2.3 UIState定義

```kotlin
data class MainUiState(
    // 時計
    val dateTime: DateTime = DateTime.EMPTY,

    // 天気
    val weather: Weather? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,

    // ニュース
    val currentNews: News? = null,
    val isNewsLoading: Boolean = false,
    val newsError: String? = null,

    // 音楽
    val currentTrack: Music? = null,
    val isMusicPlaying: Boolean = false
)

data class SetupUiState(
    val postalCode: String = "",
    val newsInterval: Int = 30,
    val isPostalCodeValid: Boolean = true,
    val isNewsIntervalValid: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null
)
```

---

## 3. Domain Layer

### 3.1 責務
- ビジネスロジックの実装
- ドメインモデルの定義
- データアクセスの抽象化（Repositoryインターフェース）

### 3.2 構成要素

#### 3.2.1 Entity（ドメインモデル）
**役割:**
- アプリケーションのコアとなるデータモデル
- ビジネスルールの表現

**実装ルール:**
- Pure Kotlin（Android SDKへの依存なし）
- Immutable（data class推奨）
- ビジネスロジックを含めてもよい

**例:**
```kotlin
data class DateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: DayOfWeek
) {
    val timeString: String
        get() = String.format("%02d:%02d", hour, minute)

    val dateString: String
        get() = String.format("%02d/%02d (%s)", month, day, dayOfWeek.shortName)

    companion object {
        val EMPTY = DateTime(0, 0, 0, 0, 0, DayOfWeek.SUNDAY)
    }
}

enum class DayOfWeek(val shortName: String) {
    SUNDAY("Sun"),
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat")
}

data class Weather(
    val condition: WeatherCondition,
    val currentTemperature: Int,
    val maxTemperature: Int,
    val minTemperature: Int,
    val precipitationProbability: Int
)

enum class WeatherCondition {
    SUNNY, CLOUDY, RAINY, SNOWY, OTHER
}

data class News(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: Long
)

data class Music(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long
)

data class Settings(
    val postalCode: String,
    val newsIntervalMinutes: Int
) {
    fun isValid(): Boolean {
        return postalCode.length == 7 &&
               postalCode.all { it.isDigit() } &&
               newsIntervalMinutes in 1..60
    }
}
```

#### 3.2.2 UseCase
**役割:**
- 単一の業務処理を表現
- Repositoryを使ってデータを取得・保存
- ビジネスロジックの実行

**実装ルール:**
- 1 UseCase = 1 責務
- suspend関数として定義（非同期処理）
- Repositoryインターフェースに依存

**命名規則:**
- `動詞 + 対象 + UseCase`（例: GetWeatherUseCase, SaveSettingsUseCase）

**例:**
```kotlin
class GetCurrentDateTimeUseCase {
    operator fun invoke(): DateTime {
        val now = Calendar.getInstance()
        return DateTime(
            year = now.get(Calendar.YEAR),
            month = now.get(Calendar.MONTH) + 1,
            day = now.get(Calendar.DAY_OF_MONTH),
            hour = now.get(Calendar.HOUR_OF_DAY),
            minute = now.get(Calendar.MINUTE),
            dayOfWeek = DayOfWeek.values()[now.get(Calendar.DAY_OF_WEEK) - 1]
        )
    }
}

class GetWeatherUseCase(
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Weather> {
        return try {
            val settings = settingsRepository.getSettings()
            weatherRepository.getWeather(settings.postalCode)
        } catch (e: Exception) {
            Result.Error(AppException.NetworkException(e.message ?: "Unknown error"))
        }
    }
}

class SaveSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings): Result<Unit> {
        return if (settings.isValid()) {
            settingsRepository.saveSettings(settings)
        } else {
            Result.Error(AppException.SettingsException("Invalid settings"))
        }
    }
}
```

#### 3.2.3 Repository Interface
**役割:**
- データアクセスの抽象化
- Data Layerとの境界

**実装ルール:**
- インターフェースのみ定義（実装はData Layerで）
- suspend関数として定義
- Result型で結果を返す

**例:**
```kotlin
interface WeatherRepository {
    suspend fun getWeather(postalCode: String): Result<Weather>
}

interface NewsRepository {
    suspend fun getLatestNews(count: Int): Result<List<News>>
}

interface SettingsRepository {
    suspend fun getSettings(): Settings
    suspend fun saveSettings(settings: Settings): Result<Unit>
    suspend fun hasSettings(): Boolean
}

interface MusicRepository {
    fun getAllTracks(): List<Music>
    fun playTrack(trackId: String)
    fun stopTrack()
    fun getCurrentTrack(): Music?
}
```

---

## 4. Data Layer

### 4.1 責務
- データの永続化
- 外部APIとの通信
- DTOとEntityの相互変換

### 4.2 構成要素

#### 4.2.1 Repository Implementation
**役割:**
- Repositoryインターフェースの実装
- DataSourceを使ってデータ取得
- DTOをEntityに変換

**実装ルール:**
- Domainで定義されたインターフェースを実装
- DataSourceに依存
- エラーハンドリングを実施

**例:**
```kotlin
class WeatherRepositoryImpl(
    private val weatherApiDataSource: WeatherApiDataSource
) : WeatherRepository {

    override suspend fun getWeather(postalCode: String): Result<Weather> {
        return try {
            val dto = weatherApiDataSource.fetchWeather(postalCode)
            val weather = dto.toEntity()
            Result.Success(weather)
        } catch (e: Exception) {
            Result.Error(AppException.NetworkException(e.message ?: "Failed to fetch weather"))
        }
    }
}

class SettingsRepositoryImpl(
    private val settingsLocalDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override suspend fun getSettings(): Settings {
        return settingsLocalDataSource.loadSettings()
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> {
        return try {
            settingsLocalDataSource.saveSettings(settings)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.SettingsException(e.message ?: "Failed to save settings"))
        }
    }

    override suspend fun hasSettings(): Boolean {
        return settingsLocalDataSource.hasSettings()
    }
}
```

#### 4.2.2 DataSource
**役割:**
- 実際のデータ取得処理
- API通信、ファイルIO、SharedPreferencesアクセス

**実装ルール:**
- DTOで結果を返す
- 低レベルのエラー（IOExceptionなど）をスロー
- ビジネスロジックを含めない

**例:**
```kotlin
class WeatherApiDataSource(
    private val httpClient: OkHttpClient
) {
    suspend fun fetchWeather(postalCode: String): WeatherDto = withContext(Dispatchers.IO) {
        val areaCode = convertPostalCodeToAreaCode(postalCode)
        val url = "https://weather.example.com/api/forecast/$areaCode"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP Error: ${response.code}")
        }

        val json = response.body?.string() ?: throw IOException("Empty response")
        Gson().fromJson(json, WeatherDto::class.java)
    }

    private fun convertPostalCodeToAreaCode(postalCode: String): String {
        // 郵便番号 → 地域コード変換ロジック
        // 実際はマッピングテーブルを使用
        return "130010" // 例: 東京
    }
}

class SettingsLocalDataSource(
    private val sharedPreferences: SharedPreferences
) {
    fun loadSettings(): Settings {
        val postalCode = sharedPreferences.getString(KEY_POSTAL_CODE, "") ?: ""
        val newsInterval = sharedPreferences.getInt(KEY_NEWS_INTERVAL, 30)
        return Settings(postalCode, newsInterval)
    }

    fun saveSettings(settings: Settings) {
        sharedPreferences.edit()
            .putString(KEY_POSTAL_CODE, settings.postalCode)
            .putInt(KEY_NEWS_INTERVAL, settings.newsIntervalMinutes)
            .apply()
    }

    fun hasSettings(): Boolean {
        return sharedPreferences.contains(KEY_POSTAL_CODE)
    }

    companion object {
        private const val KEY_POSTAL_CODE = "postal_code"
        private const val KEY_NEWS_INTERVAL = "news_interval"
    }
}
```

#### 4.2.3 DTO (Data Transfer Object)
**役割:**
- APIレスポンスやDBレコードの表現
- シリアライズ/デシリアライズ

**実装ルール:**
- Gsonアノテーション付きdata class
- Entityへの変換メソッドを持つ

**例:**
```kotlin
data class WeatherDto(
    @SerializedName("weather")
    val weather: String,
    @SerializedName("temp")
    val temperature: Int,
    @SerializedName("temp_max")
    val maxTemperature: Int,
    @SerializedName("temp_min")
    val minTemperature: Int,
    @SerializedName("pop")
    val precipitationProbability: Int
) {
    fun toEntity(): Weather {
        val condition = when (weather) {
            "晴れ" -> WeatherCondition.SUNNY
            "曇り" -> WeatherCondition.CLOUDY
            "雨" -> WeatherCondition.RAINY
            "雪" -> WeatherCondition.SNOWY
            else -> WeatherCondition.OTHER
        }
        return Weather(
            condition = condition,
            currentTemperature = temperature,
            maxTemperature = maxTemperature,
            minTemperature = minTemperature,
            precipitationProbability = precipitationProbability
        )
    }
}
```

---

## 5. レイヤー間の依存ルール

### 5.1 許可される依存

```
Presentation → Domain ✓
Domain → Data ✗ (インターフェースのみ)
Data → Domain ✓ (Entityの使用)
```

### 5.2 禁止される依存

```
Domain → Presentation ✗
Data → Presentation ✗
Domain → Framework (Android SDK) ✗
```

### 5.3 依存性逆転の原則

Domain LayerがRepositoryインターフェースを定義し、Data Layerがそれを実装することで、依存性を逆転させる。

```
[Domain Layer]
  ↑ 依存
[Data Layer] ← Repository実装
```

### 5.4 既知の例外（技術的負債）

#### 5.4.1 `domain/util/DeviceUtils`

`domain/util/DeviceUtils`（`isTV` / `isStrictTelevision` / `isPhone` /
`hasTouchscreen` / `getScreenSizeCategory` / `isLargeScreen`）は
`android.app.UiModeManager` / `android.content.Context` /
`android.content.pm.PackageManager` / `android.content.res.Configuration` /
`android.util.Log` / `android.os.Build` を直接importしており、
5.2の「Domain → Framework (Android SDK) ✗」に反する既知の違反。

**違反を許容している理由:**
- デバイス種別（TV / スマホ）や画面サイズの判定はOS APIの結果そのものであり、
  ビジネスルールを含まない単純な参照透過な関数（Entityやビジネスロジックへの依存なし）。
- Presentation Layer（`MainActivity` / `SplashActivity` / `SetupActivity` /
  `NewsView`）から広く参照されており、Repository経由に抽象化するほどの
  ビジネス上の意味（永続化・外部通信・キャッシュなど）を持たない。

**今回（Issue #122）追加された `isStrictTelevision` / 診断ログについて:**
Issue #122対応で `isStrictTelevision` を追加した際、`android.util.Log` /
`android.os.Build` の依存を新たに追加した（TV判定の判定材料が食い違った
ケースを収集するための診断ログ。詳細は
`requirement/issue-122-edge-to-edge-tv-detection.md` を参照）。
既存の違反パターンを踏襲する形だが、Domain層のAndroid依存をこれ以上
拡大させないため、新規のDomain層コードでは同様のAndroid SDK直接参照を
追加しないこと。

**中期的な解消方針（未着手・別Issue化を推奨）:**
- `presentation/platform`（または`data`）に `DeviceInfoProvider` 等の
  実装を置き、Domain層には `interface DeviceInfoRepository` のみを
  定義する形に置き換える。
- 診断ログは `Log` 直呼び出しではなく、Data/Presentation層に置いた
  ロガー実装をDomain側のインターフェース経由で呼び出す形にする。

---

## 6. 通信フロー例

### 6.1 天気情報取得フロー

```
1. [UI] ユーザーが画面表示
     ↓
2. [MainActivity] onCreate()でViewModelを初期化
     ↓
3. [MainViewModel] init{}でloadWeather()実行
     ↓
4. [MainViewModel] getWeatherUseCase()を呼び出し
     ↓
5. [GetWeatherUseCase] settingsRepository.getSettings()で郵便番号取得
     ↓
6. [GetWeatherUseCase] weatherRepository.getWeather(postalCode)呼び出し
     ↓
7. [WeatherRepositoryImpl] weatherApiDataSource.fetchWeather(postalCode)呼び出し
     ↓
8. [WeatherApiDataSource] HTTPリクエスト送信
     ↓
9. [External API] レスポンス返却
     ↓
10. [WeatherApiDataSource] WeatherDtoを返す
     ↓
11. [WeatherRepositoryImpl] dto.toEntity()でWeatherに変換
     ↓
12. [WeatherRepositoryImpl] Result.Success(weather)を返す
     ↓
13. [GetWeatherUseCase] Result<Weather>を返す
     ↓
14. [MainViewModel] uiStateを更新
     ↓
15. [MainActivity] StateFlowの変化を検知してUI更新
```

---

## 7. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**次のステップ:**
各機能モジュールの詳細設計書を作成する。
