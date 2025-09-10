-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: localhost    Database: eshopdb
-- ------------------------------------------------------
-- Server version	8.0.33

CREATE TABLE `addresses` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int DEFAULT NULL,
  `country_id` int DEFAULT NULL,
  `first_name` varchar(45) NOT NULL,
  `last_name` varchar(45) NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `address_line_1` varchar(64) NOT NULL,
  `address_line_2` varchar(64) DEFAULT NULL,
  `city` varchar(45) NOT NULL,
  `state` varchar(45) NOT NULL,
  `postal_code` varchar(10) NOT NULL,
  `default_address` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKn3sth7s3kur1rafwbbrqqnswt` (`country_id`),
  KEY `FKhrpf5e8dwasvdc5cticysrt2k` (`customer_id`),
  CONSTRAINT `FKhrpf5e8dwasvdc5cticysrt2k` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKn3sth7s3kur1rafwbbrqqnswt` FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `brands` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `logo` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_oce3937d2f4mpfqrycbr0l93m` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `brands_categories` (
  `brand_id` int NOT NULL,
  `category_id` int NOT NULL,
  PRIMARY KEY (`brand_id`,`category_id`),
  KEY `FK6x68tjj3eay19skqlhn7ls6ai` (`category_id`),
  CONSTRAINT `FK58ksmicdguvu4d7b6yglgqvxo` FOREIGN KEY (`brand_id`) REFERENCES `brands` (`id`),
  CONSTRAINT `FK6x68tjj3eay19skqlhn7ls6ai` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cart_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `customer_id` int DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `shipping_cost` float NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKehe6sev71h6jfmfjyeebcu1c2` (`customer_id`),
  KEY `FKqkqmvkmbtiaqn2nfqf25ymfs2` (`product_id`),
  CONSTRAINT `FKehe6sev71h6jfmfjyeebcu1c2` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKqkqmvkmbtiaqn2nfqf25ymfs2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `categories` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `alias` varchar(64) NOT NULL,
  `image` varchar(128) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `parent_id` int DEFAULT NULL,
  `all_parent_ids` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`),
  UNIQUE KEY `idx_category_alias` (`alias`),
  KEY `FKsaok720gsu4u2wrgbk10b5n8d` (`parent_id`),
  CONSTRAINT `FKsaok720gsu4u2wrgbk10b5n8d` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `countries` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `code` varchar(5) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=256 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `currencies` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `symbol` varchar(3) NOT NULL,
  `code` varchar(4) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `customers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(45) NOT NULL,
  `password` varchar(64) NOT NULL,
  `first_name` varchar(45) NOT NULL,
  `last_name` varchar(45) NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `address_line1` varchar(64) NOT NULL,
  `address_line_2` varchar(64) NOT NULL,
  `city` varchar(45) NOT NULL,
  `state` varchar(45) NOT NULL,
  `country_id` int DEFAULT NULL,
  `postal_code` varchar(10) NOT NULL,
  `created_time` datetime(6) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `verification_code` varchar(128) DEFAULT NULL,
  `authentication_type` enum('DATABASE','FACEBOOK','GOOGLE') DEFAULT NULL,
  `reset_password_token` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_rfbvkrffamfql7cjmen8v976v` (`email`),
  KEY `FK7b7p2myt0y31l4nyj1p7sk0b1` (`country_id`),
  CONSTRAINT `FK7b7p2myt0y31l4nyj1p7sk0b1` FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_details` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `quantity` int NOT NULL,
  `unit_price` float NOT NULL,
  `subtotal` float NOT NULL,
  `product_cost` float NOT NULL,
  `shipping_cost` float NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjyu2qbqt8gnvno9oe9j2s2ldk` (`order_id`),
  KEY `FK4q98utpd73imf4yhttm3w0eax` (`product_id`),
  CONSTRAINT `FK4q98utpd73imf4yhttm3w0eax` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKjyu2qbqt8gnvno9oe9j2s2ldk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_track` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `updated_time` datetime(6) DEFAULT NULL,
  `notes` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK31jv1s212kajfn3kk1ksmnyfl` (`order_id`),
  CONSTRAINT `FK31jv1s212kajfn3kk1ksmnyfl` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int DEFAULT NULL,
  `order_time` datetime(6) DEFAULT NULL,
  `payment_method` enum('COD','CREDIT_CARD') DEFAULT NULL,
  `product_cost` float NOT NULL,
  `shipping_cost` float NOT NULL,
  `subtotal` float NOT NULL,
  `tax` float NOT NULL,
  `total` float NOT NULL,
  `status` enum('CANCELLED','DELIVERED','NEW','PACKAGED','PAID','PICKED','PROCESSING','REFUNDED','RETURNED','RETURN_REQUESTED','SHIPPING') DEFAULT NULL,
  `first_name` varchar(45) NOT NULL,
  `last_name` varchar(45) NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `address_line_1` varchar(64) NOT NULL,
  `address_line_2` varchar(64) DEFAULT NULL,
  `city` varchar(45) NOT NULL,
  `state` varchar(45) NOT NULL,
  `postal_code` varchar(10) NOT NULL,
  `country` varchar(45) NOT NULL,
  `deliver_days` int NOT NULL,
  `deliver_date` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpxtb8awmi0dk6smoh2vp1litg` (`customer_id`),
  CONSTRAINT `FKpxtb8awmi0dk6smoh2vp1litg` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `product_details` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  `product_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnfvvq3meg4ha3u1bju9k4is3r` (`product_id`),
  CONSTRAINT `FKnfvvq3meg4ha3u1bju9k4is3r` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=698 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `product_images` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `product_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqnq71xsohugpqwf3c9gxmsuy` (`product_id`),
  CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=395 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `products` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL,
  `alias` varchar(256) NOT NULL,
  `short_description` varchar(1512) NOT NULL,
  `full_description` varchar(4096) NOT NULL,
  `main_image` varchar(255) NOT NULL,
  `created_time` datetime(6) DEFAULT NULL,
  `updated_time` datetime(6) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `in_stock` bit(1) DEFAULT NULL,
  `cost` float NOT NULL,
  `price` float NOT NULL,
  `discount_percent` float DEFAULT NULL,
  `length` float NOT NULL,
  `width` float NOT NULL,
  `height` float NOT NULL,
  `weight` float NOT NULL,
  `brand_id` int DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `average_rating` float NOT NULL,
  `review_count` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8qwq8q3hk7cxkp9gruxupnif5` (`alias`),
  UNIQUE KEY `UK_o61fmio5yukmmiqgnxf8pnavn` (`name`),
  KEY `FKa3a4mpsfdf4d2y6r8ra3sc8mv` (`brand_id`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  FULLTEXT KEY `products_FTS` (`name`,`short_description`,`full_description`),
  CONSTRAINT `FKa3a4mpsfdf4d2y6r8ra3sc8mv` FOREIGN KEY (`brand_id`) REFERENCES `brands` (`id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reviews` (
  `id` int NOT NULL AUTO_INCREMENT,
  `comment` varchar(300) NOT NULL,
  `head_line` varchar(126) NOT NULL,
  `rating` int NOT NULL,
  `review_time` datetime(6) NOT NULL,
  `customer_id` int DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `votes` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4sm0k8kw740iyuex3vwwv1etu` (`customer_id`),
  KEY `FKpl51cejpw4gy5swfar8br9ngi` (`product_id`),
  CONSTRAINT `FK4sm0k8kw740iyuex3vwwv1etu` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKpl51cejpw4gy5swfar8br9ngi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reviews_votes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `votes` int NOT NULL,
  `customer_id` int DEFAULT NULL,
  `review_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKckuygkph4k9llo624gn30lxvy` (`customer_id`),
  KEY `FKosupda11xqkvo80r77evmwrey` (`review_id`),
  CONSTRAINT `FKckuygkph4k9llo624gn30lxvy` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKosupda11xqkvo80r77evmwrey` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(40) NOT NULL,
  `description` varchar(140) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `settings` (
  `key` varchar(128) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `category` enum('CURRENCY','GENERAL','MAIL_SERVER','MAIL_TEMPLATES','PAYMENT') NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `shipping_rates` (
  `id` int NOT NULL AUTO_INCREMENT,
  `country_id` int DEFAULT NULL,
  `state` varchar(45) NOT NULL,
  `rate` float NOT NULL,
  `days` int NOT NULL,
  `cod_supported` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKef7sfgeybt3xn13nlt2j6sljw` (`country_id`),
  CONSTRAINT `FKef7sfgeybt3xn13nlt2j6sljw` FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `states` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `country_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKskkdphjml9vjlrqn4m5hi251y` (`country_id`),
  CONSTRAINT `FKskkdphjml9vjlrqn4m5hi251y` FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=309 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(128) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `first_name` varchar(45) NOT NULL,
  `last_name` varchar(45) NOT NULL,
  `password` varchar(64) NOT NULL,
  `photos` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users_roles` (
  `user_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKj6m8fwv7oqv74fcehir1a9ffy` (`role_id`),
  CONSTRAINT `FK2o0jvgh89lemvvo17cbqvdxaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj6m8fwv7oqv74fcehir1a9ffy` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- patition

DROP TABLE IF EXISTS orders_partitioned;
CREATE TABLE orders_partitioned LIKE orders;

-- 2) Sửa PK để đáp ứng quy tắc partition (thêm order_time vào PK)
ALTER TABLE orders_partitioned
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (id, order_time);

-- 3) Thêm partition theo NĂM
ALTER TABLE orders_partitioned
PARTITION BY RANGE COLUMNS(order_time) (
  PARTITION p2023 VALUES LESS THAN ('2024-01-01'),
  PARTITION p2024 VALUES LESS THAN ('2025-01-01'),
  PARTITION p2025 VALUES LESS THAN ('2026-01-01'),
  PARTITION pmax  VALUES LESS THAN (MAXVALUE)
);

-- 4) (Tùy chọn) copy dữ liệu từ bảng thật
-- LƯU Ý: hàng có order_time NULL sẽ rơi vào partition đầu tiên
INSERT INTO orders_partitioned SELECT * FROM orders;

-- 5) Truy vấn có PRUNING (chỉ đọc p2025)
EXPLAIN PARTITIONS
SELECT DATE(order_time) d, SUM(total) revenue
FROM orders_partitioned
WHERE order_time >= '2025-01-01' AND order_time < '2026-01-01'
GROUP BY DATE(order_time)
ORDER BY d;

-- Bảo trì:
-- Thêm năm mới 2026
ALTER TABLE orders_partitioned
REORGANIZE PARTITION pmax INTO (
  PARTITION p2026 VALUES LESS THAN ('2027-01-01'),
  PARTITION pmax  VALUES LESS THAN (MAXVALUE)
);

-- Xóa nhanh toàn bộ dữ liệu năm 2023
ALTER TABLE orders_partitioned DROP PARTITION p2023;

--    
SELECT id, name, price
FROM products
ORDER BY price
LIMIT 20 OFFSET 0;
CREATE INDEX idx_products_cat_price ON products(category_id, price, id, name);
EXPLAIN SELECT id, name, price
FROM products WHERE category_id = 5 ORDER BY price LIMIT 20;

SELECT id, name
FROM products
WHERE MATCH(name, short_description) AGAINST ('iphone' IN NATURAL LANGUAGE MODE)
LIMIT 10;



CREATE INDEX idx_orders_customer_time ON orders(customer_id, order_time);
CREATE INDEX idx_orders_status_time   ON orders(status, order_time);
CREATE INDEX idx_customers_email_phone ON customers(email, phone_number);

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_search_orders $$
CREATE PROCEDURE sp_search_orders (
  IN  p_from       DATETIME,
  IN  p_to         DATETIME,
  IN  p_status_csv VARCHAR(200),   -- ví dụ: 'PAID,SHIPPING'
  IN  p_customer_keyword VARCHAR(100), -- lọc email/phone/first/last name (LIKE)
  IN  p_min_total  DECIMAL(12,2),
  IN  p_max_total  DECIMAL(12,2),
  IN  p_sort_col   VARCHAR(30),    -- 'order_time' | 'total' | 'status'
  IN  p_sort_dir   VARCHAR(4),     -- 'ASC' | 'DESC'
  IN  p_page       INT,            -- bắt đầu từ 1
  IN  p_page_size  INT,            -- ví dụ 20/50
  OUT p_total_rows BIGINT
)
BEGIN
  DECLARE v_sql   TEXT;
  DECLARE v_cnt   TEXT;
  DECLARE v_offset INT;

  -- chuẩn hoá tham số
  IF p_page IS NULL OR p_page < 1 THEN SET p_page = 1; END IF;
  IF p_page_size IS NULL OR p_page_size < 1 THEN SET p_page_size = 20; END IF;
  SET v_offset = (p_page - 1) * p_page_size;

  -- whitelist sort
  IF p_sort_col NOT IN ('order_time','total','status') THEN SET p_sort_col = 'order_time'; END IF;
  IF UPPER(p_sort_dir) NOT IN ('ASC','DESC') THEN SET p_sort_dir = 'DESC'; END IF;

  -- base FROM/JOIN
  SET v_sql = CONCAT(
    'SELECT o.id, o.order_time, o.total, o.status, ',
    'c.id AS customer_id, c.email, c.first_name, c.last_name ',
    'FROM orders o JOIN customers c ON c.id = o.customer_id WHERE 1=1 '
  );

  -- điều kiện động
  IF p_from IS NOT NULL THEN
    SET v_sql = CONCAT(v_sql, ' AND o.order_time >= ? ');
  END IF;

  IF p_to IS NOT NULL THEN
    SET v_sql = CONCAT(v_sql, ' AND o.order_time < ? ');
  END IF;

  IF p_status_csv IS NOT NULL AND p_status_csv <> '' THEN
    SET v_sql = CONCAT(v_sql, ' AND FIND_IN_SET(o.status, ?) > 0 ');
  END IF;

  IF p_customer_keyword IS NOT NULL AND p_customer_keyword <> '' THEN
    SET v_sql = CONCAT(v_sql,
      ' AND (c.email LIKE ? OR c.phone_number LIKE ? ',
      ' OR c.first_name LIKE ? OR c.last_name LIKE ?) ');
  END IF;

  IF p_min_total IS NOT NULL THEN
    SET v_sql = CONCAT(v_sql, ' AND o.total >= ? ');
  END IF;

  IF p_max_total IS NOT NULL THEN
    SET v_sql = CONCAT(v_sql, ' AND o.total <= ? ');
  END IF;

  -- order + limit
  SET v_sql = CONCAT(v_sql, ' ORDER BY ', p_sort_col, ' ', p_sort_dir, ' LIMIT ? OFFSET ? ');

  -- bản đếm
  SET v_cnt = REPLACE(v_sql,
    SUBSTRING_INDEX(v_sql, 'FROM', 1),
    'SELECT COUNT(*) '
  );
  -- v_cnt hiện giờ là 'SELECT COUNT(*) FROM ... LIMIT ? OFFSET ?'
  -- bỏ phần LIMIT/OFFSET khỏi v_cnt
  SET v_cnt = SUBSTRING(v_cnt, 1, LOCATE(' ORDER BY ', v_cnt) - 1);

  -- Chuẩn bị tham số cho PREPARE: xây cùng thứ tự
  SET @a_from        = p_from;
  SET @a_to          = p_to;
  SET @a_status      = p_status_csv;
  SET @a_kw          = CONCAT('%', p_customer_keyword, '%');
  SET @a_min_total   = p_min_total;
  SET @a_max_total   = p_max_total;
  SET @a_limit       = p_page_size;
  SET @a_offset      = v_offset;

  -- Build danh sách tham số động (thứ tự phải khớp với số ? trong v_sql)
  SET @sql := v_sql;
  PREPARE stmt FROM @sql;

  -- vì số ? khác nhau tuỳ điều kiện, ta bind “linh hoạt”:
  -- Kỹ thuật: tạo tạm bảng kết quả để tránh phải RE-BIND nhiều lần
  DROP TEMPORARY TABLE IF EXISTS _tmp_orders;
  CREATE TEMPORARY TABLE _tmp_orders AS SELECT 1 AS dummy WHERE 1=0;

  SET @param_idx = 0;

  -- Chạy bản COUNT trước
  SET @sqlc := v_cnt;
  PREPARE stmtc FROM @sqlc;

  -- Dựng chuỗi EXECUTE theo điều kiện
  SET @dyn = 'EXECUTE stmt USING ';
  SET @dync = 'EXECUTE stmtc USING ';
  SET @sep := '';

  -- helper: macro-like
  -- Thêm tham số vào 2 câu (nếu cùng điều kiện)
  IF p_from IS NOT NULL THEN
    SET @dyn  = CONCAT(@dyn, @sep, '@a_from');  SET @sep := ',';
    SET @dync = CONCAT(@dync, '@a_from'); 
  END IF;
  IF p_to IS NOT NULL THEN
    SET @dyn  = CONCAT(@dyn, @sep, '@a_to'); 
    SET @dync = CONCAT(@dync, ',@a_to');
    SET @sep := ',';
  END IF;
  IF p_status_csv IS NOT NULL AND p_status_csv <> '' THEN
    SET @dyn  = CONCAT(@dyn, @sep, '@a_status'); 
    SET @dync = CONCAT(@dync, ',@a_status');
    SET @sep := ',';
  END IF;
  IF p_customer_keyword IS NOT NULL AND p_customer_keyword <> '' THEN
    SET @dyn  = CONCAT(@dyn, @sep, '@a_kw', ',@a_kw', ',@a_kw', ',@a_kw');
    SET @dync = CONCAT(@dync, ',@a_kw,@a_kw,@a_kw,@a_kw');
    SET @sep := ',';
  END IF;
  IF p_min_total IS NOT NULL THEN
    SET @dyn  = CONCAT(@dyn, @sep, '@a_min_total'); 
    SET @dync = CONCAT(@dync, ',@a_min_total');
    SET @sep := ',';
  END IF;
  IF p_max_total IS NOT NULL THEN
    SET @dyn  = CONCAT(@dyn, @sep, '@a_max_total'); 
    SET @dync = CONCAT(@dync, ',@a_max_total');
    SET @sep := ',';
  END IF;

  -- cuối cùng thêm LIMIT/OFFSET cho @dyn (query dữ liệu), bản COUNT không cần
  SET @dyn = CONCAT(@dyn, @sep, '@a_limit', ',@a_offset');

  -- chạy COUNT → @cnt
  SET @dync = CONCAT(@dync, ';');
  PREPARE x FROM @dync;
  EXECUTE x;
  DEALLOCATE PREPARE x;

  -- lấy kết quả COUNT(*) vào biến user @cnt
  -- mẹo: sửa v_cnt để trả về COUNT(*) AS cnt (đã làm), nên EXECUTE stmtc sẽ trả 1 resultset → ta bắt bằng:
  -- (không thể INTO local var trực tiếp), dùng SELECT ... INTO user var
  -- Đơn giản hơn: wrap thêm: SET @sqlc2 := CONCAT('SELECT COUNT(*) FROM (', SUBSTRING(@sql, LOCATE('FROM', @sql)),') t');
  -- Nhưng ở trên ta đã build sẵn v_cnt → chạy lại để lấy @cnt:
  -- Cách nhẹ: mở một con trỏ thì dài dòng, nên ở đây trả về TOTAL ở resultset 2 cho gọn.
  -- => Trả 2 resultsets: (1) data page, (2) SELECT p_total_rows AS total
  -- Chạy query dữ liệu:
  SET @dyn = CONCAT(@dyn, ';');
  PREPARE y FROM @dyn;
  EXECUTE y;
  DEALLOCATE PREPARE y;

  -- resultset 2: tổng số dòng (đếm bằng câu COUNT tương tự)
  -- Thực thi câu COUNT một lần nữa nhưng trả trực tiếp
  EXECUTE stmtc;

  DEALLOCATE PREPARE stmtc;
  DEALLOCATE PREPARE stmt;
END $$

CALL sp_search_orders('2025-01-01','2025-12-31','PAID,SHIPPING','gmail',NULL,1000,'order_time','DESC',1,20,@total);
/* Resultset #1: page dữ liệu; Resultset #2: 1 dòng COUNT(*) */

