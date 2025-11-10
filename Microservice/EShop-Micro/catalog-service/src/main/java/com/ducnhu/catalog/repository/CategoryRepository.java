package com.ducnhu.catalog.repository;

import com.ducnhu.catalog.entity.Category;
import com.ducnhu.common.exception.CategoryNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    @Query("select c from Category c where c.enabled = true order by c.name asc")
    List<Category> findAllEnabled();

    @Query("select c from Category c where c.enabled = true and c.alias = ?1")
    Category findByAliasEnabled(String alias);

    @Query("select c from Category c where c.enabled = true and c.parent is null order by c.name asc")
    List<Category> findAllTopLevel();

    @Query("SELECT c FROM Category c")
    List<Category> findAllForHierarchy();

    @Modifying
    @Query("UPDATE Category c SET c.allParentIDs = :path WHERE c.id = :id")
    void updateAllParentIds(@Param("id") Integer id, @Param("path") String path);

    default Category getByIdOrThrow(Integer id) throws CategoryNotFoundException {
        return findById(id).orElseThrow(() -> new CategoryNotFoundException(
                "Category not found with id " + id));
    }
}

