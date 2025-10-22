package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Override
    Optional<Question> findById(Long questionId);
}