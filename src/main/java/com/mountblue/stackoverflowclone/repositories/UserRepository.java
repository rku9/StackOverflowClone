package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByName(String name);

    List<User> findByReputationGreaterThanEqual(int reputation);

    List<User> findTop10ByOrderByReputationDesc();

    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<User> findByLastSeenAfter(LocalDateTime lastSeen);
}
