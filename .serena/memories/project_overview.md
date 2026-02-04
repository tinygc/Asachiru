# Asachiru / FeedWatch プロジェクト概要

## プロジェクト基本情報
- **リポジトリ**: tinygc/asachiru
- **プラットフォーム**: Android TV / スマートフォン
- **言語**: Kotlin
- **アーキテクチャ**: Clean Architecture
- **開発プロセス**: V字開発モデル、Issue Driven開発、TDD

## アプリケーション概要

### 1. Asachiru（朝チル）
- **Application ID**: `com.tinygc.asachiru`
- **用途**: 朝の時間につけっぱなしにする用途
- **テーマ**: パステルカラー、ピクセルアート風
- **特徴**: BGM再生、ビジュアライザー機能あり

### 2. FeedWatch
- **Application ID**: `com.tinygc.feedwatch`
- **用途**: RSSフィードリーダー
- **テーマ**: ダークカラー
- **特徴**: BGM・ビジュアライザーなし、RSS機能に特化

## 共通機能
1. **時計機能** - デジタル時計、日付・曜日表示
2. **天気機能** - 天気アイコン、気温、降水確率、時間帯別背景グラデーション
3. **ニュース読み上げ機能** 
   - 複数RSS選択対応
   - Android TTS使用
   - State Machine パターンで状態管理
   - 既読スキップ機能
   - 待機時間表示
4. **音楽再生機能**（Asachiruのみ） - Lo-Fi音源、音声ダッキング対応
5. **ビジュアライザー機能**（Asachiruのみ） - スペクトラムアナライザー

## 技術スタック
- **Clean Architecture**: Presentation / Domain / Data層の分離
- **Product Flavors**: asachiru / feedwatch（同一コードベースから管理）
- **マルチデバイス対応**: Android TV（リモコン）/ スマートフォン（タッチ）
- **外部API**: livedoor天気互換API、各種RSSフィード

## プロジェクト構造
```
app/src/main/     - メインソース（共通実装）
app/src/asachiru/ - Asachiru固有実装
app/src/feedwatch/- FeedWatch固有実装
requirement/      - 要件定義ドキュメント
design/           - 設計ドキュメント（Architecture.md等）
test/             - テスト結果・スクリーンショット
agent_docs/       - AI向け詳細ガイドライン
```

## 開発ガイドライン
- Issue Driven開発（GitHub Issues）
- V字開発（要件定義→設計→実装→テスト）
- TDDで実装
- Push前にREADME.md更新
- 詳細は `agent_docs/development_workflow.md` と `agent_docs/android_guidelines.md` を参照
