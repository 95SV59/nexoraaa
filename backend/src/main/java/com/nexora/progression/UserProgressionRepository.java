package com.nexora.progression;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProgressionRepository extends JpaRepository<UserProgression, Long> {
    Optional<UserProgression> findByUserId(Long userId);
}
