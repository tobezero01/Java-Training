-- -----------------------------------------------------
-- Schema full-stack-ecommerce
-- -----------------------------------------------------

USE `full-stack-ecommerce`;
ALTER TABLE customer
  ADD COLUMN dateOfBirth DATE NULL AFTER email;


-- Tạo nguồn số 0..9
WITH RECURSIVE d AS (SELECT 0 AS n UNION ALL SELECT n+1 FROM d WHERE n < 9)
SELECT 1; -- dummy để ngắt; (nếu server cấm WITH ở multi statements thì bỏ phần này)

-- Cách không dùng CTE (ổn định hơn):
-- Bảng số 0..9 qua SELECT UNION ALL
SELECT 1; -- dummy

-- Tạo 0..99,999 (5 lần CROSS JOIN)
INSERT INTO customer (first_name, last_name, email, dateOfBirth)
SELECT
  -- Chọn first_name/last_name “ngẫu nhiên” theo n
  ELT(1 + MOD(n, 12),
      'An','Bình','Châu','Dung','Giang','Hoa','Khánh','Linh','Minh','Phúc','Quân','Trang')           AS first_name,
  ELT(1 + MOD(n DIV 3, 12),
      'Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ')               AS last_name,
  -- Email duy nhất
  CONCAT(
    LOWER(REPLACE(ELT(1 + MOD(n, 12),
      'An','Bình','Châu','Dung','Giang','Hoa','Khánh','Linh','Minh','Phúc','Quân','Trang'),' ','')),
    '.',
    LOWER(REPLACE(ELT(1 + MOD(n DIV 3, 12),
      'Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ'),' ','')),
    n, '@example.com'
  ) AS email,
  -- Ngày sinh phân bố đều từ 1960-01-01 (mod để trải đều ~ 46 năm)
  DATE_ADD('1960-01-01', INTERVAL (n MOD 17000) DAY) AS dateOfBirth
FROM (
  SELECT  a.n
        + b.n*10
        + c.n*100
        + d.n*1000
        + e.n*10000      AS n
  FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
  CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
  CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
  CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
  CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
) t
WHERE n BETWEEN 1 AND 50000;   -- đổi thành 15000 nếu bạn muốn vừa đủ >10k

select count(id) from customer;

DROP INDEX idx_customer_lastname_dob ON customer;
DROP INDEX idx_customer_dob_lastname ON customer;

-- 3.1. last_name → dateOfBirth
CREATE INDEX idx_customer_lastname_dob
  ON customer (last_name, dateOfBirth);
  
EXPLAIN ANALYZE
SELECT id, first_name, last_name, dateOfBirth
FROM customer
WHERE last_name = 'Trần'
  AND dateOfBirth BETWEEN '1960-01-01' AND '1965-12-31';
-- Kỳ vọng dùng idx_customer_lastname_dob



-- 3.2. dateOfBirth → last_name
CREATE INDEX idx_customer_dob_lastname
  ON customer (dateOfBirth, last_name);

EXPLAIN ANALYZE
SELECT id, first_name, last_name, dateOfBirth
FROM customer
WHERE dateOfBirth BETWEEN '1960-01-01' AND '1965-12-31'
  AND last_name = 'Trần';
-- Kỳ vọng dùng idx_customer_dob_lastname

EXPLAIN ANALYZE
SELECT id, first_name, last_name, dateOfBirth
FROM customer
WHERE dateOfBirth BETWEEN '1960-01-01' AND '1965-12-31';
  


DROP TABLE IF EXISTS customer_p;

CREATE TABLE customer_p (
  id           BIGINT NOT NULL,
  first_name   VARCHAR(255),
  last_name    VARCHAR(255),
  email        VARCHAR(255),
  dateOfBirth  DATE NOT NULL,
  PRIMARY KEY (id, dateOfBirth),                           -- bắt buộc gồm cột partition
  KEY idx_p_lastname_dob (last_name, dateOfBirth),
  KEY idx_p_dob_lastname (dateOfBirth, last_name)
)
PARTITION BY RANGE COLUMNS (dateOfBirth) (
	PARTITION p1950 VALUES LESS THAN ('1960-01-01'),
  PARTITION p1960 VALUES LESS THAN ('1970-01-01'),
  PARTITION p1970 VALUES LESS THAN ('1980-01-01'),
  PARTITION p1980 VALUES LESS THAN ('1990-01-01'),
  PARTITION p1990 VALUES LESS THAN ('2000-01-01'),
  PARTITION p2000 VALUES LESS THAN ('2010-01-01'),
  PARTITION p2010 VALUES LESS THAN ('2020-01-01'),
  PARTITION pmax  VALUES LESS THAN (MAXVALUE)
);

INSERT INTO customer_p (id, first_name, last_name, email, dateOfBirth)
SELECT id, first_name, last_name, email, dateOfBirth
FROM customer
WHERE dateOfBirth IS NOT NULL;  -- đề phòng NULL

EXPLAIN
SELECT id, first_name, last_name
FROM customer_p
WHERE dateOfBirth BETWEEN '1990-01-01' AND '1990-12-31'
  AND last_name = 'Lê';
-- Kỳ vọng cột `partitions` chỉ ra p1990 (đã prune)

EXPLAIN
SELECT *
FROM customer_p
WHERE dateOfBirth = '1975-05-20' AND last_name = 'Nguyễn';



-- ------------------------------
--
-- Prep work
--
SET FOREIGN_KEY_CHECKS=0;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `customer`;
DROP TABLE IF EXISTS `address`;
SET FOREIGN_KEY_CHECKS=1;

--
-- Table structure for table `address`
--
CREATE TABLE `address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `zip_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `customer`
--
CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `orders`
--
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_tracking_number` varchar(255) DEFAULT NULL,
  `total_price` decimal(19,2) DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `billing_address_id` bigint DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `shipping_address_id` bigint DEFAULT NULL,
  `status` varchar(128) DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  `last_updated` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_billing_address_id` (`billing_address_id`),
  UNIQUE KEY `UK_shipping_address_id` (`shipping_address_id`),
  KEY `K_customer_id` (`customer_id`),
  CONSTRAINT `FK_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `FK_billing_address_id` FOREIGN KEY (`billing_address_id`) REFERENCES `address` (`id`),
  CONSTRAINT `FK_shipping_address_id` FOREIGN KEY (`shipping_address_id`) REFERENCES `address` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `order_items`
--
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(255) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `unit_price` decimal(19,2) DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `K_order_id` (`order_id`),
  CONSTRAINT `FK_order_id` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FK_product_id` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
