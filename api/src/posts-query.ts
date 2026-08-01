import { imageUrlFor } from './media';

/**
 * 投稿一覧の共通 SELECT。`POST_FROM` の LEFT JOIN が閲覧者の user_id を
 * 2つ（リアクション用・保存用）バインドするので、パラメータの先頭は必ず
 * `[viewerId, viewerId, ...]` の順で渡すこと。
 */
export const POST_COLUMNS = `
  p.post_id, p.user_id, p.content, p.post_type, p.progress, p.tags,
  p.image_key, p.reaction_count, p.comment_count, p.created_at,
  u.display_name, u.user_name, u.profile_image_key, g.title as goal_title,
  CASE WHEN r.reaction_id IS NOT NULL THEN 1 ELSE 0 END as my_reaction,
  CASE WHEN s.post_id IS NOT NULL THEN 1 ELSE 0 END as is_saved`;

export const POST_FROM = `
  FROM posts p
  JOIN users u ON p.user_id = u.user_id
  LEFT JOIN goals g ON p.goal_id = g.goal_id
  LEFT JOIN reactions r ON r.post_id = p.post_id AND r.user_id = ?
  LEFT JOIN saves s ON s.post_id = p.post_id AND s.user_id = ?`;

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
    reactionCount: r.reaction_count,
    commentCount: r.comment_count,
    myReaction: r.my_reaction === 1,
    isSaved: r.is_saved === 1,
    createdAt: r.created_at,
  };
}
