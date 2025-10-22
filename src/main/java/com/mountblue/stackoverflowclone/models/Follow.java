package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "follows")
@Getter
@Setter
public class Follow extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "object_type", nullable = false)
    private String objectType; // "question" or "tag"

    @Column(name = "object_id", nullable = false)
    private Long objectId; // ID of the question or tag
}
