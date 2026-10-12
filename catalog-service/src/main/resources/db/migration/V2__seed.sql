-- Dữ liệu mẫu theo nhóm hàng của trang tham khảo docauduli.com
INSERT INTO categories (id, name, slug, sort_order) VALUES
 (1, 'Cần câu đài', 'can-cau-dai', 1),
 (2, 'Dây câu cá', 'day-cau-ca', 2),
 (3, 'Phao câu đài', 'phao-cau-dai', 3),
 (4, 'Lưỡi & thẻo câu', 'luoi-theo-cau', 4),
 (5, 'Mồi câu cá', 'moi-cau-ca', 5),
 (6, 'Thùng câu cá', 'thung-cau-ca', 6),
 (7, 'Vợt & Gác cần câu', 'vot-gac-can-cau', 7),
 (8, 'Rọng đựng cá', 'rong-dung-ca', 8),
 (9, 'Túi đựng cần câu', 'tui-dung-can-cau', 9),
 (10, 'Phụ kiện câu cá', 'phu-kien-cau-ca', 10);
INSERT INTO categories (id, name, slug, parent_id, sort_order) VALUES
 (11, 'Dây trục câu', 'day-truc-cau', 2, 1),
 (12, 'Dây thẻo câu', 'day-theo-cau', 2, 2),
 (13, 'Dây PE', 'day-pe', 2, 3),
 (14, 'Lưỡi câu', 'luoi-cau', 4, 1),
 (15, 'Phao gỗ Balsa', 'phao-go-balsa', 3, 1);
ALTER TABLE categories AUTO_INCREMENT = 101;

INSERT INTO brands (id, name, slug) VALUES
 (1, 'V5', 'v5'), (2, 'Ngư Điếu', 'ngu-dieu'), (3, 'Hỏa Mã', 'hoa-ma'), (4, 'Rice Fishing', 'rice-fishing'),
 (5, 'Iseama', 'iseama');
ALTER TABLE brands AUTO_INCREMENT = 101;

INSERT INTO products (id, name, slug, sku, category_id, brand_id, short_description, description, price,
                      compare_at_price, hot, new_arrival, specs) VALUES
 (1, 'Cần câu đài V5 Big Fish', 'can-cau-dai-v5-big-fish', 'V5-BF', 1, 1,
  'Cần câu đài chuyên cá lớn, carbon cao cấp, nhiều kích thước.',
  '<p>Cần câu đài V5 Big Fish dùng carbon mô-đun cao, khỏe và nhẹ, phù hợp săn hàng tại hồ dịch vụ.</p>',
  1250000, 1500000, TRUE, FALSE, '{"chatLieu": "Carbon", "doCung": "6H", "khoiLuong": "180g"}'),
 (2, 'Cần câu V5 Plus CC-51 Synthesis tổng hợp', 'can-cau-v5-plus-cc-51-synthesis', 'CC-51', 1, 1,
  'Cần tổng hợp đa năng, bán chạy nhất.', '<p>Cần câu V5 Plus CC-51 Synthesis tổng hợp.</p>',
  734000, 989000, TRUE, FALSE, '{"chatLieu": "Carbon tổng hợp", "doCung": "5H"}'),
 (3, 'Cần Câu Ngư Điếu 5000 Thế Hệ 2 CC-50 Săn Hàng Hạng Nhẹ', 'can-cau-ngu-dieu-5000-the-he-2-cc-50', 'CC-50', 1, 2,
  'Săn hàng hạng nhẹ, thế hệ 2.', '<p>Cần Câu Ngư Điếu 5000 Thế Hệ 2.</p>',
  917000, 1050000, TRUE, FALSE, '{"chatLieu": "Carbon", "doCung": "5H"}'),
 (4, 'Cần Câu Hoả Mã CC-48 Cao Cấp Bản Kỷ Niệm', 'can-cau-hoa-ma-cc-48-ban-ky-niem', 'CC-48', 1, 3,
  'Phiên bản kỷ niệm cao cấp.', '<p>Cần Câu Hoả Mã CC-48 Cao Cấp Bản Kỷ Niệm.</p>',
  1102000, 1400000, TRUE, FALSE, '{"chatLieu": "Carbon 46T", "doCung": "6H"}'),
 (5, 'Thùng câu đài V5 Plus Box KK-69 26L Rice Fishing nâng cấp', 'thung-cau-dai-v5-plus-box-kk-69-26l', 'KK-69', 6, 4,
  'Thùng câu 26 lít, nắp ngồi chắc chắn.', '<p>Thùng câu đài V5 Plus Box KK-69 26L.</p>',
  2235000, 2800000, FALSE, TRUE, '{"dungTich": "26L"}'),
 (6, 'Gác hố đấu NGƯ ĐIẾU PLUS KK-70 RICE FISHING Chuyên Săn Hàng', 'gac-ho-dau-ngu-dieu-plus-kk-70', 'KK-70', 7, 2,
  'Gác hố đấu chuyên săn hàng.', '<p>Gác hố đấu NGƯ ĐIẾU PLUS KK-70.</p>',
  756000, 950000, FALSE, FALSE, NULL),
 (7, 'Dây trục câu PE 8 sợi 100m', 'day-truc-cau-pe-8-soi-100m', 'PE8', 13, 4,
  'Dây PE bện 8 sợi, ít giãn.', '<p>Dây PE 8 sợi dài 100m.</p>',
  120000, NULL, FALSE, TRUE, '{"chieuDai": "100m", "soSoi": 8}'),
 (8, 'Phao câu đài gỗ Balsa Bạc Linh Sam', 'phao-cau-dai-go-balsa-bac-linh-sam', 'PHAO-BALSA', 15, 4,
  'Phao gỗ Balsa nhạy tín hiệu.', '<p>Phao câu đài gỗ Balsa.</p>',
  85000, NULL, FALSE, TRUE, NULL),
 (9, 'Lưỡi câu Iseama không ngạnh', 'luoi-cau-iseama-khong-nganh', 'ISEAMA', 14, 5,
  'Lưỡi thép carbon, không ngạnh, gói 10 chiếc.', '<p>Lưỡi câu Iseama không ngạnh.</p>',
  35000, NULL, FALSE, FALSE, NULL);
ALTER TABLE products AUTO_INCREMENT = 1001;

INSERT INTO product_variants (id, product_id, sku, name, price, compare_at_price, sort_order) VALUES
 (1, 1, 'V5-BF-36', '3m6', 1250000, 1500000, 1),
 (2, 1, 'V5-BF-45', '4m5', 1350000, 1600000, 2),
 (3, 1, 'V5-BF-54', '5m4', 1450000, 1700000, 3),
 (4, 2, 'CC-51-36', '3m6', 734000, 989000, 1),
 (5, 2, 'CC-51-45', '4m5', 834000, 1089000, 2),
 (6, 3, 'CC-50-45', '4m5', 917000, 1050000, 1),
 (7, 3, 'CC-50-54', '5m4', 1017000, 1150000, 2),
 (8, 4, 'CC-48-39', '3m9', 1102000, 1400000, 1),
 (9, 4, 'CC-48-45', '4m5', 1202000, 1500000, 2),
 (10, 5, 'KK-69-26L', '26L', 2235000, 2800000, 1),
 (11, 6, 'KK-70', 'Mặc định', 756000, 950000, 1),
 (12, 7, 'PE8-10', 'PE 1.0', 120000, NULL, 1),
 (13, 7, 'PE8-15', 'PE 1.5', 125000, NULL, 2),
 (14, 7, 'PE8-20', 'PE 2.0', 130000, NULL, 3),
 (15, 8, 'BALSA-1', 'Size 1', 85000, NULL, 1),
 (16, 8, 'BALSA-2', 'Size 2', 90000, NULL, 2),
 (17, 8, 'BALSA-3', 'Size 3', 95000, NULL, 3),
 (18, 9, 'ISEAMA-3', 'Số 3', 35000, NULL, 1),
 (19, 9, 'ISEAMA-4', 'Số 4', 35000, NULL, 2),
 (20, 9, 'ISEAMA-5', 'Số 5', 38000, NULL, 3);
ALTER TABLE product_variants AUTO_INCREMENT = 10001;

INSERT INTO product_images (product_id, url, alt, sort_order, thumbnail) VALUES
 (1, 'https://bizweb.dktcdn.net/thumb/medium/100/436/612/products/can-cau-dai-v5-big-fish-cac-kich-thuoc.jpg?v=1740132741173',
  'Cần câu đài V5 Big Fish các kích thước', 1, TRUE);
