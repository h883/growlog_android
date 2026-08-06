# GrowLog システム仕様書

| 項目 | 内容 |
|---|---|
| システム名 | GrowLog |
| 版数 | 1.1 |
| 更新日 | 2026-08-03 |
| 対象 | Android アプリ、Cloudflare Worker API、D1 データベース |
| 基準 | 現在の実装 |

## 1. 概要

GrowLog は、日々の挑戦・目標・集中時間を記録し、他の利用者と共有する Android 向け SNS である。投稿、目標、集中セッション、DM、グループを一つのアカウントで利用できる。

本仕様書は、実装済みの挙動を基準とする。設計案だけで未実装の機能は「未提供」と明記する。

## 2. システム構成

```text
Android アプリ (Kotlin / Jetpack Compose)
  ├─ Google Sign-In / Firebase Authentication
  └─ HTTPS + Firebase ID トークン
          ↓
Cloudflare Workers + Hono
  ├─ Firebase ID トークン検証
  ├─ Cloudflare D1 (SQLite): アプリデータ
  └─ Cloudflare Workers KV: 投稿・アバター画像
```

| 層 | 採用技術 |
|---|---|
| クライアント | Kotlin、Jetpack Compose、Material 3、Navigation Compose、OkHttp、Coil |
| 認証 | Firebase Authentication、Google Sign-In |
| API | Cloudflare Workers、Hono、TypeScript |
| データベース | Cloudflare D1（SQLite） |
| 画像ストレージ | Cloudflare Workers KV |

API ベース URL は `https://growlog-api.nopzqql.workers.dev/api/v1` とする。

## 3. 認証・初回利用フロー

### 3.1 起動時の遷移

```text
アプリ起動
  ↓
起動画面（必ず表示）
  ↓
Firebase セッション確認
  ├─ 未ログイン: チュートリアル → ログイン画面
  └─ ログイン済み: /auth/login でD1ユーザーを確認 → ホーム
                                      └─ 新規・未入力ならプロフィール作成
```

1. Firebase にログイン済みの場合、起動時に `reload()` でアカウントの有効性を確認する。
2. Firebase 側で削除・無効化されている場合は、Google と Firebase の両方からログアウトし、チュートリアルへ戻す。
3. 未ログインの場合は、起動画面の後にチュートリアルを表示する。
4. チュートリアルでは、アカウント作成またはログインを選択できる。
5. Google 認証後に API の `/auth/login` を呼び出し、D1 に利用者がなければ作成する。
6. 新規ユーザーでプロフィール入力が未完了の場合は、表示名・自己紹介の作成画面を強制表示する。登録完了後はホームへ遷移する。
7. ログアウト時は、初回案内の状態を消去し、チュートリアルから開始する。

### 3.2 認証方式

すべての保護 API に、Firebase ID トークンを以下の形式で送信する。

```http
Authorization: Bearer <Firebase ID Token>
```

Worker は Google 公開鍵（JWKS）を使用して、署名・`aud`・`iss`・有効期限を検証する。

## 4. 画面仕様

| 区分 | 画面 | 主な機能 |
|---|---|---|
| 起動 | 起動画面 | 起動アニメーション後、認証状態に応じて遷移 |
| 初回案内 | チュートリアル | GrowLog の概要表示、ログイン／アカウント作成への導線 |
| 認証 | ログイン | Google Sign-In |
| 初期設定 | プロフィール作成 | 表示名・自己紹介の登録 |
| ホーム | ホーム | おすすめ／フォロー中／最新の投稿、集中中の利用者 |
| 投稿 | 投稿作成・投稿詳細 | 投稿、画像、タグ、目標、コメント、反応、保存 |
| 発見 | 見つける | 投稿・利用者検索、トレンドタグ、おすすめ利用者 |
| 通知 | 通知 | 反応・コメント・フォロー・グループ申請／招待への対応 |
| DM | 会話一覧・チャット | 1対1メッセージ |
| 目標 | 目標一覧・詳細 | 目標作成、進捗更新、達成管理 |
| 集中 | 集中開始・実行・結果・ガーデン | タイマー、休憩、応援、育成テーマ、履歴 |
| グループ | 一覧・作成・詳細 | 参加、招待、承認、メンバー、投稿、固定投稿、集中ルーム |
| プロフィール | 自分・他者・編集 | 投稿、保存、目標、フォロー、アバター編集 |
| 設定 | 設定 | アカウント、通知、表示、集中、ログアウト |

主要画面の下部メニューは、ホーム・見つける・投稿・通知・プロフィールの構成を維持する。DM、集中、グループ、目標は各画面の導線から遷移する。

## 5. 機能仕様

### 5.1 プロフィール・フォロー

- 表示名、ユーザー名、自己紹介、アバター画像を保持する。
- ユーザー名は一意であり、プロフィールやグループ招待で利用する。
- 自分のプロフィールでは投稿、保存、目標、フォロー数、フォロワー数を確認できる。
- 他者プロフィールではフォロー／解除と DM 開始を行える。
- フォロー時は、対象者へ通知を作成する。自分自身のフォローは拒否する。

### 5.2 投稿・タイムライン

投稿は本文を必須とし、以下を任意で追加できる。

| 項目 | 内容 |
|---|---|
| 投稿種別 | `progress` / `achievement` / `insight` / `question` |
| 画像 | 1枚。投稿者自身の KV キーのみ指定可 |
| タグ | 文字列の配列 |
| 目標 | 自分の目標 1 件 |
| 進捗 | 0〜100。目標に紐づく場合は目標進捗を更新 |
| グループ | 所属グループへの投稿。公開範囲を指定可 |

タイムラインは以下の 3 フィードを提供する。

| フィード | 内容 | 並び |
|---|---|---|
| おすすめ | 直近 7 日の投稿 | 反応数降順、投稿日時降順 |
| フォロー中 | フォロー中の利用者の投稿 | 新着順 |
| 最新 | 閲覧可能な全投稿 | 新着順 |

- 投稿に反応（いいね）の付与・解除を行える。
- 投稿を保存・保存解除できる。
- 投稿詳細でコメントの投稿ができ、自分のコメントのみ削除できる。
- コメント、反応、フォローは対象者への通知を作成する（自分自身による操作を除く）。

### 5.3 目標

| 項目 | 内容 |
|---|---|
| タイトル | 必須 |
| 説明 | 任意 |
| 開始日・期限 | 開始日は必須、期限は任意 |
| 進捗 | 0〜100 |
| 達成状態 | 100% 到達時または明示操作で達成状態にする |

利用者は自分の目標を作成・更新できる。投稿に目標と進捗を指定した場合、当該目標の進捗と活動回数を更新する。

### 5.4 検索・発見

- 投稿本文・タグを対象に検索する。
- 表示名・ユーザー名・自己紹介を対象に利用者検索を行う。
- 未検索時は、トレンドタグ、目標付き投稿、おすすめ利用者を表示する。
- 検索条件の SQL ワイルドカードはエスケープする。

### 5.5 DM

- 利用者間の 1 対 1 会話を提供する。
- 同じ相手との会話は 1 件に集約する。
- 会話一覧には相手、最新メッセージ、未読件数、集中状態を表示する。
- 会話を開くと相手からのメッセージを既読にする。
- 会話参加者以外は会話本文を取得できない。

### 5.6 集中セッション・ガーデン

集中セッションは、作業内容・予定時間・関連目標・公開範囲・集中モード・育成テーマを指定して開始する。

| 状態 | 説明 |
|---|---|
| `active` | 集中中 |
| `paused` | 休憩中。経過時間を加算しない |
| `completed` | 完了 |
| `cancelled` | 中断 |

- 同時に進行できるセッションは利用者ごとに 1 件までとする。
- 経過時間・到達段階はサーバーが算出し、端末時刻には依存しない。
- 他者は進行中のセッションに応援（リアクション）を送れる。自分自身には送れない。
- 集中中に受信した通常通知は保留し、終了後に表示する。
- ガーデンは、完了・中断を問わず週ごとの到達段階、回数、合計時間を表示する。
- テーマは `plant`、`city`、`robot`、`space`、`campfire`、`aquarium`、`adventure` を提供する。
- グループには当月の集中実績を集約表示する「グループの森」を提供する。

### 5.7 グループ

#### グループ情報

| 項目 | 内容 |
|---|---|
| グループ名 | 必須 |
| グループ ID | 英数字・ハイフン・アンダースコア、3〜32文字、一意 |
| 説明 | 任意 |
| 公開範囲 | `public` / `approval` / `private` |
| 参加方式 | 公開範囲に連動して `open` / `approval` / `invite` |
| 最大人数 | 2〜5000。既定 500 |

- 1 ユーザーが所有できる有効グループは 3 件までとする。
- 作成者は `owner` として自動参加する。
- 非公開グループは、メンバー以外には一覧・詳細を返さない。
- 投稿・メンバー一覧は閲覧権限のある利用者だけが取得できる。
- 参加者はグループ投稿を作成できる。
- `owner` / `admin` / `moderator` はグループ設定の変更・固定投稿操作を実行できる。
- 固定投稿は最大 3 件とする。
- オーナーはそのまま退出できない。

#### 参加方式

| 方式 | 利用者の操作 | サーバー処理 |
|---|---|---|
| 公開参加 (`open`) | 「参加する」 | 即時にメンバーへ追加 |
| 承認制 (`approval`) | 「参加を申請」 | 保留申請を作成し、`owner` と `admin` へ通知 |
| 招待制 (`invite`) | 招待待ち | 管理権限者がユーザー名で招待する |

承認制の詳細は以下のとおり。

1. 利用者が参加申請を行うと、同一グループ・同一利用者で保留中の申請がないことを確認する。
2. 管理者通知に「{利用者名}を参加させますか」の承認・却下操作を表示する。
3. 管理権限者が承認した場合、申請者を `member` として有効メンバーへ追加する。却下時は参加させない。

招待制の詳細は以下のとおり。

1. `owner`、`admin`、`moderator` は、招待制グループの詳細画面でユーザー名を入力し招待できる。
2. 招待先の利用者にはグループ招待通知を作成する。
3. 招待先は通知の「参加」または「辞退」を選択できる。
4. 参加を選択した場合のみ有効メンバーへ追加する。
5. 既にメンバーである利用者、または同一の保留招待がある利用者への招待は拒否する。

### 5.8 通知

| 種別 | 発生条件 | 受信者の操作 |
|---|---|---|
| `reaction` | 投稿への反応 | 確認 |
| `comment` | 投稿へのコメント | 確認 |
| `follow` | フォロー | 確認 |
| 集中リアクション | 集中セッションへの応援 | 確認 |
| `group_join_request` | 承認制グループへの参加申請 | 管理者が承認／却下 |
| `group_invitation` | 招待制グループへの招待 | 招待先が参加／辞退 |

- 通知は一覧取得と一括既読ができる。
- 集中セッション中の通常通知は保留する。
- グループ参加申請とグループ招待の通知には、参加可否を選ぶ UI を表示する。

### 5.9 設定

設定では、プロフィール編集、通知設定、表示設定、集中の既定時間、アカウント操作、ログアウトを提供する。ログアウト後はチュートリアルへ戻る。

## 6. API 仕様

全 API は JSON を使用する。成功時は 200 または 201、入力不正は 400、認証失敗は 401、権限不足は 403、対象なしは 404、状態競合は 409 を返す。

| 区分 | 主なエンドポイント |
|---|---|
| 認証 | `POST /auth/login` |
| 利用者 | `GET/PATCH /users/me`、`GET /users/me/posts`、`GET /users/me/saves` |
| 他者・フォロー | `GET /users/:userName/profile`、`GET /users/:userName/posts`、`POST /users/:userName/follow` |
| 投稿 | `GET/POST /posts`、`GET /posts/:postId`、`POST /posts/:postId/reactions`、`POST /posts/:postId/save` |
| コメント | `GET/POST /posts/:postId/comments`、`DELETE /posts/:postId/comments/:commentId` |
| 目標 | `GET /goals/my`、`POST /goals`、`PATCH /goals/:goalId` |
| 発見 | `GET /discover`、`GET /discover/search` |
| 通知 | `GET /notifications`、`POST /notifications/read-all` |
| DM | `GET/POST /conversations`、`GET /conversations/:conversationId/messages`、`POST /conversations/:conversationId/messages` |
| 集中 | `POST /focus-sessions`、`GET /focus-sessions/active`、休憩・再開・完了・中断・応援・ガーデン取得 |
| 画像 | `POST /uploads/image`、`POST /uploads/avatar`、`GET /images/:key` |

#### グループ API

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/groups` | 一覧・検索・自分のグループ絞り込み |
| POST | `/groups` | グループ作成 |
| GET/PATCH | `/groups/:groupId` | 詳細取得・管理権限者による更新 |
| POST | `/groups/:groupId/join` | 公開参加または承認制の参加申請 |
| POST | `/groups/:groupId/leave` | 退出 |
| POST | `/groups/:groupId/invitations` | 管理権限者がユーザー名で招待 |
| POST | `/groups/:groupId/join-requests/:requestId/decision` | 管理権限者が申請を承認／却下 |
| POST | `/groups/:groupId/invitations/:invitationId/decision` | 招待先が参加／辞退 |
| GET | `/groups/:groupId/members` | メンバー一覧 |
| GET/POST | `/groups/:groupId/posts` | グループ投稿の取得・作成 |
| POST/DELETE | `/groups/:groupId/posts/:postId/pin` | 固定・固定解除 |
| GET | `/groups/:groupId/focus-room` | グループ集中状況 |

## 7. データ仕様

主要テーブルは次のとおり。

| テーブル | 用途 |
|---|---|
| `users` | 利用者、Firebase UID、プロフィール、ガーデンテーマ |
| `posts` / `comments` / `reactions` / `saves` | 投稿、コメント、反応、保存 |
| `goals` | 目標・進捗・達成状態 |
| `follows` | フォロー関係 |
| `notifications` | 通知。グループID、申請ID、招待IDを保持可能 |
| `conversations` / `messages` | 1対1会話とメッセージ |
| `groups` / `group_members` | グループ・所属・ロール |
| `group_join_requests` | 承認制グループの参加申請 |
| `group_invitations` | 招待制グループの保留招待 |
| `focus_sessions` / `focus_reactions` | 集中セッションと応援 |

参加申請と招待の状態は以下とする。

| テーブル | 保留 | 完了状態 |
|---|---|---|
| `group_join_requests` | `pending` | `approved` / `rejected` |
| `group_invitations` | `pending` | `accepted` / `declined` |

`group_join_requests` は `(group_id, requester_user_id)`、`group_invitations` は `(group_id, invitee_user_id)` の保留状態を一意にする。

## 8. セキュリティ・権限

- すべての保護 API は Firebase ID トークンを必須とする。
- API ではログイン者に対応する D1 ユーザーを取得し、所有者・参加者・会話参加者を検証する。
- 画像キーは投稿者または利用者自身の名前空間だけを受け付ける。
- グループの参加申請の承認は、対象グループの管理権限者だけが行える。
- グループ招待の応答は、招待先本人だけが行える。
- 非公開グループは、非メンバーに存在を公開しない。
- SQL の入力値はプレースホルダで束縛する。

## 9. 運用・ビルド

### Android

```powershell
.\gradlew.bat --offline :app:assembleDebug --console=plain
```

出力 APK は `app/build/outputs/apk/debug/app-debug.apk` とする。

### Worker / D1

```powershell
cd api
npx.cmd tsc --noEmit
npx.cmd wrangler d1 migrations apply growlog-db --remote
npx.cmd wrangler deploy
```

マイグレーションは `api/migrations` に連番で追加し、D1 の `d1_migrations` と整合させる。現在は `0010_group_invitations_and_requests.sql` まで反映済みである。

## 10. 未提供・今後の対象

- グループのロール変更、オーナー権限譲渡、グループ削除の UI
- グループ参加申請の申請者への承認結果通知
- 通報・モデレーション、管理ログ
- 通知の個別既読
- 集中中一覧のページング
- 制限対象アプリの検知、自動一時停止
- グループ内 Q&A の解決状態

これらは本仕様書の実装済み機能には含めない。
