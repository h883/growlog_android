-- 1対1のDM。同じ相手との会話が二重に作られないよう、
-- user_a_id < user_b_id になるよう並べ替えてから保存する（UNIQUE が効くようにするため）
CREATE TABLE IF NOT EXISTS conversations (
  conversation_id TEXT PRIMARY KEY,
  user_a_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  user_b_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  last_message TEXT,
  last_message_at TEXT,
  created_at TEXT NOT NULL,
  UNIQUE(user_a_id, user_b_id)
);

CREATE TABLE IF NOT EXISTS messages (
  message_id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
  sender_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  is_read INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_conversations_a ON conversations(user_a_id, last_message_at);
CREATE INDEX IF NOT EXISTS idx_conversations_b ON conversations(user_b_id, last_message_at);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id, created_at);
