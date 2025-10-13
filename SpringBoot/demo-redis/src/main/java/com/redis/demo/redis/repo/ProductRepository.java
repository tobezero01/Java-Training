package com.redis.demo.redis.repo;// src/main/java/com/eshop/product/repository/ProductRepository.java

import com.redis.demo.redis.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Lấy product kèm images & details (JOIN FETCH) để render trang chi tiết
    @Query("""
           select distinct p from Product p
           left join fetch p.images
           left join fetch p.details
           where p.id = :id and p.enabled = true
           """)
    Optional<Product> findActiveProductWithMedia(@Param("id") Integer id);

    // Lấy danh sách ID liên quan cùng category (loại trừ chính nó)
    @Query("""
           select p.id from Product p
           where p.enabled = true and p.category.id = :catId and p.id <> :id
           order by p.createdTime desc
           """)
    List<Integer> findRelatedIds(@Param("catId") Integer catId,
                                 @Param("id") Integer id,
                                 Pageable pageable);

    @Query("select p.id from Product p where p.enabled = true")
    List<Integer> findAllActiveIds();

    @Query("""
       select p.id from Product p
       where p.enabled = true and p.category.id = :catId
       order by p.createdTime desc, p.id desc
       """)
    List<Integer> findActiveIdsByCategoryOrderCreated(@Param("catId") Integer catId, Pageable pageable);

    @Query("select p from Product p where p.enabled = true and p.id in :ids")
    List<Product> findActiveByIds(@Param("ids") List<Integer> ids);

    @Query("""
       select distinct p.category.id from Product p
       where p.enabled = true and p.category.id is not null
       """)
    List<Integer> findAllActiveCategoryIds();
}
