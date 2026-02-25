package org.amalitech.service;

import lombok.RequiredArgsConstructor;
import org.amalitech.entities.User;
import org.amalitech.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request)
            throws OAuth2AuthenticationException {

        OAuth2User oauthUser = super.loadUser(request);

        String email = oauthUser.getAttribute("email");
        String sub = oauthUser.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name, sub));

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                oauthUser.getAttributes(),
                "email"
        );
    }

    private User createUser(String email, String name, String sub) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(sub);
        user.setRole(Role.USER);
        return userRepository.save(user);
    }
}
