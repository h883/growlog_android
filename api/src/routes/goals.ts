import { Hono } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';

const goals = new Hono<AppContext>();

goals.get('/my', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const result = await c.env.DB.prepare(
    `SELECT goal_id, title, description, progress, start_date, end_date, is_achieved, activity_count, created_at
     FROM goals WHERE user_id = ? ORDER BY created_at DESC`
  ).bind(me.user_id).all();

  return c.json({
    goals: (result.results as any[]).map((g) => ({
      goalId: g.goal_id,
      title: g.title,
      description: g.description,
      progress: g.progress,
      startDate: g.start_date,
      endDate: g.end_date ?? null,
      isAchieved: g.is_achieved === 1,
      activityCount: g.activity_count,
    })),
  });
});

goals.post('/', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const body = await c.req.json<{
    title: string;
    description?: string;
    startDate: string;
    endDate?: string;
  }>();

  if (!body.title?.trim()) {
    return c.json({ error: 'Title is required' }, 400);
  }

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const goalId = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO goals (goal_id, user_id, title, description, progress, start_date, end_date, created_at, updated_at)
     VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?)`
  ).bind(goalId, me.user_id, body.title.trim(), body.description?.trim() ?? '',
    body.startDate, body.endDate ?? null, now, now).run();

  return c.json({ goalId }, 201);
});

goals.patch('/:goalId', authMiddleware, async (c) => {
  const firebaseUser = c.get('firebaseUser');
  const goalId = c.req.param('goalId');
  const body = await c.req.json<{
    progress?: number;
    title?: string;
    description?: string;
    isAchieved?: boolean;
  }>();
  const now = new Date().toISOString();

  const me = await c.env.DB.prepare(
    'SELECT user_id FROM users WHERE firebase_uid = ?'
  ).bind(firebaseUser.uid).first<{ user_id: string }>();
  if (!me) return c.json({ error: 'User not found' }, 404);

  const progress = body.progress === undefined
    ? null
    : Math.max(0, Math.min(100, Math.round(body.progress)));
  const isAchieved = body.isAchieved === undefined ? null : (body.isAchieved ? 1 : 0);

  // achieved_at は「達成にした瞬間だけ記録し、取り消したら消す」。
  // isAchieved が未指定(null)のときは既存値をそのまま保つ
  const { meta } = await c.env.DB.prepare(
    `UPDATE goals SET
       title = COALESCE(?, title),
       description = COALESCE(?, description),
       progress = COALESCE(?, progress),
       is_achieved = COALESCE(?, is_achieved),
       achieved_at = CASE
           WHEN ? IS NULL THEN achieved_at
           WHEN ? = 1 THEN ?
           ELSE NULL
         END,
       updated_at = ?
     WHERE goal_id = ? AND user_id = ?`
  ).bind(
    body.title ?? null, body.description ?? null, progress,
    isAchieved, isAchieved, isAchieved, now,
    now, goalId, me.user_id
  ).run();

  if (meta.changes === 0) return c.json({ error: 'Goal not found' }, 404);

  return c.json({ ok: true });
});

export default goals;
