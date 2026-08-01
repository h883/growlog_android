import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';
import { deferredFlag } from '../focus-state';
import { POST_COLUMNS, POST_FROM, OUTSIDE_GROUP_CONDITION, mapPostRow } from '../posts-query';

const follows = new Hono<AppContext>();

follows.post('/:userName/follow', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const userName = c.req.param('userName');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const target = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE user_name = ?'
  ).bind(userName).first<{ user_id: string }>();
  if (!target) return c.json({ error: 'Target user not found' }, 404);

  if (target.user_id === me.user_id) {
    return c.json({ error: 'Cannot follow yourself' }, 400);
  }

  const existing = await c.env.DB.prepare(
    'SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?'
  ).bind(me.user_id, target.user_id).first();

  const now = new Date().toISOString();
  let following: boolean;

  if (existing) {
    await c.env.DB.prepare(
      'DELETE FROM follows WHERE follower_id = ? AND following_id = ?'
    ).bind(me.user_id, target.user_id).run();
    following = false;
  } else {
    await c.env.DB.prepare(
      'INSERT INTO follows (follower_id, following_id, created_at) VALUES (?, ?, ?)'
    ).bind(me.user_id, target.user_id, now).run();
    following = true;

    const notifId = crypto.randomUUID();
    await c.env.DB.prepare(
      `INSERT INTO notifications (notification_id, user_id, type, actor_id, is_deferred, created_at)
       VALUES (?, ?, 'follow', ?, ?, ?)`
    ).bind(notifId, target.user_id, me.user_id,
      await deferredFlag(c, target.user_id), now).run();
  }

  return c.json({ following });
});

follows.get('/:userName/profile', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const userName = c.req.param('userName');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const user = await c.env.DB.prepare(
    `SELECT user_id, display_name, user_name, biography, profile_image_key
     FROM users WHERE user_name = ?`
  ).bind(userName).first<any>();
  if (!user) return c.json({ error: 'User not found' }, 404);

  const [followerRow, followingRow, isFollowingRow, postCountRow, goalCountRow] = await Promise.all([
    c.env.DB.prepare('SELECT COUNT(*) as cnt FROM follows WHERE following_id = ?').bind(user.user_id).first<{ cnt: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as cnt FROM follows WHERE follower_id = ?').bind(user.user_id).first<{ cnt: number }>(),
    c.env.DB.prepare('SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?').bind(me.user_id, user.user_id).first(),
    c.env.DB.prepare('SELECT COUNT(*) as cnt FROM posts WHERE user_id = ?').bind(user.user_id).first<{ cnt: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as cnt FROM goals WHERE user_id = ?').bind(user.user_id).first<{ cnt: number }>(),
  ]);

  // 集中中なら、コメントやDMを送る側に「通知は終了後に届く」と伝えられるようにする
  const focusRow = await c.env.DB.prepare(
    `SELECT activity_title FROM focus_sessions
     WHERE user_id = ? AND status IN ('active', 'paused')`
  ).bind(user.user_id).first<{ activity_title: string }>();

  return c.json({
    userId: user.user_id,
    displayName: user.display_name,
    userName: user.user_name,
    biography: user.biography ?? '',
    profileImageUrl: imageUrlFor(c.req.url, user.profile_image_key),
    followerCount: followerRow?.cnt ?? 0,
    followingCount: followingRow?.cnt ?? 0,
    isFollowing: !!isFollowingRow,
    postCount: postCountRow?.cnt ?? 0,
    goalCount: goalCountRow?.cnt ?? 0,
    isFocusing: focusRow !== null,
    focusActivityTitle: focusRow?.activity_title ?? null,
  });
});

follows.get('/:userName/posts', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const userName = c.req.param('userName');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const target = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE user_name = ?'
  ).bind(userName).first<{ user_id: string }>();
  if (!target) return c.json({ error: 'User not found' }, 404);

  const result = await c.env.DB.prepare(
    `SELECT ${POST_COLUMNS} ${POST_FROM}
     WHERE p.user_id = ? AND ${OUTSIDE_GROUP_CONDITION}
     ORDER BY p.created_at DESC LIMIT 50`
  ).bind(me.user_id, me.user_id, target.user_id).all();

  return c.json({
    posts: (result.results as any[]).map((r) => mapPostRow(c.req.url, r)),
  });
});

export default follows;
