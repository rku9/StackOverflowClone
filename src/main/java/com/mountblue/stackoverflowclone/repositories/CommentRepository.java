package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Override
    Optional<Comment> findById(Long commentId);
}
