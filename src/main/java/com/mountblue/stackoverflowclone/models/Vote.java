package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "post_id", nullable = false)
    private Long postId; // can refer to Question or Answer

    @Column(name = "post_type", nullable = false)
    private String postType; // "question" or "answer"

    @Column(name = "vote_value", nullable = false)
    private int voteValue; // +1 for upvote, -1 for downvote

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
