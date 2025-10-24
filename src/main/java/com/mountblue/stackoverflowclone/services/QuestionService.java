package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionResponseDto;
import com.mountblue.stackoverflowclone.dtos.TagResponseDto;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.Tag;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.TagRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    //temp userRepo
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;

    public QuestionService(QuestionRepository questionRepository,
                           UserRepository userRepository,
                           TagRepository tagRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Question createQuestion(QuestionFormDto questionFormDto) {
        Question question = new Question();
        question.setTitle(questionFormDto.title());
        question.setBody(questionFormDto.body());
        question.setTags(extractTags(questionFormDto.tags()));
        Long authorId = questionFormDto.authorId() != null && questionFormDto.authorId() > 0
                ? questionFormDto.authorId()
                : 1L;
        question.setAuthor(userRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException("Author not found")));
        return questionRepository.save(question);
    }

    @Transactional
    public Question updateQuestion(Long id, QuestionFormDto questionFormDto) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));
        existing.setTitle(questionFormDto.title());
        existing.setBody(questionFormDto.body());
        existing.setTags(extractTags(questionFormDto.tags()));
        return questionRepository.save(existing);
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    /**
     * Splits the comma-separated tag string, normalizes, fetches existing or creates new tags
     */
    private List<Tag> extractTags(String tagListString) {
        if (tagListString == null || tagListString.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tagListString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String normalized = s.toLowerCase();
                    return tagRepository.findByName(normalized)
                            .orElseGet(() -> {
                                // create Tag manually instead of using builder
                                Tag tag = new Tag();
                                tag.setName(normalized);
                                return tagRepository.save(tag);
                            });
                })
                .collect(Collectors.toList());
    }
    public void voteQuestion(Question question, String choice){
        question.setScore(choice.equals("upvote") ? question.getScore() + 1 : question.getScore() - 1);
        questionRepository.save(question);
    }

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }
}
