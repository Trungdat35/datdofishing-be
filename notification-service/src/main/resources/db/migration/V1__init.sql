CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    audience    VARCHAR(10)  NOT NULL,          -- USER hoặc ADMIN
    user_id     BIGINT,                          -- NULL khi gửi cho tất cả admin
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        VARCHAR(1000),
    link        VARCHAR(500),
    data        JSON,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_audience ON notifications(audience, created_at DESC);

-- Nhật ký email đã gửi để tra cứu
CREATE TABLE email_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    to_address  VARCHAR(255) NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    template    VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    error       VARCHAR(1000),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
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
