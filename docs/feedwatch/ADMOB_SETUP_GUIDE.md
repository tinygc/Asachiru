# RSS FeedWatch - AdMob 設定ガイド

## 概要

このドキュメントでは、RSS FeedWatch用のAdMobアプリを登録し、広告を設定する手順を説明します。

**重要**: FeedWatchはAsachiruとは別のアプリケーションIDを持つため、**別のAdMob App ID**が必要です。

## 前提条件

- Google AdMobアカウント（無料）
- 広告を表示するアプリのパッケージ名: `com.tinygc.feedwatch`

## ステップ1: AdMobアカウントの作成（未作成の場合）

### 1.1 AdMobにアクセス

https://admob.google.com/home/ にアクセス

### 1.2 サインイン

Googleアカウントでサインイン

### 1.3 アカウント情報の入力

1. 国または地域: 日本
2. タイムゾーン: (GMT+09:00) 日本時間
3. 通貨: 日本円（JPY）
4. 利用規約に同意

## ステップ2: FeedWatch用アプリの登録

### 2.1 新しいアプリを追加

1. AdMobホームで「アプリ」→「アプリを追加」をクリック
2. 「アプリはすでにアプリストアに公開されていますか？」→「いいえ」を選択
   - **注意**: 初回リリースの場合は「いいえ」を選択します

### 2.2 アプリ情報の入力

```
アプリ名: RSS FeedWatch
プラットフォーム: Android
アプリのカテゴリ: ニュース＆雑誌
```

### 2.3 App IDの取得

アプリが作成されると、以下の形式のApp IDが発行されます：

```
ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
```

**重要**: このApp IDをメモしておいてください。後ほどAndroidManifestに設定します。

### 2.4 アプリの設定

1. 「アプリの設定」をクリック
2. 「ストアの詳細」セクション
   ```
   パッケージ名: com.tinygc.feedwatch
   ストアURL: (Play Store公開後に追加)
   ```

## ステップ3: 広告ユニットの作成

### 3.1 バナー広告ユニットの作成

1. 「広告ユニット」タブをクリック
2. 「広告ユニットを作成」→「バナー」を選択
3. 広告ユニット情報を入力：

```
広告ユニット名: FeedWatch Main Banner
広告フォーマット: バナー
広告サイズ: スマート バナー（推奨）
```

4. 「広告ユニットを作成」をクリック

### 3.2 広告ユニットIDの取得

広告ユニットが作成されると、以下の形式の広告ユニットIDが発行されます：

```
ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

**重要**: この広告ユニットIDもメモしておいてください。

## ステップ4: build.gradleへの設定

### 4.1 build.gradleの編集

`app/build.gradle` を開き、feedwatch flavorにAdMob App IDを設定します。

現在の構成：
```gradle
productFlavors {
    asachiru {
        dimension "app"
        applicationId "com.tinygc.asachiru"
        versionCode 1
        versionName "1.0"
        resValue "string", "app_name", "朝チル (AsaChil)"
    }

    feedwatch {
        dimension "app"
        applicationId "com.tinygc.feedwatch"
        versionCode 1
        versionName "1.0"
        resValue "string", "app_name", "RSS FeedWatch"
    }
}
```

以下のように修正（AdMob App IDを追加）：

```gradle
productFlavors {
    asachiru {
        dimension "app"
        applicationId "com.tinygc.asachiru"
        versionCode 1
        versionName "1.0"
        resValue "string", "app_name", "朝チル (AsaChil)"
        // Asachiru用のAdMob App ID
        manifestPlaceholders = [
            admobAppId: "ca-app-pub-4454020110016010~4810797955"
        ]
    }

    feedwatch {
        dimension "app"
        applicationId "com.tinygc.feedwatch"
        versionCode 1
        versionName "1.0"
        resValue "string", "app_name", "RSS FeedWatch"
        // FeedWatch用のAdMob App ID（ステップ2.3で取得したID）
        manifestPlaceholders = [
            admobAppId: "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"  // ← 実際のIDに置き換える
        ]
    }
}
```

### 4.2 AndroidManifest.xmlの編集

`app/src/main/AndroidManifest.xml` を開き、AdMob App IDの設定を以下のように変更：

**変更前**:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-4454020110016010~4810797955"/>
```

**変更後**:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${admobAppId}"/>
```

これにより、flavor毎に異なるAdMob App IDが設定されます。

### 4.3 広告ユニットIDの設定（任意）

広告ユニットIDもflavor毎に変えたい場合、`strings.xml`に設定します。

`app/src/feedwatch/res/values/strings.xml` を作成：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- FeedWatch用の広告ユニットID -->
    <string name="admob_banner_unit_id">ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ</string>
</resources>
```

コード側で以下のように使用：

```kotlin
val adUnitId = getString(R.string.admob_banner_unit_id)
```

## ステップ5: テスト広告の確認

### 5.1 テスト用のデバイスIDを追加

開発中は実際の広告ではなく、テスト広告を使用します。

```kotlin
MobileAds.initialize(context) { initializationStatus ->
    // テストデバイスの設定
    val configuration = RequestConfiguration.Builder()
        .setTestDeviceIds(listOf("YOUR_TEST_DEVICE_ID"))
        .build()
    MobileAds.setRequestConfiguration(configuration)
}
```

### 5.2 テストデバイスIDの取得

1. アプリを実行
2. Logcatで以下のようなログを確認：
   ```
   I/Ads: Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"))
   ```
3. このIDをコードに追加

### 5.3 テスト広告の表示確認

- テスト広告には「Test Ad」というラベルが表示されます
- クリックしても収益は発生しません
- 実際の広告配信前に必ずテストすること

## ステップ6: リリース前の確認

### 6.1 チェックリスト

リリース前に以下を確認してください：

- [ ] FeedWatch用の新しいAdMobアプリを作成済み
- [ ] AdMob App IDを取得済み
- [ ] 広告ユニットIDを取得済み
- [ ] build.gradleにmanifestPlaceholdersを追加済み
- [ ] AndroidManifest.xmlで${admobAppId}を使用
- [ ] テスト広告で動作確認済み
- [ ] テストデバイス設定を削除（本番ビルド時）
- [ ] 実際の広告ユニットIDを設定（本番ビルド時）

### 6.2 本番用広告ユニットIDの設定

リリースビルド前に、テスト用広告ユニットIDから本番用広告ユニットIDに変更してください。

## ステップ7: Play Store公開後の設定

### 7.1 アプリのリンク

Play Storeで公開されたら、AdMobにアプリのリンクを追加します：

1. AdMobの「アプリ」→対象アプリを選択
2. 「アプリの設定」→「ストアの詳細」
3. ストアURLを追加：
   ```
   https://play.google.com/store/apps/details?id=com.tinygc.feedwatch
   ```

### 7.2 広告配信の確認

- 公開後、数時間〜1日で広告配信が開始されます
- AdMobダッシュボードで広告のインプレッション数を確認
- 収益レポートを定期的にチェック

## トラブルシューティング

### 問題1: 広告が表示されない

**原因**:
- App IDが間違っている
- 広告ユニットIDが間違っている
- AdMobの審査が完了していない（通常数時間）

**解決策**:
1. Logcatでエラーを確認
2. App IDと広告ユニットIDを再確認
3. テスト広告IDで動作確認

### 問題2: "The ad request is successful but no ad is returned"

**原因**: 広告在庫がない、またはリクエスト設定に問題

**解決策**:
1. テスト広告IDで確認
2. AdMobの「広告の健全性」をチェック
3. 地域設定を確認

### 問題3: アプリがクラッシュする

**原因**: AdMob SDKの初期化に失敗

**解決策**:
1. dependencies に正しいバージョンが設定されているか確認
2. ProGuardルールを確認（リリースビルドの場合）
3. 例外ハンドリングを追加

## AdMob収益化のベストプラクティス

### 1. 広告配置
- ユーザー体験を損なわない位置に配置
- 誤クリックを誘発しない

### 2. 広告頻度
- 過度な広告表示は避ける
- ユーザーのフィードバックを参考に調整

### 3. 広告フォーマット
- バナー広告: 常時表示
- インタースティシャル: 画面遷移時（使用する場合）
- リワード広告: ユーザーがメリットを得られる場合（使用する場合）

## 参考リンク

- [Google AdMob](https://admob.google.com/)
- [AdMob Android スタートガイド](https://developers.google.com/admob/android/quick-start)
- [AdMob ポリシー](https://support.google.com/admob/answer/6128543)
- [広告フォーマット](https://support.google.com/admob/answer/6128738)

## サポート

質問や問題がある場合：

- **Email**: tinygc404@gmail.com
- **GitHub**: https://github.com/tinygc/asachiru

---

**作成日**: 2025-11-25
**最終更新**: 2025-11-25
