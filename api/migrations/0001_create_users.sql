CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    firebase_uid TEXT NOT NULL UNIQUE,
    email TEXT,
    display_name TEXT NOT NULL,
    user_name TEXT NOT NULL UNIQUE,
    biography TEXT,
    profile_image_key TEXT,
    privacy_type TEXT NOT NULL DEFAULT 'public',
    account_status TEXT NOT NULL DEFAULT 'active',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    last_login_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX IF NOT EXISTS idx_users_user_name ON users(user_name);
