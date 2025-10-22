package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.Tag;
import com.mountblue.stackoverflowclone.services.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService){
        this.questionService = questionService;
    }

    @GetMapping("/new")
    public String showQuestionForm(Model model){
        model.addAttribute("questionForm", new QuestionFormDto(0L, "", "", "", 0L));
        return "question-form";
    }

    @PostMapping("/new")
    public String submitQuestionForm(@ModelAttribute("questionForm") QuestionFormDto questionFormDto,
                                     BindingResult result, Model model) {
        if (result.hasErrors()){
            return "question-form";
        }
        System.out.println(questionFormDto);
        Question savedQuestion = questionService.saveQuestion(questionFormDto);

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

        String tagString = question.getTags().stream().map(Tag::getName)
                        .collect(Collectors.joining(", "));
        QuestionFormDto questionFormDto = new QuestionFormDto(id, question.getTitle(), question.getBody(), tagString, question.getAuthor().getId());
        model.addAttribute("question", questionFormDto);
        // Load answers also if needed
        return "question-form";
    }

    @GetMapping("/edit/{id}")
    public String showQuestionEditForm(@PathVariable Long id, Model model){
        Question question = questionService.findById(id).get();

        String tagString = question.getTags().stream().map(Tag::getName)
                .collect(Collectors.joining(", "));
        QuestionFormDto questionFormDto = new QuestionFormDto(id, question.getTitle(), question.getBody(), tagString, question.getAuthor().getId());
        model.addAttribute("question", questionFormDto);
        // Load answers also if needed
        return "question-form";
    }

    @PatchMapping("/{id}")
    public String editQuestionDetails(@PathVariable Long id, @ModelAttribute("questionForm") QuestionFormDto questionForm,
                                      BindingResult result, Model model){
        questionService.saveQuestion(questionForm);
        return "redirect:/questions/" + questionForm.id();
    }

    @PostMapping("/vote/{id}")
    public String voteQuestion(@PathVariable Long id, @RequestParam("choice") String choice, Model model){
        Question question = questionService.findById(id).get();
        questionService.voteQuestion(question, choice);
        return "redirect:/questions/" + question.getId();
    }
}
