package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("SELECT DISTINCT t FROM Tag t " +
            "JOIN t.questions q " +
            "WHERE q.author.id = :userId " +
            "ORDER BY t.name")
    List<Tag> findDistinctTagsByUserId(@Param("userId") Long userId);

    @Query("SELECT t, COUNT(q) FROM Tag t " +
            "JOIN t.questions q " +
            "WHERE q.author.id = :userId " +
            "GROUP BY t " +
            "ORDER BY COUNT(q) DESC")
    List<Object[]> findTagsWithCountByUserId(@Param("userId") Long userId);

    @Query("SELECT t, COUNT(q) FROM Tag t " +
            "LEFT JOIN t.questions q " +
            "GROUP BY t " +
            "ORDER BY COUNT(q) DESC")
    List<Object[]> findAllTagsWithCount();

    List<Tag> findByNameIn(List<String> names);

    boolean existsByName(String name);

    List<Tag> findAllByOrderByNameAsc();
}
