package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.models.*;
import com.mountblue.stackoverflowclone.services.CommentService;
import com.mountblue.stackoverflowclone.services.QuestionService;
import com.mountblue.stackoverflowclone.services.AnswerService;
import com.mountblue.stackoverflowclone.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
public class CommentController {

    private final CommentService commentService;
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService,
                             QuestionService questionService,
                             AnswerService answerService,
                             UserService userService) {
        this.commentService = commentService;
        this.questionService = questionService;
        this.answerService = answerService;
        this.userService = userService;
    }

    @PostMapping("/questions/{questionId}/comments")
    public String addQuestionComment(@PathVariable Long questionId,
                                     @RequestParam String content,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        Question question = questionService.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        User user = userService.findByEmail(principal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));



        commentService.createQuestionComment(content, question, user);
        return "redirect:/questions/" + questionId;
    }

    @PostMapping("/answers/{answerId}/comments")
    public String addAnswerComment(@PathVariable Long answerId,
                                   @RequestParam String content,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        Answer answer = answerService.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
        User user = userService.findByEmail(principal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        commentService.createAnswerComment(content, answer, user);
        Long questionId = answer.getQuestion().getId();
        return "redirect:/questions/" + questionId;
    }

    @PostMapping("/questions/{questionId}/comments/{commentId}/delete")
    public String deleteQuestionComment(@PathVariable Long questionId,
                                        @PathVariable Long commentId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        if (!commentService.isCommentOwner(commentId, principal.getEmail())) {
            throw new AccessDeniedException("You don't have permission to delete this comment");
        }
        commentService.delete(commentId);
        return "redirect:/questions/" + questionId;
    }

    @PostMapping("/answers/{answerId}/comments/{commentId}/delete")
    public String deleteAnswerComment(@PathVariable Long answerId,
                                      @PathVariable Long commentId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (!commentService.isCommentOwner(commentId, principal.getEmail())) {
            throw new AccessDeniedException("You don't have permission to delete this comment");
        }
        Answer answer = answerService.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
        commentService.delete(commentId);
        return "redirect:/questions/" + answer.getQuestion().getId();
    }

    @PostMapping("/comments/{commentId}/edit")
    public String updateComment(@PathVariable Long commentId,
                                @RequestParam String content,
                                @RequestParam String returnUrl,
                                @AuthenticationPrincipal UserPrincipal principal) {
        if (!commentService.isCommentOwner(commentId, principal.getEmail())) {
            throw new AccessDeniedException("You don't have permission to edit this comment");
        }
        commentService.update(commentId, content);
        return "redirect:" + returnUrl;
    }
}
