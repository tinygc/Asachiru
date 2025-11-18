# ニュース機能の法的リスク対応: RSS/TTS仕様変更

## 背景
現在のニュース機能は、NHK RSSを自動取得してTTS読み上げする実装になっているが、法的リスクが高いことが判明。

### 法的問題点
- NHK RSSの商用利用は規約違反の可能性
- TTS音声化は著作権法上の「複製」「翻案」に該当
- 有料/広告付きアプリでの利用は営利目的とみなされる
- 訴訟リスク: 検知確率80-90%, 損害額500万〜5,000万円規模

参考: Perplexity AI調査レポート (NHK RSS TTS法律調査)

---

## 採用する新仕様 ✅

### RSS登録
- **プリセット選択**: NHK、Yahoo!ニュース、毎日新聞等から選択
- **カスタムURL入力**: ユーザーが任意のRSS URLを手動入力可能
- **デフォルト**: 空白 (ユーザーが明示的に選択)

### ニュース表示
- **通常表示**: タイトルと時刻のみ (従来通り)
- **決定キー押下**: 詳細(概要)をポップアップ表示
- **ポップアップ中**: 
  - 次の記事への自動遷移を停止
  - 戻るキーでポップアップを閉じる
  - TTS ON時は概要を読み上げ

### TTS機能
- **デフォルト**: OFF (ユーザーが明示的にON)
- **操作**: 左右方向キーでON/OFFトグル切り替え
- **動作**: ON時のみ、ポップアップ表示中に AVSpeechSynthesizer で読み上げ
- **法的位置づけ**: ユーザーの明示的操作による私的使用

### リモコン操作
- **決定キー**: 詳細ポップアップ表示
- **戻るキー**: ポップアップを閉じる
- **左右キー**: TTS ON/OFF切り替え
- **上下キー**: (将来) 前後記事移動 (optional)

---

## 法的リスク評価

### この仕様のリスクレベル: ★★☆☆☆ (約10-15%)
**Feedly型 (★☆☆☆☆) よりやや高いが、完全統合型 (★★★★★) より大幅に安全**

#### リスク低減要因
- ユーザーの明示的操作 (決定キー + 左右キーでTTS ON)
- プリセット + 手動入力の選択式 (特定メディアを狙ってない)
- TTS デフォルトOFF (アプリが自動音声化してない)
- デバイスTTS API使用 (音声ファイル保存なし)
- 基本機能はRSSリーダー (音声化はオプション)

#### 残存リスク
- TTS機能をアプリが提供している事実
- プリセットにNHK等の主要メディアを含む
- カラオケ法理により「管理者」とみなされる可能性

#### 対策
- 利用規約: 「著作権侵害はユーザー責任」明記
- README: 「デバイスのアクセシビリティ機能を使用」と記載
- マーケティング: 「RSSリーダー」として宣伝 (音声化を強調しない)

---

## 実装タスク

### Phase 1: バックエンド改修

#### Settings Entity拡張
- Settings.kt: 以下フィールド追加
  - rssUrl: String? (選択/入力されたRSS URL)
  - enableTts: Boolean = false (TTS有効フラグ)
  - rssPreset: String? (プリセット名, 例: "NHK", "Yahoo")

#### Repository/UseCase
- NewsRepository: 固定URLから Settings.rssUrl を参照に変更
- SaveSettingsUseCase: 新フィールド保存対応
- GetSettingsUseCase: 新フィールド取得対応

#### TTS制御
- ReadNewsUseCase: Settings.enableTts をチェック
  - false → TTS実行しない
  - true → 従来通り読み上げ
- TtsManager: ユーザー操作トリガー前提に変更

### Phase 2: フロントエンド改修

#### NewsView改修
- 通常モード: タイトル + 時刻表示 (現状維持)
- ポップアップモード追加:
  - 決定キー検知 → 詳細表示切り替え
  - 概要テキストを全画面表示
  - 戻るキー → ポップアップ閉じる
- ポップアップ表示中は記事遷移を停止

#### MainActivity改修
- リモコンイベントハンドリング:
  - onKeyDown オーバーライド
  - KeyEvent.KEYCODE_DPAD_CENTER → 詳細表示
  - KeyEvent.KEYCODE_BACK → ポップアップ閉じる
  - KeyEvent.KEYCODE_DPAD_LEFT/RIGHT → TTS切り替え
- ViewModelに操作通知

#### MainViewModel改修
- showNewsDetail: Boolean 状態追加
- toggleTts() メソッド追加
- closeNewsDetail() メソッド追加

#### 設定画面 (SetupActivity)
- RSS URL選択UI追加:
  - Spinner: プリセット選択
    - "選択してください" (デフォルト)
    - "NHK"
    - "Yahoo!ニュース"
    - "毎日新聞"
    - "カスタムURL入力"
  - EditText: カスタムURL入力欄 (Spinner="カスタム"時のみ表示)
- TTS設定UI追加:
  - CheckBox: "読み上げ機能を有効にする" (デフォルトOFF)
  - 説明文: "※著作権は各コンテンツ提供者に帰属します"

### Phase 3: プリセット定義
- RssPresets.kt 作成:
  ```kotlin
  object RssPresets {
      val PRESETS = mapOf(
          "NHK" to "https://www.nhk.or.jp/rss/news/cat0.xml",
          "Yahoo!ニュース" to "https://news.yahoo.co.jp/rss/topics/top-picks.xml",
          "毎日新聞" to "https://mainichi.jp/rss/etc/mainichi-flash.rss"
          // 他追加可能
      )
  }
  ```

### Phase 4: ドキュメント更新
- README.md: 法的考慮事項セクション追加
  - RSS利用はユーザーの責任
  - デバイスのアクセシビリティ機能使用
  - 商用利用時は各メディアの規約確認を推奨
- 利用規約作成 (TERMS_OF_SERVICE.md)
  - 著作権侵害はユーザー責任と明記
  - アプリは中立的なツール提供のみ
- アプリストア説明文修正:
  - "朝活支援RSSリーダー"
  - "お気に入りのRSSフィードを登録"
  - (避ける) "ニュースを音声で聞く"

### Phase 5: UI/UX調整
- NewsView: ポップアップデザイン
  - 半透明背景
  - 概要テキスト中央表示
  - "TTS: ON/OFF" インジケーター表示
- TTS切り替え時のビジュアルフィードバック
  - 左右キー押下 → ON/OFFアニメーション
- 設定画面: RSS選択時のプレビュー (optional)

---

## テスト計画
- 単体テスト: Settings新フィールド
- 統合テスト: TTS ON/OFF切り替え
- E2Eテスト: リモコン操作フロー
- 法的レビュー: 利用規約文言確認

---

## マイルストーン
- **Phase 1-2**: v1.1 (必須)
- **Phase 3-5**: v1.2 (推奨)
- **v1.0リリース前**: 最低限Phase 1-2完了

---

## 関連
- Related to #64 (実装済みだが仕様変更が必要)
- Related to #63 (UI調整が必要)
