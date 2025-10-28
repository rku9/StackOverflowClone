package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByName(String name);

    List<User> findByReputationGreaterThanEqual(int reputation);

    List<User> findTop10ByOrderByReputationDesc();

    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<User> findByLastSeenAfter(LocalDateTime lastSeen);

    Page<User> findByNameContainingIgnoreCase(
            String name, Pageable pageable);

    // Sorted by reputation DESC
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            GROUP BY u.id
            ORDER BY u.reputation DESC
            """)
    Page<Object[]> findAllUsersWithStatsByReputation(Pageable pageable);

    // Sorted by vote count DESC
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT v.id) DESC
            """)
    Page<Object[]> findAllUsersWithStatsByVotes(Pageable pageable);

    // Sorted by created date DESC
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            GROUP BY u.id
            ORDER BY u.createdAt DESC
            """)
    Page<Object[]> findAllUsersWithStatsByCreatedAt(Pageable pageable);

    // Sorted by question count DESC
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT q.id) DESC
            """)
    Page<Object[]> findAllUsersWithStatsByQuestions(Pageable pageable);

    // Sorted by answer count DESC
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT a.id) DESC
            """)
    Page<Object[]> findAllUsersWithStatsByAnswers(Pageable pageable);

    // Search users - sorted by reputation
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            GROUP BY u.id
            ORDER BY u.reputation DESC
            """)
    Page<Object[]> findUsersWithStatsBySearchAndReputation(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Search users - sorted by votes
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT v.id) DESC
            """)
    Page<Object[]> findUsersWithStatsBySearchAndVotes(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Search users - sorted by created date
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            GROUP BY u.id
            ORDER BY u.createdAt DESC
            """)
    Page<Object[]> findUsersWithStatsBySearchAndCreatedAt(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Search users - sorted by question count
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT q.id) DESC
            """)
    Page<Object[]> findUsersWithStatsBySearchAndQuestions(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Search users - sorted by answer count
    @Query("""
            SELECT u,
            COUNT(DISTINCT v.id),
            COUNT(DISTINCT q.id),
            COUNT(DISTINCT a.id)
            FROM User u
            LEFT JOIN Vote v ON v.user.id = u.id
            LEFT JOIN Question q ON q.author.id = u.id
            LEFT JOIN Answer a ON a.author.id = u.id
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT a.id) DESC
            """)
    Page<Object[]> findUsersWithStatsBySearchAndAnswers(@Param("searchTerm") String searchTerm, Pageable pageable);

}
