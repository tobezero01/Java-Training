package com.orm.repository;

import com.orm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    // Gốc: không lọc
    @Query("""
    select u.id as userId,
           u.email as email,
           u.firstName as firstName,
           u.lastName as lastName,
           r.id as roleId,
           r.name as roleName
    from User u
      join u.roles r
    """)
    Page<UserRoleView> pageUserRole(Pageable pageable);

    // Lọc theo danh sách roleId
    @Query("""
    select u.id as userId,
           u.email as email,
           u.firstName as firstName,
           u.lastName as lastName,
           r.id as roleId,
           r.name as roleName
    from User u
      join u.roles r
    where r.id in :roleIds
    """)
    Page<UserRoleView> pageUserRoleByRoleIds(@Param("roleIds") List<Integer> roleIds, Pageable pageable);

    // Lọc theo danh sách roleName
    @Query("""
    select u.id as userId,
           u.email as email,
           u.firstName as firstName,
           u.lastName as lastName,
           r.id as roleId,
           r.name as roleName
    from User u
      join u.roles r
    where lower(r.name) in :roleNames
    """)
    Page<UserRoleView> pageUserRoleByRoleNames(@Param("roleNames") List<String> roleNames, Pageable pageable);

    // đảm bảo khi findAll(spec, pageable) sẽ fetch roles cùng trang hiện tại
    @Override
    @EntityGraph(attributePaths = "roles")
    Page<User> findAll(Specification<User> spec, Pageable pageable);

}
