package com.orm.dto;

import lombok.*;

@Getter @Setter  @NoArgsConstructor @AllArgsConstructor
public class RoleBriefDto {
    private Integer id ;
    private String name;

    public RoleBriefDto(String s) {
        this.name = s;
    }
}
