CREATE TABLE IF NOT EXISTS saves (
  user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  post_id TEXT NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
  created_at TEXT NOT NULL,
  PRIMARY KEY (user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_saves_user ON saves(user_id, created_at);
