# Issue #26: MainActivity実装

**Labels:** implementation, presentation

## 概要
メイン画面のActivityを実装する。

## タスク
- [ ] MainActivity実装
- [ ] レイアウトXML作成
- [ ] ViewModelの監視
- [ ] Custom Viewの配置と更新
- [ ] ライフサイクル処理（onResume等）
- [ ] UIテスト作成

## 受け入れ条件
- 全てのCustom Viewが正しく配置されていること
- ViewModelの状態変更がUIに反映されること
- Foreground復帰時に天気が更新されること
- UIテストが全てパスすること

## 依存Issue
- #21, #22, #23, #24, #25

## 参考
- design/Module_Clock.md
- design/DataFlow.md
