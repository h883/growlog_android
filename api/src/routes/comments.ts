import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';
import { deferredFlag } from '../focus-state';

const comments = new Hono<AppContext>();

comments.get('/:postId/comments', authMiddleware, async (c) => {
  const postId = c.req.param('postId');

  const result = await c.env.DB.prepare(
    `SELECT c.comment_id, c.user_id, c.content, c.created_at,
            u.display_name, u.user_name, u.profile_image_key
     FROM comments c
     JOIN users u ON c.user_id = u.user_id
     WHERE c.post_id = ?
     ORDER BY c.created_at ASC
     LIMIT 100`
  ).bind(postId).all();

  return c.json({
    comments: (result.results as any[]).map((r) => ({
      commentId: r.comment_id,
      userId: r.user_id,
      displayName: r.display_name,
      userName: r.user_name,
      userImageUrl: imageUrlFor(c.req.url, r.profile_image_key),
      content: r.content,
      createdAt: r.created_at,
    })),
  });
});

comments.post('/:postId/comments', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const postId = c.req.param('postId');
  const body = await c.req.json<{ content: string }>();

  if (!body.content?.trim()) {
    return c.json({ error: 'Content is required' }, 400);
  }

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const post = await c.env.DB.prepare(
    'SELECT user_id FROM posts WHERE post_id = ?'
  ).bind(postId).first<{ user_id: string }>();
  if (!post) return c.json({ error: 'Post not found' }, 404);

  const commentId = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO comments (comment_id, post_id, user_id, content, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).bind(commentId, postId, me.user_id, body.content.trim(), now, now).run();

  await c.env.DB.prepare(
    'UPDATE posts SET comment_count = comment_count + 1, updated_at = ? WHERE post_id = ?'
  ).bind(now, postId).run();

  if (post.user_id !== me.user_id) {
    const notifId = crypto.randomUUID();
    await c.env.DB.prepare(
      `INSERT INTO notifications (notification_id, user_id, type, actor_id, post_id, comment_id, is_deferred, created_at)
       VALUES (?, ?, 'comment', ?, ?, ?, ?, ?)`
    ).bind(notifId, post.user_id, me.user_id, postId, commentId,
      await deferredFlag(c, post.user_id), now).run();
  }

  return c.json({ commentId, createdAt: now }, 201);
});

comments.delete('/:postId/comments/:commentId', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const postId = c.req.param('postId');
  const commentId = c.req.param('commentId');
  const now = new Date().toISOString();

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const { meta } = await c.env.DB.prepare(
    'DELETE FROM comments WHERE comment_id = ? AND post_id = ? AND user_id = ?'
  ).bind(commentId, postId, me.user_id).run();

  if (meta.changes > 0) {
    await c.env.DB.prepare(
      'UPDATE posts SET comment_count = MAX(0, comment_count - 1), updated_at = ? WHERE post_id = ?'
    ).bind(now, postId).run();
  }

  return c.json({ ok: true });
});

export default comments;
