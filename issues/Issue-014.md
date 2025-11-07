# Issue #14: 天気API DataSource実装

**Labels:** implementation, data

## 概要
天気APIからデータを取得するDataSourceを実装する。

## タスク
- [ ] WeatherApiDataSource実装
- [ ] WeatherDto実装
- [ ] PostalCodeConverter実装（JSONマッピングテーブル含む）
- [ ] テスト作成（MockWebServer使用）

## 受け入れ条件
- 天気APIから正しくデータ取得できること
- DTOが正しくパースされること
- 郵便番号変換が正しく動作すること
- エラーハンドリングが適切であること
- テストが全てパスすること

## 参考
- design/Module_Weather.md
- design/DESIGN_REVIEW.md（郵便番号変換の修正内容）
