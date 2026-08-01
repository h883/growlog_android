import { Context } from 'hono';
import { AppContext } from './types';

/**
 * 集中中のユーザーへの通知は保留する（仕様書 6）。
 * 通知を作る側がこれを呼び、is_deferred の値を決める。
 */
export async function isFocusing(
  c: Context<AppContext>,
  userId: string
): Promise<boolean> {
  const row = await c.env.DB.prepare(
    `SELECT 1 FROM focus_sessions
     WHERE user_id = ? AND status IN ('active', 'paused')`
  ).bind(userId).first();
  return row !== null;
}

/** 集中中なら 1、そうでなければ 0 を返す。INSERT にそのまま渡す用 */
export async function deferredFlag(
  c: Context<AppContext>,
  userId: string
): Promise<number> {
  return (await isFocusing(c, userId)) ? 1 : 0;
}
