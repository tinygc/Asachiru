# Issue #25: MainViewModel実装

**Labels:** implementation, presentation

## 概要
メイン画面のViewModelを実装する。

## タスク
- [ ] MainViewModel実装
- [ ] MainUiState定義
- [ ] 各機能の初期化・定期更新実装
- [ ] StateFlow管理
- [ ] テスト作成

## 受け入れ条件
- 時計が1秒ごとに更新されること
- 天気が30分ごとに更新されること
- ニュースが設定間隔で読み上げられること
- 音楽が再生されること
- 状態管理が正しく動作すること
- テストが全てパスすること

## 依存Issue
- #9, #10, #11, #12, #13

## 参考
- design/DataFlow.md
- 各Module設計書
