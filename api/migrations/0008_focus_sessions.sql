-- 集中ステータス機能 初期版
CREATE TABLE IF NOT EXISTS focus_sessions (
  focus_session_id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  goal_id TEXT REFERENCES goals(goal_id) ON DELETE SET NULL,
  group_id TEXT REFERENCES groups(group_id) ON DELETE SET NULL,
  activity_title TEXT NOT NULL,
  -- active / paused / completed / cancelled
  status TEXT NOT NULL DEFAULT 'active',
  -- all / followers / group / private
  visibility_type TEXT NOT NULL DEFAULT 'followers',
  -- light / focus / serious
  focus_mode TEXT NOT NULL DEFAULT 'light',
  planned_duration_seconds INTEGER,
  actual_duration_seconds INTEGER,
  -- 休憩の累計。仕様書の表には無いが、経過時間を正しく出すために持つ
  paused_seconds INTEGER NOT NULL DEFAULT 0,
  break_count INTEGER NOT NULL DEFAULT 0,
  started_at TEXT NOT NULL,
  paused_at TEXT,
  completed_at TEXT,
  reflection TEXT,
  concentration_rating INTEGER,
  end_reason TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS focus_reactions (
  focus_reaction_id TEXT PRIMARY KEY,
  focus_session_id TEXT NOT NULL REFERENCES focus_sessions(focus_session_id) ON DELETE CASCADE,
  user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  -- cheer(邪魔せず応援) / together(一緒に集中中) / talk_later(終了後に話したい)
  reaction_type TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(focus_session_id, user_id, reaction_type)
);

-- 集中中に届いた通知は保留し、セッション終了時にまとめて表示する（仕様書 6）
ALTER TABLE notifications ADD COLUMN is_deferred INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_focus_sessions_user ON focus_sessions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_active ON focus_sessions(status, started_at);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_group ON focus_sessions(group_id, status);
CREATE INDEX IF NOT EXISTS idx_focus_reactions_session ON focus_reactions(focus_session_id);
