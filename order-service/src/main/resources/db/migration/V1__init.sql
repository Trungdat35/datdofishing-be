CREATE TABLE orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(30)  NOT NULL UNIQUE,
    user_id           BIGINT       NOT NULL,
    user_email        VARCHAR(255),
    user_name         VARCHAR(150),
    status            VARCHAR(20)  NOT NULL,
    subtotal          BIGINT       NOT NULL,
    discount          BIGINT       NOT NULL DEFAULT 0,
    shipping_fee      BIGINT       NOT NULL DEFAULT 0,
    total             BIGINT       NOT NULL,
    payment_method    VARCHAR(20)  NOT NULL DEFAULT 'COD',
    recipient_name    VARCHAR(150) NOT NULL,
    phone             VARCHAR(20)  NOT NULL,
    shipping_address  VARCHAR(500) NOT NULL,
    note              VARCHAR(500),
    cancel_reason     VARCHAR(500),
    created_by_instance VARCHAR(100),
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version           BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_user ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders(status, created_at);

CREATE TABLE order_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    variant_id      BIGINT       NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    variant_name    VARCHAR(150),
    sku             VARCHAR(80),
    image_url       VARCHAR(1000),
    unit_price      BIGINT       NOT NULL,
    original_price  BIGINT       NOT NULL,
    flash_sale      BOOLEAN      NOT NULL DEFAULT FALSE,
    quantity        INT          NOT NULL,
    line_total      BIGINT       NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE order_status_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT      NOT NULL,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20) NOT NULL,
    note         VARCHAR(500),
    changed_by   VARCHAR(100),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_order_history_order ON order_status_history(order_id);

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

-- ShedLock: job định kỳ chỉ chạy trên 1 instance tại 1 thời điểm
CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until  TIMESTAMP(3)  NOT NULL,
    locked_at   TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by   VARCHAR(255) NOT NULL
);
