package org.amalitech.mappers;

import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.entities.Role;
import org.amalitech.entities.User;
import org.amalitech.service.RoleService;
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
