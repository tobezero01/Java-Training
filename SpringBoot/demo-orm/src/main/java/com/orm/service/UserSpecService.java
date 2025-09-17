package com.orm.service;

import com.orm.dto.RoleBriefDto;
import com.orm.dto.UserWithRolesDto;
import com.orm.entity.Role;
import com.orm.entity.User;
import com.orm.repository.UserRepository;
import com.orm.repository.UserSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSpecService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserWithRolesDto> searchByNameAndRole(String name,
                                                      List<String> roleNames,
                                                      int page, int size,
                                                      String sort, String dir)
    {
        Specification<User> spec = Specification.where(UserSpecs.nameContains(name))
                .and(UserSpecs.hasRoleNames(roleNames));

        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProp = switch (sort == null ? "" : sort) {
            case "email" -> "email";
            case "firstName" -> "firstName";
            case "lastName" -> "lastName";
            default -> "id";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        Page<User> p = userRepository.findAll(spec, pageable);

        return p.map(u -> new UserWithRolesDto(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.isEnabled(),
                u.getRoles().stream()
                        .map(r -> new RoleBriefDto(r.getId(), r.getName()))
                        .toList()
        ));
    }
}
