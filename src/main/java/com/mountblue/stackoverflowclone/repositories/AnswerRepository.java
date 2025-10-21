package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    @Override
    Optional<Answer> findById(Long answerId);
}