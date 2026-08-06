-- 作業ガーデン：集中セッションの経過を「割合」ではなく「段階」で表す

-- plant / city / robot / space / campfire / aquarium / adventure
ALTER TABLE focus_sessions ADD COLUMN garden_theme TEXT NOT NULL DEFAULT 'plant';

-- 終了時に到達段階を確定させる。あとから庭を並べ直せるように保存しておく
ALTER TABLE focus_sessions ADD COLUMN garden_stage INTEGER NOT NULL DEFAULT 0;

-- 次回の既定テーマ。毎回選び直させない
ALTER TABLE users ADD COLUMN garden_theme TEXT NOT NULL DEFAULT 'plant';

-- 「今週の庭」を引くときに completed_at で範囲を絞るため
CREATE INDEX IF NOT EXISTS idx_focus_sessions_completed
  ON focus_sessions(user_id, status, completed_at);
