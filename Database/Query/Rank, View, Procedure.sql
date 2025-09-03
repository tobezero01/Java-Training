SELECT 
    p.id, p.name,
    SUM(od.subtotal) as revenue,
    RANK() OVER (ORDER BY SUM(od.subtotal) DESC) AS rk 
FROM order_details od 
JOIN products p ON p.id = od.product_id
GROUP BY p.id, p.name 
ORDER BY revenue DESC;
