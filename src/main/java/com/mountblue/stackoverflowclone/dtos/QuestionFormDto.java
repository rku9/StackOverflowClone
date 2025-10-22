package com.mountblue.stackoverflowclone.dtos;

public record QuestionFormDto(
        Long id,
        String title,
        String body,
        String tags,
        Long authorId
) { }
