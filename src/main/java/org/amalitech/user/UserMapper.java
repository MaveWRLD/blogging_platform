package org.amalitech.user;

import org.amalitech.user.dto.CreateUserRequest;
import org.amalitech.user.dto.UpdateUserRequest;
import org.amalitech.user.dto.UserDto;
import org.amalitech.user.Role;
import org.amalitech.user.User;
import org.amalitech.user.RoleService;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;

import java.util.Set;
import java.util.HashSet;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @ObjectFactory
    default User createUser(CreateUserRequest request, RoleService roleService) {
        Set<Role> defaultRoles = new HashSet<>();
        Role readerRole = roleService.getRoleByName("reader");
        defaultRoles.add(readerRole);

        return User.registerReader(
                request.getUsername(), request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName(), defaultRoles
        );
    };

    /**
     * Updates existing User entity with values from UpdateUserRequest.
     * Only non-null fields from request are applied.
     */
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
