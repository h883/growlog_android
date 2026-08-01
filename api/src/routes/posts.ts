import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { POST_COLUMNS, POST_FROM, OUTSIDE_GROUP_CONDITION, mapPostRow } from '../posts-query';
import { deferredFlag } from '../focus-state';

const posts = new Hono<AppContext>();

posts.get('/', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const cursor = c.req.query('cursor');
  const feed = c.req.query('feed'); // 'following' | 'popular' | 未指定(=新着順)

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const params: string[] = [me.user_id, me.user_id];
  let from = POST_FROM;

  if (feed === 'following') {
    from += ' JOIN follows f ON f.following_id = p.user_id AND f.follower_id = ?';
    params.push(me.user_id);
  }

  // グループ限定の投稿は、外のタイムラインには出さない
  const conditions: string[] = [OUTSIDE_GROUP_CONDITION];

  // 人気順は直近1週間から拾う。古い投稿がいつまでも上に居座らないようにするため
  if (feed === 'popular') {
    conditions.push('p.created_at > ?');
    params.push(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString());
  }
  if (cursor) {
    conditions.push('p.created_at < ?');
    params.push(cursor);
  }

  const where = ` WHERE ${conditions.join(' AND ')}`;
  // 人気順は created_at で辿れないのでカーソルページングを行わない
  const orderBy = feed === 'popular'
    ? ' ORDER BY p.reaction_count DESC, p.created_at DESC'
    : ' ORDER BY p.created_at DESC';

  const query = `SELECT ${POST_COLUMNS} ${from}${where}${orderBy} LIMIT 21`;

  const result = await c.env.DB.prepare(query).bind(...params).all();
  const rows = result.results as any[];
  const hasMore = rows.length > 20;
  const items = rows.slice(0, 20);

  return c.json({
    posts: items.map((r) => mapPostRow(c.req.url, r)),
    nextCursor: hasMore && feed !== 'popular' ? items[items.length - 1].created_at : null,
  });
});

posts.get('/:postId', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const postId = c.req.param('postId');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const row = await c.env.DB.prepare(
    `SELECT ${POST_COLUMNS} ${POST_FROM} WHERE p.post_id = ?`
  ).bind(me.user_id, me.user_id, postId).first<any>();
  if (!row) return c.json({ error: 'Post not found' }, 404);

  return c.json(mapPostRow(c.req.url, row));
});

posts.post('/', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const body = await c.req.json<{
    content: string;
    postType?: string;
    goalId?: string;
    progress?: number;
    tags?: string[];
    imageKey?: string;
  }>();

  if (!body.content?.trim()) {
    return c.json({ error: 'Content is required' }, 400);
  }

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  // 自分がアップロードした画像のキーのみ添付できる
  if (body.imageKey && !body.imageKey.startsWith(`posts/${me.user_id}/`)) {
    return c.json({ error: 'Invalid imageKey' }, 400);
  }

  const postId = crypto.randomUUID();
  const now = new Date().toISOString();
  const tags = JSON.stringify(body.tags ?? []);
  const postType = body.postType ?? 'progress';

  await c.env.DB.prepare(
    `INSERT INTO posts (post_id, user_id, content, post_type, goal_id, progress, tags, image_key, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(postId, me.user_id, body.content.trim(), postType,
    body.goalId ?? null, body.progress ?? null, tags,
    body.imageKey ?? null, now, now).run();

  if (body.goalId) {
    // 目標つきの投稿で進捗を指定したときは、目標側の進捗もそこまで進める
    const progress = body.progress === undefined || body.progress === null
      ? null
      : Math.max(0, Math.min(100, Math.round(body.progress)));

    await c.env.DB.prepare(
      `UPDATE goals SET activity_count = activity_count + 1,
                        progress = COALESCE(?, progress),
                        updated_at = ?
       WHERE goal_id = ? AND user_id = ?`
    ).bind(progress, now, body.goalId, me.user_id).run();
  }

  return c.json({ postId, createdAt: now }, 201);
});

posts.post('/:postId/reactions', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const postId = c.req.param('postId');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const existing = await c.env.DB.prepare(
    'SELECT reaction_id FROM reactions WHERE post_id = ? AND user_id = ?'
  ).bind(postId, me.user_id).first<{ reaction_id: string }>();

  const now = new Date().toISOString();
  let reacted: boolean;

  if (existing) {
    await c.env.DB.prepare(
      'DELETE FROM reactions WHERE post_id = ? AND user_id = ?'
    ).bind(postId, me.user_id).run();
    await c.env.DB.prepare(
      'UPDATE posts SET reaction_count = MAX(0, reaction_count - 1), updated_at = ? WHERE post_id = ?'
    ).bind(now, postId).run();
    reacted = false;
  } else {
    const reactionId = crypto.randomUUID();
    await c.env.DB.prepare(
      'INSERT INTO reactions (reaction_id, post_id, user_id, reaction_type, created_at) VALUES (?, ?, ?, ?, ?)'
    ).bind(reactionId, postId, me.user_id, 'like', now).run();
    await c.env.DB.prepare(
      'UPDATE posts SET reaction_count = reaction_count + 1, updated_at = ? WHERE post_id = ?'
    ).bind(now, postId).run();
    reacted = true;

    // Notify post owner
    const postOwner = await c.env.DB.prepare(
      'SELECT user_id FROM posts WHERE post_id = ?'
    ).bind(postId).first<{ user_id: string }>();
    if (postOwner && postOwner.user_id !== me.user_id) {
      const notifId = crypto.randomUUID();
      await c.env.DB.prepare(
        `INSERT INTO notifications (notification_id, user_id, type, actor_id, post_id, is_deferred, created_at)
         VALUES (?, ?, 'reaction', ?, ?, ?, ?)`
      ).bind(notifId, postOwner.user_id, me.user_id, postId,
        await deferredFlag(c, postOwner.user_id), now).run();
    }
  }

  const post = await c.env.DB.prepare(
    'SELECT reaction_count FROM posts WHERE post_id = ?'
  ).bind(postId).first<{ reaction_count: number }>();

  return c.json({ reacted, reactionCount: post?.reaction_count ?? 0 });
});

posts.post('/:postId/save', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const postId = c.req.param('postId');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const post = await c.env.DB.prepare(
    'SELECT post_id FROM posts WHERE post_id = ?'
  ).bind(postId).first();
  if (!post) return c.json({ error: 'Post not found' }, 404);

  const existing = await c.env.DB.prepare(
    'SELECT 1 FROM saves WHERE user_id = ? AND post_id = ?'
  ).bind(me.user_id, postId).first();

  if (existing) {
    await c.env.DB.prepare(
      'DELETE FROM saves WHERE user_id = ? AND post_id = ?'
    ).bind(me.user_id, postId).run();
    return c.json({ saved: false });
  }

  await c.env.DB.prepare(
    'INSERT INTO saves (user_id, post_id, created_at) VALUES (?, ?, ?)'
  ).bind(me.user_id, postId, new Date().toISOString()).run();

  return c.json({ saved: true });
});

export default posts;
