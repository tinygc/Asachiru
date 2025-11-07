# Issue #19: TtsManager実装

**Labels:** implementation, presentation

## 概要
TTS（Text-to-Speech）を管理するマネージャークラスを実装する。

## タスク
- [ ] TtsManager実装（非同期初期化対応）
- [ ] CompletableDeferredによる初期化待機実装
- [ ] テスト作成

## 受け入れ条件
- TTS初期化が非同期で正しく行われること
- 初期化完了前のspeak()呼び出しが適切に処理されること
- 読み上げが正しく動作すること
- テストが全てパスすること

## 参考
- design/Module_News.md
- design/DESIGN_REVIEW.md（TTS初期化の修正内容）
