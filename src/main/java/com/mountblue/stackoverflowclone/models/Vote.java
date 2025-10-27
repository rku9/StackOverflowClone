package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "votes")
@Getter
@Setter
public class Vote extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "post_id", nullable = false)
    private Long postId; // can refer to Question or Answer

    @Column(name = "post_type", nullable = false)
    private String postType; // "question" or "answer"

    @Column(name = "vote_value", nullable = false, columnDefinition = "integer default 0")
    private int voteValue; // +1 for upvote, -1 for downvote
}
