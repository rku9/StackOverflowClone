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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
     * Controller expects a no-arg getAllQuestions() that returns all questions.
     */
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
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

    /**
     * Controller expects Page<QuestionResponseDto> from service for search.
     * Reuse getSeachedQuestions(...) and map to DTOs.
     */
    public Page<QuestionResponseDto> search(String query, Pageable pageable) {
        Page<Question> results = getSeachedQuestions(pageable, query);
        return results.map(question -> new QuestionResponseDto(
                question.getId(),
                question.getAuthor().getUsername(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                question.getViewCount(),
                question.getScore(),
                question.getTags().stream()
                        .map(tag -> new TagResponseDto(tag.getId(), tag.getName(), Collections.emptyList()))
                        .collect(Collectors.toList())
        ));
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
        LocalDateTime cutoffDate = null;
        if (daysOld != null && daysOld > 0) {
            cutoffDate = LocalDateTime.now().minusDays(daysOld);
        }

        return questionRepository.searchQuestionsWithFilters(
                sanitizedKeyword,
                minScore,
                hasNoAnswers,
                hasNoUpvotedOrAccepted,
                cutoffDate,
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

        Page<Question> basePage = selectBaseResult(searchQuery);
        List<Question> filtered = applyFilters(basePage.getContent(), searchQuery);

        return paginate(filtered, pageable);
    }

    private Page<Question> selectBaseResult(SearchQuery searchQuery) {
        Map<String, String> stringFilters = searchQuery.getStringFilters();
        Map<String, Integer> numericFilters = searchQuery.getNumericFilters();
        List<String> tags = searchQuery.getTags();
        List<String> keywords = searchQuery.getKeywords();

        if (stringFilters.containsKey("user")) {
            String username = stringFilters.get("user");
            return questionRepository.findByAuthor_Username(Pageable.unpaged(), username);
        }

        if (!tags.isEmpty()) {
            return questionRepository.findQuestionsByAllTags(tags, tags.size(), Pageable.unpaged());
        }

        if (numericFilters.containsKey("answers")) {
            Integer ansCount = numericFilters.get("answers");
            return questionRepository.findQuestionsByAnswerCount(ansCount, Pageable.unpaged());
        }

        if (numericFilters.containsKey("score")) {
            Integer minScore = numericFilters.get("score");
            return questionRepository.findQuestionsByMinScore(minScore, Pageable.unpaged());
        }

        if (!keywords.isEmpty()) {
            String keyword = keywords.get(0);
            return questionRepository.searchQuestionsByKeyword(keyword, Pageable.unpaged());
        }

        return questionRepository.findAll(Pageable.unpaged());
    }

    private List<Question> applyFilters(List<Question> source, SearchQuery searchQuery) {
        List<Question> working = new ArrayList<>(new LinkedHashSet<>(source));

        Map<String, String> stringFilters = searchQuery.getStringFilters();
        Map<String, Integer> numericFilters = searchQuery.getNumericFilters();
        List<String> tags = searchQuery.getTags();
        List<String> keywords = searchQuery.getKeywords();

        if (stringFilters.containsKey("tag")) {
            String explicitTag = stringFilters.get("tag").toLowerCase(Locale.ROOT);
            working = working.stream()
                    .filter(q -> questionHasAllTags(q, List.of(explicitTag)))
                    .collect(Collectors.toList());
        }

        if (!tags.isEmpty()) {
            working = working.stream()
                    .filter(q -> questionHasAllTags(q, tags))
                    .collect(Collectors.toList());
        }

        if (numericFilters.containsKey("answers")) {
            int minAnswers = numericFilters.get("answers");
            working = working.stream()
                    .filter(q -> q.getAnswers() != null && q.getAnswers().size() >= minAnswers)
                    .collect(Collectors.toList());
        }

        if (numericFilters.containsKey("score")) {
            int minScore = numericFilters.get("score");
            working = working.stream()
                    .filter(q -> q.getScore() >= minScore)
                    .collect(Collectors.toList());
        }

        if (!keywords.isEmpty()) {
            working = working.stream()
                    .filter(q -> keywords.stream().allMatch(keyword -> questionMatchesKeyword(q, keyword)))
                    .collect(Collectors.toList());
        }

        return working;
    }

    private Page<Question> paginate(List<Question> items, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(items);
        }

        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }

        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<Question> pageContent = items.subList(start, end);
        return new PageImpl<>(pageContent, pageable, items.size());
    }

    private boolean questionHasAllTags(Question question, List<String> requiredTags) {
        if (requiredTags.isEmpty()) {
            return true;
        }

        Set<String> questionTags = question.getTags().stream()
                .filter(Objects::nonNull)
                .map(Tag::getName)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return requiredTags.stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .allMatch(questionTags::contains);
    }

    private boolean questionMatchesKeyword(Question question, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String needle = keyword.toLowerCase(Locale.ROOT);

        if (question.getTitle() != null && question.getTitle().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }

        if (question.getBody() != null && question.getBody().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }

        return question.getTags().stream()
                .map(Tag::getName)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(tagName -> tagName.contains(needle));
    }

}
