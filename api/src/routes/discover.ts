import { Hono, Context } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';
import { POST_COLUMNS, POST_FROM, OUTSIDE_GROUP_CONDITION, mapPostRow, viewerBinds } from '../posts-query';

const discover = new Hono<AppContext>();

/** LIKE のワイルドカードをエスケープして部分一致パターンにする */
function likePattern(q: string): string {
  return `%${q.replace(/[\\%_]/g, (ch) => `\\${ch}`)}%`;
}

async function requireMe(c: Context<AppContext>) {
  const firebaseUser = c.get('firebaseUser');
  return c.env.DB.prepare('SELECT user_id FROM users WHERE firebase_uid = ?')
    .bind(firebaseUser.uid)
    .first<{ user_id: string }>();
}

/** 検索なしの初期表示: 人気タグ + まだフォローしていないおすすめユーザー */
discover.get('/', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const [tagRows, userRows, challengeRows] = await Promise.all([
    // tags は JSON 配列で入っているため SQL では集計できない。直近の投稿を引いて JS で数える
    c.env.DB.prepare(
      'SELECT tags FROM posts ORDER BY created_at DESC LIMIT 200'
    ).all(),
    c.env.DB.prepare(
      `SELECT u.user_id, u.display_name, u.user_name, u.biography, u.profile_image_key,
              COUNT(f.follower_id) as follower_count
       FROM users u
       LEFT JOIN follows f ON f.following_id = u.user_id
       WHERE u.user_id != ?
         AND u.user_id NOT IN (SELECT following_id FROM follows WHERE follower_id = ?)
       GROUP BY u.user_id
       ORDER BY follower_count DESC, u.created_at DESC
       LIMIT 10`
    ).bind(me.user_id, me.user_id).all(),
    // 「今週の挑戦」= 目標が紐付いた最近の投稿
    c.env.DB.prepare(
      `SELECT ${POST_COLUMNS} ${POST_FROM}
       WHERE p.goal_id IS NOT NULL AND ${OUTSIDE_GROUP_CONDITION}
       ORDER BY p.created_at DESC LIMIT 5`
    ).bind(...viewerBinds(me.user_id)).all(),
  ]);

  const counts = new Map<string, number>();
  for (const row of tagRows.results as any[]) {
    let tags: unknown;
    try {
      tags = JSON.parse(row.tags || '[]');
    } catch {
      continue;
    }
    if (!Array.isArray(tags)) continue;
    for (const tag of tags) {
      if (typeof tag !== 'string' || !tag.trim()) continue;
      counts.set(tag, (counts.get(tag) ?? 0) + 1);
    }
  }

  const trendingTags = [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 12)
    .map(([tag, count]) => ({ tag, count }));

  return c.json({
    trendingTags,
    suggestedPosts: (challengeRows.results as any[]).map((r) => mapPostRow(c.req.url, r)),
    suggestedUsers: (userRows.results as any[]).map((r) => ({
      userId: r.user_id,
      displayName: r.display_name,
      userName: r.user_name,
      biography: r.biography ?? '',
      profileImageUrl: imageUrlFor(c.req.url, r.profile_image_key),
      followerCount: r.follower_count ?? 0,
    })),
  });
});

/** 投稿とユーザーを横断検索。q が空なら空配列を返す */
discover.get('/search', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const q = (c.req.query('q') ?? '').trim();
  if (!q) return c.json({ posts: [], users: [] });

  const pattern = likePattern(q);

  const [postRows, userRows] = await Promise.all([
    c.env.DB.prepare(
      `SELECT ${POST_COLUMNS} ${POST_FROM}
       WHERE ${OUTSIDE_GROUP_CONDITION}
         AND (p.content LIKE ? ESCAPE '\\' OR p.tags LIKE ? ESCAPE '\\')
       ORDER BY p.created_at DESC LIMIT 30`
    ).bind(...viewerBinds(me.user_id), pattern, pattern).all(),
    c.env.DB.prepare(
      `SELECT u.user_id, u.display_name, u.user_name, u.biography, u.profile_image_key,
              COUNT(f.follower_id) as follower_count
       FROM users u
       LEFT JOIN follows f ON f.following_id = u.user_id
       WHERE u.display_name LIKE ? ESCAPE '\\' OR u.user_name LIKE ? ESCAPE '\\'
       GROUP BY u.user_id
       ORDER BY follower_count DESC
       LIMIT 20`
    ).bind(pattern, pattern).all(),
  ]);

  return c.json({
    posts: (postRows.results as any[]).map((r) => mapPostRow(c.req.url, r)),
    users: (userRows.results as any[]).map((r) => ({
      userId: r.user_id,
      displayName: r.display_name,
      userName: r.user_name,
      biography: r.biography ?? '',
      profileImageUrl: imageUrlFor(c.req.url, r.profile_image_key),
      followerCount: r.follower_count ?? 0,
    })),
  });
});

export default discover;
