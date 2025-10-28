package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT DISTINCT a FROM Answer a " +
            "LEFT JOIN FETCH a.question q " +
            "LEFT JOIN FETCH q.tags " +
            "LEFT JOIN FETCH a.author " +
            "WHERE a.author.id = :userId " +
            "ORDER BY a.createdAt DESC")
    List<Answer> findByAuthorIdWithQuestion(@Param("userId") Long userId);

    @Query("SELECT a FROM Answer a " +
            "LEFT JOIN FETCH a.author " +
            "WHERE a.question.id = :questionId " +
            "ORDER BY a.accepted DESC, a.score DESC, a.createdAt ASC")
    List<Answer> findByQuestionIdOrderByAcceptedAndScore(@Param("questionId") Long questionId);


    @Override
    Optional<Answer> findById(Long answerId);

    List<Answer> getAllByQuestionId(Long questionId);

    List<Answer> getAnswersByQuestionId(Long questionId);

}