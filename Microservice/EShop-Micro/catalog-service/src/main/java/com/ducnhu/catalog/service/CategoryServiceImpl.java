package com.ducnhu.catalog.service;

import com.ducnhu.catalog.dto.CategoryDTO;
import com.ducnhu.catalog.dto.CategoryNodeDTO;
import com.ducnhu.catalog.entity.Category;
import com.ducnhu.catalog.mapper.CategoryMapper;
import com.ducnhu.catalog.repository.CategoryRepository;
import com.ducnhu.common.cache.CacheKey;
import com.ducnhu.common.cache.CacheTtl;
import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.common.exception.CategoryNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final RedisCacheService redisCacheService;


    @Override
    public List<CategoryDTO> listNoChildrenCategories() {
        return redisCacheService.getOrLoad(
                CacheKey.catLeaves(),
                new TypeReference<List<CategoryDTO>>() {
                },
                CacheTtl.CATEGORY,
                () -> {
                    List<Category> enabled = categoryRepository.findAllEnabled();
                    List<Category> res = new ArrayList<>();
                    for (Category category : enabled) {
                        Set<Category> ch = category.getChildren();
                        if (ch == null || ch.isEmpty()) res.add(category);
                    }
                    return res.stream().map(CategoryMapper :: toDto).toList();
                }
        );
    }


    @Override
    public CategoryDTO getCategory(String alias) {
        CategoryDTO dto = redisCacheService.getOrLoad(
                CacheKey.catByAlias(alias),
                CategoryDTO.class,
                CacheTtl.CATEGORY,
                () -> {
                    var category = categoryRepository.findByAliasEnabled(alias);
                    return (category == null) ? null : CategoryMapper.toDto(category);
                });

        return dto;
    }

    @Override
    public List<CategoryNodeDTO> listCategoryTree() {

        return redisCacheService.getOrLoad(
                CacheKey.catTree(),
                new TypeReference<List<CategoryNodeDTO>>() {},
                CacheTtl.CATEGORY,
                () -> {
                    //  loader: build cây an toàn concurrency
                    List<Category> categories = categoryRepository.findAllEnabled(); // đã enabled

                    // map id -> node dto (song song OK vì không chia sẻ cấu trúc)
                    ConcurrentMap<Integer, CategoryNodeDTO> nodes = categories
                            .parallelStream()
                            .collect(Collectors.toConcurrentMap(
                                    c -> c.getId(),
                                    c -> CategoryNodeDTO.leaf(
                                            c.getId(), c.getName(), c.getAlias(), c.getImage()
                                    )
                            ));

                    //  GHÉP CHA-CON tuần tự (tránh add vào list từ nhiều thread)
                    List<CategoryNodeDTO> roots = new ArrayList<>();
                    for (Category category : categories) {
                        CategoryNodeDTO node = nodes.get(category.getId());
                        Category parent = category.getParent();
                        if (parent == null) {
                            roots.add(node);
                        } else {
                            CategoryNodeDTO parentNode = nodes.get(parent.getId());
                            if (parentNode != null) {
                                parentNode.children().add(node);   // children là ArrayList -> tuần tự an toàn
                            } else {
                                roots.add(node);
                            }
                        }
                    }

                    sortTreeByName(roots);
                    return roots;
                }
        );
    }

    private void sortTreeByName(List<CategoryNodeDTO> list) {
        list.sort(Comparator.comparing(CategoryNodeDTO::name, String.CASE_INSENSITIVE_ORDER));
        for (CategoryNodeDTO node : list) {
            sortTreeByName(node.children());
        }
    }

    @Override
    public List<CategoryDTO> listTopLevelParents() {
//        return categoryRepository.findAllTopLevel().stream().map(CategoryMapper :: toDto).toList();
        return redisCacheService.getOrLoad(
                CacheKey.catTop(),
                new TypeReference<List<CategoryDTO>>() {},
                CacheTtl.CATEGORY,
                () -> categoryRepository.findAllTopLevel().stream().map(CategoryMapper :: toDto).toList()
        );
    }

    @Override
    public List<CategoryDTO> getAncestorsPath(Integer id) throws CategoryNotFoundException {
//        Category cur = categoryRepository.getByIdOrThrow(id);
//        LinkedList<Category> path = new LinkedList<>();
//        while (cur != null) {
//            path.addFirst(cur);
//            cur = cur.getParent();
//        }
//        return path;
        return redisCacheService.getOrLoad(
                CacheKey.catPath(id),
                new TypeReference<List<CategoryDTO>>() {},
                CacheTtl.CATEGORY,
                () -> {
                    Category cur = null;
                    try {
                        cur = categoryRepository.getByIdOrThrow(id);
                    } catch (CategoryNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    LinkedList<Category> path = new LinkedList<>();
                    while (cur != null) {
                        path.addFirst(cur);
                        cur = cur.getParent();
                    }
                    return path.stream().map(CategoryMapper :: toDto).toList();
                }
        );
    }
}
