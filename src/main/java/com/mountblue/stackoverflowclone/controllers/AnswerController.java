package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.UserPrincipal;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import com.mountblue.stackoverflowclone.services.AnswerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public String submitAnswerForm(@ModelAttribute("answerForm") AnswerFormDto answerFormDto,
                                   BindingResult result,
                                   Model model,
                                   @RequestParam("questionId") Long questionId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        if (result.hasErrors()){
            return "question-show";
        }
        answerService.saveAnswer(answerFormDto, questionId, principal);

        return "redirect:/questions/" + questionId;
    }
    @PostMapping("/vote/{id}")
    public String voteAnswer(@PathVariable Long id,
                             @RequestParam("choice") String choice,
                             @RequestParam("questionId") Long questionId,
                             @RequestParam("postType") String postType,
                             @AuthenticationPrincipal UserPrincipal principal,
                             Model model){
        Answer answer = answerRepository.findById(id).get();
        answerService.voteAnswer(answer, choice, postType, principal, id);
        return "redirect:/questions/" + questionId;
    }

    @PostMapping("/accept/{answerId}")
    public String acceptAnswer(@PathVariable Long answerId,
                               @RequestParam("questionId") Long questionId,
                               @AuthenticationPrincipal UserPrincipal principal) {
        answerService.acceptAnswer(answerId, principal);
        return "redirect:/questions/" + questionId;
    }

    @PatchMapping("/edit/{answerId}")
    public String editAnswerDetails(@PathVariable Long answerId,
                                    @RequestParam("questionId") Long questionId,
                                    @RequestParam("body") String body) {
        answerService.editAnswerBody(answerId, questionId, body);
        return "redirect:/questions/" + questionId;
    }

    @DeleteMapping("/{id}")
    public String deleteAnswer(@PathVariable("id") Long answerId,
                               @RequestParam("questionId") Long questionId) {
        answerService.deleteAnswer(answerId);
        return "redirect:/questions/" + questionId;
    }
}
