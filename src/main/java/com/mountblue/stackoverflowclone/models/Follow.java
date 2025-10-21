package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "object_type", nullable = false)
    private String objectType; // "question" or "tag"

    @Column(name = "object_id", nullable = false)
    private Long objectId; // ID of the question or tag

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
