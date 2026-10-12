-- Chạy 1 lần khi container PostgreSQL khởi tạo lần đầu.
-- Mỗi service có database + user riêng (database-per-service).
CREATE USER auth_user WITH PASSWORD 'auth_pass';
CREATE DATABASE auth_db OWNER auth_user;

CREATE USER catalog_user WITH PASSWORD 'catalog_pass';
CREATE DATABASE catalog_db OWNER catalog_user;

CREATE USER inventory_user WITH PASSWORD 'inventory_pass';
CREATE DATABASE inventory_db OWNER inventory_user;

CREATE USER order_user WITH PASSWORD 'order_pass';
CREATE DATABASE order_db OWNER order_user;

CREATE USER promotion_user WITH PASSWORD 'promotion_pass';
CREATE DATABASE promotion_db OWNER promotion_user;

CREATE USER content_user WITH PASSWORD 'content_pass';
CREATE DATABASE content_db OWNER content_user;

CREATE USER notification_user WITH PASSWORD 'notification_pass';
CREATE DATABASE notification_db OWNER notification_user;
