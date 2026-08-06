/**
 * 「いま集中中」を、集中画面の外（タイムライン・プロフィール・DM一覧など）でも
 * 出せるようにするための共通部品。
 *
 * 公開範囲の判定はここ1か所に集約する。呼び出し側で書き分けると、
 * private のセッションが漏れる事故が起きやすいため。
 */

import { gardenStage, normalizeTheme } from './garden';

/** presenceColumn が閲覧者の user_id を何回バインドするか */
export const PRESENCE_BIND_COUNT = 3;

/**
 * SELECT のリストに差し込む相関サブクエリ。`focus_presence` 列を返す。
 * [userAlias] は users テーブルの別名（例: 'u'）。
 *
 * 閲覧者の user_id を PRESENCE_BIND_COUNT 個ぶん消費する。
 * SELECT は FROM より前に評価されるので、バインドは必ず先頭に並べること。
 */
export function presenceColumn(userAlias: string): string {
  return `
  (SELECT json_object(
            'focusSessionId', fp.focus_session_id,
            'status', fp.status,
            'activityTitle', fp.activity_title,
            'startedAt', fp.started_at,
            'pausedAt', fp.paused_at,
            'pausedSeconds', COALESCE(fp.paused_seconds, 0),
            'gardenTheme', fp.garden_theme,
            'plannedDurationSeconds', fp.planned_duration_seconds
          )
     FROM focus_sessions fp
    WHERE fp.user_id = ${userAlias}.user_id
      AND fp.status IN ('active', 'paused')
      AND (
        fp.user_id = ?
        OR fp.visibility_type = 'all'
        OR (fp.visibility_type = 'followers'
            AND EXISTS (SELECT 1 FROM follows fo
                         WHERE fo.follower_id = ? AND fo.following_id = fp.user_id))
        OR (fp.visibility_type = 'group' AND fp.group_id IS NOT NULL
            AND EXISTS (SELECT 1 FROM group_members gm
                         WHERE gm.group_id = fp.group_id AND gm.user_id = ?
                           AND gm.member_status = 'active'))
      )
    ORDER BY fp.started_at DESC
    LIMIT 1) as focus_presence`;
}

/** presenceColumn ぶんのバインド値 */
export function presenceBinds(viewerId: string): string[] {
  return Array(PRESENCE_BIND_COUNT).fill(viewerId);
}

/**
 * focus_presence 列を、クライアントに返す形へ変換する。
 * 経過秒数はサーバーで確定させ、端末の時計に依存させない（focus.ts と同じ規則）。
 */
export function mapPresence(raw: unknown, nowMs: number) {
  if (!raw) return null;

  let p: any;
  try {
    p = typeof raw === 'string' ? JSON.parse(raw) : raw;
  } catch {
    return null;
  }
  if (!p?.startedAt) return null;

  const startedMs = Date.parse(p.startedAt);
  if (Number.isNaN(startedMs)) return null;

  const pausedSeconds = p.pausedSeconds ?? 0;
  // 休憩中は、休憩に入った時点で経過時間を止める
  const referenceMs = p.status === 'paused' && p.pausedAt ? Date.parse(p.pausedAt) : nowMs;
  const elapsed = Math.max(0, Math.floor((referenceMs - startedMs) / 1000) - pausedSeconds);

  return {
    focusSessionId: p.focusSessionId,
    status: p.status,
    activityTitle: p.activityTitle,
    startedAt: p.startedAt,
    elapsedSeconds: elapsed,
    gardenTheme: normalizeTheme(p.gardenTheme),
    gardenStage: gardenStage(elapsed, p.plannedDurationSeconds ?? null),
  };
}

/**
 * 指定ユーザーのプレゼンスを単体で引く。プロフィールなど、
 * 一覧クエリに相乗りできない場所で使う。
 */
export async function fetchPresence(
  db: D1Database,
  viewerId: string,
  targetUserId: string
) {
  const row = await db
    .prepare(
      `SELECT ${presenceColumn('u')} FROM users u WHERE u.user_id = ?`
    )
    .bind(...presenceBinds(viewerId), targetUserId)
    .first<{ focus_presence: string | null }>();

  return mapPresence(row?.focus_presence, Date.now());
}
