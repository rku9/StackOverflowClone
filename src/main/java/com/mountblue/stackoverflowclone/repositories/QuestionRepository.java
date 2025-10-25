package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Override
    Optional<Question> findById(Long questionId);

    @Query("SELECT DISTINCT q FROM Question q " +
            "LEFT JOIN q.tags t " +
            "WHERE (:keyword IS NULL OR q.title LIKE %:keyword% OR q.body LIKE %:keyword%) " +
            "AND (:tags IS NULL OR t.name IN :tags) " +
            "AND (:username IS NULL OR q.author.name LIKE %:username%)"
    )
    Page<Question> searchQuestions(
            @Param("keyword") String keyword,
            @Param("tags") List<String> tags,
            @Param("username") String username,
            Pageable pageable
    );


}