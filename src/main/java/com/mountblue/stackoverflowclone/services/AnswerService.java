package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;

    public AnswerService(AnswerRepository answerRepository){
        this.answerRepository = answerRepository;
    }

    public Answer saveAnswer(AnswerFormDto answerFormDto, Long questionId) {
        return null;
    }
}
