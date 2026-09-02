package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.dtos.LoginRequest;
import org.amalitech.user.User;
import org.amalitech.enums.TokenType;
import org.amalitech.exception.CustomExceptionHandler;
import org.amalitech.factories.TokenFactory;
import org.amalitech.user.UserService;
import org.amalitech.token.Jwt;
import org.amalitech.token.JwtParser;
import org.amalitech.token.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private TokenFactory tokenFactory;

    @Mock
    private JwtParser jwtParser;

    @Mock
    private TokenBlacklistService blacklistService;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private Jwt accessToken;
    private Jwt refreshToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        testUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", Set.of());
        testUser.setId(1L);

        // Create mock JWT objects using reflection or by mocking the strategy behavior
        accessToken = mock(Jwt.class);
        refreshToken = mock(Jwt.class);
        
        when(accessToken.toString()).thenReturn("access-token");
        when(refreshToken.toString()).thenReturn("refresh-token");
        when(accessToken.getTokenType()).thenReturn(TokenType.ACCESS);
        when(refreshToken.getTokenType()).thenReturn(TokenType.REFRESH);
        when(accessToken.getUserId()).thenReturn(1L);
        when(refreshToken.getUserId()).thenReturn(1L);
        when(accessToken.getExpiresAt()).thenReturn(Instant.ofEpochMilli(System.currentTimeMillis() + 3600000));
        when(refreshToken.getExpiresAt()).thenReturn(Instant.ofEpochMilli(System.currentTimeMillis() + 604800000));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnJwtResponseAndSetCookie() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tokenFactory.generateToken(TokenType.ACCESS, testUser)).thenReturn(accessToken);
        when(tokenFactory.generateToken(TokenType.REFRESH, testUser)).thenReturn(refreshToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).findByUsername("testuser");
        verify(tokenFactory).generateToken(TokenType.ACCESS, testUser);
        verify(tokenFactory).generateToken(TokenType.REFRESH, testUser);
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowAuthenticationException() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(userService, tokenFactory);
    }

    @Test
    void login_WithMissingUsername_ShouldReturnBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        when(jwtParser.parseToken("refresh-token")).thenReturn(refreshToken);
        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(tokenFactory.generateToken(TokenType.ACCESS, testUser)).thenReturn(accessToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"));

        verify(jwtParser).parseToken("refresh-token");
        verify(userService).findByUserId(1L);
        verify(tokenFactory).generateToken(TokenType.ACCESS, testUser);
    }

    @Test
    void refreshToken_WithExpiredToken_ShouldReturnUnauthorized() throws Exception {
        Jwt expiredToken = mock(Jwt.class);
        when(expiredToken.getTokenType()).thenReturn(TokenType.REFRESH);
        when(expiredToken.getUserId()).thenReturn(1L);
        when(expiredToken.isExpired()).thenReturn(true);
        
        when(jwtParser.parseToken("expired-token")).thenReturn(expiredToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "expired-token")))
                .andExpect(status().isUnauthorized());

        verify(jwtParser).parseToken("expired-token");
        verifyNoInteractions(userService, tokenFactory);
    }

    @Test
    void refreshToken_WithAccessToken_ShouldReturnUnauthorized() throws Exception {
        when(jwtParser.parseToken("access-token")).thenReturn(accessToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "access-token")))
                .andExpect(status().isUnauthorized());

        verify(jwtParser).parseToken("access-token");
        verifyNoInteractions(userService, tokenFactory);
    }

    @Test
    void refreshToken_WithMissingCookie_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_WithValidTokens_ShouldBlacklistTokensAndClearCookie() throws Exception {
        when(jwtParser.parseToken("access-token")).thenReturn(accessToken);
        when(jwtParser.parseToken("refresh-token")).thenReturn(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isNoContent());

        verify(jwtParser).parseToken("access-token");
        verify(jwtParser).parseToken("refresh-token");
        verify(blacklistService).blacklist("access-token", accessToken.getExpiresAt());
        verify(blacklistService).blacklist("refresh-token", refreshToken.getExpiresAt());
    }

    @Test
    void logout_WithNoTokens_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verifyNoInteractions(jwtParser, blacklistService);
    }

    @Test
    void logout_WithExpiredTokens_ShouldNotBlacklist() throws Exception {
        Jwt expiredToken = mock(Jwt.class);
        when(expiredToken.isExpired()).thenReturn(true);
        when(jwtParser.parseToken("expired-token")).thenReturn(expiredToken);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "expired-token")))
                .andExpect(status().isNoContent());

        verify(jwtParser).parseToken("expired-token");
        verify(blacklistService, never()).blacklist(anyString(), any(Instant.class));
    }

    @Test
    void logout_WithInvalidToken_ShouldNotBlacklist() throws Exception {
        when(jwtParser.parseToken("invalid-token")).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isNoContent());

        verify(jwtParser).parseToken("invalid-token");
        verify(blacklistService, never()).blacklist(anyString(), any(Instant.class));
    }
}
