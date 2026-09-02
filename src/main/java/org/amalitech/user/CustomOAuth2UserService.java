package org.amalitech.user;

import lombok.AllArgsConstructor;
import org.amalitech.user.UserRepository;
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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Object emailAttr = oAuth2User.getAttribute("email");
        String email = emailAttr != null ? emailAttr.toString() : null;

        var optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            var user = optionalUser.get();
            log.debug("Found existing OAuth2 user by email: {}", email);
            return new CustomUserPrincipal(user, oAuth2User.getAttributes());
        }

        log.debug("OAuth2 user not yet persisted (email: {}); will be created by Oauth2SuccessHandler", email);
        return oAuth2User;
    }
}