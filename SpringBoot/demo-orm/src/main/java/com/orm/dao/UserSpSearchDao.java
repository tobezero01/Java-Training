package com.orm.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


@Repository
@RequiredArgsConstructor
public class UserSpSearchDao {
    private final JdbcTemplate jdbc;

    public Map<String, Object> searchByNameAndRole(
            String name,
            List<String> roleNames,
            int page, int size,
            String sort, String dir
    ) {
        String csv = (roleNames == null || roleNames.isEmpty())
                ? null
                : String.join(",", roleNames);

        SimpleJdbcCall call = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_search_users_by_name_role");

        MapSqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_name", name)
                .addValue("p_role_names_csv", csv)
                .addValue("p_page", page)
                .addValue("p_size", size)
                .addValue("p_sort", sort)
                .addValue("p_dir", dir);

        // xong ra có 2 result-sets: #result-set-1 (data), #result-set-2 (count)
        return call.execute(in);
    }
}
