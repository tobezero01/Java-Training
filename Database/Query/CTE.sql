use eshopdb;

-- Doanh thu theo sản phẩm trong khoảng thời gian
WITH rev AS (
  SELECT od.product_id, SUM(od.subtotal) AS revenue
  FROM orders o
  JOIN order_details od ON od.order_id = o.id
  WHERE o.order_time >= '2024-01-01' AND o.order_time < '2025-02-01'
  GROUP BY od.product_id
)
SELECT p.id, p.name, b.name AS brand, c.name AS category, r.revenue
FROM rev r
JOIN products p  ON p.id = r.product_id
LEFT JOIN brands b ON b.id = p.brand_id
LEFT JOIN categories c ON c.id = p.category_id
ORDER BY r.revenue DESC;

-- Doanh thu theo category 
WITH rev_product AS (
  SELECT od.product_id, SUM(od.subtotal) AS revenue
  FROM orders o
  JOIN order_details od ON od.order_id = o.id
  GROUP BY od.product_id
),
rev_category AS (
  SELECT p.category_id, SUM(rp.revenue) AS revenue
  FROM rev_product rp
  JOIN products p ON p.id = rp.product_id
  GROUP BY p.category_id
)
SELECT c.id, c.name, rc.revenue
FROM rev_category rc
JOIN categories c ON c.id = rc.category_id
ORDER BY rc.revenue DESC;

--Xếp hạng khách hàng theo tổng chi tiêu (có đồng hạng)
WITH spend AS (
    SELECT o.customer_id, SUM(o.total) AS total_spent
    FROM orders o 
    GROUP BY o.customer_id
),
 ranked AS (
    SELECT s.*,
        RANK() OVER (ORDER BY s.total_spent DESC) AS rnk, 
        DENSE_RANK() OVER (ORDER BY s.total_spent DESC) AS drnk
    FROM spend s
)
SELECT r.rnk, r.drnk, c.last_name, c.email, r.total_spent
FROM ranked r
JOIN customers c ON c.id = r.customer_id
ORDER BY r.rnk, c.email;