# データフロー設計書

## 1. 概要

本ドキュメントは「朝チル (AsaChil)」アプリケーションにおける全体的なデータフローを定義する。

---

## 2. 全体データフロー図

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ MainActivity │  │SetupActivity │  │SplashActivity│      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐      │
│  │MainViewModel │  │SetupViewModel│  │SplashViewModel│     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          │ invoke()         │ invoke()         │ invoke()
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼─────────────┐
│                         Domain Layer                         │
│  ┌────────────────────────────────────────────────────┐     │
│  │                    UseCases                        │     │
│  │  - GetCurrentDateTimeUseCase                       │     │
│  │  - GetWeatherUseCase / RefreshWeatherUseCase       │     │
│  │  - GetLatestNewsUseCase / ReadNewsUseCase          │     │
│  │  - PlayMusicUseCase / GetCurrentTrackUseCase       │     │
│  │  - SaveSettingsUseCase / GetSettingsUseCase        │     │
│  └────────┬───────────────────────────────────────────┘     │
│           │                                                   │
│  ┌────────▼───────────────────────────────────────────┐     │
│  │            Repository Interfaces                   │     │
│  │  - WeatherRepository                               │     │
│  │  - NewsRepository                                  │     │
│  │  - SettingsRepository                              │     │
│  │  - MusicRepository                                 │     │
│  └────────┬───────────────────────────────────────────┘     │
└───────────┼──────────────────────────────────────────────────┘
            │ implements
┌───────────▼──────────────────────────────────────────────────┐
│                         Data Layer                           │
│  ┌────────────────────────────────────────────────────┐     │
│  │         Repository Implementations                 │     │
│  │  - WeatherRepositoryImpl                           │     │
│  │  - NewsRepositoryImpl                              │     │
│  │  - SettingsRepositoryImpl                          │     │
│  │  - MusicRepositoryImpl                             │     │
│  └────────┬───────────────────────────────────────────┘     │
│           │                                                   │
│  ┌────────▼───────────────────────────────────────────┐     │
│  │              DataSources                           │     │
│  │  Remote:                                           │     │
│  │    - WeatherApiDataSource                          │     │
│  │    - NewsRssDataSource                             │     │
│  │  Local:                                            │     │
│  │    - SettingsLocalDataSource                       │     │
│  │    - MusicLocalDataSource                          │     │
│  └────────┬───────────────────────────────────────────┘     │
└───────────┼──────────────────────────────────────────────────┘
            │
┌───────────▼──────────────────────────────────────────────────┐
│                      External / System                       │
│  - 天気予報API (weather.tsukumijima.net)                     │
│  - NHKニュースRSS (www3.nhk.or.jp)                          │
│  - SharedPreferences (Android)                               │
│  - MediaPlayer (Android)                                     │
│  - Calendar (Java)                                           │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. 機能別データフロー

### 3.1 アプリ起動フロー

```
[User]
  │
  │ アプリ起動
  │
  ▼
[SplashActivity]
  │
  │ onCreate()
  │
  ▼
[SplashViewModel]
  │
  │ checkSettings()
  │
  ▼
[CheckSettingsExistUseCase]
  │
  │ invoke()
  │
  ▼
[SettingsRepository]
  │
  │ hasSettings()
  │
  ▼
[SettingsLocalDataSource]
  │
  │ SharedPreferences.contains()
  │
  ▼
[SharedPreferences] → true/false を返す
  │
  ├─ true → MainActivityへ遷移
  │
  └─ false → SetupActivityへ遷移
```

### 3.2 初回設定フロー

```
[SetupActivity]
  │
  │ ユーザーが郵便番号・ニュース間隔を入力
  │
  ▼
[SetupViewModel]
  │
  │ saveSettings()
  │
  ▼
[SaveSettingsUseCase]
  │
  │ invoke(settings)
  │ ├─ バリデーション実施
  │ └─ OK
  │
  ▼
[SettingsRepository]
  │
  │ saveSettings()
  │
  ▼
[SettingsLocalDataSource]
  │
  │ SharedPreferences.edit().putString/putInt()
  │
  ▼
[SharedPreferences] → 保存完了
  │
  ▼
[SetupViewModel] → uiState.isComplete = true
  │
  ▼
[SetupActivity] → MainActivityへ遷移
```

### 3.3 時計機能フロー

```
[MainViewModel]
  │
  │ init {} → startClockUpdate()
  │
  ▼
[Coroutine Loop (1秒ごと)]
  │
  │ invoke()
  │
  ▼
[GetCurrentDateTimeUseCase]
  │
  │ Calendar.getInstance()
  │
  ▼
[Calendar (Java API)]
  │
  │ 現在日時を取得
  │
  ▼
[GetCurrentDateTimeUseCase]
  │
  │ DateTime Entityに変換
  │
  ▼
[MainViewModel]
  │
  │ uiState.dateTime を更新
  │
  ▼
[MainActivity]
  │
  │ StateFlow監視 → UI更新
  │
  ▼
[ClockView] → 画面に時刻を描画
```

### 3.4 天気機能フロー

```
[MainViewModel]
  │
  │ init {} → loadWeather()
  │ or onResume() → refreshWeather()
  │
  ▼
[GetWeatherUseCase]
  │
  │ invoke()
  │
  ▼
[SettingsRepository]
  │
  │ getSettings() → 郵便番号取得
  │
  ▼
[WeatherRepository]
  │
  │ getWeather(postalCode)
  │
  ▼
[WeatherRepositoryImpl]
  │
  │ 郵便番号 → 地域コード変換
  │
  ▼
[PostalCodeConverter]
  │
  │ convertToAreaCode()
  │
  ▼
[WeatherApiDataSource]
  │
  │ fetchWeather(areaCode)
  │ HTTP GET Request
  │
  ▼
[天気予報API]
  │
  │ JSON Response
  │
  ▼
[WeatherApiDataSource]
  │
  │ JSONをWeatherDtoにパース
  │
  ▼
[WeatherRepositoryImpl]
  │
  │ WeatherDto → Weather Entityに変換
  │
  ▼
[GetWeatherUseCase]
  │
  │ Result<Weather> を返す
  │
  ▼
[MainViewModel]
  │
  │ uiState.weather を更新
  │
  ▼
[MainActivity]
  │
  │ StateFlow監視 → UI更新
  │
  ▼
[WeatherView] → 天気情報を描画
```

### 3.5 ニュース機能フロー

```
[MainViewModel]
  │
  │ init {} → startNewsReading()
  │ 10秒待機後、定期実行
  │
  ▼
[GetLatestNewsUseCase]
  │
  │ invoke(count=10)
  │
  ▼
[NewsRepository]
  │
  │ getLatestNews(10)
  │
  ▼
[NewsRepositoryImpl]
  │
  │
  ▼
[NewsRssDataSource]
  │
  │ fetchLatestNews()
  │ HTTP GET Request
  │
  ▼
[NHKニュースRSS]
  │
  │ XML Response
  │
  ▼
[RssParser]
  │
  │ XMLをパースしてNewsDtoリスト作成
  │
  ▼
[NewsRepositoryImpl]
  │
  │ NewsDto → News Entityに変換
  │
  ▼
[GetLatestNewsUseCase]
  │
  │ Result<List<News>> を返す
  │
  ▼
[ReadNewsUseCase]
  │
  │ invoke(newsList)
  │ ニュースリストをループ
  │
  ▼
[TtsManager]
  │
  │ speak(news.getSpeechText())
  │
  ▼
[TextToSpeech (Android)]
  │
  │ 音声読み上げ
  │
  ▼
[MainViewModel]
  │
  │ uiState.currentNews を更新（読み上げ中のニュース）
  │
  ▼
[MainActivity]
  │
  │ StateFlow監視 → UI更新
  │
  ▼
[NewsView] → ニュースタイトルを描画
```

### 3.6 音楽再生フロー

```
[MainViewModel]
  │
  │ init {} → startMusicPlayback()
  │
  ▼
[PlayMusicUseCase]
  │
  │ invoke()
  │
  ▼
[MusicRepository]
  │
  │ getAllTracks()
  │
  ▼
[MusicLocalDataSource]
  │
  │ 組み込み音源リストを返す
  │
  ▼
[MusicRepository]
  │
  │ playTrack(firstTrack.id)
  │
  ▼
[MusicPlayer]
  │
  │ play(track)
  │
  ▼
[MediaPlayer (Android)]
  │
  │ res/raw/lofi_01.mp3 を再生
  │
  ▼
[MusicPlayer]
  │
  │ onCompletionListener → 次の曲へ
  │ クロスフェード処理
  │
  ▼
[MediaPlayer (Android)]
  │
  │ 次の曲を再生（ループ）
  │
  ▼
[GetCurrentTrackUseCase]
  │
  │ invoke() → 定期実行（1秒ごと）
  │
  ▼
[MusicRepository]
  │
  │ getCurrentTrack()
  │
  ▼
[MusicPlayer]
  │
  │ 現在再生中の曲を返す
  │
  ▼
[MainViewModel]
  │
  │ uiState.currentTrack を更新
  │
  ▼
[MainActivity]
  │
  │ StateFlow監視 → UI更新
  │
  ▼
[MusicView] → 曲名を描画
```

### 3.7 ビジュアライザーフロー

```
[MainActivity]
  │
  │ onCreate() → VisualizerViewを初期化
  │
  ▼
[VisualizerView]
  │
  │ startVisualizer(audioSessionId)
  │
  ▼
[MusicPlayer]
  │
  │ getAudioSessionId()
  │
  ▼
[MediaPlayer (Android)]
  │
  │ audioSessionIdを返す
  │
  ▼
[Visualizer (Android)]
  │
  │ setDataCaptureListener()
  │ 波形データをキャプチャ
  │
  ▼
[VisualizerView]
  │
  │ onWaveFormDataCapture()
  │ 波形データからバーの高さを計算
  │
  ▼
[VisualizerView]
  │
  │ invalidate() → 再描画
  │
  ▼
[VisualizerView]
  │
  │ onDraw() → スペクトラムバーを描画
```

---

## 4. 状態管理フロー

### 4.1 MainViewModelの状態管理

```kotlin
MainUiState
├── dateTime: DateTime              // 時計
├── weather: Weather?               // 天気
├── isWeatherLoading: Boolean       // 天気ローディング状態
├── weatherError: String?           // 天気エラー
├── currentNews: News?              // 現在読み上げ中のニュース
├── isNewsLoading: Boolean          // ニュースローディング状態
├── newsError: String?              // ニュースエラー
├── currentTrack: Music?            // 現在再生中の曲
└── isMusicPlaying: Boolean         // 音楽再生状態
```

**更新フロー:**
```
UseCase実行
  │
  ▼
Result<T>を取得
  │
  ├─ Success → uiState.update { it.copy(data = result.data) }
  │
  └─ Error → uiState.update { it.copy(error = result.exception.message) }
  │
  ▼
StateFlow更新
  │
  ▼
UIが変更を監視 (collect)
  │
  ▼
UI更新
```

---

## 5. エラーハンドリングフロー

### 5.1 ネットワークエラー

```
[DataSource]
  │
  │ HTTP Request
  │
  ▼
[External API]
  │
  │ エラー（タイムアウト、404、500など）
  │
  ▼
[DataSource]
  │
  │ throw IOException("HTTP Error: ${code}")
  │
  ▼
[Repository]
  │
  │ catch (e: IOException)
  │ Result.Error(AppException.NetworkException(...))
  │
  ▼
[UseCase]
  │
  │ Result.Error を返す
  │
  ▼
[ViewModel]
  │
  │ when (result) {
  │   is Error → uiState.update { it.copy(error = ...) }
  │ }
  │
  ▼
[Activity]
  │
  │ エラーメッセージをUIに表示
  │ 他の機能は正常動作を継続
```

### 5.2 バリデーションエラー

```
[SetupViewModel]
  │
  │ ユーザー入力
  │
  ▼
[validatePostalCode()]
  │
  │ 7桁の数字かチェック
  │
  ├─ OK → isPostalCodeValid = true
  │
  └─ NG → isPostalCodeValid = false
  │
  ▼
[SetupActivity]
  │
  │ isPostalCodeValid == false
  │ EditText.error = "エラーメッセージ"
```

---

## 6. 非同期処理フロー

### 6.1 Coroutineの使用

```
[ViewModel]
  │
  │ viewModelScope.launch {
  │   ...
  │ }
  │
  ▼
[UseCase (suspend function)]
  │
  │ withContext(Dispatchers.IO) {
  │   ...
  │ }
  │
  ▼
[Repository (suspend function)]
  │
  │ DataSource呼び出し
  │
  ▼
[DataSource (suspend function)]
  │
  │ withContext(Dispatchers.IO) {
  │   HTTP Request
  │ }
```

**Dispatcherの使い分け:**
- `Dispatchers.Main`: UI更新
- `Dispatchers.IO`: ネットワーク通信、ファイルIO
- `Dispatchers.Default`: CPU集約的な処理（JSONパース等）

---

## 7. ライフサイクル連動フロー

### 7.1 Activity/ViewModelのライフサイクル

```
[MainActivity]
  │
  │ onCreate()
  │   ├─ ViewModel初期化
  │   ├─ View初期化
  │   └─ StateFlow監視開始
  │
  │ onResume()
  │   └─ viewModel.onResume() → 天気情報再取得
  │
  │ onPause()
  │   (特に処理なし)
  │
  │ onDestroy()
  │   └─ ViewModelは自動的にクリーンアップ
```

### 7.2 ViewModelのCoroutineスコープ

```
viewModelScope.launch {
  while (isActive) {
    // 定期実行処理
    delay(interval)
  }
}

// ViewModel破棄時に自動的にキャンセル
```

---

## 8. データキャッシング戦略

### 8.1 天気情報

**キャッシュなし（毎回取得）**
- 更新頻度: 30分ごと、またはForeground復帰時
- 理由: 最新情報を常に表示するため

### 8.2 ニュース情報

**キャッシュなし（毎回取得）**
- 更新頻度: ユーザー設定の間隔（1～60分）
- 理由: 最新ニュースを常に読み上げるため

### 8.3 設定情報

**SharedPreferencesに永続化**
- 読み込み: アプリ起動時
- 書き込み: 初回設定時
- 理由: アプリ再起動後も設定を保持するため

### 8.4 音源情報

**アプリリソースに組み込み**
- キャッシュ不要
- 理由: ローカルリソースのため

---

## 9. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**次のステップ:**
設計書全体のレビューを実施する。
