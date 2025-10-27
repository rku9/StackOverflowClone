package com.mountblue.stackoverflowclone.dtos;

import lombok.Getter;

@Getter
public class EmailTaskDto {
    private final String email;
    private final String subject;
    private final String body;
    private int attempts = 0;

    public EmailTaskDto(String email, String subject, String body) {
        this.email = email;
        this.subject = subject;
        this.body = body;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
