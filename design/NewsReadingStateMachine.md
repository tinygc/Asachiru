# ニュース読み上げ State Machine 設計書

## 概要
ニュース読み上げ機能の状態管理を State Machine パターンで再設計し、複雑な状態遷移を明確化する。

## 状態定義

### NewsReadingState (sealed class)

```kotlin
sealed class NewsReadingState {
    // 待機中（初期状態）
    object Idle : NewsReadingState()
    
    // 初回開始待機中（10秒広告 + 3秒待機）
    data class WaitingForStart(
        val startTimeMs: Long,
        val showAd: Boolean = false,
        val adEndTimeMs: Long = 0L
    ) : NewsReadingState()
    
    // ニュース取得中
    object FetchingNews : NewsReadingState()
    
    // 記事読み上げ/表示中
    data class ReadingArticle(
        val articleIndex: Int,
        val totalArticles: Int,
        val article: News,
        val estimatedEndTimeMs: Long,
        val isPaused: Boolean = false
    ) : NewsReadingState()
    
    // 記事間待機中（5秒）
    data class ArticleInterval(
        val nextArticleIndex: Int,
        val totalArticles: Int,
        val endTimeMs: Long,
        val isPaused: Boolean = false
    ) : NewsReadingState()
    
    // セッション間待機中（10秒広告 + 5分待機）
    data class SessionInterval(
        val endTimeMs: Long,
        val showAd: Boolean = false,
        val adEndTimeMs: Long = 0L,
        val noNewArticlesSinceMs: Long = 0L
    ) : NewsReadingState()
}
```

## イベント定義

### NewsReadingEvent (sealed class)

```kotlin
sealed class NewsReadingEvent {
    object AppStarted : NewsReadingEvent()
    object StartTimerExpired : NewsReadingEvent()
    data class NewsFetched(val articles: List<News>) : NewsReadingEvent()
    object ArticleCompleted : NewsReadingEvent()
    object IntervalExpired : NewsReadingEvent()
    object AllArticlesCompleted : NewsReadingEvent()
    object SessionIntervalExpired : NewsReadingEvent()
    data class TtsSettingChanged(val enabled: Boolean) : NewsReadingEvent()
    object DetailOpened : NewsReadingEvent()
    object DetailClosed : NewsReadingEvent()
    object BackgroundTransition : NewsReadingEvent()
    object ForegroundTransition : NewsReadingEvent()
}
```

## 状態遷移図

```
[Idle]
  ↓ AppStarted
[WaitingForStart] (10秒広告 + 3秒待機)
  ├─ 最初の10秒: showAd=true（広告表示）
  └─ 残り3秒: showAd=false（待機）
  ↓ StartTimerExpired
[FetchingNews]
  ↓ NewsFetched
[ReadingArticle(0, isPaused=false)]
  ↓ DetailOpened
[ReadingArticle(0, isPaused=true)] ← 一時停止、カウント停止
  ↓ DetailClosed
[ReadingArticle(0, isPaused=false)] ← 再開
  ↓ ArticleCompleted
[ArticleInterval(isPaused=false)]
  ↓ IntervalExpired
[ReadingArticle(1, isPaused=false)]
  ↓ ArticleCompleted
  ... (全記事繰り返し)
  ↓ AllArticlesCompleted
[SessionInterval] (10秒広告 + 5分待機)
  ├─ 最初の10秒: showAd=true（広告表示）
  └─ 残り5分: showAd=false（待機）
  ↓ SessionIntervalExpired
[FetchingNews] (ループ)
```

## TTS設定変更時の動作

- `TtsSettingChanged` イベント発生時:
  - `ReadingArticle` 状態の場合: 現在の記事を再読み込み（TTS設定反映）
  - 他の状態: 設定のみ更新、次回読み上げ時に反映

## カウントダウン表示

- 各状態から次のイベントまでの残り時間を計算
- `isPaused=true` の場合、カウントダウンは停止するが残り時間は保持

## 実装クラス

### NewsReadingStateMachine.kt
- State Machine本体
- イベントハンドリング
- 状態遷移ロジック

### MainViewModel.kt (修正)
- State Machineのインスタンス保持
- UIイベントをState Machineイベントに変換
- 状態からUIStateへの変換

## メリット

1. **明確な状態管理**: 現在の状態が一目瞭然
2. **予測可能な動作**: イベントに対する遷移が明確
3. **テスタブル**: 状態遷移を単体テストできる
4. **デバッグしやすい**: 状態ログで問題箇所を特定
5. **拡張性**: 新しい状態/イベント追加が容易
