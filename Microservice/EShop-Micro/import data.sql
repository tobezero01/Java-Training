CREATE DATABASE IF NOT EXISTS catalog_eshopdb;
CREATE DATABASE IF NOT EXISTS cart_eshopdb;
CREATE DATABASE IF NOT EXISTS customer_eshopdb;
CREATE DATABASE IF NOT EXISTS checkout_eshopdb;
CREATE DATABASE IF NOT EXISTS payment_eshopdb;
CREATE DATABASE IF NOT EXISTS order_eshopdb;
CREATE DATABASE IF NOT EXISTS shipping_eshopdb;
CREATE DATABASE IF NOT EXISTS settings_eshopdb;
CREATE DATABASE IF NOT EXISTS customer_address_eshopdb;

-- CÁC CỘT CÓ Ở catalog_eshopdb.products NHƯNG KHÔNG CÓ Ở eshopdb.products
SELECT c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE
FROM information_schema.columns c
WHERE c.table_schema='catalog_eshopdb' AND c.table_name='products'
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns m
    WHERE m.table_schema='eshopdb' AND m.table_name='products'
      AND m.COLUMN_NAME=c.COLUMN_NAME
);

SELECT m.COLUMN_NAME, m.COLUMN_TYPE, m.IS_NULLABLE
FROM information_schema.columns m
WHERE m.table_schema='eshopdb' AND m.table_name='products'
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema='catalog_eshopdb' AND c.table_name='products'
      AND c.COLUMN_NAME=m.COLUMN_NAME
);
SET FOREIGN_KEY_CHECKS=0;

-- 2.1 BRANDS
INSERT INTO catalog_eshopdb.brands (id, name, logo)
SELECT b.id, b.name, b.logo
FROM eshopdb.brands b
ON DUPLICATE KEY UPDATE name=VALUES(name), logo=VALUES(logo);

-- 2.2 CATEGORIES (tạo alias nếu thiếu; giữ parent_id)
INSERT INTO catalog_eshopdb.categories (id, name, alias, image, enabled, parent_id, all_parent_ids)
SELECT  c.id,
        c.name,
        /* alias: nếu trống -> tạo từ name + '-' + id để đảm bảo unique, tránh lỗi dấu tiếng Việt */
        CASE WHEN c.alias IS NULL OR c.alias=''
             THEN CONCAT(LOWER(REPLACE(REPLACE(REPLACE(c.name,' ','-'),'/', '-'),'\\','-')),'-',c.id)
             ELSE c.alias END,
        c.image,
        IFNULL(c.enabled,1),
        c.parent_id,
        c.all_parent_ids
FROM eshopdb.categories c
ON DUPLICATE KEY UPDATE
  name=VALUES(name), alias=VALUES(alias), image=VALUES(image),
  enabled=VALUES(enabled), parent_id=VALUES(parent_id), all_parent_ids=VALUE(all_parent_ids);
  
  
-- 2.3 BRANDS_CATEGORIES (bảng liên kết)
INSERT IGNORE INTO catalog_eshopdb.brands_categories (brand_id, category_id)
SELECT bc.brand_id, bc.category_id
FROM eshopdb.brands_categories bc;

-- 2.4 PRODUCTS
/* Mapping quan trọng:
   - discount_percent (mới) = ROUND((1 - discount_price/price)*100,2) nếu monolith có discount_price>0
     nếu monolith đã có discount_percent thì ưu tiên giữ; nếu cả hai không có thì =0
   - alias: bắt buộc, nếu thiếu -> tạo từ name + '-' + id (tránh trùng)
   - main_image: nếu null -> lấy ảnh đầu tiên từ product_images
*/
INSERT INTO catalog_eshopdb.products
( id, name, alias, short_description, full_description,
  created_time, updated_time, enabled, in_stock,
  cost, price, discount_percent,
  length, width, height, weight,
  category_id, brand_id, main_image,
  review_count, average_rating )
SELECT
  p.id, p.name,
  CASE
    WHEN p.alias IS NULL OR p.alias = ''
      THEN CONCAT(
             LOWER(REPLACE(REPLACE(REPLACE(p.name,' ','-'),'/', '-'),'\\','-')),
             '-', p.id
           )
    ELSE p.alias
  END,
  p.short_description, p.full_description,
  p.created_time, p.updated_time,
  IFNULL(p.enabled, 1),
  IFNULL(p.in_stock, 1),
  IFNULL(p.cost, 0), IFNULL(p.price, 0),

  /* Chỉ dùng discount_percent có sẵn; null thì về 0 */
  COALESCE(p.discount_percent, 0),

  IFNULL(p.length, 0), IFNULL(p.width, 0),
  IFNULL(p.height, 0), IFNULL(p.weight, 0),
  p.category_id, p.brand_id,
  COALESCE(
    p.main_image,
    (SELECT i.name
     FROM eshopdb.product_images i
     WHERE i.product_id = p.id
     ORDER BY i.id
     LIMIT 1),
    'no-image.jpg'
  ),
  IFNULL(p.review_count, 0), IFNULL(p.average_rating, 0)
FROM eshopdb.products p
ON DUPLICATE KEY UPDATE
  name              = VALUES(name),
  alias             = VALUES(alias),
  short_description = VALUES(short_description),
  full_description  = VALUES(full_description),
  created_time      = VALUES(created_time),
  updated_time      = VALUES(updated_time),
  enabled           = VALUES(enabled),
  in_stock          = VALUES(in_stock),
  cost              = VALUES(cost),
  price             = VALUES(price),
  discount_percent  = VALUES(discount_percent),
  length            = VALUES(length),
  width             = VALUES(width),
  height            = VALUES(height),
  weight            = VALUES(weight),
  category_id       = VALUES(category_id),
  brand_id          = VALUES(brand_id),
  main_image        = VALUES(main_image),
  review_count      = VALUES(review_count),
  average_rating    = VALUES(average_rating);

-- 2.5 PRODUCT IMAGES
INSERT INTO catalog_eshopdb.product_images (id, name, product_id)
SELECT i.id, i.name, i.product_id
FROM eshopdb.product_images i
ON DUPLICATE KEY UPDATE name=VALUES(name), product_id=VALUES(product_id);

-- 2.6 PRODUCT DETAILS
INSERT INTO catalog_eshopdb.product_details (id, name, value, product_id)
SELECT d.id, d.name, d.value, d.product_id
FROM eshopdb.product_details d
ON DUPLICATE KEY UPDATE name=VALUES(name), value=VALUES(value), product_id=VALUES(product_id);

SET FOREIGN_KEY_CHECKS=1;


SET @now = NOW();
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO customer_eshopdb.countries
( id, name, code )
SELECT
  c.id,
  TRIM(c.name),
  UPPER(TRIM(COALESCE(c.code,'')))
FROM eshopdb.countries c
ON DUPLICATE KEY UPDATE
  name         = VALUES(name),
  code         = VALUES(code)
;

INSERT INTO customer_eshopdb.states
( id, name, country_id )
SELECT
  s.id,
  TRIM(s.name),
  s.country_id
FROM eshopdb.states s
ON DUPLICATE KEY UPDATE
  name       = VALUES(name),
  country_id = VALUES(country_id)
;

INSERT INTO customer_eshopdb.addresses
( id, customer_id, first_name, last_name, phone_number,
  address_line_1, address_line_2, city, state, postal_code,
  country_id, country_name, default_address )
SELECT
  a.id, a.customer_id, a.first_name, a.last_name, a.phone_number,
  a.address_line_1, a.address_line_2, a.city,

  -- Nếu a.state rỗng thì thử map theo bảng states để lấp tên state
  COALESCE(NULLIF(TRIM(a.state), ''), st_by_name.name) AS state,

  a.postal_code,

  -- Giữ nguyên country_id từ nguồn
  a.country_id,

  -- Lấy tên country theo id (nếu không có thì để rỗng)
  COALESCE(ctry_by_id.name, '') AS country_name,

  IFNULL(a.default_address, 0)
FROM eshopdb.addresses a
LEFT JOIN customer_eshopdb.countries ctry_by_id
  ON ctry_by_id.id = a.country_id
LEFT JOIN customer_eshopdb.states st_by_name
  ON st_by_name.country_id = a.country_id
 AND UPPER(TRIM(st_by_name.name)) = UPPER(TRIM(a.state))
ON DUPLICATE KEY UPDATE
  customer_id     = VALUES(customer_id),
  first_name      = VALUES(first_name),
  last_name       = VALUES(last_name),
  phone_number    = VALUES(phone_number),
  address_line_1  = VALUES(address_line_1),
  address_line_2  = VALUES(address_line_2),
  city            = VALUES(city),
  state           = VALUES(state),
  postal_code     = VALUES(postal_code),
  country_id      = VALUES(country_id),
  country_name    = VALUES(country_name),
  default_address = VALUES(default_address);
  
INSERT INTO eshop_shippingdb.shipping_rates
(country_id, state, rate, days, cod_supported)
SELECT
  s.country_id,
  LEFT(COALESCE(NULLIF(TRIM(s.state), ''), ''), 45) AS state,
  CAST(s.rate AS DECIMAL(10,2)) AS rate,
  IFNULL(s.days, 0) AS days,
  IFNULL(s.cod_supported, 0) AS cod_supported
FROM eshopdb.shipping_rates s
WHERE s.country_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  rate = VALUES(rate),
  days = VALUES(days),
  cod_supported = VALUES(cod_supported);
SET FOREIGN_KEY_CHECKS = 1;

WITH RECURSIVE cat AS (
  -- Root: không có parent
  SELECT id, parent_id, CAST('-' AS CHAR(255)) AS path
  FROM catalog_eshopdb.categories
  WHERE parent_id IS NULL

  UNION ALL
  -- Con: cộng dồn đường đi + parent_id
  SELECT c.id, c.parent_id, CONCAT(cat.path, c.parent_id, '-')
  FROM catalog_eshopdb.categories c
  JOIN cat ON c.parent_id = cat.id
)
UPDATE catalog_eshopdb.categories t
JOIN cat ON cat.id = t.id
SET t.all_parent_ids = cat.path
WHERE IFNULL(t.all_parent_ids,'') = '';
