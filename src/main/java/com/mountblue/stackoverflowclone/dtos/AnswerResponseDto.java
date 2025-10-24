package com.mountblue.stackoverflowclone.dtos;

import java.time.LocalDateTime;

public record AnswerResponseDto(Long questionId,
                                Long answerId,
                                String body,
                                String bodyHtml,
                                String authorName,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt,
                                int score) {

}
