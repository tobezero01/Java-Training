package com.ducnhu.catalog.service;


import com.ducnhu.catalog.dto.CategoryDTO;
import com.ducnhu.catalog.dto.CategoryNodeDTO;
import com.ducnhu.common.exception.CategoryNotFoundException;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> listTopLevelParents();
    List<CategoryDTO> listNoChildrenCategories();
    CategoryDTO getCategory(String alias) throws CategoryNotFoundException;
    List<CategoryNodeDTO> listCategoryTree();
    List<CategoryDTO> getAncestorsPath(Integer id) throws CategoryNotFoundException;
}
