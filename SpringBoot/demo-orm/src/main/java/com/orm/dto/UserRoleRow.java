package com.orm.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserRoleRow {
    private Integer userId;
    private String  email;
    private String  firstName;
    private String  lastName;
    private Integer roleId;
    private String  roleName;
}
