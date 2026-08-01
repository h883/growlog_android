# データベース設計

Cloudflare D1（SQLite）を使用。データベース名 `growlog-db`。

日時はすべて ISO 8601 文字列（`2026-07-29T08:00:00.000Z`）で保存する。
ID はアプリケーション側で `crypto.randomUUID()` を生成して入れる。

## マイグレーション

| ファイル | 内容 |
|---|---|
| `0001_create_users.sql` | `users` |
| `0002_create_posts_goals.sql` | `goals` / `posts` / `reactions` |
| `0003_comments_follows_notifications.sql` | `comments` / `follows` / `notifications` |
| `0004_post_images.sql` | `posts.image_key` を追加 |
| `0005_saves.sql` | `saves` |

**リモートの D1 には自動適用されない。** 追加したら明示的に実行する。

```bash
cd api && npx wrangler d1 execute growlog-db --remote --file=migrations/0005_saves.sql
```

---

## テーブル

### users

| カラム | 型 | 備考 |
|---|---|---|
| user_id | TEXT | PK |
| firebase_uid | TEXT | UNIQUE。Firebase の `sub` |
| email | TEXT | |
| display_name | TEXT | NOT NULL |
| user_name | TEXT | UNIQUE NOT NULL。`@` 付きで表示する識別子 |
| biography | TEXT | |
| profile_image_key | TEXT | KV のキー |
| privacy_type | TEXT | 既定 `public`。**未使用** |
| account_status | TEXT | 既定 `active`。**未使用** |
| created_at / updated_at / last_login_at | TEXT | NOT NULL |

インデックス: `firebase_uid`, `user_name`

### posts

| カラム | 型 | 備考 |
|---|---|---|
| post_id | TEXT | PK |
| user_id | TEXT | → users(user_id) CASCADE |
| content | TEXT | NOT NULL |
| post_type | TEXT | 既定 `progress`。`progress` / `achievement` / `insight` / `question` |
| goal_id | TEXT | → goals(goal_id) SET NULL |
| progress | INTEGER | 0〜100。null 可 |
| tags | TEXT | **JSON 配列の文字列**。既定 `'[]'` |
| image_key | TEXT | KV のキー |
| reaction_count | INTEGER | 非正規化カウンタ |
| comment_count | INTEGER | 非正規化カウンタ |
| created_at / updated_at | TEXT | NOT NULL |

インデックス: `user_id`, `created_at`

`tags` を JSON 文字列で持っているため SQL で集計できない。
人気タグの算出は直近 200 件を取得して JS 側で数えている（`routes/discover.ts`）。

### goals

| カラム | 型 | 備考 |
|---|---|---|
| goal_id | TEXT | PK |
| user_id | TEXT | → users(user_id) CASCADE |
| title | TEXT | NOT NULL |
| description | TEXT | 既定 `''` |
| progress | INTEGER | 0〜100。既定 0 |
| start_date | TEXT | NOT NULL。`YYYY-MM-DD` |
| end_date | TEXT | null 可 |
| is_achieved | INTEGER | 0 / 1 |
| achieved_at | TEXT | 達成にした瞬間だけ記録し、取り消すと NULL に戻る |
| activity_count | INTEGER | この目標に紐付いた投稿数 |
| created_at / updated_at | TEXT | NOT NULL |

インデックス: `user_id`

### reactions

| カラム | 型 | 備考 |
|---|---|---|
| reaction_id | TEXT | PK |
| post_id | TEXT | → posts(post_id) CASCADE |
| user_id | TEXT | → users(user_id) CASCADE |
| reaction_type | TEXT | 既定 `like`。**現状 like のみ** |
| created_at | TEXT | NOT NULL |

`UNIQUE(post_id, user_id)` / インデックス: `post_id`

### comments

| カラム | 型 | 備考 |
|---|---|---|
| comment_id | TEXT | PK |
| post_id | TEXT | → posts(post_id) CASCADE |
| user_id | TEXT | → users(user_id) CASCADE |
| content | TEXT | NOT NULL |
| created_at / updated_at | TEXT | NOT NULL |

インデックス: `post_id`

### follows

| カラム | 型 | 備考 |
|---|---|---|
| follower_id | TEXT | → users(user_id) CASCADE |
| following_id | TEXT | → users(user_id) CASCADE |
| created_at | TEXT | NOT NULL |

`PRIMARY KEY (follower_id, following_id)` / インデックス: 両方向

### notifications

| カラム | 型 | 備考 |
|---|---|---|
| notification_id | TEXT | PK |
| user_id | TEXT | 通知の受け取り手 |
| type | TEXT | `reaction` / `comment` / `follow` |
| actor_id | TEXT | 通知を発生させた人 |
| post_id | TEXT | → posts CASCADE。follow では null |
| comment_id | TEXT | → comments SET NULL |
| is_read | INTEGER | 0 / 1 |
| created_at | TEXT | NOT NULL |

インデックス: `(user_id, created_at)`

### saves

| カラム | 型 | 備考 |
|---|---|---|
| user_id | TEXT | → users(user_id) CASCADE |
| post_id | TEXT | → posts(post_id) CASCADE |
| created_at | TEXT | NOT NULL |

`PRIMARY KEY (user_id, post_id)` / インデックス: `(user_id, created_at)`

---

## 投稿一覧クエリの共通化

投稿を返す箇所が posts / users / follows / discover の 4 つあるため、
`api/src/posts-query.ts` に SELECT 句と FROM 句、マッパーを切り出している。

```ts
SELECT ${POST_COLUMNS} ${POST_FROM} WHERE ...
```

`POST_FROM` の LEFT JOIN が**閲覧者の user_id を 2 つバインドする**
（`my_reaction` 用と `is_saved` 用）。そのため bind の先頭は必ず

```ts
.bind(viewerId, viewerId, ...その他)
```

の順にすること。ここを間違えると他人のいいね状態が混ざる。

---

## 画像ストレージ (Workers KV)

名前空間 `growlog-api-MEDIA`（バインディング名 `MEDIA`）。

| 項目 | 内容 |
|---|---|
| キー | `posts/{userId}/{uuid}.{ext}` / `avatars/{userId}/{uuid}.{ext}` |
| 値 | 画像のバイト列 |
| メタデータ | `{ contentType: "image/jpeg" }` |

D1 側には `posts.image_key` と `users.profile_image_key` にキーだけを保存し、
配信 URL は Worker がリクエストの origin から組み立てる。

### R2 ではなく KV を使っている理由

R2 の利用開始にはサブスクリプション追加（利用規約への同意と支払い方法への課金契約）が必要で、
それを避けたいという判断のため。KV は追加契約なしの無料枠で使える。

代償として次の制約がある。

- 書き込みの反映に最大 60 秒（投稿直後に画像が出ないことがある）
- 1 日 1000 書き込み / 10 万読み込み、ストレージ 1GB
- 1 値あたり 25MiB（アップロードは 5MB で制限済み）

R2 に移行する場合は `api/src/routes/uploads.ts` の `put` / `getWithMetadata` と
`wrangler.jsonc` のバインディングを差し替えるだけでよい。
D1 のスキーマは変更不要（キー文字列の保存形式は同じ）。
