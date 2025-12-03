package com.ducnhu.catalog.repository;

import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.minio.projection.ProductExportView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("""
            select p.id as id,
                   p.name as name,
                   p.alias as alias,
                   p.price as price,
                   (p.price * (1 - p.discountPercent / 100.0)) as discountPrice,
                   p.inStock as inStock,
                   p.averageRating as averageRating,
                   p.reviewCount as reviewCount,
                   p.createdTime as createdTime,
                   c.name as categoryName
            from Product p
            left join p.category c
            where (:afterId is null or p.id > :afterId)
            order by p.id asc
            """)
    List<ProductExportView> scanAfterId(@Param("afterId") Integer afterId, Pageable pageable);

    /**
     * Lấy min/max id để chia range nếu muốn chạy nhiều worker
     */
    @Query("select min(p.id) from Product p")
    Integer minId();

    @Query("select max(p.id) from Product p")
    Integer maxId();

    List<Product> findByIdGreaterThan(Integer id, Pageable pageable);

    @Query("""
                SELECT p
                FROM Product p
                WHERE p.enabled = true
                  AND (p.category.id = :categoryId
                       OR p.category.allParentIDs LIKE CONCAT('%-', :categoryId, '-%'))
                ORDER BY p.name ASC
            """)
    List<Product> listByCategory(@Param("categoryId") Integer categoryId);

    @Query(
            value = """
                        SELECT p
                        FROM Product p
                        WHERE p.enabled = true
                          AND (:catId IS NULL
                               OR p.category.id = :catId
                               OR p.category.allParentIDs LIKE CONCAT('%-', :catId, '-%'))
                    """,
            countQuery = """
                        SELECT COUNT(p.id)
                        FROM Product p
                        WHERE p.enabled = true
                          AND (:catId IS NULL
                               OR p.category.id = :catId
                               OR p.category.allParentIDs LIKE CONCAT('%-', :catId, '-%'))
                    """
    )
    Page<Product> findPageByCategory(@Param("catId") Integer categoryId, Pageable pageable);

    @Query("""
                SELECT p
                FROM Product p
                WHERE p.enabled = true
                  AND p.alias = :alias ORDER BY p.alias ASC
            """)
    Product findByAlias(@Param("alias") String alias);

    @Query(value = """
            SELECT p.id
            FROM Product p
            WHERE p.enabled = true
              AND ( :minReviewCount IS NULL OR p.reviewCount >= :minReviewCount ) 
              AND ( :categoryId IS NULL
                    OR p.category.id = :categoryId
                    OR p.category.allParentIDs LIKE CONCAT('%-', :categoryId, '-%') )
            """,
            countQuery = """
                    SELECT COUNT(p.id)
                    FROM Product p
                    WHERE p.enabled = true
                      AND ( :minReviewCount IS NULL OR p.reviewCount >= :minReviewCount ) 
                      AND ( :categoryId IS NULL
                            OR p.category.id = :categoryId
                            OR p.category.allParentIDs LIKE CONCAT('%-', :categoryId, '-%') )
                    """)
    Page<Integer> findIdsByCategory(@Param("categoryId") Integer categoryId,
                                    @Param("minReviewCount") Integer minReviewCount,
                                    Pageable pageable);

    @Query(
            value = """
                    SELECT p.id
                    FROM Product p
                    WHERE p.enabled = true
                      AND ( :keyword IS NULL
                            OR LOWER(p.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(p.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) )
                    """,
            countQuery = """
                    SELECT COUNT(p.id)
                    FROM Product p
                    WHERE p.enabled = true
                      AND ( :keyword IS NULL
                            OR LOWER(p.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(p.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) )
                    """
    )
    Page<Integer> findIdsByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdIn(@Param("ids") List<Integer> ids);
}
