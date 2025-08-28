use eshopdb;

-- lấy sản phẩm kèm brand vs cate (INNER JOIN cơ bản)
SELECT p.id, p.name AS product_name, b.name AS brand, c.name AS category
FROM products p
JOIN brands b     ON b.id = p.brand_id
JOIN categories c ON c.id = p.category_id;

-- Mọi khách hàng và tổng số/tổng tiền đơn (LEFT JOIN để không bỏ sót khách
SELECT c.id
FROM customers c
LEFT JOIN orders o ON o.cus