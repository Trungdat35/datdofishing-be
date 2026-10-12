CREATE TABLE stock (
    variant_id           BIGINT PRIMARY KEY,
    product_id           BIGINT      NOT NULL,
    quantity             INT         NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved             INT         NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    low_stock_threshold  INT         NOT NULL DEFAULT 5,
    updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_stock_product ON stock(product_id);

-- Hàng đang giữ cho từng đơn
CREATE TABLE reservations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT      NOT NULL,
    order_code  VARCHAR(30) NOT NULL,
    variant_id  BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_reservations_order ON reservations(order_id);

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
