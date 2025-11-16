# 朝チル (AsaChil)

## 概要

朝の時間につけっぱなしにする用途で、時計・天気・ニュース読み上げをチルい音楽とともに提供するAndroid TVアプリケーションです。

パステルカラーでカラフルなピクセルアート風デザインで、リラックスした朝の時間を演出します。

## 主要機能

### 時計機能
- デジタル時計（24時間表示）
- 日付・曜日表示（曜日の色分け対応）

### 天気機能
- 天気アイコン表示
- 現在気温、最高気温、最低気温、降水確率の表示
- 郵便番号による地域設定

### ニュース読み上げ機能
- NHKニュースRSSから最大10件を取得し、古い順（昇順）に自動読み上げ
- 同一セッション内で一度読み上げた記事は次回以降スキップ（既読スキップ）
- Android標準TTS（Text-to-Speech）を使用
- 読み上げ間隔を1〜60分で設定可能

### 音楽再生機能
- Lo-Fiジャンルのフリー音源をループ再生
- 3秒間のクロスフェード対応

### ビジュアライザー機能
- スペクトラムアナライザー（縦のバータイプ）
- パステルレインボーカラー（8色）
- 音楽に反応してリアルタイムに表示

## 技術スタック

- **プラットフォーム**: Android TV
- **開発環境**: Windows 11 PowerShell
- **アーキテクチャ**: Clean Architecture
- **開発手法**: TDD（テスト駆動開発、t-wada方式）
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
```bash
# テストレポートの確認
# app/build/reports/tests/testDebugUnitTest/index.html
```

## 貢献

バグ報告や機能要望は、GitHubのIssuesで受け付けています。

## 開発者

- GitHub: [@tinygc](https://github.com/tinygc)
- Email: tinygc404@gmail.com

## ライセンス

TBD

---

**文書作成日**: 2025-11-06
**最終更新日**: 2025-11-08
