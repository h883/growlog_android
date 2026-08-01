-- グループ機能 第1段階: groups / group_members と、posts へのグループ列追加
CREATE TABLE IF NOT EXISTS groups (
  group_id TEXT PRIMARY KEY,
  owner_user_id TEXT NOT NULL REFERENCES users(user_id),
  group_name TEXT NOT NULL,
  group_slug TEXT NOT NULL UNIQUE,
  description TEXT,
  category_id TEXT,
  icon_image_key TEXT,
  cover_image_key TEXT,
  -- public / approval / private
  visibility_type TEXT NOT NULL DEFAULT 'public',
  -- open / approval / invite
  join_type TEXT NOT NULL DEFAULT 'open',
  maximum_members INTEGER NOT NULL DEFAULT 500,
  member_count INTEGER NOT NULL DEFAULT 1,
  -- active / suspended / deleted
  status TEXT NOT NULL DEFAULT 'active',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT
);

CREATE TABLE IF NOT EXISTS group_members (
  group_member_id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE,
  user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  -- owner / admin / moderator / member
  role TEXT NOT NULL DEFAULT 'member',
  -- active / muted / banned / left
  member_status TEXT NOT NULL DEFAULT 'active',
  joined_at TEXT NOT NULL,
  muted_until TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(group_id, user_id)
);

-- グループに属さない通常投稿では group_id を NULL にする
ALTER TABLE posts ADD COLUMN group_id TEXT;
-- members: グループ内のみ / public: グループ外にも公開
ALTER TABLE posts ADD COLUMN group_visibility TEXT;
ALTER TABLE posts ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_groups_slug ON groups(group_slug);
CREATE INDEX IF NOT EXISTS idx_groups_visibility ON groups(visibility_type, status);
CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members(group_id, role);
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id, member_status);
CREATE INDEX IF NOT EXISTS idx_posts_group ON posts(group_id, created_at);
