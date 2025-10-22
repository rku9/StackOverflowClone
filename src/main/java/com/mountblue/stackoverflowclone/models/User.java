package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseModel {

    private String username;

    @Column(name = "email_hash")
    private String emailHash;

    private int reputation;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(columnDefinition = "TEXT")
    private String preferences;

    @OneToMany(
            mappedBy = "author",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<Question> questions;

    @OneToMany(
            mappedBy = "author",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<Answer> answers;

    @OneToMany(
            mappedBy = "author",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<Comment> comments;

    @OneToMany(
            mappedBy = "user",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<Vote> votes;

    @OneToMany(
            mappedBy = "user",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<Follow> follows;

    @OneToMany(
            mappedBy = "owner",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<Upload> uploads;
}
