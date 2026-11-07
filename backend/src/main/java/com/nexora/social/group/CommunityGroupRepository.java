package com.nexora.social.group;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityGroupRepository extends JpaRepository<CommunityGroup, Long> {
    Optional<CommunityGroup> findByName(String name);
    Optional<CommunityGroup> findByTag(String tag);
    boolean existsByName(String name);
    boolean existsByTag(String tag);
}
