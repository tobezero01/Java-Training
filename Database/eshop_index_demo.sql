
-- ================================================================
-- ESHOP Index Demo Script (MySQL 8+)
-- Safe to run: uses demo_* tables only. No changes to your real tables.
-- ================================================================

-- You can copy/paste parts into MySQL Workbench or run the whole file.

-- Improve recursion limit for seeding (if needed)
SET SESSION cte_max_recursion_depth = 50000;

-- ================================================================
-- 0) Clean up old demo
-- ================================================================
DROP TABLE IF EXISTS demo_products;
DROP TABLE IF EXISTS demo_customers;

-- ================================================================
-- 1) Demo table: demo_products (derived from ESHOP products)
-- ================================================================
CREATE TABLE demo_products (
  id             INT PRIMARY KEY,
  category_id    INT NOT NULL,
  brand_id       INT NOT NULL,
  name           VARCHAR(255) NOT NULL,
  short_description TEXT NULL,
  price          DECIMAL(10,2) NOT NULL,
  updated_time   DATETIME(6) NOT NULL,
  alias          VARCHAR(255) NULL
) ENGINE=InnoDB;

-- Seed ~3000 rows deterministically using a recursive CTE
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n+1 FROM seq WHERE n < 3000
)
INSERT INTO demo_products (id, category_id, brand_id, name, short_description, price, updated_time, alias)
SELECT
  n AS id,
  1 + (n % 50)        AS category_id,       -- 50 categories
  1 + (n % 100)       AS brand_id,          -- 100 brands
  CONCAT('Product ', n, IF(n % 7 = 0, ' iPhone', IF(n % 11 = 0, ' Galaxy', ''))) AS name,
  CONCAT('This is product ', n, ' with features like camera, battery, iphone, galaxy, phone, case, charger') AS short_description,
  ROUND( (n % 1000) * 1.23, 2) AS price,
  TIMESTAMP('2025-01-01 00:00:00') + INTERVAL (n % 240) DAY AS updated_time,
  CONCAT('alias-', n, IF(n % 5 = 0, '-iphone', ''))
FROM seq;

-- ================================================================
-- 2) Baseline query (no index) → expect table scan / filesort
-- ================================================================
-- Try to list products in a category ordered by price (classic shop listing)
EXPLAIN
SELECT id, name, price
FROM demo_products
WHERE category_id = 5
ORDER BY price
LIMIT 20;

-- ================================================================
-- 3) Composite index to support filter + sort
-- ================================================================
CREATE INDEX idx_demo_cat_price ON demo_products(category_id, price);

-- Re-check the plan → expect use of idx_demo_cat_price and no filesort
EXPLAIN
SELECT id, name, price
FROM demo_products
WHERE category_id = 5
ORDER BY price
LIMIT 20;

-- ================================================================
-- 4) Covering index trick: include selected columns in the index key
-- NOTE: MySQL has no INCLUDE; we extend the key. Use judiciously.
-- ================================================================
DROP INDEX idx_demo_cat_price ON demo_products;
CREATE INDEX idx_demo_cat_price_cover ON demo_products(category_id, price, id, name);

-- Re-check → Extra should show "Using index" (covering)
EXPLAIN
SELECT id, name, price
FROM demo_products
WHERE category_id = 5
ORDER BY price
LIMIT 20;

-- ================================================================
-- 5) Multi-filter: brand + category (composite vs index-merge)
-- ================================================================
EXPLAIN
SELECT id
FROM demo_products
WHERE brand_id = 10 AND category_id = 5;

-- Composite for common combo
CREATE INDEX idx_demo_brand_category ON demo_products(brand_id, category_id);

-- Re-check
EXPLAIN
SELECT id
FROM demo_products
WHERE brand_id = 10 AND category_id = 5;

-- ================================================================
-- 6) TEXT search: LIKE vs FULLTEXT
-- ================================================================
-- LIKE '%iphone%' can't use BTREE → scan
EXPLAIN
SELECT id, name
FROM demo_products
WHERE name LIKE '%iphone%'
LIMIT 10;

-- Add FULLTEXT on name + short_description
ALTER TABLE demo_products
  ADD FULLTEXT INDEX ftx_demo_name_desc (name, short_description);

-- Switch query to MATCH AGAINST (uses fulltext index)
EXPLAIN
SELECT id, name
FROM demo_products
WHERE MATCH(name, short_description) AGAINST ('iphone' IN NATURAL LANGUAGE MODE)
LIMIT 10;

-- Boolean mode example: require iphone-like prefix and galaxy
EXPLAIN
SELECT id, name
FROM demo_products
WHERE MATCH(name, short_description) AGAINST ('+iphone* +galaxy' IN BOOLEAN MODE)
LIMIT 10;

-- ================================================================
-- 7) Functional / generated-column index for case-insensitive email
-- ================================================================
CREATE TABLE demo_customers (
  id INT PRIMARY KEY,
  email VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

INSERT INTO demo_customers VALUES
  (1, 'Alice@Example.com'),
  (2, 'bob@example.com');

-- Add generated lower-case column + unique index
ALTER TABLE demo_customers
  ADD COLUMN email_lower VARCHAR(255) GENERATED ALWAYS AS (LOWER(email)) STORED,
  ADD UNIQUE INDEX uq_demo_email_lower (email_lower);

-- Test: inserting duplicate email with different case should fail
-- Expect: Duplicate entry error
-- INSERT INTO demo_customers(id, email) VALUES (3, 'alice@example.com');

-- ================================================================
-- 8) Prefix index for long alias with prefix search
-- ================================================================
-- Show plan with no dedicated index
EXPLAIN
SELECT id
FROM demo_products
WHERE alias LIKE 'alias-1%'
LIMIT 10;

-- Add prefix index (32 chars)
CREATE INDEX idx_demo_alias_prefix ON demo_products(alias(32));

-- Re-check plan
EXPLAIN
SELECT id
FROM demo_products
WHERE alias LIKE 'alias-1%'
LIMIT 10;

-- ================================================================
-- 9) Invisible index A/B testing
-- ================================================================
ALTER TABLE demo_products ALTER INDEX idx_demo_cat_price_cover INVISIBLE;

-- Expect the plan to avoid using the invisible index (may degrade)
EXPLAIN
SELECT id, name, price
FROM demo_products
WHERE category_id = 5
ORDER BY price
LIMIT 20;

-- Make it visible again
ALTER TABLE demo_products ALTER INDEX idx_demo_cat_price_cover VISIBLE;

EXPLAIN
SELECT id, name, price
FROM demo_products
WHERE category_id = 5
ORDER BY price
LIMIT 20;

-- ================================================================
-- End of demo
-- ================================================================
