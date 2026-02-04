# 広告（AdMob）実装状況

## 現状（実装済み）
### ✅ スマートフォン向けは完全実装済み
1. **SDK統合**: play-services-ads:23.0.0 導入済み
2. **App ID設定**: 
   - Asachiru: `ca-app-pub-4454020110016010~4810797955`
   - FeedWatch: `ca-app-pub-4454020110016010~3627678411`
3. **広告ユニットID設定**:
   - Asachiru: `ca-app-pub-4454020110016010/3966288443`
   - FeedWatch: `ca-app-pub-4454020110016010/7480003958`
4. **Manifest設定**: AdMob App IDをmeta-dataで設定済み
5. **実装コード**:
   - `AdRepository.kt`: 初期化・広告読み込み処理
   - `MainActivity.kt`: アダプティブバナー表示ロジック
   - TVデバイス検出時は広告を自動スキップ（`DeviceUtils.isTV()`）

## ❌ Android TV向けは非対応（意図的）
### 理由
- **AdMob公式がAndroid TVを非サポート**
- 現在のコードでは `DeviceUtils.isTV()` 時に広告を表示しない設計

### 実装箇所
```kotlin
// MainActivity.kt setupAdView()
if (DeviceUtils.isTV(applicationContext)) {
    android.util.Log.d("AdView", "TV device detected, skipping AdView setup")
    return
}

// MainActivity.kt updateAdView()
if (DeviceUtils.isTV(applicationContext)) {
    binding.adViewContainer.visibility = android.view.View.GONE
    binding.adCountdownView.visibility = android.view.View.GONE
    return
}
```

## Android TV向け広告を実装する場合の必要作業
### 1. TV向け広告SDKの選定
- **AdMob for TV**: 公式非対応のため使用不可
- **代替SDK検討**:
  - Amazon Publisher Services (APS)
  - IronSource
  - Custom広告ソリューション

### 2. 実装が必要な項目
1. TV向け広告SDK依存関係追加（build.gradle）
2. TV用広告ユニットID取得・設定
3. `updateAdView()` と `setupAdView()` のTV判定分岐を修正
4. TV向けUIレイアウト調整（広告コンテナサイズ等）
5. リモコン操作での広告インタラクション対応
6. プライバシーポリシー更新（TV向けSDK追加の明記）

### 3. 法的・ポリシー考慮事項
- Google Play Store TV要件に準拠
- 各広告プラットフォームのTV向けポリシー確認
- ユーザー同意取得（GDPR、COPPA等）
