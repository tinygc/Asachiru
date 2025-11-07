# Issue #3: 共通クラス実装（Result型、AppException等）

**Labels:** implementation, domain

## 概要
アプリ全体で使用する共通クラスを実装する。

## タスク
- [ ] Result型の実装（Success, Error）
- [ ] AppException（NetworkException, ApiException, ParseException, SettingsException）
- [ ] テスト作成

## 受け入れ条件
- Result型が正しく動作すること
- 各Exceptionが定義されていること
- テストが全てパスすること

## 参考
- design/Architecture.md の「8. エラーハンドリング」
- design/LayerDefinition.md
