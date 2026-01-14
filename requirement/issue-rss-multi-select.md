# Issue: RSSフィード複数選択機能

## 概要
現在は単一のRSSフィードしか選択できないけど、複数のRSSフィードを選択してニュースを取得できるようにするっちゃ！

## 背景（WHY）
- ユーザーが複数のニュースソースから情報を得たいニーズがある
- 現状は1つのRSSフィードのみ選択可能で、複数のソースを見たい場合は設定を変更する必要がある
- 複数RSS対応により、ユーザーエクスペリエンスが向上する

## 要件（WHAT）

### 機能要件
1. **RSSフィード複数選択**
   - プリセットから複数のRSSフィードを選択可能
   - チェックボックス形式でUI提供
   - 最低1つ以上のRSS選択を必須とする

2. **カスタムURL対応**
   - カスタムURLも複数追加可能
   - 各カスタムURLに名前を付けられる（任意）

3. **ニュース取得ロジック**
   - 選択された全てのRSSフィードからニュースを取得
   - 各フィードから取得した記事を統合して表示
   - 公開日時でソート（新しい順）

4. **設定の保存**
   - 選択されたRSSフィードのリストを永続化
   - アプリ再起動後も設定を保持

### 非機能要件
1. **パフォーマンス**
   - 複数フィード取得時も快適な動作を保つ
   - 並列処理で効率的に取得

2. **エラーハンドリング**
   - 一部のフィードが取得失敗しても他のフィードから取得を継続
   - エラーメッセージで失敗したフィードを通知

### UI/UX要件
1. **設定画面**
   - RSSプリセット一覧をチェックボックスで表示
   - カスタムURL追加ボタン
   - 選択されたRSS一覧を確認できる

2. **メイン画面**
   - ニュース記事表示時に配信元（フィード名）を表示
   - 配信元でフィルタリング機能（オプション）

## 画面遷移
```
SetupActivity
├─ RSS選択（チェックボックスリスト）
│  ├─ NHK ☑
│  ├─ Yahoo!ニュース ☑
│  ├─ 毎日新聞 ☐
│  ├─ ...
│  └─ カスタムURL追加 [+]
└─ 保存して開始
```

## データモデル変更
```kotlin
// Before: 単一RSS
data class Settings(
    val rssUrl: String,
    val rssPreset: String?
)

// After: 複数RSS
data class Settings(
    val rssFeeds: List<RssFeed>,  // 複数RSS対応
)

data class RssFeed(
    val name: String,        // フィード名（プリセット名 or カスタム名）
    val url: String,         // RSS URL
    val isCustom: Boolean    // カスタムURLかどうか
)
```

## 影響範囲
- `data/model/Settings.kt` - データモデル変更
- `presentation/setup/SetupUiState.kt` - UI状態変更
- `presentation/setup/SetupViewModel.kt` - ViewModel変更
- `presentation/setup/SetupActivity.kt` - UI実装変更
- `domain/usecase/news/GetNewsUseCase.kt` - ニュース取得ロジック変更
- `data/repository/NewsRepositoryImpl.kt` - 複数RSS取得実装

## 技術的考慮事項
1. **並列処理**
   - Kotlin Coroutinesでasync/awaitを使用
   - 複数フィードを並列取得

2. **データ統合**
   - 複数フィードの記事をマージ
   - 重複記事の検出（タイトル・URLで判定）

3. **後方互換性**
   - 既存の単一RSS設定からマイグレーション処理

## テスト要件
1. **単体テスト**
   - SetupViewModelの複数RSS選択ロジック
   - GetNewsUseCaseの複数フィード取得・統合ロジック
   - データマイグレーション処理

2. **E2Eテスト**
   - 複数RSS選択→保存→メイン画面で表示確認
   - カスタムURL追加→保存→取得確認

## 実装優先度
1. **Phase 1**: データモデル・Repository変更
2. **Phase 2**: UseCase・ViewModel変更
3. **Phase 3**: UI実装（チェックボックスリスト）
4. **Phase 4**: テスト作成

## 備考
- asachiru/feedwatch両フレーバー対応
- 既存のプリセット定義（RssPresets.kt）は変更不要
- マイグレーション処理で既存ユーザーの設定を複数RSS形式に変換
