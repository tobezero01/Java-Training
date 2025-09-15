use eshopdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_search_users_by_name_role $$
CREATE PROCEDURE sp_search_users_by_name_role(
    IN p_name              VARCHAR(200),
    IN p_role_names_csv    VARCHAR(2000),
    IN p_page              INT,
    IN p_size              INT,
    IN p_sort              VARCHAR(20),
    IN p_dir               VARCHAR(4)
)
BEGIN
    DECLARE v_where     TEXT DEFAULT ' WHERE 1=1 ';
    DECLARE v_order_col VARCHAR(32);
    DECLARE v_dir       VARCHAR(4);

    -- tìm theo tên
    IF p_name IS NOT NULL AND LENGTH(TRIM(p_name)) > 0 THEN
        SET v_where = CONCAT(
            v_where,
            ' AND (LOWER(u.first_name) LIKE CONCAT("%", LOWER(', QUOTE(p_name), '), "%")',
            '  OR  LOWER(u.last_name)  LIKE CONCAT("%", LOWER(', QUOTE(p_name), '), "%"))'
        );
    END IF;

    -- lọc theo roleName
    IF p_role_names_csv IS NOT NULL AND LENGTH(TRIM(p_role_names_csv)) > 0 THEN
        /* so khớp bằng FIND_IN_SET(LOWER(r.name), 'admin,editor') */
        SET v_where = CONCAT(
            v_where,
            ' AND FIND_IN_SET(LOWER(r.name), ', QUOTE(LOWER(p_role_names_csv)), ') > 0'
        );
    END IF;

    SET v_order_col = CASE LOWER(p_sort)
        WHEN 'email'      THEN 'u.email'
        WHEN 'first_name' THEN 'u.first_name'
        WHEN 'last_name'  THEN 'u.last_name'
        ELSE 'u.id'
    END;

    SET v_dir = CASE WHEN LOWER(p_dir) = 'desc' THEN 'DESC' ELSE 'ASC' END;

    -- user- role
    SET @sql_data = CONCAT(
        'SELECT u.id, u.email, u.first_name, u.last_name, CAST(u.enabled AS UNSIGNED) AS enabled,',
        '       GROUP_CONCAT(DISTINCT r.name ORDER BY r.name SEPARATOR '','') AS rolesCsv ',
        'FROM users u ',
        'LEFT JOIN users_roles ur ON ur.user_id = u.id ',
        'LEFT JOIN roles r        ON r.id = ur.role_id ',
        v_where,
        ' GROUP BY u.id ',
        ' ORDER BY ', v_order_col, ' ', v_dir,
        ' LIMIT ', p_size, ' OFFSET ', (p_page * p_size)
    );

    -- count 
    SET @sql_count = CONCAT(
        'SELECT COUNT(DISTINCT u.id) AS total_count ',
        'FROM users u ',
        'LEFT JOIN users_roles ur ON ur.user_id = u.id ',
        'LEFT JOIN roles r        ON r.id = ur.role_id ',
        v_where
    );

    -- 2 res
    PREPARE s1 FROM @sql_data;  EXECUTE s1;  DEALLOCATE PREPARE s1;
    PREPARE s2 FROM @sql_count; EXECUTE s2; DEALLOCATE PREPARE s2;
END $$

DELIMITER ;

CALL sp_search_users_by_name_role('Nhu', 'Admin', 0, 5, 'email', 'asc');

