//package com.mountblue.stackoverflowclone.controllers;
//
//import com.mountblue.stackoverflowclone.models.Answer;
//import com.mountblue.stackoverflowclone.models.Question;
//import com.mountblue.stackoverflowclone.models.Tag;
//import com.mountblue.stackoverflowclone.models.User;
//import com.mountblue.stackoverflowclone.services.UserProfileService;
//import com.mountblue.stackoverflowclone.services.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//import java.util.List;
//import java.util.Map;
//
//// @Controller // disabled: merged into UserController
//// @RequestMapping("/users")
//public class UserProfileController {
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private UserProfileService userProfileService;
//
//    /**
//     * Main user profile page - shows questions by default
//     */
//    @GetMapping("/{userId}")
//    public String showUserProfile(@PathVariable Long userId,
//                                  @RequestParam(required = false, defaultValue = "newest") String tab,
//                                  Principal principal,
//                                  Model model) {
//        maybeIncrementProfileViewCount(userId, principal);
//
//        // Get user details
//        User user = userService.findById(userId);
//
//        // Get user's questions with tags
//        List<Question> userQuestions = userProfileService.getUserQuestions(userId);
//
//        // Get user's answers with questions
//        List<Answer> userAnswers = userProfileService.getUserAnswersWithQuestions(userId);
//
//        // Get tags used by user with count
//        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);
//
//        // Sort questions based on tab
//        userQuestions = sortQuestions(userQuestions, tab);
//
//        // Add attributes to model
//        model.addAttribute("questions", userQuestions);
//        model.addAttribute("answers", userAnswers);
//        model.addAttribute("tagsWithCount", userTagsWithCount);
//        model.addAttribute("totalQuestions", userQuestions.size());
//        model.addAttribute("totalAnswers", userAnswers.size());
//        model.addAttribute("activeSection", "tags");
//        model.addAttribute("profileViews", user.getProfileViewCount());
//        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));
//
//        return "profile";
//    }
//
//    /**
//     * Endpoint for questions tab with sorting
//     */
//    @GetMapping("/{userId}/questions")
//    public String showUserQuestions(@PathVariable Long userId,
//                                    @RequestParam(defaultValue = "newest") String sortBy,
//                                    Principal principal,
//                                    Model model) {
//        maybeIncrementProfileViewCount(userId, principal);
//        User user = userService.findById(userId);
//        List<Question> questions = userProfileService.getUserQuestions(userId);
//
//        // Sort based on sortBy parameter
//        questions = sortQuestions(questions, sortBy);
//
//        // Get other data for the full page
//        List<Answer> userAnswers = userProfileService.getUserAnswersWithQuestions(userId);
//        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);
//
//        model.addAttribute("profileUser", user);
//        model.addAttribute("questions", questions);
//        model.addAttribute("answers", userAnswers);
//        model.addAttribute("tagsWithCount", userTagsWithCount);
//        model.addAttribute("totalQuestions", questions.size());
//        model.addAttribute("totalAnswers", userAnswers.size());
//        model.addAttribute("currentTab", sortBy);
//        model.addAttribute("activeSection", "questions");
//        model.addAttribute("profileViews", user.getProfileViewCount());
//        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));
//
//        return "profile";
//    }
//
//    /**
//     * Endpoint for answers tab with sorting
//     */
//    @GetMapping("/{userId}/answers")
//    public String showUserAnswers(@PathVariable Long userId,
//                                  @RequestParam(defaultValue = "newest") String sortBy,
//                                  Principal principal,
//                                  Model model) {
//        maybeIncrementProfileViewCount(userId, principal);
//        User user = userService.findById(userId);
//        List<Answer> answers = userProfileService.getUserAnswersWithQuestions(userId);
//
//        // Sort answers based on sortBy parameter
//        answers = sortAnswers(answers, sortBy);
//
//        // Get other data for the full page
//        List<Question> userQuestions = userProfileService.getUserQuestions(userId);
//        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);
//
//        model.addAttribute("profileUser", user);
//        model.addAttribute("questions", userQuestions);
//        model.addAttribute("answers", answers);
//        model.addAttribute("tagsWithCount", userTagsWithCount);
//        model.addAttribute("totalQuestions", userQuestions.size());
//        model.addAttribute("totalAnswers", answers.size());
//        model.addAttribute("currentTab", sortBy);
//        model.addAttribute("activeSection", "answers");
//        model.addAttribute("profileViews", user.getProfileViewCount());
//        model.addAttribute("peopleReached", userProfileService.calculatePeopleReached(userId));
//
//        return "profile";
//    }
//
//    /**
//     * Endpoint for tags tab
//     */
//    @GetMapping("/{userId}/tags")
//    public String showUserTags(@PathVariable Long userId,
//                               @RequestParam(defaultValue = "popular") String sortBy,
//                               Principal principal,
//                               Model model) {
//        maybeIncrementProfileViewCount(userId, principal);
//        User user = userService.findById(userId);
//        Map<Tag, Long> userTagsWithCount = userProfileService.getUserTagsWithCount(userId);
//
//        // Sort tags if needed (already sorted by count in the query)
//
//        // Get other data for the full page
//        List<Question> userQuestions = userProfileService.getUserQuestions(userId);
//        List<Answer> userAnswers = userProfileService.getUserAnswersWithQuestions(userId);
//
//        model.addAttribute("profileUser", user);
//        model.addAttribute("questions", userQuestions);
//        model.addAttribute("answers", userAnswers);
//        model.addAttribute("tagsWithCount", userTagsWithCount);
//        model.addAttribute("totalAnswers", userAnswers.size());
//        model.addAttribute("activeSection", "tags");
//
//        return "profile";
//    }
//
//    private void maybeIncrementProfileViewCount(Long profileUserId, Principal principal) {
//        if (shouldCountProfileView(profileUserId, principal)) {
//            userService.incrementProfileViewCount(profileUserId);
//        }
//    }
//
//    private boolean shouldCountProfileView(Long profileUserId, Principal principal) {
//        if (principal == null) {
//            return true;
//        }
//        return userService.findByEmail(principal.getName())
//                .map(viewer -> !viewer.getId().equals(profileUserId))
//                .orElse(true);
//    }
//    /**
//     * Helper method to sort questions
//     */
//    private List<Question> sortQuestions(List<Question> questions, String sortBy) {
//        switch (sortBy.toLowerCase()) {
//            case "score":
//                questions.sort((q1, q2) -> Integer.compare(q2.getScore(), q1.getScore()));
//                break;
//            case "activity":
//                questions.sort((q1, q2) -> {
//                    if (q2.getUpdatedAt() == null && q1.getUpdatedAt() == null) return 0;
//                    if (q2.getUpdatedAt() == null) return -1;
//                    if (q1.getUpdatedAt() == null) return 1;
//                    return q2.getUpdatedAt().compareTo(q1.getUpdatedAt());
//                });
//                break;
//            case "views":
//                questions.sort((q1, q2) -> Integer.compare(Math.toIntExact(q2.getViewCount()), Math.toIntExact(q1.getViewCount())));
//                break;
//            default: // newest
//                questions.sort((q1, q2) -> q2.getCreatedAt().compareTo(q1.getCreatedAt()));
//        }
//        return questions;
//    }
//
//    /**
//     * Helper method to sort answers
//     */
//    private List<Answer> sortAnswers(List<Answer> answers, String sortBy) {
//        switch (sortBy.toLowerCase()) {
//            case "score":
//                answers.sort((a1, a2) -> Integer.compare(a2.getScore(), a1.getScore()));
//                break;
//            case "activity":
//                answers.sort((a1, a2) -> {
//                    if (a2.getUpdatedAt() == null && a1.getUpdatedAt() == null) return 0;
//                    if (a2.getUpdatedAt() == null) return -1;
//                    if (a1.getUpdatedAt() == null) return 1;
//                    return a2.getUpdatedAt().compareTo(a1.getUpdatedAt());
//                });
//                break;
//            default: // newest
//                answers.sort((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()));
//        }
//        return answers;
//    }
//}
