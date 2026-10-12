CREATE TABLE flash_sales (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    start_at    DATETIME(6)  NOT NULL,
    end_at      DATETIME(6)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (end_at > start_at)
);

CREATE TABLE flash_sale_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    flash_sale_id   BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    variant_id      BIGINT        NOT NULL,
    product_name    VARCHAR(255)  NOT NULL,
    product_slug    VARCHAR(280),
    variant_name    VARCHAR(150),
    image_url       VARCHAR(1000),
    original_price  BIGINT        NOT NULL,
    sale_price      BIGINT        NOT NULL,
    quota           INT           NOT NULL CHECK (quota >= 0),
    sold            INT           NOT NULL DEFAULT 0 CHECK (sold >= 0),
    sort_order      INT           NOT NULL DEFAULT 0,
    UNIQUE (flash_sale_id, variant_id),
    FOREIGN KEY (flash_sale_id) REFERENCES flash_sales(id) ON DELETE CASCADE
);

-- Suất đã giữ theo đơn hàng, để trả lại khi đơn bị hủy
CREATE TABLE flash_sale_claims (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code     VARCHAR(30) NOT NULL,
    flash_sale_id  BIGINT      NOT NULL,
    variant_id     BIGINT      NOT NULL,
    quantity       INT         NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_claims_order ON flash_sale_claims(order_code);

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
