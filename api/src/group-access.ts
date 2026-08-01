import { Context } from 'hono';
import { AppContext } from './types';

export interface GroupRow {
  group_id: string;
  owner_user_id: string;
  group_name: string;
  group_slug: string;
  description: string | null;
  visibility_type: string;
  join_type: string;
  maximum_members: number;
  member_count: number;
  status: string;
  icon_image_key: string | null;
  cover_image_key: string | null;
  created_at: string;
}

export type GroupRole = 'owner' | 'admin' | 'moderator' | 'member' | null;

/** グループを ID でも slug でも引けるようにしておく（URL は slug を使うため） */
export async function findGroup(
  c: Context<AppContext>,
  idOrSlug: string
): Promise<GroupRow | null> {
  return c.env.DB.prepare(
    `SELECT group_id, owner_user_id, group_name, group_slug, description,
            visibility_type, join_type, maximum_members, member_count, status,
            icon_image_key, cover_image_key, created_at
     FROM groups
     WHERE (group_id = ? OR group_slug = ?) AND status != 'deleted'`
  ).bind(idOrSlug, idOrSlug).first<GroupRow>();
}

/** 参加していなければ null。退出・BAN 済みも null 扱いにする */
export async function roleInGroup(
  c: Context<AppContext>,
  groupId: string,
  userId: string
): Promise<GroupRole> {
  const row = await c.env.DB.prepare(
    `SELECT role FROM group_members
     WHERE group_id = ? AND user_id = ? AND member_status = 'active'`
  ).bind(groupId, userId).first<{ role: string }>();
  return (row?.role as GroupRole) ?? null;
}

/** 投稿・メンバー一覧など、中身を見られるか */
export function canViewContent(group: GroupRow, role: GroupRole): boolean {
  if (role !== null) return true;
  // 公開グループだけが、非メンバーにも中身を見せる
  return group.visibility_type === 'public';
}

/** グループの概要（名前・説明・人数）を見られるか。非公開は隠す */
export function canViewOverview(group: GroupRow, role: GroupRole): boolean {
  if (role !== null) return true;
  return group.visibility_type !== 'private';
}

export function canModerate(role: GroupRole): boolean {
  return role === 'owner' || role === 'admin' || role === 'moderator';
}

export function canEditGroup(role: GroupRole): boolean {
  return role === 'owner' || role === 'admin';
}

/** グループIDに使える文字は英数字・ハイフン・アンダースコアのみ（仕様書 4.4） */
export function isValidSlug(slug: string): boolean {
  return /^[A-Za-z0-9_-]{3,32}$/.test(slug);
}
