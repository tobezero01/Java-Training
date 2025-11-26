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
            select p.id as id, p.name as name, p.alias as alias, p.price as price,
                      p.discountPrice as discountPrice, p.inStock as inStock,
                      p.averageRating as averageRating, p.reviewCount as reviewCount,
                      p.createdTime as createdTime, c.name as categoryName
            from Product p left join p.category c
            where (:afterId is null or p.id > :afterId)
            order by p.id asc
            """)
    List<ProductExportView> scanAfterId(@Param("afterId") Integer afterId, Pageable pageable);

    /** Lấy min/max id để chia range nếu muốn chạy nhiều worker */
    @Query("select min(p.id) from Product p")
    Integer minId();

    @Query("select max(p.id) from Product p")
    Integer maxId();

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

//    @Query(value = """
//                SELECT *
//                FROM Products p
//                WHERE p.enabled = 1
//                  AND MATCH (name, short_description, full_description)
//                      AGAINST (?1)
//            """, nativeQuery = true)
//    Page<Product> search(String keyword, Pageable pageable);

//    @Modifying(flushAutomatically = true, clearAutomatically = true)
//    @Query("""
//                UPDATE Product p
//                SET p.averageRating = COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId), 0),
//                    p.reviewCount   = (SELECT COUNT(r.id) FROM Review r WHERE r.product.id = :productId)
//                WHERE p.id = :productId
//            """)
//    void updateReviewCountAndAverageRating(@Param("productId") Integer productId);

//    @Query(
//            value = """
//        SELECT DISTINCT p
//        FROM OrderDetail od
//          JOIN od.order o
//          JOIN od.product p
//        WHERE o.customer.id = :customerId
//          AND o.status IN :statuses
//          AND p.enabled = true
//        """,
//            countQuery = """
//        SELECT COUNT(DISTINCT p.id)
//        FROM OrderDetail od
//          JOIN od.order o
//          JOIN od.product p
//        WHERE o.customer.id = :customerId
//          AND o.status IN :statuses
//          AND p.enabled = true
//        """
//    )
//    Page<Product> findPurchasedByCustomerAndStatuses(@Param("customerId") Integer customerId,
//                                                     @Param("statuses") Collection<OrderStatus> statuses,
//                                                     Pageable pageable);

//    @Query("""
//        SELECT (COUNT(od) > 0)
//        FROM OrderDetail od
//          JOIN od.order o
//        WHERE o.customer.id = :customerId
//          AND EXISTS (
//            SELECT 1
//            FROM OrderTrack ot
//            WHERE ot.order = o
//              AND ot.status IN :statuses
//          )
//    """)
//    boolean existsAnyCompletedPurchaseFromTrack(@Param("customerId") Integer customerId,
//                                                @Param("statuses") Collection<OrderStatus> statuses);

    /* === FIXED: lấy IDs theo category và tất cả cấp con qua allParentIDs === */
    @Query(
            value = """
                    SELECT p.id
                    FROM Product p
                    WHERE p.enabled = true
                      AND ( :categoryId IS NULL
                            OR p.category.id = :categoryId
                            OR p.category.allParentIDs LIKE CONCAT('%-', :categoryId, '-%') )
                    """,
            countQuery = """
                    SELECT COUNT(p.id)
                    FROM Product p
                    WHERE p.enabled = true
                      AND ( :categoryId IS NULL
                            OR p.category.id = :categoryId
                            OR p.category.allParentIDs LIKE CONCAT('%-', :categoryId, '-%') )
                    """
    )
    Page<Integer> findIdsByCategory(@Param("categoryId") Integer categoryId, Pageable pageable);

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
