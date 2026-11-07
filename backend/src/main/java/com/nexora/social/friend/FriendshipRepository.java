package com.nexora.social.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.user.id = :userId AND f.friend.id = :friendId) OR (f.user.id = :friendId AND f.friend.id = :userId)")
    Optional<Friendship> findRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Query("SELECT f FROM Friendship f WHERE (f.user.id = :userId OR f.friend.id = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAllAcceptedFriends(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.friend.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findPendingIncomingRequests(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findPendingOutgoingRequests(@Param("userId") Long userId);
}
