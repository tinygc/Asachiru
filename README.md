# Asachiru / FeedWatch - Android TV アプリケーション

## 概要

このリポジトリには、2つのマルチデバイス対応アプリケーションが含まれています：

### 1. 朝チル (AsaChil)
朝の時間につけっぱなしにする用途で、時計・天気・ニュース読み上げをチルい音楽とともに提供するアプリケーションです。
パステルカラーでカラフルなピクセルアート風デザインで、リラックスした朝の時間を演出します。

### 2. RSS FeedWatch
お気に入りのRSSフィードを快適にチェックできるRSSリーダーアプリケーションです。
音声読み上げ（TTS）機能を搭載し、大画面で情報を快適に閲覧できます。

**対応デバイス**:
- Android TV（リモコン操作）
- スマートフォン（タッチ/フリック操作）

**Application ID**:
- AsaChil: `com.tinygc.asachiru`
- FeedWatch: `com.tinygc.feedwatch`

**Product Flavors**: 両アプリは同一のコードベースから、Product Flavors機能を使用して管理されています。

## 主要機能

### 時計機能
- デジタル時計（24時間表示）
- 日付・曜日表示（曜日の色分け対応）

### 天気機能
- 天気アイコン表示
- 現在気温、最高気温、最低気温、降水確率の表示
- 郵便番号による地域設定

### ニュース読み上げ機能
- ユーザーが選択したRSSフィードから最大10件を取得し、古い順（昇順）に自動読み上げ
- プリセットRSS (NHK, Yahoo!ニュース, 毎日新聞など) またはカスタムURLに対応
- Android標準TTS（Text-to-Speech）を使用
- TTS機能はデフォルトOFF（ユーザーが明示的に有効化）
- 読み上げ間隔を1〜60分で設定可能
- 同一セッション内で一度読み上げた記事は次回以降スキップ（既読スキップ）

#### 法的考慮事項
- **RSS利用**: ユーザーが自己責任で配信元を選択する仕組みです。商用利用時は各メディアの利用規約を確認してください。
- **音声読み上げ**: デバイスのアクセシビリティ機能（Android TTS）を利用しており、アプリは中立的なツールとして機能します。
- **著作権**: 各コンテンツの著作権は配信元に帰属します。アプリはRSSリーダーとして動作し、コンテンツの複製・再配布は行いません。
- **免責事項**: 詳細は [利用規約](TERMS_OF_SERVICE.md) をご確認ください。

### 音楽再生機能
- Lo-Fiジャンルのフリー音源を3曲ループ再生
- **再生中の曲情報表示**: タイトル、アーティストを表示

### 音声連携（TTS＋ダッキング）
- ニュース読み上げ中は音楽ボリュームを30%へ自動ダッキングし可聴性向上
- 読み上げ完了時に元の音量へ復帰（重複読み上げに対して冪等性保持）
### ビジュアライザー機能
- スペクトラムアナライザー（縦のバータイプ）
- パステルレインボーカラー（8色）
- 音楽に反応してリアルタイムに表示
 - FFTベース周波数解析（バー毎に帯域平均を対数スケールで正規化 / 平滑化対応）

 - MediaPlayerのAudioSessionIdに同期（グローバルシングルトン化したMusicPlayer）
	- RECORD_AUDIO権限が未許可またはエミュレータ未対応の場合はフェイク波形アニメへフォールバック（音楽と完全同期しない）
  - Runtime権限ダイアログによる一時的なActivity onPauseでも終了しない設計へ変更（2025-11-17 対応）
## 技術スタック

- **プラットフォーム**: Android TV / スマートフォン
- **対応デバイス**: 
  - Android TV（リモコン操作、Leanback UI）
  - スマートフォン（タッチ/フリック操作、横画面固定）
- **アーキテクチャ**: Clean Architecture
- **プロセス**: V字開発モデル、Issue Driven開発

**最終更新日**: 2025-12-01

### 最近の更新 (2025-12-01)
1. **スマートフォン対応完了** 🎉
   - Phase 1: Manifest修正（Leanback optional化、LAUNCHER追加）
   - Phase 2: Setup画面対応（ScrollView追加、padding最適化）
   - Phase 3: Main画面対応（設定ボタン追加、レイアウト調整）
   - Phase 4: タッチ操作実装（フリック操作、UI最適化）
   - デバイスタイプ判定機能追加（DeviceUtils）
   - キーボード表示制御追加（スマホのみ）
   
2. **デバイス別最適化**
   - **Android TV**: リモコン操作（全機能維持）
     - 決定キー（短押し/長押し）
     - 方向キー（上下左右）
     - フォーカスナビゲーション
   - **スマートフォン**: タッチ/フリック操作
     - 左右フリック: TTS切り替え
     - 上下フリック: ニュース送り/戻し
     - シングルタップ: 詳細表示切り替え
     - 設定ボタンタップ: 設定画面へ
     - ソフトキーボード自動表示
   
3. **UI/UX調整**
   - スマホ向けフォントサイズ最適化（18sp/14sp）
   - デバイス別padding調整（TV: 48dp、スマホ: 16dp）
   - ScrollView追加（小画面対応）
   - レイアウトマージン最適化

### 過去の更新 (2025-11-21)
1. **ライフサイクルクラッシュ修正**: `MainActivity.onStop()` 内の `finish()` 呼び出しを削除し、`IllegalArgumentException: Activity client record must not be null` を解消。
   - 原因: onStopでActivityを強制終了すると、まだ処理中のUIトランザクション（Coroutine内のUI操作/AdMob初期化など）が ActivityThread 内で参照するレコードを失いクラッシュ。
   - 対策: バックグラウンド移行時は状態保存と停止のみ行い、ユーザー操作（BACKキー長押しなど）で明示終了とする設計へ。
   - 影響範囲: 機能的変更なし（自動終了挙動のみ撤廃）。安定性向上。
2. **ニュース詳細ポップアップ遷移抑止**: TTS ONで記事読了直後にポップアップが自動的に閉じて次の記事へ進んでしまう問題を修正。
   - 原因: `ArticleCompleted` イベントが `ReadingArticle.isPaused=true`（詳細表示中）でもそのまま状態遷移を実行していた。
   - 対策: `ReadingArticle` に `hasCompletedReading` フラグを追加し、詳細表示中は読了イベントを遷移保留。ポップアップ閉鎖 (`DetailClosed`) 時に再開。
   - 挙動: ポップアップ表示中は現在の記事が固定され、閉じるまで次の記事やインターバルへ進まない。ユーザーの集中閲覧体験を改善。
3. **要件定義書・設計書の更新**: 実装済み機能（RSS選択、既読スキップ、詳細ポップアップ、State Machine、時間帯別背景等）を反映し、ドキュメントを最新化。

### 過去の更新 (2025-11-20)
1. **State Machineパターン導入**: ニュース読み上げ機能を State Machine パターンで再設計
   - `NewsReadingState` sealed class で明確な状態定義（Idle, WaitingForStart, FetchingNews, ReadingArticle, ArticleInterval, SessionInterval）
   - `NewsReadingEvent` sealed class でイベント駆動の状態遷移（AppStarted, TtsSettingChanged, DetailOpened/Closed など）
   - `NewsReadingStateMachine` class で状態遷移ロジックを集約
   - 複雑だった複数タイマーとコルーチンの管理を State Machine に統一し、予測可能な動作を実現
   - TTS ON/OFF 切り替え時の不具合（記事が進まなくなる問題）を根本的に解決
   - 詳細表示時の一時停止機能を `isPaused` フラグで管理し、カウントダウンも正しく停止・再開
   - MainViewModel のコード行数を大幅に削減（588行 → 約250行）し保守性向上
   - 設計ドキュメント `design/NewsReadingStateMachine.md` を追加

2. **TTS読み上げ完了待ち機能**: TTS ON時に読み上げが途中で次の記事に進む問題を修正
   - TTS ON: `onReadArticle()`の完了を待ってから次の記事へ遷移（タイマー不使用）
   - TTS OFF: 文字数ベース（1文字200ms+余裕1秒）でタイマー設定
   - 記事の長さに関係なく、実際の読み上げ時間に合わせて進行

3. **TTS ON/OFF切り替え時のカウントダウン修正**: 読み上げ途中でOFFにした時にカウントダウンが0秒のままになる問題を修正
   - TTS ON→OFF: タイマーを開始して文字数ベースでカウントダウン表示
   - TTS OFF→ON: タイマーをキャンセルして読み上げ完了待ちに切り替え
   - `TtsSettingChanged`イベントで適切に状態とタイマーを管理

4. **上下キーナビゲーション復活**: State Machine導入時に失われた手動ナビゲーション機能を再実装
   - `NavigateToPrevious`/`NavigateToNext`イベントを追加
   - ReadingArticle状態とArticleInterval状態で前後の記事へ移動可能
   - 手動選択後も自動進行を継続（タイマー再設定）

### 過去の更新 (2025-11-19)
1. **デバッグ情報にカウントダウン表示追加**: 次の記事表示までの残り秒数をデバッグ情報欄に表示
   - MainUiStateにdebugNextNewsRemainingSecondsフィールドを追加
   - MainViewModelで1秒ごとにカウントダウンを計算して更新
   - DEBUG BUILDでのみ表示される開発者向け機能
2. **バックグラウンド遷移時のTTS・BGM停止修正**: アプリがバックグラウンドに遷移した際にTTSとBGMが停止しない問題を修正
   - `TtsManager.stop()`メソッドを改善し、読み上げ停止時に音楽の音量を即座に元に戻す処理を追加
   - `MainViewModel.onPause()`でバックグラウンド移行時に音楽プレイヤーを停止
   - `MainViewModel.onStop()`で完全停止時に音楽プレイヤーを停止
   - `MainViewModel.onResume()`でフォアグラウンド復帰時に音楽再生を再開
   - バックグラウンド遷移時に確実にTTSとBGMが停止するように改善
3. **音楽プレーヤーのループ再生機能を修正**: 1曲再生終了後に次の曲に進まない問題を修正
   - `MusicRepositoryImpl`で`playTrack()`呼び出し時に全トラックリストを設定
   - `IMusicPlayer`インターフェースに`setTrackList()`メソッドを追加
   - 3曲のループ再生が正常に動作するようテスト追加
   - `playNext()`のロジックを改善して、nextPlayerの適切な処理と再生開始を実装
4. **音楽プレーヤーの時間表示を削除**: ユーザーフィードバックに基づき、音楽プレーヤーから再生時間表示を削除しシンプルな表示に変更
   - 曲タイトルとアーティスト名のみを表示するようUI調整
5. **MediaPlayer例外処理の改善**: MusicPlayer.getAudioSessionId()でIllegalStateExceptionが発生してアプリがクラッシュする問題を修正
   - MediaPlayerが解放済みまたは不正な状態でaudioSessionIdを取得しようとした際の例外を適切にキャッチ
   - クラッシュの代わりに0を返すことでアプリの安定性を向上
5. **テキストサイズ最適化**: 実際のTV表示に最適化するため、すべてのテキストサイズをDP単位（SP）に変換し約50%縮小
   - カスタムビュー（時計/天気/ニュース/音楽）でTypedValue.applyDimension()を使用してSP→PX変換
   - 設定画面とスプラッシュ画面のテキストサイズも調整
   - ClockView: 時刻 120sp→60sp、日付 48sp→24sp
   - WeatherView: アイコン 96sp→48sp、テキスト 36sp→18sp、ラベル 28sp→14sp
   - NewsView: タイトル 48sp→24sp、時刻 32sp→16sp、詳細ポップアップ 46sp→23sp/36sp→18sp
   - MusicTrackView: タイトル 36sp→18sp、アーティスト 24sp→12sp

### 過去の更新 (2025-11-17)
1. 時間帯別背景グラデーション（早朝/昼/夕方/夜）実装し視認性改善
2. ニュース表示位置を垂直中央へ調整（下寄り問題を解消）
3. MP3メタデータから曲タイトル/アーティスト/再生時間を自動取得
4. ニュース読み上げ時の音楽ダッキングを30%へ最適化
5. MusicPlayerをアプリ全域で共有するグローバルシングルトンへ変更（Visualizer連携のため）
6. Visualizer起動ロジックを`MainActivity`へ追加しAudioSessionId変化時に再初期化するよう改善（ログ付き）
7. RECORD_AUDIO権限要求と許可後即Visualizer再初期化処理追加 / onPauseでfinish()削除
8. Visualizer失敗時リトライ(最大3回)＋平滑化導入で過度なチラつきを低減 / Fallback時は独自アニメ表示
9. FFTモード実装（waveform/FFT切替可能）＆ dBスケール正規化で音楽同期精度向上
- **プロセス**: V字開発モデル、Issue Driven開発

## 外部API

- 天気予報API（livedoor天気互換）
- NHKニュースRSS

## プロジェクト状況

### 現在のフェーズ: 実装工程完了 ✅

- ✅ 要件定義完了
- ✅ アーキテクチャ設計完了
- ✅ 各モジュール設計完了
- ✅ 設計レビュー完了
- ✅ 実装工程完了（Issues #1-31）
- ✅ テスト工程完了（Unit/Integration/E2E）
- ⏭️ 次: 実機テスト・デプロイ

## ディレクトリ構成

```
asachiru/
├── requirement/              # 要件定義ドキュメント
│   ├── BaseRequirements.md  # 基本要件定義書
│   ├── ui_mockup.html        # UIモックアップ
│   └── ui_mockup_pastel.html # パステル版UIモックアップ
├── design/                   # 設計ドキュメント
│   ├── Architecture.md       # アーキテクチャ設計書
│   ├── LayerDefinition.md    # レイヤー構成詳細定義書
│   ├── Module_*.md           # 各モジュール設計書
│   ├── DataFlow.md           # データフロー設計書
│   └── DESIGN_REVIEW.md      # 設計レビュー報告書
├── issues/                   # Issue管理（#1-31）
├── test/                     # テストドキュメント
│   ├── E2E_TEST_RESULTS.md   # E2Eテスト結果
│   └── SCREENSHOTS.md        # 画面レイアウト詳細
├── app/                      # Androidアプリケーション
│   └── src/
│       ├── main/java/com/tinygc/asachiru/
│       │   ├── domain/       # Domain Layer
│       │   │   ├── entity/   # エンティティ
│       │   │   ├── repository/ # リポジトリインターフェース
│       │   │   ├── usecase/  # ユースケース
│       │   │   └── common/   # 共通クラス
│       │   ├── data/         # Data Layer
│       │   │   ├── repository/ # リポジトリ実装
│       │   │   ├── datasource/ # データソース
│       │   │   └── dto/      # DTO
│       │   ├── presentation/ # Presentation Layer
│       │   │   ├── main/     # メイン画面
│       │   │   ├── setup/    # 設定画面
│       │   │   ├── splash/   # スプラッシュ画面
│       │   │   ├── common/   # 共通（ViewModelFactory）
│       │   │   └── util/     # ユーティリティ
│       │   └── di/           # 依存性注入
│       └── test/             # テストコード
│           ├── java/         # Unit Tests
│           ├── integration/  # Integration Tests
│           └── e2e/          # E2E Tests
└── README.md                 # 本ファイル
```

## セットアップ

### 必要要件

- Android Studio (最新版推奨)
- JDK 11以上
- Android SDK 28以上
- Android TV エミュレータまたは実機

### ビルド手順

1. **リポジトリのクローン**
```bash
git clone https://github.com/tinygc/asachiru.git
cd asachiru
```

2. **Android Studioで開く**
- Android Studioを起動
- "Open an Existing Project"を選択
- `asachiru`ディレクトリを選択

3. **依存関係の同期**
- Gradleの自動同期を待つ
- または、`./gradlew build`を実行

4. **テストの実行**
```bash
# 全テストを実行
./gradlew test

# 特定のテストを実行
./gradlew test --tests "com.tinygc.asachiru.domain.*"
```

5. **アプリのビルド**
```bash
# Debug版
./gradlew assembleDebug

# Release版
./gradlew assembleRelease
```

6. **エミュレータ/実機での実行**
- Android TVエミュレータを起動
- Run → Run 'app'を選択

### 初回設定

アプリ初回起動時に以下の設定が必要です：

1. **郵便番号**: 7桁の数字（例: 1000001）
2. **ニュース読み上げ間隔**: 1～60分

## 実装状況

### 完了済み

- ✅ **Issue #1-31**: 全機能実装完了
- ✅ **Unit Tests**: 200+テストケース
- ✅ **Integration Tests**: 16テストケース
- ✅ **E2E Tests**: 22テストケース（成功率100%）

### テストカバレッジ

- **Domain Layer**: 100%
- **Data Layer**: 100%
- **Presentation Layer**: 100%

詳細は `test/E2E_TEST_RESULTS.md` を参照してください。

### 統計情報

- **総ファイル数**: 142ファイル
- **総コード行数**: 約14,000行
- **テストコード割合**: 約40%

## 今後の予定

### 実機テスト
- [ ] Android TV実機での動作確認
- [ ] TTS（音声合成）の動作検証
- [ ] 音楽再生とクロスフェードの確認
- [ ] ビジュアライザーの描画確認

### パフォーマンス最適化
- [ ] メモリ使用量の監視
- [ ] バッテリー消費量の測定
- [ ] 長時間起動テスト

### 追加機能（検討中）
- [ ] 複数地域の天気表示
- [ ] カスタムニュースソース対応
- [ ] 音楽プレイリスト機能
- [ ] テーマカラーカスタマイズ

## トラブルシューティング

### ビルドエラー
```bash
# Gradleキャッシュのクリア
./gradlew clean

# 依存関係の再解決
./gradlew --refresh-dependencies
```

### テストエラー
### Visualizerが初期化エラー(-3)になる
`AudioEffect set(): ... status: -1` / `Visualizer initCheck failed -3` がログに出る場合:
- 権限: `RECORD_AUDIO` が許可されているか (TVでは自動付与されない場合あり)
- エミュレータ制限: 一部AVDはオーディオエフェクト未実装で失敗します（フェイクアニメで代替）
- 再生タイミング: MediaPlayer開始直後はセッションIDが安定しないので1秒後に再試行すると成功することあり
- 実機確認: 実機(Android TV)でエラーが再現するか比較テスト推奨

暫定対策としてフェイク波形アニメを表示（動的な緩やかな揺らぎ）しUI破綻を回避しています。

```bash
# テストレポートの確認
# app/build/reports/tests/testDebugUnitTest/index.html
```

## 貢献

バグ報告や機能要望は、GitHubのIssuesで受け付けています。

## 開発者

- GitHub: [@tinygc](https://github.com/tinygc)
- Email: tinygc404@gmail.com

## RSS FeedWatch - Play Storeリリース情報

### リリースドキュメント

FeedWatchのPlay Storeへのリリースに関する詳細なドキュメントは以下を参照してください：

| ドキュメント | 内容 |
|------------|------|
| [`RELEASE_SUMMARY.md`](docs/feedwatch/RELEASE_SUMMARY.md) | **リリース完全ガイド（最初に読む）** |
| [`ADMOB_SETUP_GUIDE.md`](docs/feedwatch/ADMOB_SETUP_GUIDE.md) | AdMob設定ガイド |
| [`RELEASE_BUILD_GUIDE.md`](docs/feedwatch/RELEASE_BUILD_GUIDE.md) | リリースビルドガイド |
| [`GOOGLE_PLAY_CONSOLE_GUIDE.md`](docs/feedwatch/GOOGLE_PLAY_CONSOLE_GUIDE.md) | Play Console登録ガイド |
| [`PLAY_STORE_LISTING.md`](docs/feedwatch/PLAY_STORE_LISTING.md) | ストアリスティング情報 |
| [`PRIVACY_POLICY.md`](docs/feedwatch/PRIVACY_POLICY.md) | プライバシーポリシー |

### クイックスタート

FeedWatchをPlay Storeにリリースする手順：

1. **AdMob登録**: FeedWatch用のAdMobアプリを登録し、App IDを取得
2. **App ID設定**: `app/build.gradle` の feedwatch flavor に取得したIDを設定
3. **AABビルド**: `.\gradlew.bat :app:bundleFeedwatchRelease` を実行
4. **Play Console**: Google Play Consoleでアプリを登録
5. **リリース**: AABをアップロードして公開

詳細は [`docs/feedwatch/RELEASE_SUMMARY.md`](docs/feedwatch/RELEASE_SUMMARY.md) を参照してください。

### 重要な注意事項

- ⚠️ FeedWatchは**Asachiruとは別のAdMob App ID**が必要です
- ⚠️ 署名鍵（keystore）も**別ファイル**を使用: `feedwatch-release.jks`
- ⚠️ `local.properties` に署名情報を設定してください（Gitにはコミットされません）

## Product Flavorsによる管理

このプロジェクトは、Gradle Product Flavorsを使用して複数のアプリを管理しています：

### ビルド方法

```bash
# AsaChil用のビルド
.\gradlew.bat :app:assembleAsachiruDebug    # Debug版
.\gradlew.bat :app:bundleAsachiruRelease    # Release版（AAB）

# FeedWatch用のビルド
.\gradlew.bat :app:assembleFeedwatchDebug   # Debug版
.\gradlew.bat :app:bundleFeedwatchRelease   # Release版（AAB）
```

### Flavor設定

各Flavorは以下の設定を持ちます：

| 項目 | AsaChil | FeedWatch |
|-----|---------|-----------|
| Application ID | com.tinygc.asachiru | com.tinygc.feedwatch |
| アプリ名 | 朝チル (AsaChil) | RSS FeedWatch |
| AdMob App ID | （Asachiru用） | （FeedWatch用、要設定） |
| Keystore | asachiru-release.jks | feedwatch-release.jks |

## ライセンス

TBD

---

**文書作成日**: 2025-11-06
**最終更新日**: 2025-11-25
