package com.orm.dao;

import com.orm.dto.UserRoleRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserJdbcDao {
    private final JdbcTemplate jdbc;

    private static final Map<String, String> SORT_MAP = Map.of(
            "id", "u.id",
            "email", "u.email",
            "firstName", "u.first_name",
            "lastName", "u.last_name"
    );

    private String safeOrder(String sort, String dir) {
        String col = SORT_MAP.getOrDefault(sort, "u.id");
        String direction = "desc".equalsIgnoreCase(dir) ? "desc" : "asc";
        return col + " " + direction;
    }

    public List<UserRoleRow> findPage(int page, int size, String sort, String dir) {
        int offset = page * size;
        String order = safeOrder(sort, dir);
        String sql = """
        select u.id as user_id, u.email, u.first_name, u.last_name,
               r.id as role_id, r.name as role_name
        from users u
        join users_roles ur on ur.user_id = u.id
        join roles r on r.id = ur.role_id
        order by %s limit ? offset ?
        """.formatted(order);

        return jdbc.query(sql, rs -> {
            List<UserRoleRow> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new UserRoleRow(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getInt("role_id"),
                        rs.getString("role_name")
                ));
            }
            return out;
        }, size, offset);
    }

    public long countAllJoinRows() {
        String sql = """
                select count(*) 
                from users u
                join users_roles ur on ur.user_id = u.id
                join roles r on r.id = ur.role_id
                """;
        return jdbc.queryForObject(sql, Long.class);
    }
}
