CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    avatar_url      VARCHAR(500),
    roles           VARCHAR(100) NOT NULL DEFAULT 'CUSTOMER',
    provider        VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    google_sub      VARCHAR(100) UNIQUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE user_addresses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    recipient_name  VARCHAR(150) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    province        VARCHAR(100) NOT NULL,
    district        VARCHAR(100) NOT NULL,
    ward            VARCHAR(100) NOT NULL,
    street          VARCHAR(255) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_addresses_user ON user_addresses(user_id);

-- Khóa RSA ký JWT, lưu DB để nhiều instance dùng chung một khóa
CREATE TABLE signing_keys (
    kid          VARCHAR(64) PRIMARY KEY,
    private_key  TEXT        NOT NULL,
    public_key   TEXT        NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE outbox_events (
    id            CHAR(36) PRIMARY KEY,
    topic         VARCHAR(100) NOT NULL,
    msg_key       VARCHAR(100),
    payload       JSON        NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at  DATETIME(6)
);
CREATE INDEX idx_outbox_unpublished ON outbox_events(published_at, created_at);

CREATE TABLE processed_events (
    event_id      VARCHAR(64) PRIMARY KEY,
    processed_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
