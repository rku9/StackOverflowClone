package com.mountblue.stackoverflowclone.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String redirectToQuestions() {
        return "redirect:/questions";
    }
}
