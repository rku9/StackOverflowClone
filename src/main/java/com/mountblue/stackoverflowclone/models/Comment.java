package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment extends BaseModel {

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "post_type", nullable = false)
    private String postType; // "question" or "answer"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String body;
}
