INSERT INTO articles (title, slug, summary, content, tags, status, author_name, published_at) VALUES
 ('Câu Cá Trắm Cỏ: Cách Câu Hiệu Quả, Dễ Dính Cá Cho Người Mới', 'cau-ca-tram-co-cach-cau-hieu-qua',
  'Hiểu tập tính ăn, thời điểm hoạt động và chọn dụng cụ phù hợp để câu trắm cỏ dễ dính.',
  '<p>Cá trắm cỏ ăn mạnh vào sáng sớm và chiều muộn. Nên dùng cần câu đài 4m5 trở lên, dây trục PE 2.0 và lưỡi số 6 đến 8.</p>',
  '["kinh-nghiem", "tram-co"]', 'PUBLISHED', 'DatDo Fishing', NOW(6) - INTERVAL 6 DAY),
 ('Cá Trôi Là Cá Gì? Cách Câu Cá Trôi Sao Cho Hiệu Quả', 'ca-troi-la-ca-gi-cach-cau-ca-troi',
  'Cá trôi là loài cá nước ngọt quen thuộc ở ao hồ, sông ngòi và hồ câu dịch vụ.',
  '<p>Cá trôi ăn nhẹ, nên dùng phao nhạy và thẻo mảnh. Mồi cám có mùi chua nhẹ cho hiệu quả tốt.</p>',
  '["kinh-nghiem", "ca-troi"]', 'PUBLISHED', 'DatDo Fishing', NOW(6) - INTERVAL 15 DAY),
 ('Lựa Chọn Cần Câu Cá Mè Phù Hợp: Rẻ, Đẹp, Chất Lượng', 'lua-chon-can-cau-ca-me-phu-hop',
  'Cần câu cá mè phù hợp giúp thao tác dễ dàng và kiểm soát cá tốt hơn.',
  '<p>Câu mè nên chọn cần dài 5m4 đến 6m3, độ cứng 5H đến 6H để chịu tải khi cá giãy.</p>',
  '["chon-can", "ca-me"]', 'PUBLISHED', 'DatDo Fishing', NOW(6) - INTERVAL 27 DAY),
 ('Top Các Cần Câu Cá Rô Đồng Giá Rẻ, Chính Hãng', 'top-can-cau-ca-ro-dong-gia-re',
  'Cần câu cá rô đồng phù hợp giúp mỗi buổi đi câu nhẹ nhàng và chủ động hơn.',
  '<p>Cá rô đồng nhỏ, nên dùng cần ngắn 2m7 đến 3m6, nhẹ tay và lưỡi số 3 đến 4.</p>',
  '["chon-can", "ca-ro"]', 'PUBLISHED', 'DatDo Fishing', NOW(6) - INTERVAL 41 DAY);

INSERT INTO pages (slug, title, content) VALUES
 ('chinh-sach-doi-tra', 'Chính sách đổi trả', '<p>Đổi trả trong 7 ngày với sản phẩm lỗi do nhà sản xuất, còn nguyên tem mác.</p>'),
 ('chinh-sach-bao-hanh', 'Chính sách bảo hành', '<p>Cần và máy câu bảo hành 12 tháng. Liên hệ hotline để được hỗ trợ.</p>'),
 ('chinh-sach-van-chuyen', 'Chính sách vận chuyển', '<p>Miễn phí giao hàng cho đơn từ 500.000đ. Đơn nhỏ hơn phí 30.000đ.</p>');
