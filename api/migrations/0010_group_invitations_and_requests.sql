CREATE TABLE IF NOT EXISTS group_join_requests (
  request_id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE,
  requester_user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pending',
  reviewed_by_user_id TEXT REFERENCES users(user_id),
  created_at TEXT NOT NULL,
  reviewed_at TEXT
);

CREATE TABLE IF NOT EXISTS group_invitations (
  invitation_id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE,
  invitee_user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  inviter_user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pending',
  created_at TEXT NOT NULL,
  responded_at TEXT
);

ALTER TABLE notifications ADD COLUMN group_id TEXT REFERENCES groups(group_id) ON DELETE CASCADE;
ALTER TABLE notifications ADD COLUMN group_join_request_id TEXT REFERENCES group_join_requests(request_id) ON DELETE CASCADE;
ALTER TABLE notifications ADD COLUMN group_invitation_id TEXT REFERENCES group_invitations(invitation_id) ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_group_join_requests_pending
  ON group_join_requests(group_id, requester_user_id) WHERE status = 'pending';
CREATE UNIQUE INDEX IF NOT EXISTS idx_group_invitations_pending
  ON group_invitations(group_id, invitee_user_id) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_group_join_requests_group ON group_join_requests(group_id, status);
CREATE INDEX IF NOT EXISTS idx_group_invitations_invitee ON group_invitations(invitee_user_id, status);
