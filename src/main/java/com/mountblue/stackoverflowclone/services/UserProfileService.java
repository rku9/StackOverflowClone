package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.Tag;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TagRepository tagRepository;

    public List<Question> getUserQuestions(Long userId) {
        return questionRepository.findByAuthorIdWithTags(userId);
    }

    public List<Answer> getUserAnswersWithQuestions(Long userId) {
        return answerRepository.findByAuthorIdWithQuestion(userId);
    }

    public List<Tag> getUserTags(Long userId) {
        return tagRepository.findDistinctTagsByUserId(userId);
    }

    public Map<Tag, Long> getUserTagsWithCount(Long userId) {
        List<Object[]> results = tagRepository.findTagsWithCountByUserId(userId);
        Map<Tag, Long> tagCountMap = new LinkedHashMap<>();

        for (Object[] result : results) {
            Tag tag = (Tag) result[0];
            Long count = (Long) result[1];
            tagCountMap.put(tag, count);
        }

        return tagCountMap;
    }

    public int calculatePeopleReached(Long userId) {
        List<Question> questions = getUserQuestions(userId);
        return questions.stream()
                .mapToInt(question -> Math.toIntExact(question.getViewCount()))
                .sum();
    }
}

