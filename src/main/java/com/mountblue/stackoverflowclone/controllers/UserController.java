package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.SignUpRequestDto;
import com.mountblue.stackoverflowclone.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
//@RequestMapping("/users")
public class UserController {

    private UserService userService;
    private final UserDetailsService userDetailsService;

    public UserController(UserService userService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/login")
    public String showLoginForm(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        model.addAttribute("loginRequestDto", new com.mountblue.stackoverflowclone.dtos.LoginRequestDto());
        return "login";
    }

    @GetMapping("/signup")
    public String showRegistrationForm(Model model) {
        SignUpRequestDto signUpRequestDto = new SignUpRequestDto();
        model.addAttribute("signUpRequestDto", signUpRequestDto);
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute SignUpRequestDto signUpRequestDto, Model model, HttpServletRequest request) {
        String name = signUpRequestDto.getName();
        String email = signUpRequestDto.getEmail();
        String password = signUpRequestDto.getPassword();
        String confirmPassword = signUpRequestDto.getConfirmPassword();
        try {
            userService.register(name, email, password, confirmPassword);
            // Auto-login the user by setting Authentication in the SecurityContext
            UserDetails userDetails = userDetailsService.loadUserByUsername(name);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Ensure session is created so SecurityContext persists
            request.getSession(true);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("signUpRequestDto", signUpRequestDto);
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }
    }
}
