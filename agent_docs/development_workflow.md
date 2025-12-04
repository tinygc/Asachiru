# 開発ワークフロー

## V字開発プロセス

### 要件定義
- `requirement/` へドキュメントを格納
- ユーザからの要求に対して詳細をヒアリングしながら実施
- ユーザが完了を宣言するまで設計に着手しない

### 設計
- `design/` へドキュメントを格納
- Architecture設計を実施し、Function/Module単位でドキュメント作成
- 指定がない場合は Clean Architecture
- Unityベースの場合は MVP4U

### 実装
- t-wadaの提唱するTDDで実装
- Function/Module完了時にベテランエンジニア視点でレビュー

### テスト
- `test/` へドキュメントやスクリーンキャプチャを格納
- 要件をすべて満たしていることをTestで検証
- Test実施毎に結果をドキュメント出力
- Android TVはリモコンキー操作をADBで実施し、1コマンド毎にスクリーンキャプチャ取得

## Commit とPush
- `git add` → `git commit` → `git push` を実施
- Commit messageは conventional commits に従う（feat:, fix:, test:, chore:）
- Pushする前に変更ファイル一覧を確認
