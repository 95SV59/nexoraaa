package com.nexora.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures PLAYER/ADMIN roles and default administrative/demo accounts exist on startup.
 */
@Component
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RoleSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role playerRole = roleRepository.findByName(Role.PLAYER)
                .orElseGet(() -> roleRepository.save(new Role(Role.PLAYER)));
        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(Role.ADMIN)));

        userRepository.findByUsername("admin").ifPresentOrElse(
                u -> { u.setPasswordHash(passwordEncoder.encode("admin123")); userRepository.save(u); },
                () -> {
                    User admin = new User("admin", "admin@nexora.com", passwordEncoder.encode("admin123"), adminRole);
                    admin.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=admin");
                    userRepository.save(admin);
                }
        );

        userRepository.findByUsername("player1").ifPresentOrElse(
                u -> { u.setPasswordHash(passwordEncoder.encode("player123")); userRepository.save(u); },
                () -> {
                    User player1 = new User("player1", "player1@nexora.com", passwordEncoder.encode("player123"), playerRole);
                    player1.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=player1");
                    userRepository.save(player1);
                }
        );

        userRepository.findByUsername("player2").ifPresentOrElse(
                u -> { u.setPasswordHash(passwordEncoder.encode("player123")); userRepository.save(u); },
                () -> {
                    User player2 = new User("player2", "player2@nexora.com", passwordEncoder.encode("player123"), playerRole);
                    player2.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=player2");
                    userRepository.save(player2);
                }
        );
    }
}
