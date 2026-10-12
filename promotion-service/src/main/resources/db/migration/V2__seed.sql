-- Flash sale mẫu đang diễn ra trong 7 ngày kể từ lúc khởi tạo DB
INSERT INTO flash_sales (id, name, start_at, end_at) VALUES
 (1, 'FLASH SALE SẢN PHẨM BÁN CHẠY', NOW(6) - INTERVAL 1 HOUR, NOW(6) + INTERVAL 7 DAY);
ALTER TABLE flash_sales AUTO_INCREMENT = 101;

INSERT INTO flash_sale_items (flash_sale_id, product_id, variant_id, product_name, product_slug, variant_name,
                              original_price, sale_price, quota, sort_order) VALUES
 (1, 2, 4, 'Cần câu V5 Plus CC-51 Synthesis tổng hợp', 'can-cau-v5-plus-cc-51-synthesis', '3m6', 734000, 690000, 10, 1),
 (1, 3, 6, 'Cần Câu Ngư Điếu 5000 Thế Hệ 2 CC-50 Săn Hàng Hạng Nhẹ', 'can-cau-ngu-dieu-5000-the-he-2-cc-50', '4m5', 917000, 879000, 10, 2),
 (1, 4, 8, 'Cần Câu Hoả Mã CC-48 Cao Cấp Bản Kỷ Niệm', 'can-cau-hoa-ma-cc-48-ban-ky-niem', '3m9', 1102000, 1050000, 5, 3),
 (1, 5, 10, 'Thùng câu đài V5 Plus Box KK-69 26L Rice Fishing nâng cấp', 'thung-cau-dai-v5-plus-box-kk-69-26l', '26L', 2235000, 2100000, 5, 4),
 (1, 6, 11, 'Gác hố đấu NGƯ ĐIẾU PLUS KK-70 RICE FISHING Chuyên Săn Hàng', 'gac-ho-dau-ngu-dieu-plus-kk-70', 'Mặc định', 756000, 720000, 5, 5);
