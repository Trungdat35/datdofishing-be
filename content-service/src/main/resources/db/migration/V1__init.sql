CREATE TABLE articles (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    slug             VARCHAR(280) NOT NULL UNIQUE,
    summary          VARCHAR(500),
    cover_image_url  VARCHAR(1000),
    content          MEDIUMTEXT   NOT NULL,
    -- Thẻ và dữ liệu SEO linh hoạt lưu JSON (thay cho MongoDB)
    tags             JSON        NOT NULL DEFAULT (JSON_ARRAY()),
    meta             JSON,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    author_name      VARCHAR(150),
    view_count       BIGINT       NOT NULL DEFAULT 0,
    published_at     DATETIME(6),
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_articles_published ON articles(status, published_at DESC);

CREATE TABLE pages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug        VARCHAR(180) NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    content     MEDIUMTEXT   NOT NULL,
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE banners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255),
    image_url   VARCHAR(1000) NOT NULL,
    link_url    VARCHAR(1000),
    position    VARCHAR(30)   NOT NULL DEFAULT 'HOME_SLIDER',
    sort_order  INT           NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE
);
