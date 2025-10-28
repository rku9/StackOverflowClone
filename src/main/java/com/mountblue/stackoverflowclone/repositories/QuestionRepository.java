package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Override
    Optional<Question> findById(Long questionId);

    @Query("SELECT DISTINCT q FROM Question q " +
            "LEFT JOIN FETCH q.tags " +
            "LEFT JOIN FETCH q.author " +
            "WHERE q.author.id = :userId " +
            "ORDER BY q.createdAt DESC")
    List<Question> findByAuthorIdWithTags(@Param("userId") Long userId);

    @Query("SELECT DISTINCT q FROM Question q " +
            "JOIN q.tags t " +
            "WHERE t.name = :tagName " +
            "ORDER BY q.createdAt DESC")
    List<Question> findByTagName(@Param("tagName") String tagName);

    @Query("""
        SELECT q
        FROM Question q
        WHERE SIZE(q.answers) >= :answerCount
        ORDER BY q.createdAt DESC
    """)
    Page<Question> findQuestionsByAnswerCount(@Param("answerCount") int answerCount, Pageable pageable);

    Page<Question> findByAuthor_Name(Pageable pageable, String name);

    @Query("""
        SELECT q
        FROM Question q
        WHERE q.score >= :minScore
        ORDER BY q.score DESC
    """)
    Page<Question> findQuestionsByMinScore(@Param("minScore") int minScore, Pageable pageable);

    @Query("""
        SELECT DISTINCT q FROM Question q LEFT JOIN q.tags t
        WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(q.body) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY q.createdAt DESC
    """)
    Page<Question> searchQuestionsByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT q FROM Question q JOIN q.tags t
        WHERE LOWER(t.name) IN :tagNames
        GROUP BY q.id
        HAVING COUNT(DISTINCT t.id) = :tagCount
        ORDER BY q.createdAt DESC
    """)
    Page<Question> findQuestionsByAllTags(@Param("tagNames") List<String> tagNames,
                                          @Param("tagCount") long tagCount,
                                          Pageable pageable);

    @Query("""
        SELECT DISTINCT q FROM Question q
        LEFT JOIN q.tags t
        WHERE (:keyword IS NULL OR
              LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
              LOWER(q.body) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
              LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minScore IS NULL OR q.score >= :minScore)
        AND (:hasNoAnswers = false OR SIZE(q.answers) = 0)
        AND (:hasNoUpvotedOrAccepted = false OR 
             (SELECT COUNT(ans) FROM Answer ans 
              WHERE ans.question = q 
              AND (ans.accepted = true OR ans.score > 0)) = 0)
        AND (:cutoffDate IS NULL OR q.createdAt <= :cutoffDate)
    """)
    Page<Question> searchQuestionsWithFilters(
            @Param("keyword") String keyword,
            @Param("minScore") Integer minScore,
            @Param("hasNoAnswers") boolean hasNoAnswers,
            @Param("hasNoUpvotedOrAccepted") boolean hasNoUpvotedOrAccepted,
            @Param("cutoffDate") LocalDateTime cutoffDate,
            Pageable pageable
    );

    @Query("SELECT DISTINCT q FROM Question q JOIN q.tags t WHERE t IN :tags AND q.id != :questionId ORDER BY q.createdAt DESC")
    List<Question> findRelatedQuestionsByTags(@Param("tags") Set<Tag> tags, @Param("questionId") Long questionId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Question q SET q.viewCount = q.viewCount + 1 WHERE q.id = :id")
    int incrementViewCount(@Param("id") Long id);

}