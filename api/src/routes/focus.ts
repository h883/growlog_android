import { Hono, Context } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';

const focus = new Hono<AppContext>();

const REACTION_TYPES = ['cheer', 'together', 'talk_later'];

async function requireMe(c: Context<AppContext>) {
  const firebaseUser = c.get('firebaseUser');
  return c.env.DB.prepare('SELECT user_id FROM users WHERE firebase_uid = ?')
    .bind(firebaseUser.uid)
    .first<{ user_id: string }>();
}

/**
 * 集中セッション1件を返す形に整える。
 * 経過時間はサーバー側で確定させ、端末の時計に依存させない。
 */
function mapSession(requestUrl: string, r: any, nowMs: number) {
  const startedMs = Date.parse(r.started_at);
  const pausedSeconds = r.paused_seconds ?? 0;

  // 休憩中は、休憩に入った時点で経過時間を止める
  const referenceMs = r.status === 'paused' && r.paused_at
    ? Date.parse(r.paused_at)
    : r.completed_at ? Date.parse(r.completed_at) : nowMs;

  const elapsed = Math.max(0, Math.floor((referenceMs - startedMs) / 1000) - pausedSeconds);

  return {
    focusSessionId: r.focus_session_id,
    userId: r.user_id,
    displayName: r.display_name,
    userName: r.user_name,
    profileImageUrl: imageUrlFor(requestUrl, r.profile_image_key),
    activityTitle: r.activity_title,
    goalTitle: r.goal_title ?? null,
    groupId: r.group_id ?? null,
    status: r.status,
    visibilityType: r.visibility_type,
    focusMode: r.focus_mode,
    plannedDurationSeconds: r.planned_duration_seconds ?? null,
    actualDurationSeconds: r.actual_duration_seconds ?? null,
    elapsedSeconds: elapsed,
    breakCount: r.break_count ?? 0,
    startedAt: r.started_at,
    completedAt: r.completed_at ?? null,
    reflection: r.reflection ?? null,
    concentrationRating: r.concentration_rating ?? null,
    cheerCount: r.cheer_count ?? 0,
    myReactions: (r.my_reactions as string | null)?.split(',').filter(Boolean) ?? [],
  };
}

const SESSION_SELECT = `
  SELECT f.*, u.display_name, u.user_name, u.profile_image_key,
         g.title as goal_title,
         (SELECT COUNT(*) FROM focus_reactions fr WHERE fr.focus_session_id = f.focus_session_id) as cheer_count,
         (SELECT GROUP_CONCAT(fr2.reaction_type) FROM focus_reactions fr2
           WHERE fr2.focus_session_id = f.focus_session_id AND fr2.user_id = ?) as my_reactions
  FROM focus_sessions f
  JOIN users u ON u.user_id = f.user_id
  LEFT JOIN goals g ON g.goal_id = f.goal_id`;

/** 集中セッションを開始する */
focus.post('/', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const body = await c.req.json<{
    activityTitle: string;
    goalId?: string;
    groupId?: string;
    plannedDurationSeconds?: number;
    visibilityType?: string;
    focusMode?: string;
  }>();

  const activityTitle = body.activityTitle?.trim();
  if (!activityTitle) return c.json({ error: '作業内容を入力してください' }, 400);

  // 同時に複数走らせない。既存があればそれを返す
  const existing = await c.env.DB.prepare(
    `SELECT focus_session_id FROM focus_sessions
     WHERE user_id = ? AND status IN ('active', 'paused')`
  ).bind(me.user_id).first<{ focus_session_id: string }>();
  if (existing) {
    return c.json({ error: '既に集中セッションが進行中です', focusSessionId: existing.focus_session_id }, 409);
  }

  const visibility = ['all', 'followers', 'group', 'private'].includes(body.visibilityType ?? '')
    ? body.visibilityType!
    : 'followers';
  const mode = ['light', 'focus', 'serious'].includes(body.focusMode ?? '')
    ? body.focusMode!
    : 'light';

  const sessionId = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO focus_sessions
       (focus_session_id, user_id, goal_id, group_id, activity_title, status,
        visibility_type, focus_mode, planned_duration_seconds, started_at, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?)`
  ).bind(sessionId, me.user_id, body.goalId ?? null, body.groupId ?? null, activityTitle,
    visibility, mode, body.plannedDurationSeconds ?? null, now, now, now).run();

  return c.json({ focusSessionId: sessionId, startedAt: now }, 201);
});

/**
 * いま集中している人の一覧。自分のセッションは公開範囲に関係なく含める。
 * 他人は、全体公開／フォロー中／同じグループ のいずれかを満たすものだけ。
 */
focus.get('/active', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const result = await c.env.DB.prepare(
    `${SESSION_SELECT}
     WHERE f.status IN ('active', 'paused')
       AND (
         f.user_id = ?
         OR f.visibility_type = 'all'
         OR (f.visibility_type = 'followers'
             AND EXISTS (SELECT 1 FROM follows fo
                          WHERE fo.follower_id = ? AND fo.following_id = f.user_id))
         OR (f.visibility_type = 'group' AND f.group_id IS NOT NULL
             AND EXISTS (SELECT 1 FROM group_members gm
                          WHERE gm.group_id = f.group_id AND gm.user_id = ?
                            AND gm.member_status = 'active'))
       )
     ORDER BY f.started_at ASC
     LIMIT 50`
  ).bind(me.user_id, me.user_id, me.user_id, me.user_id).all();

  const now = Date.now();
  return c.json({
    sessions: (result.results as any[]).map((r) => mapSession(c.req.url, r, now)),
  });
});

focus.get('/:sessionId', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const row = await c.env.DB.prepare(
    `${SESSION_SELECT} WHERE f.focus_session_id = ?`
  ).bind(me.user_id, c.req.param('sessionId') ?? '').first<any>();
  if (!row) return c.json({ error: 'Session not found' }, 404);

  // 自分のみ公開のセッションは本人にしか返さない
  if (row.visibility_type === 'private' && row.user_id !== me.user_id) {
    return c.json({ error: 'Session not found' }, 404);
  }

  return c.json(mapSession(c.req.url, row, Date.now()));
});

/** 本人のセッションを取得し、他人からの操作を弾く */
async function loadOwnSession(c: Context<AppContext>, userId: string) {
  const row = await c.env.DB.prepare(
    'SELECT * FROM focus_sessions WHERE focus_session_id = ?'
  ).bind(c.req.param('sessionId') ?? '').first<any>();
  if (!row) return null;
  if (row.user_id !== userId) return null;
  return row;
}

focus.post('/:sessionId/pause', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const session = await loadOwnSession(c, me.user_id);
  if (!session) return c.json({ error: 'Session not found' }, 404);
  if (session.status !== 'active') return c.json({ error: '集中中ではありません' }, 400);

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE focus_sessions SET status = 'paused', paused_at = ?,
            break_count = break_count + 1, updated_at = ?
     WHERE focus_session_id = ?`
  ).bind(now, now, session.focus_session_id).run();

  return c.json({ status: 'paused' });
});

focus.post('/:sessionId/resume', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const session = await loadOwnSession(c, me.user_id);
  if (!session) return c.json({ error: 'Session not found' }, 404);
  if (session.status !== 'paused') return c.json({ error: '休憩中ではありません' }, 400);

  // 休憩していた分を累計に足し、経過時間から除外する
  const pausedFor = session.paused_at
    ? Math.max(0, Math.floor((Date.now() - Date.parse(session.paused_at)) / 1000))
    : 0;
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `UPDATE focus_sessions SET status = 'active', paused_at = NULL,
            paused_seconds = paused_seconds + ?, updated_at = ?
     WHERE focus_session_id = ?`
  ).bind(pausedFor, now, session.focus_session_id).run();

  return c.json({ status: 'active' });
});

/** 保留していた通知を、セッション終了時にまとめて表示状態へ戻す */
async function releaseDeferredNotifications(c: Context<AppContext>, userId: string) {
  await c.env.DB.prepare(
    'UPDATE notifications SET is_deferred = 0 WHERE user_id = ? AND is_deferred = 1'
  ).bind(userId).run();
}

async function finishSession(
  c: Context<AppContext>,
  status: 'completed' | 'cancelled'
) {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const session = await loadOwnSession(c, me.user_id);
  if (!session) return c.json({ error: 'Session not found' }, 404);
  if (session.status === 'completed' || session.status === 'cancelled') {
    return c.json({ error: '既に終了しています' }, 400);
  }

  const body = await c.req.json<{
    reflection?: string;
    concentrationRating?: number;
    endReason?: string;
  }>().catch(() => ({} as any));

  const nowMs = Date.now();
  const referenceMs = session.status === 'paused' && session.paused_at
    ? Date.parse(session.paused_at)
    : nowMs;
  const actual = Math.max(
    0,
    Math.floor((referenceMs - Date.parse(session.started_at)) / 1000) - (session.paused_seconds ?? 0)
  );
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `UPDATE focus_sessions SET status = ?, completed_at = ?, actual_duration_seconds = ?,
            reflection = COALESCE(?, reflection),
            concentration_rating = COALESCE(?, concentration_rating),
            end_reason = COALESCE(?, end_reason),
            updated_at = ?
     WHERE focus_session_id = ?`
  ).bind(status, now, actual, body.reflection?.trim() ?? null,
    body.concentrationRating ?? null, body.endReason ?? null, now,
    session.focus_session_id).run();

  await releaseDeferredNotifications(c, me.user_id);

  return c.json({
    status,
    actualDurationSeconds: actual,
    breakCount: session.break_count ?? 0,
    activityTitle: session.activity_title,
  });
}

focus.post('/:sessionId/complete', authMiddleware, (c) => finishSession(c, 'completed'));
focus.post('/:sessionId/cancel', authMiddleware, (c) => finishSession(c, 'cancelled'));

/** 集中中に送れるのはリアクションだけ（仕様書 6） */
focus.post('/:sessionId/reactions', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const body = await c.req.json<{ reactionType?: string }>().catch(() => ({} as any));
  const type = REACTION_TYPES.includes(body.reactionType ?? '') ? body.reactionType! : 'cheer';

  const sessionId = c.req.param('sessionId') ?? '';
  const session = await c.env.DB.prepare(
    'SELECT user_id, status FROM focus_sessions WHERE focus_session_id = ?'
  ).bind(sessionId).first<{ user_id: string; status: string }>();
  if (!session) return c.json({ error: 'Session not found' }, 404);
  if (session.user_id === me.user_id) {
    return c.json({ error: '自分のセッションには送れません' }, 400);
  }

  const existing = await c.env.DB.prepare(
    'SELECT focus_reaction_id FROM focus_reactions WHERE focus_session_id = ? AND user_id = ? AND reaction_type = ?'
  ).bind(sessionId, me.user_id, type).first();

  const now = new Date().toISOString();
  if (existing) {
    await c.env.DB.prepare(
      'DELETE FROM focus_reactions WHERE focus_session_id = ? AND user_id = ? AND reaction_type = ?'
    ).bind(sessionId, me.user_id, type).run();
  } else {
    await c.env.DB.prepare(
      `INSERT INTO focus_reactions (focus_reaction_id, focus_session_id, user_id, reaction_type, created_at)
       VALUES (?, ?, ?, ?, ?)`
    ).bind(crypto.randomUUID(), sessionId, me.user_id, type, now).run();
  }

  const count = await c.env.DB.prepare(
    'SELECT COUNT(*) as cnt FROM focus_reactions WHERE focus_session_id = ?'
  ).bind(sessionId).first<{ cnt: number }>();

  return c.json({ reacted: !existing, reactionType: type, cheerCount: count?.cnt ?? 0 });
});

export default focus;

/** グループの集中ルーム。メンバーのみ閲覧できる */
export const focusRoom = new Hono<AppContext>();

focusRoom.get('/:groupId/focus-room', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const groupId = c.req.param('groupId') ?? '';
  const group = await c.env.DB.prepare(
    `SELECT group_id FROM groups WHERE (group_id = ? OR group_slug = ?) AND status != 'deleted'`
  ).bind(groupId, groupId).first<{ group_id: string }>();
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const member = await c.env.DB.prepare(
    `SELECT 1 FROM group_members
     WHERE group_id = ? AND user_id = ? AND member_status = 'active'`
  ).bind(group.group_id, me.user_id).first();
  if (!member) return c.json({ error: 'メンバーのみ閲覧できます' }, 403);

  const todayStart = new Date();
  todayStart.setUTCHours(0, 0, 0, 0);

  const [activeRows, totalRow] = await Promise.all([
    c.env.DB.prepare(
      `${SESSION_SELECT}
       WHERE f.group_id = ? AND f.status IN ('active', 'paused')
       ORDER BY f.started_at ASC LIMIT 50`
    ).bind(me.user_id, group.group_id).all(),
    c.env.DB.prepare(
      `SELECT COALESCE(SUM(actual_duration_seconds), 0) as total
       FROM focus_sessions
       WHERE group_id = ? AND status = 'completed' AND completed_at >= ?`
    ).bind(group.group_id, todayStart.toISOString()).first<{ total: number }>(),
  ]);

  const now = Date.now();
  return c.json({
    activeSessions: (activeRows.results as any[]).map((r) => mapSession(c.req.url, r, now)),
    todayTotalSeconds: totalRow?.total ?? 0,
  });
});
