# GrowLog フロントエンド仕様書

Android アプリ（Jetpack Compose）の現状仕様。実装済みの内容だけを記載する。
API 側は [api-reference.md](api-reference.md)、DB は [database.md](database.md) を参照。

最終更新: 2026-08-02

---

## 1. 技術構成

| 項目 | 内容 |
|---|---|
| UI | Jetpack Compose + Material3 |
| 画面遷移 | Navigation Compose（単一 Activity） |
| 状態管理 | ViewModel + StateFlow（`collectAsState()` で購読） |
| 通信 | OkHttp + `org.json` の手書きパース |
| 画像 | Coil 2.7（`AsyncImage`） |
| 認証 | Firebase Authentication（Google Sign-In） |
| minSdk / targetSdk | 24 / 36 |

DI ライブラリは使わず、`ApiClient.instance` のシングルトンと ViewModel の直接生成で構成している。
引数が必要な ViewModel（`ChatViewModel`、`PostDetailViewModel` など）は
`NavGraph` 内の匿名 `ViewModelProvider.Factory` で生成する。

### パッケージ構成

```
com.example.sns_v1
├── MainActivity.kt        起動制御・ログイン・Scaffold
├── GrowLogApp.kt          Application。Coil に認証ヘッダーを付ける
├── AppState.kt            ログイン中ユーザーの userId / userName / displayName
├── auth/TokenManager      Firebase ID トークンの取得
├── model/                 データクラスのみ（ロジックは最小限）
├── navigation/            Route 定義と NavGraph
├── network/               GrowLogApi（全 API 呼び出し）
├── ui/components/         画面をまたいで使う部品
├── ui/screens/            画面
├── ui/theme/              配色・タイポグラフィ
├── utils/                 画像圧縮・相対時刻
└── viewmodel/             画面ごとの状態
```

---

## 2. 起動フローと画面の出し分け

`MainActivity` が `Crossfade` で4段階を出し分ける。

```
LaunchScreen（起動演出。毎回必ず表示）
  ↓ sessionVerified が立つまで待つ
TutorialScreen（未ログイン時。3ページのオンボーディング）
  ↓ 「次へ」→ AccountCreationScreen ／ 「ログインはこちら」→ 本体へ
AccountCreationScreen（表示名・自己紹介を入力してから Google 認証へ）
  ↓
Scaffold + NavHost（本体）
```

- 起動時に Firebase の `currentUser.reload()` を実行し、削除・無効化されたアカウントを検出してサインアウトする。
- セッション確認が終わるまで `LaunchScreen` を出し続ける（`sessionVerified`）。
- 既にログイン済みの場合、開始 destination が Home のため `LaunchedEffect` での遷移は
  `currentRoute == Route.Login.route` のときだけ行う。これをしないと Home がバックスタックに二重に積まれる。

---

## 3. ナビゲーション

### ルート定義（`Route.kt`）

| Route | パス | 引数 |
|---|---|---|
| Login | `login` | |
| Home | `home` | |
| Discover | `discover` | |
| CreatePost | `create_post` | |
| Notifications | `notifications` | |
| Profile | `profile` | |
| EditProfile | `profile/edit` | |
| Settings | `settings` | |
| Messages | `messages` | |
| Focus | `focus` | |
| Groups | `groups` | |
| GroupDetail | `groups/{groupId}` | groupId |
| Chat | `messages/{conversationId}` | conversationId |
| PostDetail | `posts/{postId}` | postId |
| UserProfile | `users/{userName}` | userName |

### ボトムナビ（`BottomNavBar.kt`）

Figma の Floating Navbar を再現した浮遊バー。5項目。

| 位置 | ラベル | 遷移先 |
|---|---|---|
| 1 | ホーム | Home |
| 2 | 見つける | Discover |
| 3 | 通知 | Notifications |
| 4 | DM | Messages |
| 5 | マイページ | Profile |

- 選択中の項目の上に円形のノッチが浮く。`NotchedBarShape` が角丸矩形から円を
  `PathOperation.Difference` で抜いて実現している。ノッチの X 座標はアニメーションで移動する。
- バーと FAB（投稿ボタン）は上記5ルートにいるときだけ表示する（`showBottomBar`）。
- 投稿は X と同じく右下の FAB から行う。ナビバーには入れない。

`EditProfile` は `Profile` の `ViewModelStoreOwner` を共有し、編集結果がそのまま
プロフィール画面へ反映されるようにしている。

---

## 4. デザインシステム

### 配色（`Color.kt`）

X（旧 Twitter）を下敷きにした、白地・黒文字・青アクセント。面ではなく細い線で区切る。

| 用途 | ライト | ダーク |
|---|---|---|
| 背景 / カード | `#FFFFFF` | `#000000` |
| 沈める面（検索窓・入力欄） | `#F7F9F9` | `#16181C` |
| 主文字 | `#0F1419` | `#E7E9EA` |
| 副文字 | `#536471` | `#71767B` |
| 区切り線 | `#EFF3F4` | `#2F3336` |
| アクセント | `#1D9BF0` | 同左 |
| アクセント（薄） | `#D3E9F8` | `#12354F` |
| いいね | `#F91880` | 同左 |
| 主要ボタン（フォローなど） | `#0F1419` | — |

`Theme.kt` では `surfaceContainer` 系を全段階明示している。未指定だと
Material3 の既定である紫がダイアログやチップに出るため。

### タイポグラフィ（`Type.kt`）

本文 15sp / 行高 25.5sp を基準。見出し 18〜28sp、補助 12〜14sp。
Compose の `Typography` を上書きしているが、画面側は `fontSize` を直接指定している箇所も多い。

### 共通部品

| 部品 | 役割 |
|---|---|
| `GrowLogTopBar` | 「GrowLog」＋右アイコン（集中・検索・DM・通知）。`actions` で差し替え可 |
| `UnderlineTabs` | 等幅タブ。選択中のみ固定幅 52dp の下線 |
| `PostCard` | タイムラインの1件。アバター左・区切り線で仕切る |
| `UserAvatar` | 画像がなければ頭文字のアバター |
| `AppCard` | 角丸のカード面 |
| `ProgressRow` | 目標の進捗バー |
| `GoalCard` / `CreateGoalDialog` | 目標の表示・作成 |
| `FocusPresenceBadge` | 集中プレゼンス（後述） |
| `BottomNavBar` | 浮遊ナビバー |

---

## 5. 画面一覧

| 画面 | ファイル | 主な内容 |
|---|---|---|
| ホーム | `HomeScreen` | おすすめ/フォロー中/最新の3タブ、集中中レール、投稿一覧 |
| 見つける | `DiscoverScreen` | 検索、トレンドタグ、今週の挑戦、おすすめユーザー、グループ入口 |
| 通知 | `NotificationsScreen` | 通知一覧。集中中に届いたものは保留され終了後に出る |
| DM 一覧 | `MessagesScreen` | 会話一覧。相手のプレゼンス表示あり |
| チャット | `ChatScreen` | 1対1のメッセージ |
| マイページ | `ProfileScreen` | プロフィール、集中状況、今週の庭、挑戦中の進捗、投稿/目標/保存/達成記録の4タブ |
| プロフィール編集 | `EditProfileScreen` | 表示名・自己紹介 |
| 他人のプロフィール | `UserProfileScreen` | プロフィール、集中状況、フォロー、投稿一覧 |
| 投稿作成 | `CreatePostScreen` | 本文・画像・タグ・目標紐付け |
| 投稿詳細 | `PostDetailScreen` | 投稿本文とコメント |
| 集中 | `FocusScreen` | 作業ガーデン（後述） |
| グループ一覧 | `GroupsScreen` | 検索・参加中・作成 |
| グループ詳細 | `GroupDetailScreen` | ホーム/メンバー/情報の3タブ、みんなの森 |
| 設定 | `SettingsScreen` | ログアウトなど |

---

## 6. 集中ステータス（プレゼンス）

Discord のオンライン表示に相当する。集中画面の外でも「いま集中中」が見える。

### 表示場所

| 場所 | 表示 |
|---|---|
| ホーム上部 | 「いま集中中 N人」の横スクロール。アバター＋育成中の絵文字＋作業内容 |
| タイムラインの投稿 | 名前の横にミニバッジ、アバター右下にインジケータ |
| 投稿詳細 | 同上 |
| マイページ | 「「〇〇」を育てています」の1行 |
| 他人のプロフィール | 同上（「樹さんは〜」と主語付き） |
| DM 一覧 | 相手のアバターにインジケータ＋名前の横にバッジ |

### 部品（`FocusPresenceBadge.kt`）

| Composable | 用途 |
|---|---|
| `FocusDot` | 集中中はゆっくり明滅、休憩中は停止してグレー |
| `FocusAvatarIndicator` | アバター右下に重ねる点。背景色で縁取る |
| `FocusMiniBadge` | 名前の横の「🌿 集中中」 |
| `FocusStatusLine` | 「〇〇さんは「△△」を育てています」＋経過時間＋段階 |

### 更新

ホームでは 30 秒ごとに集中状況だけを取り直す（`LaunchedEffect` のループ）。
他画面へ移ると NavHost が Home を破棄するため、ポーリングも止まる。
取得した一覧で既存の投稿の `authorFocus` も更新するので、タイムラインを
読み直さなくてもバッジが最新になる。

公開範囲（全体/フォロワー/グループ/自分のみ）の判定はサーバー側で完結しており、
クライアントは受け取った内容をそのまま描画する。

---

## 7. 作業ガーデン

集中の経過をパーセントではなく「段階」で表す。

### 段階

0〜5 の6段階。段階番号はサーバーが確定させ、クライアントは名前と絵文字だけを持つ（`GardenTheme.kt`）。

| 段階 | 予定時間なし | 予定時間ありのとき |
|---|---|---|
| 0 | 開始 | 0% |
| 1 | 15分 | 12.5% |
| 2 | 30分 | 25% |
| 3 | 60分 | 50% |
| 4 | 90分 | 75% |
| 5 | 120分 | 100% |

予定時間を設定している場合は、その時間を最終段階として等分する。
30分でも2時間でも同じ割合で進む。

### テーマ（7種）

| テーマ | 段階0 → 段階5 |
|---|---|
| ガーデン | 🌰 種を植えた → 🌱 発芽 → 🌿 若葉 → 🪴 大きく育つ → 🌳 木になった → 🌸 花が咲いた |
| 街づくり | 🚧 更地を整地 → 🧱 基礎工事 → 🏠 建物が建つ → 🏘 通りができる → 🏢 高層ビル → 🌆 街が完成 |
| ロボット | 📐 設計開始 → 🔩 フレーム完成 → ⚙️ モーター搭載 → 🔌 配線完了 → 💡 プログラム起動 → 🤖 ロボット完成 |
| 宇宙開発 | 📋 設計図を引く → 🔧 部品が届く → 🛠 機体を組む → ⛽ 燃料充填 → 🚀 発射台へ → 🌌 打ち上げ成功 |
| 焚き火 | 🪵 薪を組む → ✨ 着火 → 🕯 小さな炎 → 🔥 燃え上がる → 🎇 熾火が輝く → 🏕 満天の焚き火 |
| 水族館 | 💧 水を張る → 🐟 小魚が来た → 🪸 水草が育つ → 🐠 群れが増える → 🐋 大きな魚 → 🐳 にぎやかな水槽 |
| 冒険 | 🎒 旅の支度 → 🚶 出発 → 🌲 森を抜ける → ⛰ 山を越える → 🗺 目的地が見えた → 🏁 到達 |

テーマは開始ダイアログで選ぶ。前回選んだテーマが次回の初期選択になる。

### 集中画面（`FocusScreen`）

未開始のときは「いま何に取り組みますか」＋「集中をはじめる」。
開始後は次の順で並ぶ。

```
集中しています / 休憩中
作業内容
（目標）

        🌿          ← 68sp。数字ではなくこれを主役にする
      若葉
  作業開始から 42分

■■■□□□              ← 6マスの段階トラック。パーセントは出さない
次の段階「大きく育つ」まで あと18分

邪魔せず応援 3 ・ 休憩1回
[ 休憩する ] [ 終了する ]

今週の庭
🌳 🌷 🌿 🌻 🌱
集中セッション：5回
育てた時間：4時間35分

集中している人 N人
（他ユーザーのカード）
```

### 他ユーザーの見え方

```
（アバター）樹さんは集中中です

              🌿

SNSアプリの画面設計を作業中
作業開始から 42分
状態：若葉が育っています

[    そっと水をあげる 💧    ]
```

送れるのは水やりのみ。集中中は通知されず、終了後にまとめて届く。

### 終了時

```
今日はここまで育ちました

        🌿
「若葉」まで育ちました
次回はここから続けられます

作業内容  SNSアプリの画面設計
集中時間  42分
休憩回数  1回

[今日の成果を書く]
[投稿しない] [投稿する]
```

途中終了でも同じ画面を出す。枯らしたり壊したりする演出は入れない。

### グループの森（`GroupDetailScreen`）

グループのホームタブ上部に表示する。

```
みんなの森
🌿 🪴 🌳          ← いま集中中のメンバー
いま 3人が集中しています

今月、みんなで 126本 育てました
参加 12人・合計 84時間20分
```

個人の数字は並べず、合計だけを見せる。

---

## 8. ダイアログの扱い

Compose の `AlertDialog` は既定では IME に合わせて縮まない。中身が縦に長いダイアログで
キーボードを出すと、下端のボタン行が画面外に押し出されて押せなくなる。
入力欄を持つダイアログには次の2点を必ず入れる。

```kotlin
properties = DialogProperties(decorFitsSystemWindows = false),
modifier = Modifier.imePadding(),
```

さらにスクロール領域に `heightIn(max = 360.dp)` の上限を付け、ボタン行が
必ず画面内に残るようにする。`StartFocusDialog` と `FocusResultDialog` に適用済み。

無効化したボタンは、なぜ押せないのかを画面上に出す（例：「作業内容を入力すると開始できます」）。

---

## 9. 画像

- 投稿・アバターの画像は `ImageUtils.compressImageForUpload` で長辺 1440px・JPEG 85% に再エンコードしてから送る。再エンコードにより EXIF は落ちる。
- `BitmapFactory.decodeStream` は `inJustDecodeBounds = true` のとき必ず null を返す。戻り値で成否を判定してはいけない（サイズは `Options` 側に入る）。
- 画像 API は認証必須のため、`GrowLogApp` が Coil の OkHttp に `Authorization` ヘッダーを差し込む。`AndroidManifest.xml` の `android:name=".GrowLogApp"` を外すと全画像が 401 になる。

---

## 10. 実装上の決まりごと

- ソースの置換に PowerShell を使わない。日本語が文字化けする。
- `ViewModel` は状態を `MutableStateFlow` で持ち、公開は `asStateFlow()` のみ。
- 一覧の楽観更新（いいね・保存）は失敗時に元へ戻す（`revertReaction` / `revertSave`）。
- タブ切り替え中に前のリクエストが返ってきた場合は結果を捨てる（`if (_feed.value == requested)`）。
- エラーは画面上部の帯に出し、タップで消せるようにする。

---

## 11. 未実装・今後

| 項目 | 状態 |
|---|---|
| 集中画面を離れたときの自動一時停止 | 未実装。手動の「休憩する」のみ |
| 制限対象アプリの検知（「寄り道」記録） | 未実装。UsageStats / Accessibility 権限が必要 |
| 光を届ける／応援メッセージの予約 | サーバーは `light` を受け付けるが UI 未実装 |
| 前回の段階からの再開（キャリーオーバー） | 文言のみ。実際の引き継ぎは未実装 |
| ホームの集中中レールのページング | `/focus-sessions/active` の上限 50 件に依存 |
| グループ機能の第3〜5段階 | 参加申請・招待・管理者権限・通報などが未実装 |
| 不適切語句・画像のモデレーション | 未着手 |
