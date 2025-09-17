package com.orm.repository;

import com.orm.entity.User;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class UserSpecs {
    private UserSpecs() {}

    /** Tìm theo tên: match trên firstName hoặc lastName; hỗ trợ nhiều token ("nguyen an"). */
    public static Specification<User> nameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return cb.conjunction();
            List<String> tokens = Arrays.stream(name.trim().toLowerCase().split("//s+"))
                    .filter(s -> !s.isBlank()).toList();
            if (tokens.isEmpty()) return cb.conjunction();

            return cb.and(tokens.stream().map(token -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), "%" + token + "%"),
                    cb.like(cb.lower(root.get("lastName")),  "%" + token + "%")
            )).toArray(Predicate[]::new)
            );
        };
    }

    /** Lọc theo danh sách role name (không phân biệt hoa/thường). */
    public static Specification<User> hasRoleNames(Collection<String> roleNames) {
        return (root, query, cb) -> {
            if (roleNames == null || roleNames.isEmpty()) return cb.conjunction();
            List<String> normalized = roleNames.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isBlank()).toList();
            if (normalized.isEmpty()) return cb.conjunction();

            var roles = root.join("roles", JoinType.LEFT);
            query.distinct(true);
            return cb.lower(roles.get("name")).in(normalized);
        };
    }
}
