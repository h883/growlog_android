# GrowLog

目標への挑戦と日々の進捗を記録・共有する Android 向け SNS アプリ。

「小さな前進を、ことばにする」をコンセプトに、目標を立て、進捗を投稿し、
他のユーザーの取り組みを見て互いに励まし合うことを目的としている。

---

## 構成

| レイヤー | 技術 |
|---|---|
| クライアント | Android (Kotlin / Jetpack Compose) |
| 認証 | Firebase Authentication (Google Sign-In) |
| API | Cloudflare Workers + Hono |
| データベース | Cloudflare D1 (SQLite) |
| 画像ストレージ | Cloudflare Workers KV |

```
[Android アプリ]
   |  Firebase ID トークンを Authorization: Bearer で送信
   v
[Cloudflare Workers (Hono)]
   |  jose で Google の公開鍵を取得しトークンを検証
   +--> [D1]  ユーザー・投稿・目標・コメント・フォロー・通知・保存
   +--> [KV]  投稿画像・プロフィール画像の実体
```

### 各種 ID

| 項目 | 値 |
|---|---|
| Android パッケージ名 | `com.example.sns_v1` |
| Firebase プロジェクト | `growlog-6cf61` |
| Workers URL | https://growlog-api.nopzqql.workers.dev |
| D1 データベース | `growlog-db` (`4db67004-9d59-4088-96d2-3999ffd15b57`) |
| KV 名前空間 | `growlog-api-MEDIA` (`b9fd96dd1c6c4525bb13d04a9c00eb97`) |

---

## ディレクトリ構成

```
SNSV1/
├── api/                        Cloudflare Workers (API)
│   ├── migrations/             D1 マイグレーション (0001〜0005)
│   ├── src/
│   │   ├── index.ts            ルーティングのマウント
│   │   ├── types.ts            Env バインディング定義
│   │   ├── media.ts            画像の MIME/サイズ制限と URL 組み立て
│   │   ├── posts-query.ts      投稿一覧の共通 SELECT とマッパー
│   │   ├── middleware/auth.ts  Firebase ID トークン検証
│   │   └── routes/             エンドポイント実装
│   └── wrangler.jsonc          バインディング設定
│
└── app/src/main/java/com/example/sns_v1/
    ├── MainActivity.kt         Firebase 認証・Scaffold・ログアウト
    ├── AppState.kt             ログイン中ユーザーの保持
    ├── auth/TokenManager.kt    ID トークン取得
    ├── model/                  データクラス
    ├── network/GrowLogApi.kt   API クライアント (OkHttp + org.json)
    ├── navigation/             Route 定義と NavGraph
    ├── viewmodel/              画面ごとの ViewModel
    ├── ui/screens/             各画面
    ├── ui/components/          PostCard・UserAvatar・GoalCard 等
    └── utils/                  画像圧縮・日時整形
```

---

## セットアップと実行

### API

```bash
cd api && npm install
```

ローカル開発サーバー:

```bash
cd api && npx wrangler dev
```

本番デプロイ:

```bash
cd api && npx wrangler deploy
```

マイグレーションの適用（**リモートには自動適用されないので明示的に実行する**）:

```bash
cd api && npx wrangler d1 execute growlog-db --remote --file=migrations/0005_saves.sql
```

### Android

Android Studio で開いてビルドするか、コマンドラインから:

```bash
./gradlew assembleDebug
```

生成物は **`app/build/outputs/apk/debug/app-debug.apk`**。
`app/build/intermediates/apk/debug/` にも同名のファイルがあるが、こちらは更新されないので使わないこと。

デバッグビルドは test-only フラグが付くため、インストールには `-t` が必要:

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

---

## 機能

### 認証
Google Sign-In でログインし、Firebase の ID トークンを Workers 側で検証する。
初回ログイン時にメールアドレスのローカル部からユーザー名を自動生成して D1 にレコードを作成する。

`MainActivity.onCreate` で起動時に必ず `/auth/login` を呼んでいる。
Firebase のセッション復元時にも D1 側を同期する必要があるため、この呼び出しは省略できない。

### タイムライン
3 つのフィードをタブで切り替える。

| タブ | 取得条件 |
|---|---|
| おすすめ | 直近 7 日間の投稿をリアクション数順 |
| フォロー中 | フォローしているユーザーの投稿を新着順 |
| 最新 | 全体を新着順 |

### 投稿
本文・投稿タイプ（今日の進捗／達成報告／気づき／相談）に加えて、
写真・目標・進捗・タグを任意で添付できる。
目標と進捗を同時に指定すると、目標側の進捗もその値に更新される。

### 目標
作成、進捗の増減（±10%）、達成/取り消しに対応。
「目標」タブに挑戦中のもの、「達成記録」タブに達成済みのものを表示する。

### ソーシャル
いいね、コメント、フォロー、ブックマーク（保存）、通知。
通知はリアクション・コメント・フォローの 3 種類が自動生成される。

### 検索・発見
投稿本文とタグ、ユーザー名・表示名を横断検索する。
検索していないときは人気のタグ（直近 200 投稿から集計）と、
まだフォローしていないおすすめユーザーを表示する。

### 画像
端末側で長辺 1600px（アバターは 512px）に縮小し、EXIF の回転を適用して JPEG に統一してから
アップロードする。HEIC などサーバーが受け付けない形式への対策と転送量削減を兼ねている。

---

## 実装状況

### 実装済み

- Google Sign-In / ログアウト
- タイムライン 3 種（おすすめ・フォロー中・最新）
- 投稿の作成（本文・タイプ・写真・目標・進捗・タグ）
- 投稿詳細画面（本文全文＋コメント）
- いいね・コメント（追加/削除）・ブックマーク
- フォロー / フォロワー
- 通知一覧・全件既読
- 目標の作成・進捗更新・達成
- プロフィール表示・編集（表示名・自己紹介・アバター）
- 他ユーザーのプロフィール
- 検索（投稿・ユーザー）、人気タグ、おすすめユーザー
- 設定画面（アカウント情報・バージョン・ログアウト）

### 未実装

- 投稿の編集・削除
- 通知の未読バッジ（`isRead` は API にあるが UI 未使用）
- タイムラインの追加読み込み（`nextCursor` を API は返すが UI が未使用）
- 他ユーザーの目標一覧（件数のみ表示）
- リリースビルドの設定（署名・ProGuard・アイコン差し替え）
- 自動テスト（ひな形のみ）

---

## 既知の制約・注意点

### Workers KV を画像ストレージに使っている
本来 R2 が適任だが、R2 の利用開始には利用規約への同意と支払い方法への課金契約を伴う
サブスクリプション追加が必要なため見送り、追加契約なしで使える KV を選んだ。

そのため以下の制約がある。

- 書き込みの反映に最大 60 秒かかることがあり、投稿直後に画像が表示されない場合がある
- 無料枠は 1 日 1000 書き込み・10 万読み込み、ストレージ 1GB
- 1 値あたり 25MiB（アップロードは 5MB で制限済み）

R2 を有効化する場合、`api/src/routes/uploads.ts` の `put` / `getWithMetadata` と
`wrangler.jsonc` のバインディングを差し替えるだけで移行できる。

### デプロイ直後は新エンドポイントが 404 を返すことがある
Cloudflare のエッジで新旧バージョンが数分間混在するため、
追加したばかりのエンドポイントが 404 と正常応答を交互に返すことがある。時間が経てば揃う。

### エミュレータのブラックアウト
アプリが起動しても画面が真っ黒になることがある。クラッシュログは出ず、
UI ツリー上は要素が存在してタップも効くが描画だけされない。
スプラッシュからアプリ本体へのウィンドウ遷移がエミュレータ側で固まっている状態で、
**エミュレータを再起動すれば解消する**。アプリ側の不具合ではない。

### ソースコードの一括置換に PowerShell を使わない
`Set-Content -Encoding utf8` はファイルのエンコーディングを壊し、
日本語がすべて文字化けする。置換はエディタか編集ツールで行うこと。

---

## ドキュメント

- [API リファレンス](docs/api-reference.md)
- [データベース設計](docs/database.md)
