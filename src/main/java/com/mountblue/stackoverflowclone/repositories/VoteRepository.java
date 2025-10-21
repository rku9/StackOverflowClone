package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Vote;
import com.mountblue.stackoverflowclone.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByUserAndPostIdAndPostType(User user, Long postId, String postType);

    Optional<Vote> findByUserIdAndPostIdAndPostType(Long userId, Long postId, String postType);

    List<Vote> findByPostIdAndPostType(Long postId, String postType);

    List<Vote> findByUser(User user);

    List<Vote> findByUserId(Long userId);

    @Query("SELECT SUM(v.voteValue) FROM Vote v WHERE v.postId = :postId AND v.postType = :postType")
    Integer sumVotesByPostIdAndPostType(@Param("postId") Long postId, @Param("postType") String postType);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.postId = :postId AND v.postType = :postType AND v.voteValue = 1")
    Long countUpvotesByPostIdAndPostType(@Param("postId") Long postId, @Param("postType") String postType);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.postId = :postId AND v.postType = :postType AND v.voteValue = -1")
    Long countDownvotesByPostIdAndPostType(@Param("postId") Long postId, @Param("postType") String postType);

    void deleteByUserIdAndPostIdAndPostType(Long userId, Long postId, String postType);

    boolean existsByUserIdAndPostIdAndPostType(Long userId, Long postId, String postType);
}
