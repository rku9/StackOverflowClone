package com.mountblue.stackoverflowclone.dto;

import com.mountblue.stackoverflowclone.models.User;

public record UserWithStatsDTO(
        User user,
        Long voteCount,
        Long questionCount,
        Long answerCount
) {}