import { imageUrlFor } from './media';
import { presenceColumn, presenceBinds, mapPresence } from './focus-presence';

/**
 * 投稿一覧の共通 SELECT。
 *
 * 閲覧者の user_id を、プレゼンス用・リアクション用・保存用で複数回バインドする。
 * 個数を呼び出し側に散らすとズレるので、必ず `viewerBinds(viewerId)` を
 * パラメータの先頭に展開して渡すこと。
 */
export const POST_COLUMNS = `
  p.post_id, p.user_id, p.content, p.post_type, p.progress, p.tags,
  p.image_key, p.group_id, p.reaction_count, p.comment_count, p.created_at,
  u.display_name, u.user_name, u.profile_image_key, g.title as goal_title,
  CASE WHEN r.reaction_id IS NOT NULL THEN 1 ELSE 0 END as my_reaction,
  CASE WHEN s.post_id IS NOT NULL THEN 1 ELSE 0 END as is_saved,
  ${presenceColumn('u')}`;

export const POST_FROM = `
  FROM posts p
  JOIN users u ON p.user_id = u.user_id
  LEFT JOIN goals g ON p.goal_id = g.goal_id
  LEFT JOIN reactions r ON r.post_id = p.post_id AND r.user_id = ?
  LEFT JOIN saves s ON s.post_id = p.post_id AND s.user_id = ?`;

/**
 * POST_COLUMNS + POST_FROM が消費する、閲覧者 user_id のバインド列。
 * SELECT は FROM より前に評価されるので、プレゼンスぶんが先に来る。
 */
export function viewerBinds(viewerId: string): string[] {
  return [
    ...presenceBinds(viewerId), // SELECT 内のプレゼンス
    viewerId,                   // LEFT JOIN reactions
    viewerId,                   // LEFT JOIN saves
  ];
}

/**
 * グループ外のタイムラインに出してよい投稿の条件。
 * グループ投稿は、公開グループで「グループ外にも公開」を選んだものだけ出す（仕様書 9.3）。
 * WHERE の先頭に置けるよう、単独で成立する式にしてある。
 */
export const OUTSIDE_GROUP_CONDITION =
  `(p.group_id IS NULL OR p.group_visibility = 'public')`;

export function mapPostRow(requestUrl: string, r: any) {
  return {
    postId: r.post_id,
    userId: r.user_id,
    displayName: r.display_name,
    userName: r.user_name,
    authorImageUrl: imageUrlFor(requestUrl, r.profile_image_key),
    content: r.content,
    postType: r.post_type,
    goalTitle: r.goal_title ?? null,
    progress: r.progress ?? null,
    tags: JSON.parse(r.tags || '[]'),
    imageUrl: imageUrlFor(requestUrl, r.image_key),
    groupId: r.group_id ?? null,
    reactionCount: r.reaction_count,
    commentCount: r.comment_count,
    myReaction: r.my_reaction === 1,
    isSaved: r.is_saved === 1,
    // 投稿者がいま集中していれば、その状況を一緒に返す（Discord のプレゼンス相当）
    authorFocus: mapPresence(r.focus_presence, Date.now()),
    createdAt: r.created_at,
  };
}
