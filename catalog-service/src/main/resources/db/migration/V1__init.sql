CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(180) NOT NULL UNIQUE,
    parent_id   BIGINT,
    sort_order  INT          NOT NULL DEFAULT 0,
    image_url   VARCHAR(1000),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE brands (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(150) NOT NULL,
    slug      VARCHAR(180) NOT NULL UNIQUE,
    logo_url  VARCHAR(1000)
);

CREATE TABLE products (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    slug               VARCHAR(280) NOT NULL UNIQUE,
    sku                VARCHAR(80),
    category_id        BIGINT,
    brand_id           BIGINT,
    short_description  VARCHAR(500),
    description        MEDIUMTEXT,
    price              BIGINT       NOT NULL,
    compare_at_price   BIGINT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    hot                BOOLEAN      NOT NULL DEFAULT FALSE,
    new_arrival        BOOLEAN      NOT NULL DEFAULT FALSE,
    specs              JSON,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (brand_id) REFERENCES brands(id)
);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand_id);

CREATE TABLE product_variants (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT       NOT NULL,
    sku               VARCHAR(80)  NOT NULL UNIQUE,
    name              VARCHAR(150) NOT NULL,
    price             BIGINT       NOT NULL,
    compare_at_price  BIGINT,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order        INT          NOT NULL DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
CREATE INDEX idx_variants_product ON product_variants(product_id);

-- Ảnh sản phẩm: admin dán URL (ví dụ ảnh CDN bizweb.dktcdn.net)
CREATE TABLE product_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT        NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    alt         VARCHAR(255),
    sort_order  INT           NOT NULL DEFAULT 0,
    thumbnail   BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
CREATE INDEX idx_images_product ON product_images(product_id);

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
