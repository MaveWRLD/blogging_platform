package org.amalitech.dtos.userDtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.amalitech.entities.Role;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserWithRoleDto {
    private Long id;
    private String username;
    private String email;
    private List<Role> roles;
}