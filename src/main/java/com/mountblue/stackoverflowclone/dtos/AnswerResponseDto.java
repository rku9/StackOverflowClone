package com.mountblue.stackoverflowclone.dtos;

import com.mountblue.stackoverflowclone.models.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record AnswerResponseDto(Long questionId,
                                Long answerId,
                                String body,
                                String bodyHtml,
                                String authorName,
                                String authorEmail,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt,
                                int score,
                                List<Comment> comments,
                                boolean accepted) {

}
