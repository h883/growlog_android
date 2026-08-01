import { Hono, Context } from 'hono';
import { AppContext } from '../types';
import { authMiddleware } from '../middleware/auth';
import { imageUrlFor } from '../media';
import { POST_COLUMNS, POST_FROM, mapPostRow } from '../posts-query';
import {
  GroupRow,
  GroupRole,
  canEditGroup,
  canViewContent,
  canViewOverview,
  findGroup,
  isValidSlug,
  roleInGroup,
} from '../group-access';

const groups = new Hono<AppContext>();

/** 1ユーザーが作れるグループ数の上限（仕様書 4.2） */
const MAX_GROUPS_PER_USER = 3;

async function requireMe(c: Context<AppContext>) {
  const firebaseUser = c.get('firebaseUser');
  return c.env.DB.prepare('SELECT user_id FROM users WHERE firebase_uid = ?')
    .bind(firebaseUser.uid)
    .first<{ user_id: string }>();
}

function mapGroup(requestUrl: string, g: GroupRow, role: GroupRole) {
  return {
    groupId: g.group_id,
    groupSlug: g.group_slug,
    groupName: g.group_name,
    description: g.description ?? '',
    visibilityType: g.visibility_type,
    joinType: g.join_type,
    memberCount: g.member_count,
    maximumMembers: g.maximum_members,
    iconImageUrl: imageUrlFor(requestUrl, g.icon_image_key),
    coverImageUrl: imageUrlFor(requestUrl, g.cover_image_key),
    myRole: role,
    isMember: role !== null,
    isOwner: role === 'owner',
    createdAt: g.created_at,
  };
}

/** 一覧・検索。非公開グループは検索結果に出さない（仕様書 3.3） */
groups.get('/', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const q = (c.req.query('q') ?? '').trim();
  const mineOnly = c.req.query('mine') === 'true';

  const pattern = `%${q.replace(/[\\%_]/g, (ch) => `\\${ch}`)}%`;

  // 自分が参加しているグループは、非公開でも一覧に出す
  const where = mineOnly
    ? `gm.user_id IS NOT NULL`
    : `(g.visibility_type != 'private' OR gm.user_id IS NOT NULL)`;
  const search = q ? ` AND (g.group_name LIKE ? ESCAPE '\\' OR g.description LIKE ? ESCAPE '\\')` : '';

  const params: string[] = [me.user_id];
  if (q) params.push(pattern, pattern);

  const result = await c.env.DB.prepare(
    `SELECT g.*, gm.role as my_role
     FROM groups g
     LEFT JOIN group_members gm
       ON gm.group_id = g.group_id AND gm.user_id = ? AND gm.member_status = 'active'
     WHERE g.status = 'active' AND ${where}${search}
     ORDER BY g.member_count DESC, g.created_at DESC
     LIMIT 50`
  ).bind(...params).all();

  return c.json({
    groups: (result.results as any[]).map((r) =>
      mapGroup(c.req.url, r as GroupRow, (r.my_role as GroupRole) ?? null)
    ),
  });
});

/** グループ作成 */
groups.post('/', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const body = await c.req.json<{
    groupName: string;
    groupSlug: string;
    description?: string;
    visibilityType?: string;
    maximumMembers?: number;
  }>();

  const groupName = body.groupName?.trim();
  const groupSlug = body.groupSlug?.trim();
  if (!groupName) return c.json({ error: 'グループ名を入力してください' }, 400);
  if (!groupSlug || !isValidSlug(groupSlug)) {
    return c.json({ error: 'グループIDは英数字・ハイフン・アンダースコア3〜32文字です' }, 400);
  }

  const visibility = ['public', 'approval', 'private'].includes(body.visibilityType ?? '')
    ? body.visibilityType!
    : 'public';
  // 参加方法は公開範囲から自動的に決まる
  const joinType = visibility === 'public' ? 'open' : visibility === 'approval' ? 'approval' : 'invite';

  const owned = await c.env.DB.prepare(
    `SELECT COUNT(*) as cnt FROM groups WHERE owner_user_id = ? AND status = 'active'`
  ).bind(me.user_id).first<{ cnt: number }>();
  if ((owned?.cnt ?? 0) >= MAX_GROUPS_PER_USER) {
    return c.json({ error: `作成できるグループは${MAX_GROUPS_PER_USER}個までです` }, 403);
  }

  const duplicated = await c.env.DB.prepare(
    'SELECT 1 FROM groups WHERE group_slug = ?'
  ).bind(groupSlug).first();
  if (duplicated) return c.json({ error: 'このグループIDは既に使われています' }, 409);

  const groupId = crypto.randomUUID();
  const now = new Date().toISOString();
  const maximumMembers = Math.max(2, Math.min(5000, body.maximumMembers ?? 500));

  await c.env.DB.prepare(
    `INSERT INTO groups
       (group_id, owner_user_id, group_name, group_slug, description,
        visibility_type, join_type, maximum_members, member_count, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)`
  ).bind(groupId, me.user_id, groupName, groupSlug, body.description?.trim() ?? '',
    visibility, joinType, maximumMembers, now, now).run();

  // 作成者はそのままオーナーとして参加させる
  await c.env.DB.prepare(
    `INSERT INTO group_members
       (group_member_id, group_id, user_id, role, joined_at, created_at, updated_at)
     VALUES (?, ?, ?, 'owner', ?, ?, ?)`
  ).bind(crypto.randomUUID(), groupId, me.user_id, now, now, now).run();

  return c.json({ groupId, groupSlug }, 201);
});

groups.get('/:groupId', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  // 非公開グループは、メンバー以外に存在自体を返さない
  if (!canViewOverview(group, role)) return c.json({ error: 'Group not found' }, 404);

  return c.json({
    ...mapGroup(c.req.url, group, role),
    canViewContent: canViewContent(group, role),
  });
});

groups.patch('/:groupId', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (!canEditGroup(role)) return c.json({ error: '権限がありません' }, 403);

  const body = await c.req.json<{
    groupName?: string;
    description?: string;
    visibilityType?: string;
  }>();

  const visibility = body.visibilityType && ['public', 'approval', 'private'].includes(body.visibilityType)
    ? body.visibilityType
    : null;
  const joinType = visibility === null ? null
    : visibility === 'public' ? 'open' : visibility === 'approval' ? 'approval' : 'invite';

  await c.env.DB.prepare(
    `UPDATE groups SET
       group_name = COALESCE(?, group_name),
       description = COALESCE(?, description),
       visibility_type = COALESCE(?, visibility_type),
       join_type = COALESCE(?, join_type),
       updated_at = ?
     WHERE group_id = ?`
  ).bind(body.groupName?.trim() ?? null, body.description?.trim() ?? null,
    visibility, joinType, new Date().toISOString(), group.group_id).run();

  return c.json({ ok: true });
});

/** 公開グループへの参加は即時（仕様書 6.1）。それ以外は申請が必要 */
groups.post('/:groupId/join', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (role !== null) return c.json({ error: '既に参加しています' }, 409);

  if (group.join_type !== 'open') {
    return c.json({ error: 'このグループは参加申請または招待が必要です' }, 403);
  }
  if (group.member_count >= group.maximum_members) {
    return c.json({ error: 'このグループは満員です' }, 403);
  }

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `INSERT INTO group_members
       (group_member_id, group_id, user_id, role, joined_at, created_at, updated_at)
     VALUES (?, ?, ?, 'member', ?, ?, ?)
     ON CONFLICT(group_id, user_id) DO UPDATE SET
       member_status = 'active', role = 'member', updated_at = excluded.updated_at`
  ).bind(crypto.randomUUID(), group.group_id, me.user_id, now, now, now).run();

  await c.env.DB.prepare(
    `UPDATE groups SET member_count =
       (SELECT COUNT(*) FROM group_members WHERE group_id = ? AND member_status = 'active'),
       updated_at = ? WHERE group_id = ?`
  ).bind(group.group_id, now, group.group_id).run();

  return c.json({ joined: true });
});

/** オーナーは譲渡か削除をするまで退出できない（仕様書 7） */
groups.post('/:groupId/leave', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (role === null) return c.json({ error: '参加していません' }, 400);
  if (role === 'owner') {
    return c.json({ error: 'オーナーは権限を譲渡するか、グループを削除してください' }, 403);
  }

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE group_members SET member_status = 'left', updated_at = ?
     WHERE group_id = ? AND user_id = ?`
  ).bind(now, group.group_id, me.user_id).run();

  await c.env.DB.prepare(
    `UPDATE groups SET member_count =
       (SELECT COUNT(*) FROM group_members WHERE group_id = ? AND member_status = 'active'),
       updated_at = ? WHERE group_id = ?`
  ).bind(group.group_id, now, group.group_id).run();

  return c.json({ left: true });
});

groups.get('/:groupId/members', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (!canViewContent(group, role)) return c.json({ error: '権限がありません' }, 403);

  // オーナー→管理者→モデレーター→一般 の順（仕様書 15）
  const result = await c.env.DB.prepare(
    `SELECT gm.role, gm.joined_at,
            u.user_id, u.display_name, u.user_name, u.profile_image_key
     FROM group_members gm
     JOIN users u ON u.user_id = gm.user_id
     WHERE gm.group_id = ? AND gm.member_status = 'active'
     ORDER BY CASE gm.role
                WHEN 'owner' THEN 0 WHEN 'admin' THEN 1
                WHEN 'moderator' THEN 2 ELSE 3 END,
              gm.joined_at ASC
     LIMIT 200`
  ).bind(group.group_id).all();

  return c.json({
    members: (result.results as any[]).map((r) => ({
      userId: r.user_id,
      displayName: r.display_name,
      userName: r.user_name,
      profileImageUrl: imageUrlFor(c.req.url, r.profile_image_key),
      role: r.role,
      joinedAt: r.joined_at,
    })),
  });
});

/** グループ内タイムライン。固定投稿を先頭に出す（仕様書 10） */
groups.get('/:groupId/posts', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (!canViewContent(group, role)) return c.json({ error: '権限がありません' }, 403);

  const result = await c.env.DB.prepare(
    `SELECT ${POST_COLUMNS}, p.is_pinned ${POST_FROM}
     WHERE p.group_id = ?
     ORDER BY p.is_pinned DESC, p.created_at DESC
     LIMIT 50`
  ).bind(me.user_id, me.user_id, group.group_id).all();

  return c.json({
    posts: (result.results as any[]).map((r) => ({
      ...mapPostRow(c.req.url, r),
      isPinned: r.is_pinned === 1,
    })),
  });
});

groups.post('/:groupId/posts', authMiddleware, async (c) => {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (role === null) return c.json({ error: 'メンバーのみ投稿できます' }, 403);

  const body = await c.req.json<{
    content: string;
    postType?: string;
    imageKey?: string;
    tags?: string[];
    groupVisibility?: string;
  }>();

  if (!body.content?.trim()) return c.json({ error: 'Content is required' }, 400);
  if (body.imageKey && !body.imageKey.startsWith(`posts/${me.user_id}/`)) {
    return c.json({ error: 'Invalid imageKey' }, 400);
  }

  // 公開グループ以外は、外部に出さない設定を強制する（仕様書 9.3）
  const groupVisibility = group.visibility_type === 'public' && body.groupVisibility === 'public'
    ? 'public'
    : 'members';

  const postId = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO posts
       (post_id, user_id, content, post_type, tags, image_key,
        group_id, group_visibility, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(postId, me.user_id, body.content.trim(), body.postType ?? 'progress',
    JSON.stringify(body.tags ?? []), body.imageKey ?? null,
    group.group_id, groupVisibility, now, now).run();

  return c.json({ postId, createdAt: now }, 201);
});

/** 投稿の固定・解除。モデレーター以上 */
groups.post('/:groupId/posts/:postId/pin', authMiddleware, async (c) => {
  return setPinned(c, true);
});

groups.delete('/:groupId/posts/:postId/pin', authMiddleware, async (c) => {
  return setPinned(c, false);
});

const MAX_PINNED = 3;

async function setPinned(c: Context<AppContext>, pinned: boolean) {
  const me = await requireMe(c);
  if (!me) return c.json({ error: 'User not found' }, 404);

  const group = await findGroup(c, c.req.param('groupId') ?? '');
  if (!group) return c.json({ error: 'Group not found' }, 404);

  const role = await roleInGroup(c, group.group_id, me.user_id);
  if (role !== 'owner' && role !== 'admin' && role !== 'moderator') {
    return c.json({ error: '権限がありません' }, 403);
  }

  const postId = c.req.param('postId') ?? '';

  if (pinned) {
    const current = await c.env.DB.prepare(
      'SELECT COUNT(*) as cnt FROM posts WHERE group_id = ? AND is_pinned = 1'
    ).bind(group.group_id).first<{ cnt: number }>();
    if ((current?.cnt ?? 0) >= MAX_PINNED) {
      return c.json({ error: `固定できる投稿は${MAX_PINNED}件までです` }, 400);
    }
  }

  const { meta } = await c.env.DB.prepare(
    'UPDATE posts SET is_pinned = ? WHERE post_id = ? AND group_id = ?'
  ).bind(pinned ? 1 : 0, postId, group.group_id).run();

  if (meta.changes === 0) return c.json({ error: 'Post not found' }, 404);
  return c.json({ isPinned: pinned });
}

export default groups;
