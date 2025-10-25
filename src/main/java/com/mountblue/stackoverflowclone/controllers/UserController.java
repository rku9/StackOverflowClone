package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.SignUpRequestDto;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginForm() {

        return "login";
    }

    //post mapping handled by the security config

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        SignUpRequestDto signUpRequestDto = new SignUpRequestDto();
        model.addAttribute("signUpRequestDto", signUpRequestDto);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(User user) {
        userService.saveUser(user);
        return "redirect:/users/login";
    }

}
