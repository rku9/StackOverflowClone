package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.SignUpRequestDto;
import com.mountblue.stackoverflowclone.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
            model.addAttribute("errorMessage", "Invalid email or password.");
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
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Ensure session is created so SecurityContext persists
            request.getSession(true);
            return "redirect:/questions";
        } catch (IllegalArgumentException e) {
            model.addAttribute("signUpRequestDto", signUpRequestDto);
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/users")
    public String showAllUsers(
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "reputation") String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "36") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<com.mountblue.stackoverflowclone.dto.UserWithStatsDTO> userDTOPage;

        // Fetch users with all stats
        if (!filter.isEmpty()) {
            userDTOPage = userService.searchUsersWithStats(filter, pageable, tab);
        } else {
            userDTOPage = userService.getAllUsersWithStats(pageable, tab);
        }

        model.addAttribute("usersPage", userDTOPage);
        model.addAttribute("users", userDTOPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userDTOPage.getTotalPages());
        model.addAttribute("filter", filter);
        model.addAttribute("currentTab", tab);

        return "user-list";
    }

}
