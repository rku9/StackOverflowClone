package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.AnswerResponseDto;
import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionResponseDto;
import com.mountblue.stackoverflowclone.dtos.TagResponseDto;
import com.mountblue.stackoverflowclone.models.*;
import com.mountblue.stackoverflowclone.services.AnswerService;
import com.mountblue.stackoverflowclone.services.QuestionService;
import com.mountblue.stackoverflowclone.services.FollowService;
import com.mountblue.stackoverflowclone.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.security.Principal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final AnswerService answerService;
    private final FollowService followService;
    private final UserService userService;

    public QuestionController(QuestionService questionService, AnswerService answerService,
                              FollowService followService, UserService userService) {
        this.questionService = questionService;
        this.answerService = answerService;
        this.followService = followService;
        this.userService = userService;
    }

    @GetMapping
    public String getAllQuestions(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sort", required = false) String sortParam,
            @RequestParam(value = "filters", required = false) List<String> filterParams,
            @RequestParam(value = "daysOld", required = false) Integer daysOld,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @PageableDefault(size = 15) Pageable pageable,
            Model model){
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        // Unified fetch applying query, tags, filters and sort
        List<String> normalizedTags = tags == null ? List.of() : tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        List<FilterType> filterTypes = new java.util.ArrayList<>();
        if (filterParams != null) {
            for (String f : filterParams) {
                if (f == null) continue;
                String v = f.trim();
                if (v.equalsIgnoreCase("NoAnswers")) {
                    filterTypes.add(FilterType.NO_ANSWERS);
                } else if (v.equalsIgnoreCase("NoUpvotedOrAccepted")) {
                    filterTypes.add(FilterType.NO_UPVOTED_OR_ACCEPTED_ANSWER);
                }
            }
        }

        Page<Question> questionPage = questionService.getFilteredQuestions(
                pageable,
                query,
                normalizedTags,
                filterTypes,
                daysOld,
                sortParam
        );

        // Convert to DTO
        Page<QuestionResponseDto> questionResponseDtoPage = questionPage.map(question -> {
            String markdown = question.getBody();
            String html = renderer.render(parser.parse(markdown));
            String truncatedHtml = truncateHtml(html, 150);

            return new QuestionResponseDto(
                    question.getId(),
                    question.getAuthor().getName(),
                    question.getAuthor().getEmail(),
                    question.getTitle(),
                    truncatedHtml,
                    question.getCreatedAt(),
                    question.getUpdatedAt(),
                    question.getViewCount(),
                    question.getScore(),
                    question.getAnswers() != null ? question.getAnswers().size() : 0,
                    question.getComments(),
                    question.getTags().stream()
                            .map(tag -> new TagResponseDto(tag.getId(), tag.getName(), Collections.emptyList()))
                            .collect(Collectors.toList())
            );
        });

        // Add pagination attributes to model
        model.addAttribute("questionResponseDtoList", questionResponseDtoPage.getContent());
        model.addAttribute("page", questionResponseDtoPage);
        model.addAttribute("currentPage", questionResponseDtoPage.getNumber());
        model.addAttribute("totalPages", questionResponseDtoPage.getTotalPages());
        model.addAttribute("totalItems", questionResponseDtoPage.getTotalElements());

        // Preserve query parameters for pagination links
        model.addAttribute("query", query);
        model.addAttribute("sort", sortParam);
        model.addAttribute("filters", filterParams);
        model.addAttribute("daysOld", daysOld);
        model.addAttribute("tags", tags);
       return "questions";
    }

    @GetMapping("/new")
    public String showQuestionForm(Model model){
        model.addAttribute("questionForm", new QuestionFormDto(null, "", "", "", 0L));
        return "question-form";
    }

    @PostMapping("/new")
    public String submitQuestionForm(@ModelAttribute("questionForm") QuestionFormDto questionFormDto,
                                     BindingResult result,
                                     Model model,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        if (result.hasErrors()){
            return "question-form";
        }
        Question savedQuestion = questionService.createQuestion(questionFormDto, principal);

        return "redirect:/questions/" + savedQuestion.getId();
    }


    @DeleteMapping("/{id}")
    public String deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return "redirect:/questions";
    }

    @GetMapping("/{id}")
    public String getQuestion(@PathVariable Long id,
                              Model model,
                              Principal principal){
        // Increment view count for this hit
        questionService.incrementViewCount(id);
        Question question = questionService.findById(id).get();

        List<Tag> tags = question.getTags();
        List<Answer> answers = answerService.getAnswers(question.getId());
        answers.sort(Comparator.comparing(Answer::isAccepted).reversed());

        List<TagResponseDto> tagResponseDtoList = tags.stream()
                .map(tag -> new TagResponseDto(tag.getId(), tag.getName(), Collections.emptyList()))
                .collect(Collectors.toList());

        QuestionResponseDto questionResponseDto = new QuestionResponseDto(id,
                question.getAuthor().getName(),
                question.getAuthor().getEmail(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                question.getViewCount(),
                question.getScore(),
                answers != null ? answers.size() : 0,
                question.getComments(),
                tagResponseDtoList
                );
        model.addAttribute("question", questionResponseDto);
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        List<AnswerResponseDto> answerResponseDtos = answers.stream().map(answer -> {
            String markdownBody = answer.getBody() != null ? answer.getBody() : "";
            String htmlBody = renderer.render(parser.parse(markdownBody));
            return new AnswerResponseDto(
                    answer.getQuestion().getId(),
                    answer.getId(),
                    markdownBody,
                    htmlBody,
                    answer.getAuthor() != null ? answer.getAuthor().getName() : "Unknown",
                    answer.getAuthor().getEmail(),
                    answer.getCreatedAt(),
                    answer.getUpdatedAt(),
                    answer.getScore(),
                    answer.getComments(),
                    answer.isAccepted());
        }).toList();

        List<Question> relatedQuestions = questionService.getRelatedQuestions(id, 10);
        model.addAttribute("relatedQuestions", relatedQuestions);

        String markdown = questionResponseDto.body();
        String html = renderer.render(parser.parse(markdown));
        model.addAttribute("questionHtml", html);
        model.addAttribute("answers", answerResponseDtos);

        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user -> {
                boolean following = followService.isFollowingQuestion(user.getId(), id);
                model.addAttribute("isFollowing", following);
            });
        } else {
            model.addAttribute("isFollowing", false);
        }
        // Load answers also if needed
        return "question-show";
    }

    @GetMapping("/edit/{id}")
    public String showQuestionEditForm(@PathVariable Long id, Model model){
        Question question = questionService.findById(id).get();

        String tagString = question.getTags().stream().map(Tag::getName)
                .collect(Collectors.joining(", "));
        QuestionFormDto questionFormDto = new QuestionFormDto(id, question.getTitle(), question.getBody(), tagString, question.getAuthor().getId());
        model.addAttribute("questionForm", questionFormDto);
        // Load answers also if needed
        return "question-form";
    }

    @PatchMapping("/{id}")
    public String editQuestionDetails(@PathVariable Long id, @ModelAttribute("questionForm") QuestionFormDto questionForm,
                                      BindingResult result, Model model){
        Question updated = questionService.updateQuestion(id, questionForm);
        return "redirect:/questions/" + updated.getId();
    }

    @PostMapping("/vote/{id}")
    public String voteQuestion(@PathVariable Long id,
                               @RequestParam("choice") String choice,
                               @RequestParam("postType") String postType,
                               @AuthenticationPrincipal UserPrincipal principal,
                               Model model){
        Question question = questionService.findById(id).get();
        questionService.voteQuestion(question, choice, postType, principal, id);
        return "redirect:/questions/" + question.getId();
    }

    @PostMapping("/{questionId}/follow")
    public String followQuestion(@PathVariable Long questionId, Principal principal) {
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user ->
                    followService.followQuestion(user.getId(), questionId)
            );
        }
        return "redirect:/questions/" + questionId;
    }

    @PostMapping("/{id}/unfollow")
    public String unfollowQuestion(@PathVariable Long id, Principal principal) {
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user ->
                    followService.unfollowQuestion(user.getId(), id)
            );
        }
        return "redirect:/questions/" + id;
    }

    private String truncateHtml(String html, int maxLength) {
        // Replace img tags with dots
        String htmlWithDots = html.replaceAll("<img[^>]*>", "••• ");

        // Remove all HTML tags for plain text
        String plainText = htmlWithDots.replaceAll("<[^>]+>", "");

        if (plainText.length() <= maxLength) {
            return "<p>" + plainText + "</p>";
        }

        String truncated = plainText.substring(0, maxLength).trim() + "...";
        return "<p>" + truncated + "</p>";
    }
    @GetMapping("/search")
    public String searchAll(
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @PageableDefault(size = 15) Pageable pageable,
            Model model) {

        Page<QuestionResponseDto> searchResults = questionService.search(query, pageable);

        model.addAttribute("query", query);
        model.addAttribute("questions", searchResults.getContent());
        model.addAttribute("currentPage", searchResults.getNumber());
        model.addAttribute("totalPages", searchResults.getTotalPages());
        model.addAttribute("hasNext", searchResults.hasNext());
        model.addAttribute("hasPrevious", searchResults.hasPrevious());

        return "search-results";
    }
}
