package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    public AnswerService(AnswerRepository answerRepository, UserRepository userRepository, QuestionRepository questionRepository){
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public Answer saveAnswer(AnswerFormDto answerFormDto, Long questionId) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        User author = userRepository.findById(1L)
                .orElseThrow(() -> new NoSuchElementException("Default user not found. Please ensure user with ID 1 exists in your database."));

        if (answerFormDto.body() == null || answerFormDto.body().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer body cannot be empty");
        }

        Answer answer = new Answer();
        answer.setBody(answerFormDto.body().trim());
        answer.setQuestion(question);
        answer.setAuthor(author);
        answer.setScore(0);
        answer.setAccepted(false);

        return answerRepository.save(answer);
    }
}
