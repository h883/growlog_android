# API リファレンス

ベース URL: `https://growlog-api.nopzqql.workers.dev`

## 認証

`/api/v1/images/*` を除く全エンドポイントが Firebase の ID トークンを要求する。

```
Authorization: Bearer <Firebase ID token>
```

Workers 側は Google の公開鍵証明書を取得し、`jose` で署名・`aud`・`iss` を検証する。
失敗時は `401 { "error": "Unauthorized" }` または `401 { "error": "Invalid or expired token" }`。

## 共通のレスポンス形

投稿オブジェクトは一覧・詳細・検索のすべてで同じ形を返す（`api/src/posts-query.ts` の `mapPostRow`）。

```json
{
  "postId": "uuid",
  "userId": "uuid",
  "displayName": "田山樹",
  "userName": "tayamaituki",
  "authorImageUrl": "https://.../api/v1/images/avatars/... | null",
  "content": "本文",
  "postType": "progress | achievement | insight | question",
  "goalTitle": "目標タイトル | null",
  "progress": 40,
  "tags": ["#プログラミング"],
  "imageUrl": "https://.../api/v1/images/posts/... | null",
  "reactionCount": 3,
  "commentCount": 1,
  "myReaction": false,
  "isSaved": false,
  "createdAt": "2026-07-29T08:00:00.000Z"
}
```

---

## 認証

### POST /api/v1/auth/login

ログイン。D1 にユーザーが存在しなければ作成する。
ユーザー名はメールアドレスのローカル部から生成し、衝突時はランダムな接尾辞を付ける。

レスポンス `200`（既存）/ `201`（新規）:

```json
{ "userId": "uuid", "userName": "tayamaituki", "displayName": "田山樹", "isNewUser": false }
```

---

## ユーザー

### GET /api/v1/users/me

自分のプロフィール。D1 の行をそのまま返すためスネークケースだが、
末尾の 3 つのみキャメルケースで追加している。

```json
{
  "user_id": "uuid",
  "display_name": "田山樹",
  "user_name": "tayamaituki",
  "biography": "",
  "profile_image_key": "avatars/... | null",
  "privacy_type": "public",
  "created_at": "...",
  "profileImageUrl": "https://.../api/v1/images/avatars/... | null",
  "followerCount": 0,
  "followingCount": 0
}
```

### PATCH /api/v1/users/me

プロフィール更新。指定したフィールドのみ更新される。

```json
{ "displayName": "新しい名前", "biography": "自己紹介", "profileImageKey": "avatars/{userId}/{uuid}.jpg" }
```

`profileImageKey` は `avatars/{自分の userId}/` で始まるものだけ受け付ける。
違反時は `400 { "error": "Invalid profileImageKey" }`。

### GET /api/v1/users/me/posts

自分の投稿一覧（最大 50 件、新着順）。`{ "posts": [...] }`

### GET /api/v1/users/me/saves

保存した投稿一覧（最大 50 件、保存した順）。`{ "posts": [...] }`

### GET /api/v1/users/:userName/profile

他ユーザーのプロフィール。

```json
{
  "userId": "uuid", "displayName": "...", "userName": "...", "biography": "",
  "profileImageUrl": "... | null",
  "followerCount": 0, "followingCount": 0, "isFollowing": false,
  "postCount": 3, "goalCount": 1
}
```

### GET /api/v1/users/:userName/posts

他ユーザーの投稿一覧（最大 50 件）。`{ "posts": [...] }`

### POST /api/v1/users/:userName/follow

フォローのトグル。フォロー時は相手に通知を作成する。自分自身は `400`。

```json
{ "following": true }
```

---

## 投稿

### GET /api/v1/posts

タイムライン。1 回につき最大 20 件。

| クエリ | 値 | 挙動 |
|---|---|---|
| `feed` | `popular` | 直近 7 日をリアクション数順。**ページングなし**（`nextCursor` は常に null） |
| `feed` | `following` | フォロー中のユーザーのみ、新着順 |
| `feed` | 省略 | 全体を新着順 |
| `cursor` | ISO 日時 | この日時より前の投稿を取得 |

```json
{ "posts": [...], "nextCursor": "2026-07-29T08:00:00.000Z | null" }
```

### GET /api/v1/posts/:postId

投稿 1 件。存在しなければ `404`。レスポンスは投稿オブジェクトそのもの。

### POST /api/v1/posts

投稿の作成。`content` は必須。

```json
{
  "content": "本文",
  "postType": "progress",
  "goalId": "uuid | 省略",
  "progress": 40,
  "tags": ["#タグ"],
  "imageKey": "posts/{userId}/{uuid}.jpg | 省略"
}
```

- `imageKey` は `posts/{自分の userId}/` で始まるものだけ受け付ける（違反時 `400`）
- `goalId` を指定すると、その目標の `activity_count` が +1 される
- `goalId` と `progress` を同時に指定すると、目標の `progress` もその値に更新される

レスポンス `201`:

```json
{ "postId": "uuid", "createdAt": "..." }
```

### POST /api/v1/posts/:postId/reactions

いいねのトグル。付与時は投稿者に通知を作成する（自分の投稿を除く）。

```json
{ "reacted": true, "reactionCount": 4 }
```

### POST /api/v1/posts/:postId/save

ブックマークのトグル。

```json
{ "saved": true }
```

---

## コメント

### GET /api/v1/posts/:postId/comments

古い順に最大 100 件。

```json
{
  "comments": [{
    "commentId": "uuid", "userId": "uuid",
    "displayName": "...", "userName": "...",
    "userImageUrl": "... | null",
    "content": "コメント本文",
    "createdAt": "..."
  }]
}
```

### POST /api/v1/posts/:postId/comments

コメントの追加。投稿者に通知を作成する（自分の投稿を除く）。

```json
{ "content": "コメント本文" }
```

レスポンス `201`: `{ "commentId": "uuid", "createdAt": "..." }`

### DELETE /api/v1/posts/:postId/comments/:commentId

自分のコメントのみ削除できる。他人のコメントを指定しても `{ "ok": true }` を返すが削除はされない。

---

## 目標

### GET /api/v1/goals/my

自分の目標一覧（新着順、全件）。

```json
{
  "goals": [{
    "goalId": "uuid", "title": "...", "description": "",
    "progress": 40, "startDate": "2026-07-29", "endDate": "2026-12-31 | null",
    "isAchieved": false, "activityCount": 3
  }]
}
```

### POST /api/v1/goals

目標の作成。`title` と `startDate` が必須。

```json
{ "title": "...", "description": "", "startDate": "2026-07-29", "endDate": "2026-12-31" }
```

レスポンス `201`: `{ "goalId": "uuid" }`

### PATCH /api/v1/goals/:goalId

目標の更新。指定したフィールドのみ更新される。

```json
{ "title": "...", "description": "...", "progress": 50, "isAchieved": true }
```

- `progress` は 0〜100 にクランプされる
- `isAchieved: true` で `achieved_at` に現在時刻を記録し、`false` で NULL に戻す
- 自分の目標でない、または存在しない場合は `404 { "error": "Goal not found" }`

---

## 通知

### GET /api/v1/notifications

新着順に最大 50 件。

```json
{
  "notifications": [{
    "notificationId": "uuid",
    "type": "reaction | comment | follow",
    "actorDisplayName": "...", "actorUserName": "...",
    "actorImageUrl": "... | null",
    "postId": "uuid | null",
    "postContent": "本文の先頭 50 文字 | null",
    "commentContent": "... | null",
    "isRead": false,
    "createdAt": "..."
  }]
}
```

### POST /api/v1/notifications/read-all

未読をすべて既読にする。`{ "ok": true }`

---

## 検索・発見

### GET /api/v1/discover

検索前の初期表示用。

```json
{
  "trendingTags": [{ "tag": "#プログラミング", "count": 5 }],
  "suggestedUsers": [{
    "userId": "uuid", "displayName": "...", "userName": "...",
    "biography": "", "profileImageUrl": "... | null", "followerCount": 0
  }]
}
```

- `trendingTags` は直近 200 投稿の `tags` を JS 側で集計した上位 12 件
  （`tags` が JSON 配列カラムのため SQL で集計できない）
- `suggestedUsers` は自分がまだフォローしていないユーザーをフォロワー数順に 10 件

### GET /api/v1/discover/search?q=

投稿とユーザーの横断検索。`q` が空なら空配列を返す。

- 投稿は `content` または `tags` の部分一致（新着順・最大 30 件）
- ユーザーは `display_name` または `user_name` の部分一致（フォロワー数順・最大 20 件）
- `%` `_` `\` はエスケープされる

```json
{ "posts": [...], "users": [...] }
```

---

## 画像

### POST /api/v1/uploads/image
### POST /api/v1/uploads/avatar

画像のアップロード。**multipart ではなく生のバイト列**を body に入れ、
`Content-Type` で形式を指定する。

- 対応形式: `image/jpeg` `image/png` `image/webp` `image/gif`（他は `415`）
- 上限 5MB（超過時 `413`）、空 body は `400`
- キーは `posts/{userId}/{uuid}.{ext}` または `avatars/{userId}/{uuid}.{ext}`

レスポンス `201`:

```json
{ "imageKey": "posts/.../xxx.jpg", "imageUrl": "https://.../api/v1/images/posts/.../xxx.jpg" }
```

`imageUrl` はリクエストの origin から組み立てるため、環境ごとの設定は不要。

### GET /api/v1/images/*

画像の配信。**このエンドポイントのみ認証不要**（キーに UUID を含むため推測が困難）。

キーは UUID を含み中身が差し替わらないので、キー自体を ETag として使い
`If-None-Match` が一致すれば `304` を返す。
`Cache-Control: public, max-age=31536000, immutable` を付与する。
