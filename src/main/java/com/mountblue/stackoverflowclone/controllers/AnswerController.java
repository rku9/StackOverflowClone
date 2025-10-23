package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.services.AnswerService;
import com.mountblue.stackoverflowclone.services.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/answers")
public class AnswerController {
    private final AnswerService answerService;

    public AnswerController(AnswerService answerService){
        this.answerService = answerService;
    }

    @PostMapping("/new")
    public String submitQuestionForm(@ModelAttribute("answerForm") AnswerFormDto answerFormDto,
                                     BindingResult result, Model model, @RequestParam("questionId") Long questionId) {
        if (result.hasErrors()){
            return "question-show";
        }
        Answer savedQuestion = answerService.saveAnswer(answerFormDto, questionId);

        return "redirect:/questions/" + questionId;
    }
}
