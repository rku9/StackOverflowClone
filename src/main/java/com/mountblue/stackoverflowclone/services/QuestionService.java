package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QuestionService {

    /*
    temp
     */
    UserRepository userRepository;

    QuestionRepository questionRepository;
    public QuestionService(QuestionRepository questionRepository,
                           UserRepository userRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }
    public void saveQuestion(QuestionFormDto questionFormDto){
        Question question = new Question();
        question.setTitle(questionFormDto.title());
        question.setBody(questionFormDto.body());

        question.setAuthor(userRepository.findById(1L).get());
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
}
