package com.nexora.auth;

import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.dto.RegisterRequest;
import com.nexora.security.JwtService;
import com.nexora.user.Role;
import com.nexora.user.RoleRepository;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);

        authService = new AuthService(userRepository, roleRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    @DisplayName("Successfully registers new user and issues JWT token")
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newplayer");
        req.setEmail("player@nexora.gg");
        req.setPassword("SecurePass123!");

        Role playerRole = new Role(Role.PLAYER);

        when(userRepository.existsByUsername("newplayer")).thenReturn(false);
        when(userRepository.existsByEmail("player@nexora.gg")).thenReturn(false);
        when(roleRepository.findByName(Role.PLAYER)).thenReturn(Optional.of(playerRole));
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");

        User savedUser = new User("newplayer", "player@nexora.gg", "hashedPassword", playerRole);
        savedUser.setId(42L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(42L, "newplayer", Role.PLAYER)).thenReturn("mock.jwt.token");

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("mock.jwt.token", res.token);
        assertEquals("newplayer", res.username);
        assertEquals(42L, res.userId);
        assertEquals(Role.PLAYER, res.role);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Rejects registration when username is already taken")
    void testRegisterDuplicateUsername() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existingUser");
        req.setEmail("user@nexora.gg");
        req.setPassword("pass");

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successfully logs in existing user and issues token")
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setUsername("validUser");
        req.setPassword("password123");

        Role role = new Role("PLAYER");
        User user = new User("validUser", "v@nexora.gg", "hash", role);
        user.setId(10L);

        when(userRepository.findByUsername("validUser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(10L, "validUser", "PLAYER")).thenReturn("valid.jwt.token");

        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("valid.jwt.token", res.token);
        assertEquals(10L, res.userId);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Rejects login when credentials are invalid")
    void testLoginBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("validUser");
        req.setPassword("wrongPass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
