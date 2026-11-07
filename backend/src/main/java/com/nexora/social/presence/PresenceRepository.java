package com.nexora.social.presence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<UserPresence, Long> {
    Optional<UserPresence> findByUserId(Long userId);
    List<UserPresence> findByUserIdIn(List<Long> userIds);
}
