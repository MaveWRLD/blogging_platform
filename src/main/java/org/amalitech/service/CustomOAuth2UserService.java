package org.amalitech.service;

import lombok.AllArgsConstructor;
import org.amalitech.entities.User;
import org.amalitech.entities.Role;
import org.amalitech.mappers.UserMapper;
import org.amalitech.repositories.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Service
@AllArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private UserRepository userRepository;
    private RoleService roleService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String username = Objects.requireNonNull(oAuth2User.getAttribute("username"))
                .toString()
                .replace("@gmail.com", "");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Set<Role> defaultRoles = new HashSet<>();
                    try {
                        Role readerRole = roleService.getRoleByName("READER");
                        defaultRoles.add(readerRole);
                    } catch (RuntimeException e) {
                        // Handle case where READER role doesn't exist
                        // You might want to create the role or use a different approach
                    }
                    User newUser = User.registerReader(username, email, null, null, null, defaultRoles);
                    return userRepository.save(newUser);
                });


        return new CustomUserPrincipal(user, oAuth2User.getAttributes());
    }
}