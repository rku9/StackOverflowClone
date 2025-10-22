package com.mountblue.stackoverflowclone.dtos;

import java.time.LocalDateTime;

public record QuestionResponseDto(
        Long id,
        String authorName,
        String title,
        String body,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int viewCount,
        int score
//        List<AnswerResponseDto> answers,
//        List<CommentResponseDto> comments,
//        List<TagResponseDto> tags
) {}
