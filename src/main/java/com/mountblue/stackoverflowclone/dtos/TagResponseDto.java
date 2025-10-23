package com.mountblue.stackoverflowclone.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.util.List;

public record TagResponseDto(
        Long id,
        String name,

        @JsonBackReference("question-tags")
        List<QuestionResponseDto> questions
) {

}
