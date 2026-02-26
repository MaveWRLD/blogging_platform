package org.amalitech.service;

import lombok.AllArgsConstructor;
import org.amalitech.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to parent to fetch OAuth2 user info from provider
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract email from OAuth2 attributes
        Object emailAttr = oAuth2User.getAttribute("email");
        String email = emailAttr != null ? emailAttr.toString() : null;

        // Try to find existing user by email
        var optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            // User exists in database; wrap it for Spring Security
            var user = optionalUser.get();
            log.debug("Found existing OAuth2 user by email: {}", email);
            return new CustomUserPrincipal(user, oAuth2User.getAttributes());
        }

        // User doesn't exist yet; return OAuth2User as-is
        // Oauth2SuccessHandler will create and persist the user after successful authentication
        log.debug("OAuth2 user not yet persisted (email: {}); will be created by Oauth2SuccessHandler", email);
        return oAuth2User;
    }
}