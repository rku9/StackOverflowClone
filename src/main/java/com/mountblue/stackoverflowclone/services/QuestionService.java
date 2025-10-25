package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.QuestionFilterRequestDto;
import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionResponseDto;
import com.mountblue.stackoverflowclone.dtos.TagResponseDto;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.SearchQuery;
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
    private final SearchQueryParser searchQueryParser;

    public QuestionService(QuestionRepository questionRepository,
                           UserRepository userRepository,
                           TagRepository tagRepository,
                           SearchQueryParser searchQueryParser) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
        this.searchQueryParser = searchQueryParser;
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

    public Page<Question> getQuestionsByAnswerCount(Pageable pageable, int answerCount) {
        return questionRepository.findQuestionsByAnswerCount(answerCount, pageable);
    }

    public Page<Question> getQuestionsByMinScore(Pageable pageable, int minScore) {
        return questionRepository.findQuestionsByMinScore(minScore, pageable);
    }

    public Page<Question> searchQuestionsByKeyword(Pageable pageable, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return questionRepository.findAll(pageable);
        }
        return questionRepository.searchQuestionsByKeyword(keyword.trim(), pageable);
    }

    public Page<Question> searchQuestionsByTags(Pageable pageable, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return questionRepository.findAll(pageable);
        }

        List<String> normalizedTags = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedTags.isEmpty()) {
            return questionRepository.findAll(pageable);
        }

        return questionRepository.findQuestionsByAllTags(normalizedTags, normalizedTags.size(), pageable);
    }

    public Page<Question> searchQuestionsWithFilters(Pageable pageable,
                                                     String keyword,
                                                     Integer minScore,
                                                     boolean hasNoAnswers,
                                                     boolean hasNoUpvotedOrAccepted,
                                                     Integer daysOld) {
        String sanitizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return questionRepository.searchQuestionsWithFilters(
                sanitizedKeyword,
                minScore,
                hasNoAnswers,
                hasNoUpvotedOrAccepted,
                daysOld,
                pageable
        );
    }

    //a pageable is received from the controller along with the filter dto.
    //the service receives that and then extracts the list of filters and tags and then
    //passes it to the repository to fetch the questions.
    public Page<Question> getAllQuestions(Pageable pageable,
                                          QuestionFilterRequestDto questionFilterDto) {
//        List<>


//        return questionRepository.findAll();
        return null;
    }

    public Page<Question> getSeachedQuestions(Pageable pageable, String searchString) {
        SearchQuery searchQuery = searchQueryParser.parse(searchString);

        if (searchQuery.isEmpty()) {
            return questionRepository.findAll(pageable);
        }

        // key:value filters take precedence (e.g., user:alice)
        if (searchQuery.getStringFilters().containsKey("user")) {
            String username = searchQuery.getStringFilters().get("user");
            return questionRepository.findByAuthor_Username(pageable, username);
        }

        // unquoted tokens are tags; also honor tag:xyz if parser keeps it as a string filter
        if (searchQuery.getStringFilters().containsKey("tag")) {
            String tag = searchQuery.getStringFilters().get("tag");
            return questionRepository.findQuestionsByAllTags(List.of(tag), 1, pageable);
        }
        if (!searchQuery.getTags().isEmpty()) {
            List<String> tags = searchQuery.getTags();
            return questionRepository.findQuestionsByAllTags(tags, tags.size(), pageable);
        }

        // quoted phrases are keywords
        if (!searchQuery.getKeywords().isEmpty()) {
            // combine keywords by running keyword search for each term and aggregating results
            return searchQuery.getKeywords().stream()
                    .map(keyword -> questionRepository.searchQuestionsByKeyword(keyword, pageable))
                    .findFirst()
                    .orElse(questionRepository.findAll(pageable));
        }

        return questionRepository.findAll(pageable);
    }

}
