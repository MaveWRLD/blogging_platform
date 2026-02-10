package org.amalitech.interfaces;

import org.amalitech.models.Role;

import java.util.List;

public interface RoleRepository {
    Role findByName(String name);
    List<Role> findByUserId(int userId);
}
