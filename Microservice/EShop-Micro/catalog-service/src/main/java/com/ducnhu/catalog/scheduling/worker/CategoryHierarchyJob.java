package com.ducnhu.catalog.scheduling.worker;


import com.ducnhu.catalog.entity.Category;
import com.ducnhu.catalog.repository.CategoryRepository;
import com.ducnhu.common.cache.CacheKey;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CategoryHierarchyJob {

    private final CategoryRepository categoryRepository;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "${jobs.categoryHierarchy.cron}")
    @SchedulerLock(name = "categoryHierarchyJob")
    public void rebuildAllParentIds() {
        List<Category> all = categoryRepository.findAllForHierarchy();

        // build map id->parent
        Map<Integer, Integer> parentMap = new HashMap<>();
        all.forEach(c -> parentMap.put(c.getId(), c.getParent() ==null ? null : c.getParent().getId()));

        for (Category c : all) {
            String newPath = buildPath(c.getId(), parentMap);
            if (!newPath.equals(c.getAllParentIDs())) {
                categoryRepository.updateAllParentIds(c.getId(), newPath);
                redisTemplate.opsForValue().increment(CacheKey.catVer(c.getId()));
            }
        }
    }

    private String buildPath(Integer id, Map<Integer, Integer> parentMap) {
        StringBuilder stringBuilder = new StringBuilder("-");
        Integer cur = parentMap.get(id);
        while (cur != null) {
            stringBuilder.append(cur).append("-");
            cur = parentMap.get(cur);
        }
        return stringBuilder.toString();
    }
}
