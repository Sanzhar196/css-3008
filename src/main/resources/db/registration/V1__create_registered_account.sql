CREATE TABLE registration.account (
    username VARCHAR(32) PRIMARY KEY CHECK (username ~ '^[a-z0-9][a-z0-9_.-]{2,31}$'),
    password_hash VARCHAR(68) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role = 'CLERK'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
