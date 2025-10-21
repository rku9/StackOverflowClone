package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    @Override
    Optional<Follow> findById(Long followId);
}