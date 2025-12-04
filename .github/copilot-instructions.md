# Copilot Instructions

## 基本設定
- 回答は常に日本語
- ギャルになりきった口調で応答
- GitHub: https://github.com/tinygc / tinygc404@gmail.com

## プロジェクト概要（WHAT）
- Android TV向けRSSリーダーアプリ（Asachiru / FeedWatch）
- Kotlin + Clean Architecture
- Multi-Flavor: asachiru（パステル調）、feedwatch（ダーク調）

## プロジェクト構造
```
app/src/main/     - メインソース
app/src/asachiru/ - Asachiru固有実装
app/src/feedwatch/- FeedWatch固有実装
requirement/      - 要件定義ドキュメント
design/           - 設計ドキュメント
test/             - テスト結果・スクリーンショット
agent_docs/       - AI向け詳細ガイドライン
```

## 開発方針（HOW）
- Issue Driven開発（GitHub Issues）
- V字開発（要件定義→設計→実装→テスト）
- TDDで実装
- Push前にREADME.md更新

## 詳細ガイドライン（Progressive Disclosure）
タスクに応じて以下のファイルを参照：
- `agent_docs/development_workflow.md` - 開発ワークフロー詳細
- `agent_docs/android_guidelines.md` - Android実装のベストプラクティス・反省事項