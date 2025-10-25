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

    @Query("""
        SELECT q
        FROM Question q
        WHERE SIZE(q.answers) = :answerCount
        ORDER BY q.createdAt DESC
    """)
    List<Question> findQuestionsByAnswerCount(@Param("answerCount") int answerCount);

    List<Question> findByAuthor_Username(String username);

    @Query("""
             SELECT q
             FROM Question q
             WHERE q.score >= :minScore
             ORDER BY q.score DESC
    """)
    List<Question> findQuestionsByMinScore(@Param("minScore") int minScore);

    @Query("""
            SELECT DISTINCT q FROM Question q LEFT JOIN q.tags t
            WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(q.body) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY q.createdAt DESC
    """)
    List<Question> searchQuestionsByKeyword(@Param("keyword") String keyword);

    @Query("""
            SELECT q FROM Question q JOIN q.tags t
            WHERE LOWER(t.name) IN :tagNames
            GROUP BY q.id
            HAVING COUNT(DISTINCT t.id) = :tagCount
            ORDER BY q.createdAt DESC
    """)
    List<Question> findQuestionsByAllTags(@Param("tagNames") List<String> tagNames,
                                          @Param("tagCount") long tagCount);

    @Query("""
        SELECT DISTINCT q FROM Question q
        LEFT JOIN q.tags t
        LEFT JOIN q.answers a
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
        AND (:daysOld IS NULL OR q.createdAt <= CURRENT_TIMESTAMP - :daysOld)
        ORDER BY q.createdAt DESC
    """)
    List<Question> searchQuestionsWithFilters(
            @Param("keyword") String keyword,
            @Param("minScore") Integer minScore,
            @Param("hasNoAnswers") boolean hasNoAnswers,
            @Param("hasNoUpvotedOrAccepted") boolean hasNoUpvotedOrAccepted,
            @Param("daysOld") Integer daysOld
    );

}