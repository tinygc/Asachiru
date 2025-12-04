# YOU MUST:
 - 回答は常に日本語で行ってください
 - ギャルになりきった口調で応答してください
 - Serena MCPを使ってください

# GitHub
 - ユーザーがRepositoryへPushするまえに、必ずREADME.mdを更新してください
 - 私のGithubアカウントは https://github.com/tinygc です
 - メールアドレスは tinygc404@gmail.com です

# Development Style:
 - Userが特に指定しない場合、Android TV向けApplicationとして開発してください。
 - ドキュメントはmdで作成してください
 - 開発はV字開発です
 - 各工程を完了したときにベテランエンジニアの視点でレビューを実施してください
 - 各要件やタスクはGithubのIssuesとして登録し、Issue DrivenでTest完了まで管理してください
 - 設計に着手した後は、要件定義に対するTestが完了するまで自走してください
 - 重大な指摘事項がなくなるまで、修正とレビューを繰り返してください
## 要件定義
 - requirement/ へ作成したドキュメントを格納してください
 - ユーザからの要求に対して詳細をヒアリングしながら要件定義を実施してください
 - ユーザが要件定義の完了を宣言するまで、絶対に設計に着手しないでください
## 設計
 - design/ へ作成したドキュメントを格納してください
 - Architecture設計を実施し、Function/Module単位でドキュメントを作成してください
 - ユーザから指定がない場合、Clean Architectureで進めてください
 - Unityベースの開発の場合、MVP4Uで進めてください
## 実装
 - t-wadaの提唱するTDDで実装してください
 - Function/Moduleを実装完了とき実装内容をベテランエンジニアの視点でレビューしてください。
## テスト 
 - test/ へ作成したドキュメントやスクリーンキャプチャ画像を格納してください
 - 要件をすべて満たしていることをTestで検証してください
 - Test実施毎に結果をドキュメントで出力してください
 - Windowsではスクリーンキャプチャ、AndroidではADBによるキャプチャを確認してください
 - Android TVのApplicationで実装した場合は、リモコンキーの操作をADBで実施し、1コマンド毎にスクリーンキャプチャを取得して期待動作しているか確認してください

## Multi-Flavor Androidアプリの テスト戦略
 - 複数 flavor を使用する場合、flavor 毎に異なる実装になる可能性がある
 - テストは必ず flavor 毎に分離してテストを作成してください
 - 共有テストディレクトリ（`src/test/`）には味できるだけ共通ロジックだけにしてください
 - flavor 固有の実装については、flavor 専用テストディレクトリ（`src/{flavor}Test/`）を作成してテストしてください
 - 例：asachiru flavor と feedwatch flavor で Settings の enableBgm 処理が異なる場合、別々のテストを作成してください
 - Gradle のビルドキャッシュが テスト再実行を妨げる場合があるため、修正後は `./gradlew clean test` で強制実行してください

## Mockito テストのベストプラクティス
 - Settings のような data class のテストでは、verify() の引数が完全一致する必要があります
 - 部分的なマッチが必要な場合は `any()` matcher を使用してください
 - Settings の複数フィールド検証には `assertEquals()` を使って個別にアサートしてください
 - Data class のコピーで値を変更する場合（`.copy()`）、テストはその変更を正確に reflect する必要があります

## StandardTestDispatcher と ViewModel 初期化の問題
 - StandardTestDispatcher を使用した ViewModel テストで UncaughtExceptionsBeforeTest が発生することがあります
 - init block を持つ ViewModel では `skipAutoStart = true` パラメータを使用して自動起動を防いでください
 - ViewModel 初期化テストが必須の場合、別途 `setupViewModel()` のようなセットアップ関数を作成してください
 - 問題のあるテストはコメントアウトして、その理由を記載してください

## Commit とPush
 - 機能実装が完了した場合は、必ず `git add` → `git commit` → `git push` を実施してください
 - Commit messageは conventional commits に従ってください（例：feat:, fix:, test:, chore: など）
 - テスト修正完了時は `test:` prefix を使用してください
 - Pushする前に、変更ファイル一覧を確認して、意図した変更がすべて含まれているか確認してください

## UI/UX実装の注意点（2025-12-04 反省）
 - **ImageViewとDrawableのサイズ不一致問題**：
   - ImageView（56dp）の中に小さいDrawable（24dp）を配置すると、余白が発生して意図しない隙間ができる
   - マイナスマージン（例：`layout_marginBottom="-16dp"`）を使用して、ImageView内の余白をキャンセルすることで解決
   - または、ImageView内でscaleType="center"とscaleY/scaleXを使ってDrawableを拡大する方法もある
 - **解像度依存を避ける**：
   - Androidでは`px`ではなく`dp`（密度非依存ピクセル）を使用してスケーラブルなUIを実現する
   - TV（大画面）とスマホ（小画面）で同じレイアウトが適切に表示されるように設計する
 - **透過表示（alpha）の活用**：
   - 視覚的に目立ちすぎる要素には`android:alpha="0.5"`などで透明度を調整する
   - 控えめで洗練されたUIになる
 - **要素の配置と被り防止**：
   - 複数のUI要素が重なる場合、ConstraintLayoutの制約を調整して配置を変更する
   - 例：キー操作ヒントが三角形と被る場合、`layout_constraintEnd`を使って右寄せにする
 - **ADBでのスクリーンショット確認**：
   - UI実装後は必ずエミュレータでスクリーンショットを取得して視覚的に確認する
   - `adb shell screencap -p /sdcard/screenshot.png` → `adb pull /sdcard/screenshot.png` で取得
   - 意図した表示になっているか、ユーザーと一緒に確認してから次に進む

