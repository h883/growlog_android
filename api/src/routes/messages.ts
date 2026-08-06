import { Hono, Context } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';
import { presenceColumn, presenceBinds, mapPresence } from '../focus-presence';

const messages = new Hono<AppContext>();

async function requireMe(c: Context<AppContext>) {
  const firebaseUser = c.get('firebaseUser');
  return c.env.DB.prepare('SELECT user_id FROM users WHERE firebase_uid = ?')
    .bind(firebaseUser.uid)
    .first<{ user_id: string }>();
}

/** 会話の参加者であることを確認する。参加していなければ null */
async function loadConversation(c: Context<AppContext>, conversationId: string, meId: string) {
  const row = await c.env.DB.prepare(
    'SELECT conversation_id, user_a_id, user_b_id FROM conversations WHERE conversation_id = ?'
  ).bind(conversationId).first<{ conversation_id: string; user_a_id: string; user_b_id: string }>();

  if (!row) return null;
  if (row.user_a_id !== meId && row.user_b_id !== meId) return null;
  return row;
}

/** 会話一覧。相手の情報・最後のメッセージ・未読数をまとめて返す */
messages.get('/', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const result = await c.env.DB.prepare(
    `SELECT c.conversation_id, c.last_message, c.last_message_at,
            u.user_id, u.display_name, u.user_name, u.profile_image_key,
            ${presenceColumn('u')},
            (SELECT COUNT(*) FROM messages m
              WHERE m.conversation_id = c.conversation_id
                AND m.sender_id != ? AND m.is_read = 0) as unread_count
     FROM conversations c
     JOIN users u ON u.user_id = CASE WHEN c.user_a_id = ? THEN c.user_b_id ELSE c.user_a_id END
     WHERE c.user_a_id = ? OR c.user_b_id = ?
     ORDER BY COALESCE(c.last_message_at, c.created_at) DESC
     LIMIT 50`
  ).bind(...presenceBinds(me.user_id), me.user_id, me.user_id, me.user_id, me.user_id).all();

  const now = Date.now();
  return c.json({
    conversations: (result.results as any[]).map((r) => ({
      conversationId: r.conversation_id,
      partnerUserId: r.user_id,
      partnerDisplayName: r.display_name,
      partnerUserName: r.user_name,
      partnerImageUrl: imageUrlFor(c.req.url, r.profile_image_key),
      lastMessage: r.last_message ?? null,
      lastMessageAt: r.last_message_at ?? null,
      unreadCount: r.unread_count ?? 0,
      partnerFocus: mapPresence(r.focus_presence, now),
    })),
  });
});

/** 未読の合計。ヘッダーのバッジ用 */
messages.get('/unread-count', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const row = await c.env.DB.prepare(
    `SELECT COUNT(*) as cnt FROM messages m
     JOIN conversations c ON c.conversation_id = m.conversation_id
     WHERE (c.user_a_id = ? OR c.user_b_id = ?)
       AND m.sender_id != ? AND m.is_read = 0`
  ).bind(me.user_id, me.user_id, me.user_id).first<{ cnt: number }>();

  return c.json({ unreadCount: row?.cnt ?? 0 });
});

/** 相手を指定して会話を開く。無ければ作る */
messages.post('/', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const body = await c.req.json<{ userName: string }>();
  if (!body.userName?.trim()) return c.json({ error: 'userName is required' }, 400);

  const partner = await c.env.DB.prepare(
    'SELECT user_id, display_name, user_name, profile_image_key FROM users WHERE user_name = ?'
  ).bind(body.userName.trim()).first<any>();
  if (!partner) return c.json({ error: 'User not found' }, 404);
  if (partner.user_id === me.user_id) {
    return c.json({ error: 'Cannot message yourself' }, 400);
  }

  // 並び順を固定しておくと UNIQUE 制約だけで重複を防げる
  const [a, b] = [me.user_id, partner.user_id].sort();

  const existing = await c.env.DB.prepare(
    'SELECT conversation_id FROM conversations WHERE user_a_id = ? AND user_b_id = ?'
  ).bind(a, b).first<{ conversation_id: string }>();

  if (existing) {
    return c.json({ conversationId: existing.conversation_id });
  }

  const conversationId = crypto.randomUUID();
  const now = new Date().toISOString();
  await c.env.DB.prepare(
    'INSERT INTO conversations (conversation_id, user_a_id, user_b_id, created_at) VALUES (?, ?, ?, ?)'
  ).bind(conversationId, a, b, now).run();

  return c.json({ conversationId }, 201);
});

/** 会話のメッセージ。開いた時点で相手からの未読を既読にする */
messages.get('/:conversationId/messages', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const conversationId = c.req.param('conversationId') ?? '';
  const conversation = await loadConversation(c, conversationId, me.user_id);
  if (!conversation) return c.json({ error: 'Conversation not found' }, 404);

  const partnerId = conversation.user_a_id === me.user_id
    ? conversation.user_b_id
    : conversation.user_a_id;

  const [partner, rows] = await Promise.all([
    c.env.DB.prepare(
      'SELECT user_id, display_name, user_name, profile_image_key FROM users WHERE user_id = ?'
    ).bind(partnerId).first<any>(),
    c.env.DB.prepare(
      `SELECT message_id, sender_id, content, is_read, created_at
       FROM messages WHERE conversation_id = ?
       ORDER BY created_at ASC LIMIT 200`
    ).bind(conversationId).all(),
  ]);

  await c.env.DB.prepare(
    'UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND sender_id != ? AND is_read = 0'
  ).bind(conversationId, me.user_id).run();

  return c.json({
    conversationId,
    partner: partner ? {
      userId: partner.user_id,
      displayName: partner.display_name,
      userName: partner.user_name,
      profileImageUrl: imageUrlFor(c.req.url, partner.profile_image_key),
    } : null,
    messages: (rows.results as any[]).map((r) => ({
      messageId: r.message_id,
      senderId: r.sender_id,
      content: r.content,
      isMine: r.sender_id === me.user_id,
      isRead: r.is_read === 1,
      createdAt: r.created_at,
    })),
  });
});

messages.post('/:conversationId/messages', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const conversationId = c.req.param('conversationId') ?? '';
  const conversation = await loadConversation(c, conversationId, me.user_id);
  if (!conversation) return c.json({ error: 'Conversation not found' }, 404);

  const body = await c.req.json<{ content: string }>();
  const content = body.content?.trim();
  if (!content) return c.json({ error: 'Content is required' }, 400);

  const messageId = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO messages (message_id, conversation_id, sender_id, content, created_at)
     VALUES (?, ?, ?, ?, ?)`
  ).bind(messageId, conversationId, me.user_id, content, now).run();

  // 一覧で最後のメッセージを出すため、会話側にも写しておく
  await c.env.DB.prepare(
    'UPDATE conversations SET last_message = ?, last_message_at = ? WHERE conversation_id = ?'
  ).bind(content.slice(0, 100), now, conversationId).run();

  return c.json({ messageId, createdAt: now }, 201);
});

export default messages;
