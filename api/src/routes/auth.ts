import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';

const auth = new Hono<AppContext>();

auth.post('/login', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const now = new Date().toISOString();

  const existing = await c.env.DB.prepare(
    'SELECT user_id, user_name, display_name FROM users WHERE firebase_uid = ?'
  )
    .bind(firebaseUser.uid)
    .first<{ user_id: string; user_name: string; display_name: string }>();

  if (existing) {
    await c.env.DB.prepare(
      'UPDATE users SET last_login_at = ? WHERE firebase_uid = ?'
    )
      .bind(now, firebaseUser.uid)
      .run();

    return c.json({
      userId: existing.user_id,
      userName: existing.user_name,
      displayName: existing.display_name,
      isNewUser: false,
    });
  }

  const userId = crypto.randomUUID();
  let baseUserName = (
    firebaseUser.email?.split('@')[0] ?? `user${userId.slice(0, 8)}`
  ).replace(/[^a-zA-Z0-9_]/g, '_').slice(0, 20);

  const conflict = await c.env.DB.prepare(
    'SELECT 1 FROM users WHERE user_name = ?'
  )
    .bind(baseUserName)
    .first();
  if (conflict) {
    baseUserName = `${baseUserName.slice(0, 15)}_${Math.random().toString(36).slice(2, 6)}`;
  }

  const displayName = firebaseUser.name ?? baseUserName;

  await c.env.DB.prepare(
    `INSERT INTO users
       (user_id, firebase_uid, email, display_name, user_name, created_at, updated_at, last_login_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(userId, firebaseUser.uid, firebaseUser.email ?? null, displayName, baseUserName, now, now, now)
    .run();

  return c.json(
    { userId, userName: baseUserName, displayName, isNewUser: true },
    201
  );
});

export default auth;
