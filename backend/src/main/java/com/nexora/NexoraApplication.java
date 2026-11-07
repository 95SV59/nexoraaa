package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point (Phase 3 foundation, added here so Phase 7/8 multiplayer code is
 * actually runnable). Component scan defaults to this class's package (com.nexora),
 * which covers every package created so far.
 */
@SpringBootApplication
@EnableScheduling
public class NexoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexoraApplication.class, args);
    }
}
