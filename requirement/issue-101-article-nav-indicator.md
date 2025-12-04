# Issue 101: 記事ナビゲーションUI改善（2025年トレンド対応）

## 概要
Android TV向けニュース記事リーダーで、上下キーによる記事ナビゲーションを直感的に伝えるUIを追加・改善するっちゃ！

## 要件
- 画面上下に大きめの「↑」「↓」矢印アイコン＋「前の記事」「次の記事」ラベルを表示
- 矢印は選択可能な方向のみ表示（最初/最後の記事では非表示）
- 矢印はアニメーション（跳ねる・光る等）で目立たせる
- 記事切り替え時は画面全体が上下にスライドするモーション
- 進捗バーやドットインジケーターで現在位置を示す（記事数が多い場合のみ）
- 初回起動時は「上下キーで記事切り替えできるよ！」とポップアップガイド
- 既読記事は色変化やマークで区別
- TVモードのみ表示、スマホでは非表示

## 参考
- Google TV/SmartNews/NHKプラス等のUI事例
- [Google TV UX Best Practices](https://developer.android.com/tv/ux)
- [Material Design for TV](https://m3.material.io/foundations/platform-guidance/tv)

## 備考
- デザインやアニメーションの詳細は設計フェーズで決定
- flavor分岐は不要（asachiru/TVのみ対応）
