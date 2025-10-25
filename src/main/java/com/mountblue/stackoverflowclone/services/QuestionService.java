package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionResponseDto;
import com.mountblue.stackoverflowclone.dtos.TagResponseDto;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.FilterType;
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

    /**
     * Unified filtering + sorting + pagination used by QuestionsController.
     * Applies: keyword search, tag intersection, checkbox filters, min age (daysOld), and sort.
     */
    public List<Question> getFilteredQuestions(
            Pageable pageable,
            String query,
            List<String> tags,
            List<FilterType> filterTypes,
            Integer daysOld,
            String sortParam
    ) {
        // Load all then filter in-memory (keeps repository unchanged)
        List<Question> working = new ArrayList<>(questionRepository.findAll());

        // Keyword filter (simple contains across title/body/tags)
        if (query != null && !query.isBlank()) {
            String needle = query.toLowerCase(Locale.ROOT);
            working = working.stream()
                    .filter(q -> {
                        boolean inTitle = q.getTitle() != null && q.getTitle().toLowerCase(Locale.ROOT).contains(needle);
                        boolean inBody = q.getBody() != null && q.getBody().toLowerCase(Locale.ROOT).contains(needle);
                        boolean inTags = q.getTags() != null && q.getTags().stream()
                                .map(t -> t.getName() == null ? "" : t.getName().toLowerCase(Locale.ROOT))
                                .anyMatch(name -> name.contains(needle));
                        return inTitle || inBody || inTags;
                    })
                    .collect(Collectors.toList());
        }

        // Tag filter (must contain all provided tags)
        if (tags != null && !tags.isEmpty()) {
            working = working.stream()
                    .filter(q -> questionHasAllTags(q, tags))
                    .collect(Collectors.toList());
        }

        // Checkbox filters
        boolean requireNoAnswers = filterTypes != null && filterTypes.contains(FilterType.NO_ANSWERS);
        boolean requireNoUpvotedOrAccepted = filterTypes != null && filterTypes.contains(FilterType.NO_UPVOTED_OR_ACCEPTED_ANSWER);

        if (requireNoAnswers) {
            working = working.stream()
                    .filter(q -> q.getAnswers() == null || q.getAnswers().isEmpty())
                    .collect(Collectors.toList());
        }

        if (requireNoUpvotedOrAccepted) {
            working = working.stream()
                    .filter(q -> q.getAnswers() == null || q.getAnswers().stream()
                            .noneMatch(a -> a.isAccepted() || a.getScore() > 0))
                    .collect(Collectors.toList());
        }

        // Min age (days old)
        if (daysOld != null && daysOld > 0) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
            working = working.stream()
                    .filter(q -> q.getCreatedAt() != null && (q.getCreatedAt().isBefore(cutoff) || q.getCreatedAt().isEqual(cutoff)))
                    .collect(Collectors.toList());
        }

        // Sorting
        Comparator<Question> cmp;
        if ("Oldest".equalsIgnoreCase(sortParam)) {
            cmp = Comparator.comparing(q -> q.getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("HighestScore".equalsIgnoreCase(sortParam)) {
            cmp = Comparator.comparingInt(Question::getScore)
                    .reversed()
                    .thenComparing((Question q) -> q.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("MostAnswers".equalsIgnoreCase(sortParam)) {
            cmp = Comparator.<Question>comparingInt(q -> q.getAnswers() == null ? 0 : q.getAnswers().size())
                    .reversed()
                    .thenComparing((Question q) -> q.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            // Default Newest
            cmp = Comparator.comparing((Question q) -> q.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
        }
        working.sort(cmp);

        // Paginate and return page content
        Page<Question> page = paginate(working, pageable);
        return page.getContent();
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
