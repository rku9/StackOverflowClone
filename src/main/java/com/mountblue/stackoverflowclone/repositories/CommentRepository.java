package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.question.id = :questionId ORDER BY c.createdAt ASC")
    List<Comment> findByQuestionIdWithAuthor(@Param("questionId") Long questionId);

    List<Comment> findByQuestionIdOrderByCreatedAtAsc(Long questionId);

    List<Comment> findByAnswerIdOrderByCreatedAtAsc(Long answerId);

    List<Comment> findByAuthorId(Long authorId);

    Long countByQuestionId(Long questionId);

    Long countByAnswerId(Long answerId);

    void deleteByQuestionId(Long questionId);

    void deleteByAnswerId(Long answerId);
}


