# ユーザーシナリオテストケース

## テスト環境
- **Android Version**: API 31 (Android 12)
- **Device**: Android TV エミュレータ
- **Test Framework**: JUnit 4, Robolectric, Mockito
- **Total Test Count**: 442

---

## シナリオ1: 初回セットアップ

### 目的
新規ユーザーが郵便番号を設定し、アプリを使い始める

### 前提条件
- アプリ初回起動
- 設定データなし

### テストステップ

#### 1-1. セットアップ画面の表示確認
```
Given: アプリを初回起動する
When: SetupActivityが表示される
Then: 
  - タイトルが "⚙️ 初回設定" と表示される
  - 郵便番号入力欄のラベルが "📮 郵便番号(7桁)" と表示される
  - 保存ボタンが表示される
```

**対応テスト**: `SetupActivityTest.title text view should be displayed`
**関連バグ**: #4 UI表示テキストに絵文字が含まれていない

#### 1-2. 郵便番号の入力と検証
```
Given: セットアップ画面が表示されている
When: 郵便番号 "1000001" を入力する
Then: 
  - 入力値が正しく保存される
  - 7桁の数字のみ受け付ける
```

**対応テスト**: `SetupActivityTest.postal code input validation`

#### 1-3. デフォルト設定値の確認
```
Given: セットアップを完了する
When: 設定が保存される
Then:
  - ニュース読み上げ間隔: 5分
  - TTS: OFF
  - RSS URL: null
  - RSS Preset: null
```

**対応テスト**: 
- `SettingsTest.default values should be correct`
- `GetSettingsUseCaseTest.should return default settings`

**関連バグ**: #3 デフォルトニュース読み上げ間隔の不整合

---

## シナリオ2: ニュース取得と表示

### 目的
ユーザーが最新ニュースを取得し、新規記事のみを確認する

### 前提条件
- セットアップ完了
- ネットワーク接続あり

### テストステップ

#### 2-1. 初回ニュース取得
```
Given: アプリが起動している
When: ニュース取得処理が実行される
Then:
  - RSSフィードからニュースを取得する
  - 取得した記事数が指定数(10件)と一致する
  - 全記事が新規記事として扱われる
```

**対応テスト**: `GetLatestNewsUseCaseTest.should return news result with all new articles`
**関連バグ**: #1 ニュース取得結果の型不整合

#### 2-2. 既読記事のフィルタリング
```
Given: 過去に5記事を既読にした
When: 10記事を取得する
Then:
  - allArticles: 10記事
  - newArticles: 5記事(未読のみ)
  - 既読記事はnewArticlesに含まれない
```

**対応テスト**: 
- `GetLatestNewsUseCaseTest.should filter out read articles`
- `ReadArticleRepositoryImplTest.should track read articles`

#### 2-3. 記事詳細の表示
```
Given: ニュース一覧が表示されている
When: 任意の記事を選択する
Then:
  - タイトルが表示される
  - 本文が表示される
  - 公開日時が表示される
  - 記事URLが正しく保存される
```

**対応テスト**: `NewsDtoTest.toEntity should map correctly`

---

## シナリオ3: TTS音声読み上げ

### 目的
ユーザーがニュース記事を音声で聞く

### 前提条件
- ニュース記事がロード済み
- TTS機能が有効

### テストステップ

#### 3-1. TTS ON時の読み上げ
```
Given: TTS設定がONになっている
When: ニュース記事が表示される
Then:
  - 記事タイトルと本文が読み上げられる
  - BGM音量が30%に固定される
  - 読み上げ完了後、次の記事に自動遷移する
```

**対応テスト**: `MainViewModelTest.TTS playback flow`
**関連バグ**: 
- #15 BGM音量制御の問題
- #17 TTS ON/OFF切り替え時の継続問題

#### 3-2. TTS OFF時の動作
```
Given: TTS設定がOFFになっている
When: ニュース記事が表示される
Then:
  - 音声読み上げは実行されない
  - BGMのみ再生される
  - ユーザーがリモコンで手動で次へ移動する
```

**対応テスト**: `TtsManagerTest.TTS disabled scenario`

#### 3-3. バックグラウンド遷移時の動作
```
Given: TTS/BGMが再生中
When: ホームボタンを押してバックグラウンドに移行する
Then:
  - TTS読み上げが停止する
  - BGM再生が停止する
  - アプリに戻った時、再生状態がリセットされる
```

**対応テスト**: Manual test (ADB + Screenshot)
**関連バグ**: #11 TTS/BGMバックグラウンド継続問題

---

## シナリオ4: BGM再生とループ

### 目的
ユーザーがアプリ使用中に継続的にBGMを楽しむ

### 前提条件
- 音楽ファイルが3曲用意されている
- 音楽再生機能が有効

### テストステップ

#### 4-1. BGM再生開始
```
Given: アプリが起動している
When: PlayMusicUseCaseが実行される
Then:
  - 3曲のトラックリストが設定される
  - 1曲目が再生開始される
  - 音量が30%に設定される
```

**対応テスト**: `PlayMusicUseCaseTest.should start playback`
**関連バグ**: #13 音楽プレイヤーのループ再生不具合

#### 4-2. 次曲への自動遷移
```
Given: 1曲目が再生中
When: 1曲目が終了する
Then:
  - 自動的に2曲目が再生される
  - currentTrackIndexが更新される
  - nextPlayerが適切に準備される
```

**対応テスト**: `MusicPlayerTest.playNext should transition correctly`
**関連バグ**: #14 playNext ロジックの不整合

#### 4-3. ループ再生の動作
```
Given: 3曲目が再生中
When: 3曲目が終了する
Then:
  - 1曲目に戻って再生が継続される
  - 無限ループで再生される
  - クラッシュやメモリリークが発生しない
```

**対応テスト**: `MusicPlayerTest.loop playback scenario`

#### 4-4. 音声セッションIDの取得
```
Given: MediaPlayerが未初期化の状態
When: getAudioSessionId()を呼び出す
Then:
  - IllegalStateExceptionが発生しない
  - デフォルト値0を返す
```

**対応テスト**: `MusicPlayerTest.getAudioSessionId should handle uninitialized state`
**関連バグ**: #12 MusicPlayer IllegalStateException クラッシュ

---

## シナリオ5: 天気情報の表示

### 目的
ユーザーが現在地の天気を確認する

### 前提条件
- 郵便番号が設定済み
- 天気APIが利用可能

### テストステップ

#### 5-1. 天気情報の取得
```
Given: 郵便番号 "1000001" が設定されている
When: 天気情報取得処理が実行される
Then:
  - 正しいAPIエンドポイントにリクエストが送信される
  - 天気データが正常に取得される
  - 気温、天気コードが含まれる
```

**対応テスト**: `WeatherApiDataSourceTest.should fetch weather data`

#### 5-2. 複合天気条件の優先度
```
Given: 天気予報が "晴れのち曇り" である
When: 天気情報を表示する
Then:
  - 最初の天気状態 "晴れ" が採用される
  - アイコンが晴れマークになる
```

**対応テスト**: `WeatherDtoTest.toEntity should handle mixed conditions`
**関連バグ**: #2 天気情報の優先順位ロジック

#### 5-3. 異常天気の処理
```
Given: 天気予報が "雨のち雪" である
When: 天気情報を表示する
Then:
  - "雨" が優先表示される
  - 適切なアイコンが選択される
```

**対応テスト**: `WeatherDtoTest.rainy to snowy scenario`

---

## シナリオ6: ナビゲーション操作

### 目的
ユーザーがリモコンで記事間を移動する

### 前提条件
- 複数のニュース記事がロード済み
- Android TVリモコンが使用可能

### テストステップ

#### 6-1. 次の記事へ移動
```
Given: 1記事目が表示されている
When: リモコンの下ボタンを押す
Then:
  - 2記事目に遷移する
  - 記事内容が更新される
  - TTS読み上げが開始される(TTS ON時)
```

**対応テスト**: `NewsReadingStateMachineTest.navigate to next article`

#### 6-2. 前の記事へ戻る
```
Given: 2記事目が表示されている
When: リモコンの上ボタンを押す
Then:
  - 1記事目に戻る
  - 記事内容が更新される
```

**対応テスト**: `NewsReadingStateMachineTest.navigate to previous article`

#### 6-3. 広告表示中のナビゲーション制限
```
Given: 広告が表示されている (SessionInterval showAd=true)
When: リモコンの上下ボタンを押す
Then:
  - ナビゲーションが無効化される
  - 記事に移動できない
  - 広告表示が継続される
```

**対応テスト**: `NewsReadingStateMachineTest.navigation disabled during ad`
**関連バグ**: #16 広告表示とナビゲーション制御

#### 6-4. 新記事待機中のナビゲーション
```
Given: 新記事なし待機中 (SessionInterval showAd=false)
When: リモコンの上ボタンを押す
Then:
  - 前の記事に戻れる
  - ReadingArticle状態に遷移する
```

**対応テスト**: `NewsReadingStateMachineTest.navigation from waiting state`
**関連バグ**: #16 広告表示とナビゲーション制御

---

## シナリオ7: 時計表示

### 目的
ユーザーが常に現在時刻を確認できる

### 前提条件
- アプリが起動している

### テストステップ

#### 7-1. 現在時刻の表示
```
Given: アプリのメイン画面が表示されている
When: 時計コンポーネントがレンダリングされる
Then:
  - 常に現在時刻が表示される
  - 記事の公開時刻は表示されない
  - フォーマット: "HH:mm"
```

**対応テスト**: `ClockViewModelTest.should display current time`
**関連バグ**: #16 時計が記事の公開時刻を表示してしまう

#### 7-2. 時刻の自動更新
```
Given: 時計が表示されている
When: 1分経過する
Then:
  - 時刻表示が自動的に更新される
  - リソースリークが発生しない
```

**対応テスト**: `GetCurrentDateTimeUseCaseTest.time updates correctly`

---

## シナリオ8: データ永続化

### 目的
アプリ終了後も設定と既読状態を保持する

### 前提条件
- SharedPreferencesが利用可能

### テストステップ

#### 8-1. 設定の保存
```
Given: ユーザーが設定を変更する
When: SaveSettingsUseCaseが実行される
Then:
  - 郵便番号がSharedPreferencesに保存される
  - ニュース間隔が保存される
  - RSS URLが保存される
  - TTS設定が保存される
```

**対応テスト**: `SettingsLocalDataSourceTest.save settings`

#### 8-2. 既読記事の保存
```
Given: ユーザーが記事を読み終える
When: MarkArticleAsReadUseCaseが実行される
Then:
  - 記事IDがStringSetとして保存される
  - 既存の既読IDリストに追加される
  - 重複が防止される
```

**対応テスト**: `ReadArticleRepositoryImplTest.mark as read`
**関連バグ**: #6 SharedPreferences mock設定の不足

#### 8-3. アプリ再起動時のデータ復元
```
Given: アプリを一度終了する
When: アプリを再起動する
Then:
  - 保存済み設定がロードされる
  - 既読記事リストが復元される
  - 新規記事のみがnewArticlesに含まれる
```

**対応テスト**: `GetSettingsUseCaseTest.load persisted settings`

---

## シナリオ9: エラーハンドリング

### 目的
ネットワークエラーやAPIエラーを適切に処理する

### 前提条件
- ネットワーク接続が不安定

### テストステップ

#### 9-1. ニュース取得失敗
```
Given: ネットワークが切断されている
When: ニュース取得処理を実行する
Then:
  - Result.Failureが返される
  - エラーメッセージが含まれる
  - アプリがクラッシュしない
```

**対応テスト**: `NewsRepositoryImplTest.network error handling`
**関連バグ**: #18 エッジケース処理 (空レスポンス・nullチェック)

#### 9-2. 天気API失敗
```
Given: 天気APIがエラーを返す
When: 天気情報取得を試みる
Then:
  - デフォルト値(晴れ、20℃)が使用される
  - アプリは正常に動作を継続する
```

**対応テスト**: `WeatherApiDataSourceTest.API error fallback`

#### 9-3. 無効な郵便番号
```
Given: ユーザーが無効な郵便番号を入力する
When: 天気情報を取得する
Then:
  - バリデーションエラーが返される
  - エラーメッセージが表示される
```

**対応テスト**: `PostalCodeConverterTest.invalid postal code`

---

## シナリオ10: 依存性注入とファクトリ

### 目的
アプリの全コンポーネントが正しく初期化される

### 前提条件
- アプリケーションが起動している

### テストステップ

#### 10-1. リポジトリの生成
```
Given: RepositoryFactoryが初期化される
When: 各リポジトリを生成する
Then:
  - NewsRepository, WeatherRepository, MusicRepository が生成される
  - ReadArticleRepository が生成される
  - 各リポジトリが正しいDataSourceを持つ
```

**対応テスト**: `RepositoryFactoryTest.create repositories`
**関連バグ**: #8 依存性注入の不足

#### 10-2. UseCaseの生成
```
Given: UseCaseFactoryが初期化される
When: GetLatestNewsUseCaseを生成する
Then:
  - NewsRepositoryが注入される
  - ReadArticleRepositoryが注入される
  - UseCaseが正しく動作する
```

**対応テスト**: `UseCaseFactoryTest.create use cases`

#### 10-3. ViewModelの生成
```
Given: ViewModelFactoryが初期化される
When: MainViewModelを生成する
Then:
  - すべての必要なUseCaseが注入される
  - ReadArticleRepositoryが注入される
  - ViewModelが正しく初期化される
```

**対応テスト**: `ViewModelFactoryTest.create view models`

---

## 実機テストシナリオ (ADB経由)

### 前提条件
- Android TVエミュレータまたは実機が接続されている
- ADBが利用可能

### テストステップ

#### T-1. アプリ起動とスクリーンキャプチャ
```bash
# アプリ起動
adb shell am start -n com.tinygarden.asachiru/.ui.MainActivity

# 2秒待機
sleep 2

# スクリーンキャプチャ
adb exec-out screencap -p > test/screenshots/app_launch.png
```

**期待結果**: 
- アプリが正常に起動する
- セットアップ画面または記事画面が表示される

#### T-2. リモコン操作シミュレーション
```bash
# 下ボタン押下 (次の記事)
adb shell input keyevent KEYCODE_DPAD_DOWN
sleep 2
adb exec-out screencap -p > test/screenshots/next_article.png

# 上ボタン押下 (前の記事)
adb shell input keyevent KEYCODE_DPAD_UP
sleep 2
adb exec-out screencap -p > test/screenshots/prev_article.png

# 右ボタン押下 (設定メニュー)
adb shell input keyevent KEYCODE_DPAD_RIGHT
sleep 2
adb exec-out screencap -p > test/screenshots/settings_menu.png
```

**期待結果**: 
- 各操作で適切な画面遷移が行われる
- UIが正しくレンダリングされる

#### T-3. バックグラウンド遷移テスト
```bash
# ホームボタン押下
adb shell input keyevent KEYCODE_HOME
sleep 2

# 音声が停止していることを確認 (手動で音声確認)

# アプリに戻る
adb shell am start -n com.tinygarden.asachiru/.ui.MainActivity
sleep 2
adb exec-out screencap -p > test/screenshots/resume_from_background.png
```

**期待結果**: 
- バックグラウンド移行時にTTS/BGMが停止する
- アプリ復帰時に正常に再開する

---

## 非機能テスト

### パフォーマンステスト

#### P-1. ビルド時間
```bash
./gradlew clean
./gradlew assembleDebug
```

**期待結果**: 
- クリーンビルドが60秒以内に完了する
- インクリメンタルビルドが15秒以内に完了する

#### P-2. テスト実行時間
```bash
./gradlew test
```

**期待結果**: 
- 442テストが90秒以内に完了する
- すべてのテストがパスする (100%)

#### P-3. メモリ使用量
```bash
adb shell dumpsys meminfo com.tinygarden.asachiru
```

**期待結果**: 
- アプリのメモリ使用量が150MB以下
- メモリリークが発生しない

---

## 回帰テストチェックリスト

### コード変更時の確認事項

#### 新機能追加時
- [ ] 影響を受けるすべてのUseCaseテストが更新されている
- [ ] ViewModelテストが追加されている
- [ ] データ型変更時はすべての呼び出し元を確認
- [ ] デフォルト値はSettings.DEFAULTから取得している

#### バグ修正時
- [ ] 修正内容に対応するテストケースが追加されている
- [ ] 関連する既存テストがすべてパスする
- [ ] エッジケースが考慮されている

#### UI変更時
- [ ] Robolectricテストが更新されている
- [ ] スクリーンキャプチャが取得されている
- [ ] 絵文字やアイコンの表示テキストが一致している

#### 依存関係変更時
- [ ] Factory系のテストが更新されている
- [ ] モック設定が完全である
- [ ] DIグラフ全体が整合している

---

## テスト自動化スクリプト

### 完全テストスイート実行
```bash
#!/bin/bash
# test/run_full_test_suite.sh

echo "=== Starting Full Test Suite ==="

# 1. コンパイル確認
echo "[1/4] Compiling Kotlin code..."
./gradlew compileDebugKotlin
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

# 2. ユニットテスト
echo "[2/4] Running unit tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ Unit tests failed"
    exit 1
fi

# 3. ビルド
echo "[3/4] Building APK..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

# 4. スクリーンキャプチャテスト
echo "[4/4] Running UI tests..."
./test/screenshot_test.sh
if [ $? -ne 0 ]; then
    echo "❌ UI tests failed"
    exit 1
fi

echo "✅ All tests passed!"
```

### スクリーンキャプチャテスト
```bash
#!/bin/bash
# test/screenshot_test.sh

SCREENSHOT_DIR="test/screenshots"
mkdir -p $SCREENSHOT_DIR

# アプリ起動
adb shell am start -n com.tinygarden.asachiru/.ui.MainActivity
sleep 3
adb exec-out screencap -p > $SCREENSHOT_DIR/01_launch.png

# 次の記事へ
adb shell input keyevent KEYCODE_DPAD_DOWN
sleep 2
adb exec-out screencap -p > $SCREENSHOT_DIR/02_next_article.png

# 前の記事へ
adb shell input keyevent KEYCODE_DPAD_UP
sleep 2
adb exec-out screencap -p > $SCREENSHOT_DIR/03_prev_article.png

# 設定メニュー
adb shell input keyevent KEYCODE_DPAD_RIGHT
sleep 2
adb exec-out screencap -p > $SCREENSHOT_DIR/04_settings.png

# バックグラウンド遷移
adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell am start -n com.tinygarden.asachiru/.ui.MainActivity
sleep 2
adb exec-out screencap -p > $SCREENSHOT_DIR/05_resume.png

echo "Screenshots saved to $SCREENSHOT_DIR"
```

---

## テスト結果の検証

### 成功基準

| テストカテゴリ | 成功基準 |
|--------------|---------|
| ユニットテスト | 442/442 (100%) |
| コンパイル | エラー0件 |
| ビルド時間 | ≤ 60秒 |
| メモリ使用量 | ≤ 150MB |
| スクリーンキャプチャ | 全シナリオで期待通りの表示 |

### テストレポート自動生成
```bash
./gradlew test
# HTML レポート: app/build/reports/tests/test/index.html
```

---

**作成日**: 2025-11-21  
**バージョン**: 1.0  
**関連ドキュメント**: [BUG_FIXES_2025-11-21.md](BUG_FIXES_2025-11-21.md)
