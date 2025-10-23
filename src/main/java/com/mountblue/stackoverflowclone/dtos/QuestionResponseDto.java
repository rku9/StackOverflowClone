package com.mountblue.stackoverflowclone.dtos;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionResponseDto(
        Long id,
        String authorName,
        String title,
        String body,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long viewCount,
        int score,
//        List<AnswerResponseDto> answers,
//        List<CommentResponseDto> comments,

        @JsonManagedReference("question-tags")
        List<TagResponseDto> tags
) {}
