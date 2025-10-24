package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.AnswerResponseDto;
import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionResponseDto;
import com.mountblue.stackoverflowclone.dtos.TagResponseDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.Tag;
import com.mountblue.stackoverflowclone.services.AnswerService;
import com.mountblue.stackoverflowclone.services.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final AnswerService answerService;

    public QuestionController(QuestionService questionService, AnswerService answerService) {
        this.questionService = questionService;
        this.answerService = answerService;
    }

    @GetMapping
    public String getAllQuestions(Model model){
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        List<Question> questionResponseList =  questionService.getAllQuestions();
        List<QuestionResponseDto> questionResponseDtoList = questionResponseList.stream()
                .map(question -> {
                    // Parse markdown body to HTML
                    String markdown = question.getBody();
                    String html = renderer.render(parser.parse(markdown));

                    String truncatedHtml = truncateHtml(html, 150);

                    return new QuestionResponseDto(
                            question.getId(),
                            question.getAuthor().getUsername(),
                            question.getTitle(),
                            truncatedHtml,  // Use parsed HTML instead of raw markdown
                            question.getCreatedAt(),
                            question.getUpdatedAt(),
                            question.getViewCount(),
                            question.getScore(),
                            question.getTags().stream()
                                    .map(tag -> new TagResponseDto(tag.getId(), tag.getName(), Collections.emptyList()))
                                    .collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());
       model.addAttribute("questionResponseDtoList", questionResponseDtoList);
       return "questions";
    }

    @GetMapping("/new")
    public String showQuestionForm(Model model){
        model.addAttribute("questionForm", new QuestionFormDto(null, "", "", "", 0L));
        return "question-form";
    }

    @PostMapping("/new")
    public String submitQuestionForm(@ModelAttribute("questionForm") QuestionFormDto questionFormDto,
                                     BindingResult result, Model model) {
        if (result.hasErrors()){
            return "question-form";
        }
        System.out.println(questionFormDto);
        Question savedQuestion = questionService.createQuestion(questionFormDto);

        return "redirect:/questions/" + savedQuestion.getId();
    }


    @DeleteMapping("/{id}")
    public String deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return "redirect:/questions";
    }

    @GetMapping("/{id}")
    public String getQuestion(@PathVariable Long id, Model model){
        Question question = questionService.findById(id).get();

        List<Tag> tags = question.getTags();
        List<TagResponseDto> tagResponseDtoList = tags.stream()
                .map(tag -> new TagResponseDto(tag.getId(), tag.getName(), Collections.emptyList()))
                .collect(Collectors.toList());

        QuestionResponseDto questionResponseDto = new QuestionResponseDto(id,
                question.getAuthor().getUsername(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                question.getViewCount(),
                question.getScore(),
                tagResponseDtoList
                );
        model.addAttribute("question", questionResponseDto);
        List<Answer> answers = answerService.getAnswers(question.getId());
        List<AnswerResponseDto> answerResponseDtos = answers.stream().map(answer ->
            new AnswerResponseDto(
                    answer.getQuestion().getId(),
                    answer.getId(),
                    answer.getBody(),
                    answer.getAuthor() != null ? answer.getAuthor().getUsername() : "Unknown",
                    answer.getCreatedAt(),
                    answer.getUpdatedAt(),
                    answer.getScore())
        ).toList();
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String markdown = questionResponseDto.body();
        String html = renderer.render(parser.parse(markdown));
        model.addAttribute("questionHtml", html);
        model.addAttribute("answers", answerResponseDtos);
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
    public String voteQuestion(@PathVariable Long id, @RequestParam("choice") String choice, Model model){
        Question question = questionService.findById(id).get();
        questionService.voteQuestion(question, choice);
        return "redirect:/questions/" + question.getId();
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
}
