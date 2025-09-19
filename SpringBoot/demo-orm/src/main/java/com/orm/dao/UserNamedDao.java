package com.orm.dao;

import com.orm.dto.UserRoleRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserNamedDao {
    private final NamedParameterJdbcTemplate named;

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
        order by %s
        limit :limit offset :offset
        """.formatted(order);

        Map<String, Object> map = Map.of("limit", size, "offset", offset);

        return named.query(sql, map, (rs, rowNum) ->
                new UserRoleRow(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getInt("role_id"),
                        rs.getString("role_name")
                )
        );
    }

    public long countAllJoinRows() {
        String sql = """
        select count(*) 
        from users u
        join users_roles ur on ur.user_id = u.id
        join roles r on r.id = ur.role_id
        """;
        return named.getJdbcTemplate().queryForObject(sql, Long.class);
    }
}
