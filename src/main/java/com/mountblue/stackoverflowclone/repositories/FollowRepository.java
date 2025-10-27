package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    @Override
    Optional<Follow> findById(Long followId);

    boolean existsByUser_IdAndObjectTypeAndObjectId(Long userId, String objectType, Long objectId);

    Optional<Follow> findByUser_IdAndObjectTypeAndObjectId(Long userId, String objectType, Long objectId);

    List<Follow> findAllByObjectTypeAndObjectId(String objectType, Long objectId);
}