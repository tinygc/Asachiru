# GitHub Actions リリースビルド セットアップガイド

## 概要

GitHub Actions を使って、手動トリガーでリリース用AABをビルドし、GitHub Releasesにアップロードする仕組み。

## セットアップ手順

### 1. キーストアをBase64エンコード

PowerShellで以下を実行：

```powershell
# FeedWatch用
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\feedwatch-release.jks"))

# Asachiru用
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\asachiru-release.jks"))
```

出力された長い文字列をコピーしておく。

### 2. GitHub Secrets を設定

リポジトリの設定ページを開く：
https://github.com/tinygc/asachiru/settings/secrets/actions

「New repository secret」で以下を追加：

#### FeedWatch用

| Secret名 | 値 |
|----------|-----|
| `FEEDWATCH_KEYSTORE_BASE64` | 手順1でエンコードしたキーストア |
| `FEEDWATCH_STORE_PASSWORD` | キーストアのパスワード |
| `FEEDWATCH_KEY_ALIAS` | `feedwatch` |
| `FEEDWATCH_KEY_PASSWORD` | キーのパスワード |

#### Asachiru用

| Secret名 | 値 |
|----------|-----|
| `ASACHIRU_KEYSTORE_BASE64` | 手順1でエンコードしたキーストア |
| `ASACHIRU_STORE_PASSWORD` | キーストアのパスワード |
| `ASACHIRU_KEY_ALIAS` | `asachiru` |
| `ASACHIRU_KEY_PASSWORD` | キーのパスワード |

### 3. 完了確認

全部で8つのSecretsが設定されていればOK。

## 使い方

### リリースビルドの実行

1. GitHub → **Actions** タブを開く
2. 左サイドバーの **Release Build** を選択
3. **Run workflow** ボタンをクリック
4. 以下を入力：
   - **version**: バージョン番号（例: `1.3.0`）
   - **flavor**: `feedwatch` または `asachiru`
   - **create_tag**: タグを作成するかどうか（デフォルト: true）
5. **Run workflow** をクリックして実行

### 実行結果

- ✅ 指定フレーバーのAABがビルドされる
- ✅ `{flavor}-v{version}` 形式のGitタグが作成される（例: `feedwatch-v1.3.0`）
- ✅ GitHub Releases にAABファイルがアップロードされる
- ✅ リリースノートが自動生成される

### AABのダウンロード

GitHub Releases ページからダウンロード：
https://github.com/tinygc/asachiru/releases

## トラブルシューティング

### ビルドが失敗する場合

1. **Secrets の確認**: 名前のtypoがないか確認
2. **Base64エンコード**: 改行が含まれていないか確認
3. **パスワード**: 特殊文字がエスケープされてないか確認

### タグが作成されない場合

- `create_tag` オプションが `true` になっているか確認
- 同じタグ名が既に存在していないか確認

## 注意事項

- **Secrets は絶対に公開しない**こと
- キーストアファイル (.jks) 自体はリポジトリにコミットしないこと
- `local.properties` もコミットしないこと（.gitignoreに含まれている）

## 関連ファイル

- `.github/workflows/release-build.yml` - ワークフロー定義
- `app/build.gradle` - ビルド設定・署名設定
