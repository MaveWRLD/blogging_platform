package org.amalitech.user;

import lombok.RequiredArgsConstructor;
import org.amalitech.user.Role;
import org.amalitech.user.RoleRepository;
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
