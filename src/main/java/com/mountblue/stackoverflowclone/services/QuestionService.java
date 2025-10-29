package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionResponseDto;
import com.mountblue.stackoverflowclone.dtos.TagResponseDto;
import com.mountblue.stackoverflowclone.models.*;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.TagRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import com.mountblue.stackoverflowclone.repositories.VoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;
    private final SearchQueryParser searchQueryParser;
    private final VoteRepository voteRepository;

    public QuestionService(QuestionRepository questionRepository,
                           UserRepository userRepository,
                           TagRepository tagRepository,
                           SearchQueryParser searchQueryParser,
                           VoteRepository voteRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
        this.searchQueryParser = searchQueryParser;
        this.voteRepository=voteRepository;
    }

    @Transactional
    public Question createQuestion(QuestionFormDto questionFormDto, UserPrincipal currentUser) {
        Question question = new Question();
        question.setTitle(questionFormDto.title());
        question.setBody(questionFormDto.body());
        question.setTags(extractTags(questionFormDto.tags()));
        Long authorId = questionFormDto.authorId() != null && questionFormDto.authorId() > 0
                ? questionFormDto.authorId()
                : currentUser.getId();
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

    @Transactional
    public void incrementViewCount(Long id) {
        questionRepository.incrementViewCount(id);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    private List<Tag> extractTags(String tagListString) {
        if (tagListString == null || tagListString.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tagListString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String normalized = s.toLowerCase();
                    return tagRepository.findFirstByNameIgnoreCaseOrderByIdAsc(normalized)
                            .orElseGet(() -> {
                                // create Tag manually instead of using builder
                                Tag tag = new Tag();
                                tag.setName(normalized);
                                return tagRepository.save(tag);
                            });
                })
                .collect(Collectors.toList());
    }

    public void voteQuestion(Question question, String choice, String postType, UserPrincipal principal, Long id){
        int score = question.getScore();
        User user = userRepository.findById(principal.getId()).get();
        Vote vote = voteRepository.findByUserIdAndPostIdAndPostType(user.getId(), id, postType).isEmpty()
                ? new Vote()
                : voteRepository.findByUserIdAndPostIdAndPostType(principal.getId(), id, postType).get();
        int newVoteValue = choice.equals("upvote") ? 1 : -1;
        if (vote.getPostId() == null) {
            // New vote - user hasn't voted before
            vote.setUser(user);
            vote.setPostId(id);
            vote.setPostType(postType);
            vote.setVoteValue(newVoteValue);
            score += newVoteValue;
        } else {
            // User has voted before - toggle or change vote
            int oldVoteValue = vote.getVoteValue();

            if (oldVoteValue == newVoteValue) {
                // Clicking same vote - remove vote
                voteRepository.delete(vote);
                score -= oldVoteValue;
                question.setScore(score);
                questionRepository.save(question);
                return;
            } else {
                // Changing vote (upvote to downvote or vice versa)
                score -= oldVoteValue;  // Remove old vote
                score += newVoteValue;  // Add new vote
                vote.setVoteValue(newVoteValue);
            }
        }

        voteRepository.save(vote);
        question.setScore(score);
        questionRepository.save(question);
    }

    public Page<QuestionResponseDto> search(String query, Pageable pageable) {
        Page<Question> results = getSeachedQuestions(pageable, query);
        return results.map(question -> {
            int answerCount = question.getAnswers() != null ? question.getAnswers().size() : 0;

            return new QuestionResponseDto(
                    question.getId(),
                    question.getAuthor().getId(),
                    question.getAuthor().getName(),
                    question.getAuthor().getEmail(),
                    question.getAuthor().getProfileImageUrl(),
                    question.getAuthor().getReputation(),
                    question.getTitle(),
                    question.getBody(),
                    question.getCreatedAt(),
                    question.getUpdatedAt(),
                    question.getViewCount(),
                    question.getScore(),
                    answerCount,
                    question.getComments(),
                    question.getTags().stream()
                            .map(tag -> new TagResponseDto(tag.getId(), tag.getName(), Collections.emptyList()))
                            .collect(Collectors.toList())
            );
        });
    }

    public Page<Question> getFilteredQuestions(
            Pageable pageable,
            String query,
            List<String> tags,
            List<FilterType> filterTypes,
            Integer daysOld,
            String sortParam
    ) {
        // Parse structured query and merge with UI-provided tags
        SearchQuery parsed = searchQueryParser.parse(query);
        List<String> combinedTags = new ArrayList<>();
        if (parsed.getTags() != null) combinedTags.addAll(parsed.getTags());
        if (tags != null) combinedTags.addAll(tags);
        combinedTags = combinedTags.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        SearchQuery augmented = new SearchQuery(
                parsed.getKeywords(),
                parsed.getNumericFilters(),
                parsed.getStringFilters(),
                combinedTags
        );

        // Use repository to narrow base set, then apply remaining filters in-memory
        Page<Question> base = selectBaseResult(augmented);
        List<Question> working = new ArrayList<>(base.getContent());

        // Apply structured filters (tags, numeric, string incl. isaccepted, keywords)
        working = applyFilters(working, augmented);

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

        // Paginate and return Page
        return paginate(working, pageable);
    }

    private Page<Question> selectBaseResult(SearchQuery searchQuery) {
        Map<String, String> stringFilters = searchQuery.getStringFilters();
        Map<String, Integer> numericFilters = searchQuery.getNumericFilters();
        List<String> tags = searchQuery.getTags();
        List<String> keywords = searchQuery.getKeywords();

        if (stringFilters.containsKey("user")) {
            String userToken = stringFilters.get("user");
            if (userToken != null) {
                try {
                    Long userId = Long.parseLong(userToken.trim());
                    return questionRepository.findByAuthor_Id(Pageable.unpaged(), userId);
                } catch (NumberFormatException ignore) {
                    // not a numeric id; treat as username
                }
                return questionRepository.findByAuthor_Name(Pageable.unpaged(), userToken);
            }
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

        if (numericFilters.containsKey("views")) {
            int minViews = numericFilters.get("views");
            working = working.stream()
                    .filter(q -> q.getViewCount() != null && q.getViewCount() >= minViews)
                    .collect(Collectors.toList());
        }

        // isaccepted: yes|no|true|false|1|0
        if (stringFilters.containsKey("isaccepted")) {
            String val = stringFilters.get("isaccepted");
            boolean wantAccepted = val != null && (
                    val.equalsIgnoreCase("yes") ||
                    val.equalsIgnoreCase("true") ||
                    val.equals("1")
            );
            if (wantAccepted) {
                working = working.stream()
                        .filter(q -> q.getAnswers() != null && q.getAnswers().stream().anyMatch(Answer::isAccepted))
                        .collect(Collectors.toList());
            } else {
                working = working.stream()
                        .filter(q -> q.getAnswers() == null || q.getAnswers().stream().noneMatch(Answer::isAccepted))
                        .collect(Collectors.toList());
            }
        }

        if (!keywords.isEmpty()) {
            working = working.stream()
                    .filter(q -> keywords.stream().allMatch(keyword -> questionMatchesKeyword(q, keyword)))
                    .collect(Collectors.toList());
        }

        return working;
    }

    private Page<Question> getSeachedQuestions(Pageable pageable, String rawQuery) {
        SearchQuery searchQuery = searchQueryParser.parse(rawQuery);

        // Get base (unpaged) results according to the most specific available criterion
        Page<Question> base = selectBaseResult(searchQuery);

        // Apply remaining filters in-memory
        List<Question> filtered = applyFilters(base.getContent(), searchQuery);

        // Default sort: Newest first (nulls last)
        filtered.sort(Comparator.comparing(
                (Question q) -> q.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // Paginate according to request
        return paginate(filtered, pageable);
    }

    public List<Question> getRelatedQuestions(Long questionId, int limit) {
        Question currentQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        if (currentQuestion.getTags() == null || currentQuestion.getTags().isEmpty()) {
            return new ArrayList<>();
        }

        Set<Tag> tagSet = new HashSet<>(currentQuestion.getTags());

        return questionRepository.findRelatedQuestionsByTags(
                tagSet,
                questionId,
                PageRequest.of(0, limit)
        );
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

        // Do not match tags for keyword searches; tags are handled by tag filters.
        return false;
    }

}
