package com.mountblue.stackoverflowclone.dtos;

import java.time.LocalDateTime;

public record AnswerResponseDTO(Long questionId,
                                Long answerId,
                                String body,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt,
                                Long viewCount,
                                int score) {

}
