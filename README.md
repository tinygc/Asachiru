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

### 🕐 時計機能
- デジタル時計（24時間表示）
- 日付・曜日表示（曜日の色分け対応）

### 🌤️ 天気機能
- 天気アイコン表示
- 現在気温、最高気温、最低気温、降水確率の表示
- 郵便番号による地域設定
- **時間帯別背景グラデーション**（早朝/昼/夕方/夜で自動切り替え）

### 📰 ニュース読み上げ機能
- ユーザーが選択したRSSフィードから最大10件を取得し、古い順（昇順）に自動読み上げ
- プリセットRSS (NHK, Yahoo!ニュース, 毎日新聞など) またはカスタムURLに対応
- Android標準TTS（Text-to-Speech）を使用
- TTS機能はデフォルトOFF（ユーザーが明示的に有効化）
- 読み上げ間隔を1〜60分で設定可能
- 同一セッション内で一度読み上げた記事は次回以降スキップ（既読スキップ）
- **読み上げ完了待ち機能**: TTS ON時に実際の読み上げ完了まで待機
- **ニュース位置表示**: 記事の先頭/末尾に矢印（▲▼）を表示し、位置を直感的に表示
- **待機時間表示**: 「あと○分で次の記事を取得します」と表示（Issue #108対応）

#### ニュース読み上げの状態管理
- **State Machine パターン**で複雑な状態遷移を管理
  - `Idle`: 初期状態
  - `WaitingForStart`: 初回起動時のセッション開始待ち
  - `FetchingNews`: RSSフィード取得中
  - `ReadingArticle`: ニュース読み上げ中
  - `ArticleInterval`: 記事間インターバル
  - `SessionInterval`: セッション間インターバル
- 各状態でのイベント駆動型遷移（TTS ON/OFF切り替え、ニュース前後選択など）
- 詳細表示時のポップアップ一時停止機能

#### 法的考慮事項
- **RSS利用**: ユーザーが自己責任で配信元を選択する仕組みです。商用利用時は各メディアの利用規約を確認してください。
- **音声読み上げ**: デバイスのアクセシビリティ機能（Android TTS）を利用しており、アプリは中立的なツールとして機能します。
- **著作権**: 各コンテンツの著作権は配信元に帰属します。アプリはRSSリーダーとして動作し、コンテンツの複製・再配布は行いません。
- **免責事項**: 詳細は [利用規約](TERMS_OF_SERVICE.md) をご確認ください。

### 🎵 音楽再生機能
- Lo-Fiジャンルのフリー音源を3曲ループ再生
- **再生中の曲情報表示**: タイトル、アーティストを表示
- **音声ダッキング**: ニュース読み上げ中は自動的に音楽ボリュームを30%へ低下させ、読了後に復帰

### 🎨 ビジュアライザー機能（Asachiru のみ）
- **スペクトラムアナライザー**（縦のバータイプ）
- **パステルレインボーカラー**（8色）
- **リアルタイム周波数解析**: 音楽に反応してリアルタイムに表示
  - FFTベース周波数解析（バー毎に帯域平均を対数スケールで正規化 / 平滑化対応）
  - MediaPlayerのAudioSessionIdに同期（グローバルシングルトン化したMusicPlayer）
  - RECORD_AUDIO権限が未許可またはエミュレータ未対応の場合はフェイク波形アニメへフォールバック（音楽と完全同期しない）
  - Runtime権限ダイアログによる一時的なActivity onPauseでも終了しない設計へ変更（2025-11-17 対応）
  
**Asachiru と FeedWatch の違い:**
| 機能 | Asachiru | FeedWatch |
|------|---------|-----------|
| BGM再生 | ✅ | ❌ |
| ビジュアライザー | ✅ | ❌ |
| RECORD_AUDIO権限 | ✅ | ❌ |
| その他の機能 | ✅ すべて | ✅ すべて |
## 技術スタック

- **プラットフォーム**: Android TV / スマートフォン
- **対応デバイス**: 
  - Android TV（リモコン操作、Leanback UI）
  - スマートフォン（タッチ/フリック操作、横画面固定）
- **アーキテクチャ**: Clean Architecture
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
