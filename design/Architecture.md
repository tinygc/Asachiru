# アーキテクチャ設計書

## 1. 概要

本ドキュメントは「朝チル (AsaChil)」Android TVアプリケーションのアーキテクチャ設計を定義する。Clean Architectureの原則に従い、保守性・拡張性・テスタビリティの高いアプリケーションを実現する。

---

## 2. アーキテクチャパターン

### 2.1 採用パターン
**Clean Architecture**

Clean Architectureを採用することで、以下の利点を得る:
- ビジネスロジックとフレームワークの分離
- 外部依存の抽象化による変更容易性
- 単体テストの容易性向上
- レイヤー間の依存関係の明確化

### 2.2 依存性の方向

依存性は常に外側から内側に向かう（依存性逆転の原則）:

```
Presentation Layer (UI)
    ↓ (依存)
Domain Layer (UseCase)
    ↓ (依存)
Data Layer (Repository)
```

---

## 3. レイヤー構成

### 3.1 レイヤー概要

```
┌─────────────────────────────────────────┐
│   Presentation Layer (UI)               │
│   - Activity / Fragment                 │
│   - ViewModel                           │
│   - Custom View                         │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   Domain Layer (Business Logic)         │
│   - UseCase                             │
│   - Entity                              │
│   - Repository Interface                │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   Data Layer (Data Access)              │
│   - Repository Implementation           │
│   - DataSource (API, Local)             │
│   - DTO (Data Transfer Object)          │
└─────────────────────────────────────────┐

┌─────────────────────────────────────────┐
│   Framework Layer                       │
│   - Android SDK                         │
│   - Third-party Libraries               │
└─────────────────────────────────────────┘
```

### 3.2 各レイヤーの責務

#### 3.2.1 Presentation Layer
**責務:**
- ユーザーインターフェースの表示
- ユーザー入力の受付
- ViewModelからのデータをUIに反映

**主要コンポーネント:**
- `MainActivity`: メイン画面のActivity
- `SetupActivity`: 初回設定画面のActivity
- `MainViewModel`: メイン画面の状態管理
- `SetupViewModel`: 設定画面の状態管理
- Custom Views: 時計、天気、ビジュアライザーなどのカスタムビュー

**禁止事項:**
- ビジネスロジックの実装
- 直接的なデータアクセス

#### 3.2.2 Domain Layer
**責務:**
- ビジネスロジックの実装
- ユースケースの定義
- ドメインモデル（Entity）の定義

**主要コンポーネント:**
- UseCases: 各機能のビジネスロジック
- Entities: ドメインモデル
- Repository Interfaces: データアクセスの抽象化

**特徴:**
- Android SDKへの依存なし（Pure Kotlin/Java）
- フレームワークに依存しない

#### 3.2.3 Data Layer
**責務:**
- データの永続化
- 外部APIとの通信
- データソースの管理

**主要コンポーネント:**
- Repository Implementations: Repositoryインターフェースの実装
- DataSources: API/Local/Remoteデータソース
- DTOs: データ転送オブジェクト

---

## 4. パッケージ構成

```
com.tinygc.asachiru/
├── presentation/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   ├── MainViewModel.kt
│   │   ├── MainUiState.kt
│   │   ├── NewsReadingState.kt
│   │   ├── NewsReadingEvent.kt
│   │   ├── NewsReadingStateMachine.kt
│   │   └── views/
│   │       ├── ClockView.kt
│   │       ├── WeatherView.kt
│   │       ├── NewsView.kt
│   │       ├── NewsDebugView.kt
│   │       ├── MusicTrackView.kt
│   │       ├── VisualizerView.kt
│   │       ├── BackgroundGradientView.kt
│   │       ├── ParticleView.kt
│   │       └── DotPatternView.kt
│   ├── setup/
│   │   ├── SetupActivity.kt
│   │   ├── SetupViewModel.kt
│   │   └── SetupUiState.kt
│   ├── splash/
│   │   ├── SplashActivity.kt
│   │   └── SplashViewModel.kt
│   ├── common/
│   │   └── ViewModelFactory.kt
│   └── util/
│       ├── TtsManager.kt
│       ├── MusicPlayer.kt
│       └── FlowTimer.kt
│
├── domain/
│   ├── entity/
│   │   ├── DateTime.kt
│   │   ├── DayOfWeek.kt
│   │   ├── Weather.kt
│   │   ├── WeatherCondition.kt
│   │   ├── News.kt
│   │   ├── Music.kt
│   │   └── Settings.kt
│   ├── model/
│   │   └── News.kt
│   ├── repository/
│   │   ├── WeatherRepository.kt
│   │   ├── NewsRepository.kt
│   │   ├── SettingsRepository.kt
│   │   └── MusicRepository.kt
│   ├── common/
│   │   ├── Result.kt
│   │   ├── AppException.kt
│   │   ├── ITtsManager.kt
│   │   └── IMusicPlayer.kt
│   └── usecase/
│       ├── clock/
│       │   ├── GetCurrentDateTimeUseCase.kt
│       │   └── ConvertTimestampToDateTimeUseCase.kt
│       ├── weather/
│       │   ├── GetWeatherUseCase.kt
│       │   └── RefreshWeatherUseCase.kt
│       ├── news/
│       │   ├── GetLatestNewsUseCase.kt
│       │   └── ReadNewsUseCase.kt
│       ├── music/
│       │   ├── PlayMusicUseCase.kt
│       │   └── GetCurrentTrackUseCase.kt
│       └── settings/
│           ├── SaveSettingsUseCase.kt
│           ├── GetSettingsUseCase.kt
│           └── CheckSettingsExistUseCase.kt
│
├── data/
│   ├── repository/
│   │   ├── WeatherRepositoryImpl.kt
│   │   ├── NewsRepositoryImpl.kt
│   │   ├── SettingsRepositoryImpl.kt
│   │   ├── MusicRepositoryImpl.kt
│   │   └── AdRepository.kt
│   ├── datasource/
│   │   ├── remote/
│   │   │   ├── WeatherApiDataSource.kt
│   │   │   └── NewsRssDataSource.kt
│   │   └── local/
│   │       ├── SettingsLocalDataSource.kt
│   │       └── MusicLocalDataSource.kt
│   ├── dto/
│   │   ├── WeatherDto.kt
│   │   └── NewsDto.kt
│   ├── util/
│   │   ├── PostalCodeConverter.kt
│   │   └── RssParser.kt
│   └── RssPresets.kt
│
├── di/
│   ├── RepositoryFactory.kt
│   ├── DataSourceFactory.kt
│   └── UseCaseFactory.kt
│
└── AsaChiruApplication.kt
```

---

## 5. データフロー

### 5.1 天気情報取得の例

```
[UI] MainActivity
  ↓ ユーザーアクション/自動更新
[ViewModel] MainViewModel
  ↓ execute()
[UseCase] GetWeatherUseCase
  ↓ fetch()
[Repository Interface] WeatherRepository
  ↓ 実装
[Repository Impl] WeatherRepositoryImpl
  ↓ fetch()
[DataSource] WeatherApiDataSource
  ↓ HTTP Request
[External API] 天気予報API
  ↓ HTTP Response
[DataSource] WeatherApiDataSource (DTOを返す)
  ↓ map to Entity
[Repository Impl] WeatherRepositoryImpl (Entityを返す)
  ↓ return
[UseCase] GetWeatherUseCase (Entityを返す)
  ↓ update State
[ViewModel] MainViewModel (StateFlow更新)
  ↓ observe
[UI] MainActivity (UI更新)
```

---

## 6. 依存性注入

### 6.1 依存性注入方式
**手動DIパターン**

初期バージョンでは複雑なDIフレームワークは不要と判断し、以下の方針で実装する:
- ファクトリーパターンによるインスタンス生成
- シングルトンパターンによる共有リソース管理
- コンストラクタインジェクション

### 6.2 主要ファクトリークラス

```kotlin
// 例: UseCaseFactory
object UseCaseFactory {
    fun createGetWeatherUseCase(): GetWeatherUseCase {
        val repository = RepositoryFactory.createWeatherRepository()
        return GetWeatherUseCase(repository)
    }
}

// 例: RepositoryFactory
object RepositoryFactory {
    fun createWeatherRepository(): WeatherRepository {
        val dataSource = DataSourceFactory.createWeatherApiDataSource()
        return WeatherRepositoryImpl(dataSource)
    }
}
```

### 6.3 将来の拡張
プロジェクトの成長に応じて、以下のDIフレームワーク導入を検討:
- Hilt (推奨)
- Koin

---

## 7. 状態管理

### 7.1 状態管理パターン
**StateFlow + ViewModel**

各ViewModelで以下のパターンを採用:
- `StateFlow`による状態の公開
- `MutableStateFlow`による内部状態の管理
- UIはStateFlowを監視して自動更新

### 7.2 状態の定義例

```kotlin
data class MainUiState(
    val dateTime: DateTime = DateTime.EMPTY,
    val weather: Weather? = null,
    val currentNews: News? = null,
    val currentTrack: Music? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,
    val isNewsLoading: Boolean = false,
    val newsError: String? = null
)
```

---

## 8. エラーハンドリング

### 8.1 エラーハンドリング戦略

**階層的エラーハンドリング:**
1. **Data Layer**: 例外をキャッチしてResult型で返す
2. **Domain Layer**: Result型を処理してビジネスロジック的な判断
3. **Presentation Layer**: エラー状態をUIに反映

### 8.2 Result型の定義

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
```

### 8.3 エラーの種類

```kotlin
sealed class AppException : Exception() {
    // ネットワーク関連
    data class NetworkException(override val message: String) : AppException()

    // API関連
    data class ApiException(val statusCode: Int, override val message: String) : AppException()

    // データ解析関連
    data class ParseException(override val message: String) : AppException()

    // 設定関連
    data class SettingsException(override val message: String) : AppException()
}
```

---

## 9. 非同期処理

### 9.1 非同期処理方式
**Kotlin Coroutines**

すべての非同期処理にKotlin Coroutinesを使用:
- `suspend`関数による非同期処理の定義
- `viewModelScope`によるライフサイクル管理
- `Dispatchers`による実行スレッドの制御

### 9.2 Dispatcherの使い分け

```kotlin
// UI操作
Dispatchers.Main

// ネットワーク/ファイルIO
Dispatchers.IO

// CPU集約的な処理（JSONパース等）
Dispatchers.Default
```

---

## 10. テスト戦略

### 10.1 テストレベル

**単体テスト (Unit Test):**
- Domain Layer (UseCase, Entity)
- Repository実装
- ViewModel

**統合テスト (Integration Test):**
- Repository + DataSource
- UseCase + Repository

**UIテスト (UI Test):**
- Activity
- Custom View

### 10.2 テストダブル

```kotlin
// テスト用のFake Repository
class FakeWeatherRepository : WeatherRepository {
    override suspend fun getWeather(postalCode: String): Result<Weather> {
        return Result.Success(createTestWeather())
    }
}
```

---

## 11. サードパーティライブラリ

### 11.1 必須ライブラリ

| ライブラリ | 用途 | バージョン |
|----------|------|----------|
| Kotlin Coroutines | 非同期処理 | 1.7.x |
| AndroidX Lifecycle | ViewModel, LiveData | 2.6.x |
| OkHttp | HTTP通信 | 4.11.x |
| Gson | JSON解析 | 2.10.x |
| AndroidX Media | 音楽再生 | 1.2.x |

### 11.2 オプションライブラリ

| ライブラリ | 用途 | 検討中 |
|----------|------|-------|
| Retrofit | HTTP通信の簡素化 | ○ |
| Coil | 画像読み込み（天気アイコン） | ○ |

---

## 12. セキュリティ考慮事項

### 12.1 データの保護
- 郵便番号などの設定値は`SharedPreferences`で暗号化せずに保存（個人情報ではないため）
- 外部API通信はHTTPS必須

### 12.2 API通信
- タイムアウト設定: 30秒
- リトライ処理: 最大3回
- 証明書ピンニング: 初期バージョンでは未実装

---

## 13. パフォーマンス最適化

### 13.1 最適化ポイント

**メモリ管理:**
- 画像リソースの適切なサイズ設定
- ビットマップキャッシュの活用
- 不要なオブジェクトの即時解放

**レンダリング:**
- ビジュアライザーのフレームレート制限（30fps）
- ハードウェアアクセラレーションの活用
- 描画処理の最適化

**ネットワーク:**
- 天気・ニュースのキャッシング
- 更新頻度の制御

---

## 14. ビルド設定

### 14.1 ビルドバリアント

```gradle
android {
    buildTypes {
        debug {
            // 開発用設定
            debuggable true
        }
        release {
            // リリース用設定
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 15. 拡張性への配慮

### 15.1 将来の機能追加への対応

**設定変更機能:**
- 既存のSettingsRepositoryインターフェースで対応可能
- UpdateSettingsUseCaseの追加のみで実装可能

**アナログ時計表示:**
- ClockViewの差し替え（インターフェース化による）
- 新しいAnalogClockViewの追加

**リモコン操作:**
- Presentation Layerへの入力処理追加
- 既存のUseCaseを流用可能

---

## 16. 承認

### 16.1 設計承認
- 設計者: Claude
- 作成日: 2025-11-06
- バージョン: 1.0
- レビュー状況: 未実施

---

**次のステップ:**
本アーキテクチャ設計書に基づき、各モジュールの詳細設計書を作成する。
