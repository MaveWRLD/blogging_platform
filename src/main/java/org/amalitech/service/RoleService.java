package org.amalitech.service;

import lombok.RequiredArgsConstructor;
import org.amalitech.entities.Role;
import org.amalitech.repositories.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    public Role getRoleByName(String name) {
        return findByName(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));
    }
}
