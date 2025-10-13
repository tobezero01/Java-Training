package com.ducnhu.catalog.mapper;


import com.ducnhu.catalog.dto.CategoryDTO;
import com.ducnhu.catalog.entity.Category;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryDTO toDto(Category c) {
        return new CategoryDTO(
                c.getId(),
                c.getName(),
                c.getAlias(),
                c.getImagePath(),
                c.getChildren() != null && !c.getChildren().isEmpty()
        );
    }
}
