package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.dtos.QuestionFormDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import com.mountblue.stackoverflowclone.services.AnswerService;
import com.mountblue.stackoverflowclone.services.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/answers")
public class AnswerController {
    private final AnswerService answerService;
    private final AnswerRepository answerRepository;

    public AnswerController(AnswerService answerService, AnswerRepository answerRepository){
        this.answerService = answerService;
        this.answerRepository = answerRepository;
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
    @PostMapping("/vote/{id}")
    public String voteAnswer(@PathVariable Long id, @RequestParam("choice") String choice, @RequestParam(
            "questionId") Long questionId, Model model){
        answerService.voteAnswer(id, choice);
        return "redirect:/questions/" + questionId;
    }
}
