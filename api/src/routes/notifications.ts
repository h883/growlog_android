import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';

const notifications = new Hono<AppContext>();

notifications.get('/', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const result = await c.env.DB.prepare(
    `SELECT n.notification_id, n.type, n.post_id, n.comment_id, n.group_id,
            n.group_join_request_id, n.group_invitation_id, n.is_read, n.created_at,
            a.display_name as actor_display_name, a.user_name as actor_user_name,
            a.profile_image_key as actor_image_key,
            p.content as post_content,
            cm.content as comment_content,
            g.group_name as group_name
     FROM notifications n
     JOIN users a ON n.actor_id = a.user_id
     LEFT JOIN posts p ON n.post_id = p.post_id
     LEFT JOIN comments cm ON n.comment_id = cm.comment_id
     LEFT JOIN groups g ON n.group_id = g.group_id
     WHERE n.user_id = ? AND n.is_deferred = 0
     ORDER BY n.created_at DESC
     LIMIT 50`
  ).bind(me.user_id).all();

  return c.json({
    notifications: (result.results as any[]).map((r) => ({
      notificationId: r.notification_id,
      type: r.type,
      actorDisplayName: r.actor_display_name,
      actorUserName: r.actor_user_name,
      actorImageUrl: imageUrlFor(c.req.url, r.actor_image_key),
      postId: r.post_id ?? null,
      postContent: r.post_content ? (r.post_content as string).slice(0, 50) : null,
      commentContent: r.comment_content ?? null,
      groupId: r.group_id ?? null,
      groupName: r.group_name ?? null,
      groupJoinRequestId: r.group_join_request_id ?? null,
      groupInvitationId: r.group_invitation_id ?? null,
      isRead: r.is_read === 1,
      createdAt: r.created_at,
    })),
  });
});

notifications.post('/read-all', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  await c.env.DB.prepare(
    'UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0'
  ).bind(me.user_id).run();

  return c.json({ ok: true });
});

export default notifications;
