package com.orm.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserWithRolesDto {
    private Integer id;
    private String  email;
    private String  firstName;
    private String  lastName;
    private Boolean enabled;
    private List<RoleBriefDto> roles; // từ rolesCsv tách ra

}
