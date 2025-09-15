package com.orm.service;

import com.orm.dao.UserJdbcDao;
import com.orm.dao.UserNamedDao;
import com.orm.dao.UserSpSearchDao;
import com.orm.dto.PageResponse;
import com.orm.dto.RoleBriefDto;
import com.orm.dto.UserRoleRow;
import com.orm.dto.UserWithRolesDto;
import com.orm.repository.UserRepository;
import com.orm.repository.UserRoleView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final UserJdbcDao userJdbcDao;
    private final UserNamedDao userNamedDao;
    private final UserSpSearchDao searchDao;

    @SuppressWarnings("unchecked")
    public PageResponse<UserWithRolesDto> search(
            String name, List<String> roleNames,
            int page, int size, String sort, String dir
    ) {
        Map<String,Object> out = searchDao.searchByNameAndRole(name, roleNames, page, size, sort, dir);

        List<Map<String,Object>> data  = (List<Map<String,Object>>) out.get("#result-set-1");
        List<Map<String,Object>> meta  = (List<Map<String,Object>>) out.get("#result-set-2");
        long total = 0;
        if (meta != null && !meta.isEmpty()) {
            Object v = meta.get(0).get("total_count");
            if (v instanceof Number n) total = n.longValue();
            else if (v != null) total = Long.parseLong(v.toString());
        }

        List<UserWithRolesDto> content = new ArrayList<>();
        if (data != null) {
            for (Map<String,Object> row : data) {
                String rolesCsv = Objects.toString(row.get("rolesCsv"), "");
                List<RoleBriefDto> roles = rolesCsv.isBlank() ? List.of() : Arrays.stream(rolesCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(RoleBriefDto::new)
                        .toList();

                content.add(UserWithRolesDto.builder()
                        .id((Integer) row.get("id"))
                        .email(Objects.toString(row.get("email"), null))
                        .firstName(Objects.toString(row.get("first_name"), null))
                        .lastName(Objects.toString(row.get("last_name"), null))
                        .enabled(((Number) row.get("enabled")).intValue() != 0)
                        .roles(roles)
                        .build());
            }
        }

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 1;

        return PageResponse.<UserWithRolesDto>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .sort(sort)
                .dir(dir)
                .build();
    }

    public PageResponse<UserRoleRow> pageJPA(
            int page, int size, String sort, String dir,
            List<Integer> roleIds, List<String> roleNames
    ) {
        Sort s = Sort.by("desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                (sort == null || sort.isBlank()) ? "id" : normalizeJpaSort(sort));
        Pageable pageable = PageRequest.of(page, size, s);

        Page<UserRoleView> p;
        if (roleIds != null && !roleIds.isEmpty()) {
            p = userRepo.pageUserRoleByRoleIds(roleIds, pageable);
        } else if (roleNames != null && !roleNames.isEmpty()) {
            List<String> lower = roleNames.stream().filter(Objects::nonNull)
                    .map(s2 -> s2.trim().toLowerCase()).toList();
            p = userRepo.pageUserRoleByRoleNames(lower, pageable);
        } else {
            p = userRepo.pageUserRole(pageable);
        }

        List<UserRoleRow> rows = p.getContent().stream()
                .map(v -> new UserRoleRow(
                        v.getUserId(), v.getEmail(), v.getFirstName(), v.getLastName(),
                        v.getRoleId(), v.getRoleName()))
                .toList();

        return PageResponse.<UserRoleRow>builder()
                .content(rows)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .sort(sort)
                .dir(dir)
                .build();
    }

    // 2) JdbcTemplate
    public PageResponse<UserRoleRow> pageJdbc(int page, int size, String sort, String dir) {
        List<UserRoleRow> content = userJdbcDao.findPage(page, size, sort, dir);
        long total = userJdbcDao.countAllJoinRows();
        int totalPages = (int) Math.ceil((double) total / size);
        return PageResponse.<UserRoleRow>builder()
                .content(content).page(page).size(size)
                .totalElements(total).totalPages(totalPages)
                .sort(sort).dir(dir)
                .build();
    }

    // 3) NamedParameterJdbcTemplate
    public PageResponse<UserRoleRow> pageNamed(int page, int size, String sort, String dir) {
        List<UserRoleRow> content = userNamedDao.findPage(page, size, sort, dir);
        long total = userNamedDao.countAllJoinRows();
        int totalPages = (int) Math.ceil((double) total / size);
        return PageResponse.<UserRoleRow>builder()
                .content(content).page(page).size(size)
                .totalElements(total).totalPages(totalPages)
                .sort(sort).dir(dir)
                .build();
    }

    private String normalizeJpaSort(String sort) {
        return switch (sort) {
            case "email" -> "email";
            case "firstName" -> "firstName";
            case "lastName" -> "lastName";
            default -> "id";
        };
    }
}
