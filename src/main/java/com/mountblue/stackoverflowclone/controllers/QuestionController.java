package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.services.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
    public String submitQuestionForm(@ModelAttribute("questionForm") QuestionFormDto questionForm,
                                     BindingResult result, Model model) {
        if (result.hasErrors()){
            return "question-form";
        }
        System.out.println(questionForm);
        questionService.saveQuestion(questionForm);

        return "redirect:/questions/" + questionForm.id();
    }

    @GetMapping("/{id}")
    public String showQuestionDetails(@PathVariable Long id, Model model){
        QuestionFormDto questionFormDto = questionService.findById(id);
        model.addAttribute("question", questionFormDto);
        // Load answers also if needed
        return "question-details";
    }

    @PatchMapping("/{id}")
    public String editQuestionDetails(@PathVariable Long id, @ModelAttribute("questionForm") QuestionFormDto questionForm,
                                      BindingResult result, Model model){
        questionService.saveQuestion(questionForm);
        return "redirecr:/questions/" + questionForm.id();
    }

}
