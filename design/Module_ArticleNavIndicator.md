# 記事ナビゲーションUI設計書

## 1. 概要
上下キーによる記事ナビゲーションを直感的に伝えるため、画面上下に矢印＋ラベルを配置。アニメーションや進捗表示も盛り込む。

## 2. UI構成
- 上部：`arrow_up`（ImageView）＋「前の記事」ラベル
- 下部：`arrow_down`（ImageView）＋「次の記事」ラベル
- 進捗バー/ドットインジケーター（記事数多い場合のみ）
- 初回ガイドポップアップ（上下キー説明）

## 3. レイアウト例
```xml
<ImageView android:id="@+id/arrow_up" ... />
<TextView android:id="@+id/label_up" ... />
<!-- ニュースView -->
<ImageView android:id="@+id/arrow_down" ... />
<TextView android:id="@+id/label_down" ... />
```

## 4. アニメーション
- 矢印：上下キー押下時に跳ねる/光る
- 進捗バー：記事切り替え時にスムーズに移動

## 5. UXポイント
- 最初/最後の記事では該当矢印非表示
- 既読記事は色変化やマーク
- TVモードのみ表示

## 6. 実装方針
- activity_main.xmlにImageView/TextView追加
- ViewModelで記事インデックス管理
- 記事切り替え時にアニメーション制御
- flavor分岐不要
