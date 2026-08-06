import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';
import { POST_COLUMNS, POST_FROM, OUTSIDE_GROUP_CONDITION, mapPostRow, viewerBinds } from '../posts-query';
import { fetchPresence } from '../focus-presence';

const users = new Hono<AppContext>();

users.get('/me', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');

  const user = await c.env.DB.prepare(
    `SELECT user_id, display_name, user_name, biography, profile_image_key, privacy_type, created_at
     FROM users WHERE firebase_uid = ?`
  )
    .bind(firebaseUser.uid)
    .first<any>();

  if (!user) {
    return c.json({ error: 'User not found' }, 404);
  }

  const [followerRow, followingRow, presence] = await Promise.all([
    c.env.DB.prepare('SELECT COUNT(*) as cnt FROM follows WHERE following_id = ?')
      .bind(user.user_id).first<{ cnt: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as cnt FROM follows WHERE follower_id = ?')
      .bind(user.user_id).first<{ cnt: number }>(),
    fetchPresence(c.env.DB, user.user_id, user.user_id),
  ]);

  return c.json({
    ...user,
    profileImageUrl: imageUrlFor(c.req.url, user.profile_image_key),
    followerCount: followerRow?.cnt ?? 0,
    followingCount: followingRow?.cnt ?? 0,
    focus: presence,
  });
});

users.get('/me/posts', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const result = await c.env.DB.prepare(
    `SELECT ${POST_COLUMNS} ${POST_FROM}
     WHERE p.user_id = ? AND ${OUTSIDE_GROUP_CONDITION}
     ORDER BY p.created_at DESC LIMIT 50`
  ).bind(...viewerBinds(me.user_id), me.user_id).all();

  return c.json({
    posts: (result.results as any[]).map((r) => mapPostRow(c.req.url, r)),
  });
});

users.get('/me/saves', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  // 保存した順に並べたいので saves を起点に JOIN する
  const result = await c.env.DB.prepare(
    `SELECT ${POST_COLUMNS} ${POST_FROM}
     JOIN saves ms ON ms.post_id = p.post_id AND ms.user_id = ?
     ORDER BY ms.created_at DESC LIMIT 50`
  ).bind(...viewerBinds(me.user_id), me.user_id).all();

  return c.json({
    posts: (result.results as any[]).map((r) => mapPostRow(c.req.url, r)),
  });
});

users.patch('/me', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const body = await c.req.json<{
    displayName?: string;
    biography?: string;
    profileImageKey?: string;
  }>();
  const now = new Date().toISOString();

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  // 自分がアップロードしたアバターのキーのみ設定できる
  if (body.profileImageKey && !body.profileImageKey.startsWith(`avatars/${me.user_id}/`)) {
    return c.json({ error: 'Invalid profileImageKey' }, 400);
  }

  await c.env.DB.prepare(
    `UPDATE users SET display_name = COALESCE(?, display_name),
                      biography = COALESCE(?, biography),
                      profile_image_key = COALESCE(?, profile_image_key),
                      updated_at = ?
     WHERE firebase_uid = ?`
  )
    .bind(
      body.displayName ?? null,
      body.biography ?? null,
      body.profileImageKey ?? null,
      now,
      firebaseUser.uid
    )
    .run();

  return c.json({ ok: true });
});

export default users;
