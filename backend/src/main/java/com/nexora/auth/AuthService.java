package com.nexora.auth;

import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.dto.RegisterRequest;
import com.nexora.security.JwtService;
import com.nexora.user.Role;
import com.nexora.user.RoleRepository;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Role playerRole = roleRepository.findByName(Role.PLAYER)
                .orElseThrow(() -> new IllegalStateException("PLAYER role missing — RoleSeeder should have created it"));
        User user = new User(req.getUsername(), req.getEmail(),
                passwordEncoder.encode(req.getPassword()), playerRole);
        user = userRepository.save(user);
        return issueToken(user);
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String role = user.getRole() != null ? user.getRole().getName() : Role.PLAYER;
        String token = jwtService.generateToken(user.getId(), user.getUsername(), role);
        return new AuthResponse(token, user.getId(), user.getUsername(), role);
    }
}
