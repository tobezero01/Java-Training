package com.orm.controller;

import com.orm.dto.PageResponse;
import com.orm.dto.UserRoleRow;
import com.orm.dto.UserWithRolesDto;
import com.orm.service.UserService;
import com.orm.service.UserSpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    private final UserSpecService specService;

    // JPA + JOIN (Projection)
    // GET /v1/users/jpa-join?page=0&size=5&sort=email&dir=asc
    @GetMapping("/jpa-join")
    public PageResponse<UserRoleRow> pageJpa(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(required = false) List<String> roleId,
            @RequestParam(required = false) List<String> roleName
    ) {
        var roleIds = parseIntList(roleId);
        var roleNames = parseStringList(roleName);
        return service.pageJPA(page, size, sort, dir, roleIds, roleNames);
    }

    private static List<Integer> parseIntList(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }

    private static List<String> parseStringList(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }


    // GET /v1/users/jdbc-join?page=0&size=5&sort=firstName&dir=desc
    @GetMapping("/jdbc-join")
    public PageResponse<UserRoleRow> pageJdbc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir
    ) {
        return service.pageJdbc(page, size, sort, dir);
    }


    // GET /v1/users/named-join?page=0&size=5&sort=lastName&dir=asc
    @GetMapping("/named-join")
    public PageResponse<UserRoleRow> pageNamed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir
    ) {
        return service.pageNamed(page, size, sort, dir);
    }



    // Ví dụ:
    // GET /v1/users/sp-search?name=nguyen&role=Admin,Editor&page=0&size=5&sort=email&dir=asc
    // GET /v1/users/sp-search?role=Salesperson&role=Assistant&sort=last_name&dir=desc
    @GetMapping("/sp-search")
    public PageResponse<UserWithRolesDto> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, name = "role") List<String> roleRaw,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir
    ) {
        List<String> roleNames = parseCsvParams(roleRaw);
        return service.search(name, roleNames, page, size, sort, dir);
    }

    private static List<String> parseCsvParams(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // /v1/users/search-spec?name=nguyen an&role=Admin,Editor&page=0&size=5&sort=email&dir=asc
    // /v1/users/search-spec?role=Salesperson&role=Assistant
    @GetMapping("/search-spec")
    public Page<UserWithRolesDto> searchSpec(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, name = "role") List<String> roleRaw,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir
    ) {
        List<String> roleNames = parseCsv(roleRaw);
        return specService.searchByNameAndRole(name, roleNames, page, size, sort, dir);
    }

    private static List<String> parseCsv(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
