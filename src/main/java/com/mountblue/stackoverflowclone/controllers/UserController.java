package com.mountblue.stackoverflowclone.controllers;

import com.mountblue.stackoverflowclone.dtos.SignUpRequestDto;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.Tag;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.services.UserProfileService;
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

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
//@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final UserProfileService userProfileService;

    public UserController(UserService userService,
                          UserDetailsService userDetailsService,
                          UserProfileService userProfileService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.userProfileService = userProfileService;
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

    // Merged profile endpoints
    @GetMapping("/users/{userId}")
    public String showUserProfile(@PathVariable Long userId,
                                  @RequestParam(required = false, defaultValue = "newest") String tab,
                                  Principal principal,
                                  Model model) {
        maybeIncrementProfileViewCount(userId, principal);

        User user = userService.findById(userId);
        List<Question> userQuestions = userProfileService.getUserQuestions(userId);
        List<Answer> userAnswers = userProfileService.getUserAnswersWithQuestions(userId);
        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);

        userQuestions = sortQuestions(userQuestions, tab);

        model.addAttribute("profileUser", user);
        model.addAttribute("questions", userQuestions);
        model.addAttribute("answers", userAnswers);
        model.addAttribute("tagsWithCount", userTagsWithCount);
        model.addAttribute("totalQuestions", userQuestions.size());
        model.addAttribute("totalAnswers", userAnswers.size());
        model.addAttribute("currentTab", tab);
        model.addAttribute("activeSection", "questions");
        model.addAttribute("profileViews", user.getProfileViewCount());
        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));

        return "profile";
    }

    @GetMapping("/users/{userId}/questions")
    public String showUserQuestions(@PathVariable Long userId,
                                    @RequestParam(defaultValue = "newest") String sortBy,
                                    Principal principal,
                                    Model model) {
        maybeIncrementProfileViewCount(userId, principal);
        User user = userService.findById(userId);
        List<Question> questions = userProfileService.getUserQuestions(userId);
        questions = sortQuestions(questions, sortBy);

        List<Answer> userAnswers = userProfileService.getUserAnswersWithQuestions(userId);
        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);

        model.addAttribute("profileUser", user);
        model.addAttribute("questions", questions);
        model.addAttribute("answers", userAnswers);
        model.addAttribute("tagsWithCount", userTagsWithCount);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("totalAnswers", userAnswers.size());
        model.addAttribute("currentTab", sortBy);
        model.addAttribute("activeSection", "questions");
        model.addAttribute("profileViews", user.getProfileViewCount());
        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));
        return "profile";
    }

    @GetMapping("/users/{userId}/answers")
    public String showUserAnswers(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "newest") String sortBy,
                                  Principal principal,
                                  Model model) {
        maybeIncrementProfileViewCount(userId, principal);
        User user = userService.findById(userId);
        List<Answer> answers = userProfileService.getUserAnswersWithQuestions(userId);
        answers = sortAnswers(answers, sortBy);

        List<Question> userQuestions = userProfileService.getUserQuestions(userId);
        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);

        model.addAttribute("profileUser", user);
        model.addAttribute("questions", userQuestions);
        model.addAttribute("answers", answers);
        model.addAttribute("tagsWithCount", userTagsWithCount);
        model.addAttribute("totalQuestions", userQuestions.size());
        model.addAttribute("totalAnswers", answers.size());
        model.addAttribute("currentTab", sortBy);
        model.addAttribute("activeSection", "answers");
        model.addAttribute("profileViews", user.getProfileViewCount());
        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));
        return "profile";
    }

    @GetMapping("/users/{userId}/tags")
    public String showUserTags(@PathVariable Long userId,
                               @RequestParam(defaultValue = "popular") String sortBy,
                               Principal principal,
                               Model model) {
        maybeIncrementProfileViewCount(userId, principal);
        User user = userService.findById(userId);
        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);
        List<Question> userQuestions = userProfileService.getUserQuestions(userId);
        List<Answer> userAnswers = userProfileService.getUserAnswersWithQuestions(userId);

        model.addAttribute("profileUser", user);
        model.addAttribute("questions", userQuestions);
        model.addAttribute("answers", userAnswers);
        model.addAttribute("tagsWithCount", userTagsWithCount);
        model.addAttribute("totalQuestions", userQuestions.size());
        model.addAttribute("totalAnswers", userAnswers.size());
        model.addAttribute("activeSection", "tags");
        model.addAttribute("profileViews", user.getProfileViewCount());
        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));
        return "profile";
    }

    private void maybeIncrementProfileViewCount(Long profileUserId, Principal principal) {
        if (shouldCountProfileView(profileUserId, principal)) {
            userService.incrementProfileViewCount(profileUserId);
        }
    }

    private boolean shouldCountProfileView(Long profileUserId, Principal principal) {
        if (principal == null) return true;
        return userService.findByEmail(principal.getName())
                .map(viewer -> !viewer.getId().equals(profileUserId))
                .orElse(true);
    }

    private List<Question> sortQuestions(List<Question> questions, String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "score":
                questions.sort((q1, q2) -> Integer.compare(q2.getScore(), q1.getScore()));
                break;
            case "activity":
                questions.sort((q1, q2) -> {
                    if (q2.getUpdatedAt() == null && q1.getUpdatedAt() == null) return 0;
                    if (q2.getUpdatedAt() == null) return -1;
                    if (q1.getUpdatedAt() == null) return 1;
                    return q2.getUpdatedAt().compareTo(q1.getUpdatedAt());
                });
                break;
            case "views":
                questions.sort((q1, q2) -> Integer.compare(Math.toIntExact(q2.getViewCount()), Math.toIntExact(q1.getViewCount())));
                break;
            default: // newest
                questions.sort((q1, q2) -> q2.getCreatedAt().compareTo(q1.getCreatedAt()));
        }
        return questions;
    }

    private List<Answer> sortAnswers(List<Answer> answers, String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "score":
                answers.sort((a1, a2) -> Integer.compare(a2.getScore(), a1.getScore()));
                break;
            case "activity":
                answers.sort((a1, a2) -> {
                    if (a2.getUpdatedAt() == null && a1.getUpdatedAt() == null) return 0;
                    if (a2.getUpdatedAt() == null) return -1;
                    if (a1.getUpdatedAt() == null) return 1;
                    return a2.getUpdatedAt().compareTo(a1.getUpdatedAt());
                });
                break;
            default: // newest
                answers.sort((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()));
        }
        return answers;
    }

}
