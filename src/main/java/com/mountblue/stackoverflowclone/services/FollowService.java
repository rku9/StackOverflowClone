package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Follow;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.FollowRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.dtos.EmailTaskDto;
import com.mountblue.stackoverflowclone.workers.EmailQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final EmailQueue emailQueue;
    private final String appBaseUrl;
    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    public FollowService(FollowRepository followRepository,
                         UserRepository userRepository,
                         EmailQueue emailQueue,
                         @Value("${app.base-url}") String appBaseUrl) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.emailQueue = emailQueue;
        // Normalize and validate base URL
        if (appBaseUrl == null || appBaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Property 'app.base-url' must be set (e.g., https://your-domain)");
        }
        String base = appBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.appBaseUrl = base;
    }

    public boolean isFollowingQuestion(Long userId, Long questionId) {
        if (userId == null) return false;
        return followRepository.existsByUser_IdAndObjectTypeAndObjectId(userId, "question", questionId);
    }

    public void followQuestion(Long userId, Long questionId) {
        if (isFollowingQuestion(userId, questionId)) return;
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        Follow follow = new Follow();
        follow.setUser(user);
        follow.setObjectType("question");
        follow.setObjectId(questionId);
        followRepository.save(follow);
    }

    public void unfollowQuestion(Long userId, Long questionId) {
        followRepository.findByUser_IdAndObjectTypeAndObjectId(userId, "question", questionId)
                .ifPresent(followRepository::delete);
    }

    public List<User> getFollowersForQuestion(Long questionId) {
        return followRepository.findAllByObjectTypeAndObjectId("question", questionId)
                .stream()
                .map(Follow::getUser)
                .collect(Collectors.toList());
    }

    public void notifyFollowersOfNewAnswer(Question question, Answer answer) {
        Long questionId = question.getId();
        String title = question.getTitle() != null ? question.getTitle() : ("Question #" + questionId);

        List<User> followers = getFollowersForQuestion(questionId);
        if (followers == null || followers.isEmpty()) return;

        String subject = "New answer on: " + title;
        String link = appBaseUrl + "/questions/" + questionId + "#answer-" + answer.getId();

//        String answerAuthorEmail = answer.getAuthor() != null ? answer.getAuthor().getEmail() : null;

        for (User u : followers) {
            if (u == null || u.getEmail() == null) continue;
//            if (answerAuthorEmail != null && answerAuthorEmail.equalsIgnoreCase(u.getEmail())) continue;
            String personalizedBody =
                    "<div style=\"font-family: Arial, sans-serif; font-size: 14px; color: #333;\">" +
                            "<p>Hi " + u.getEmail() + ",</p>" +
                            "<p>An answer has been submitted to a question you follow.</p>" +
                            "<p><strong>Title:</strong> " + title + "</p>" +
                            "<p><a href=\"" + link + "\" style=\"color:#0a95ff;\">View the new answer</a></p>" +
                            "<hr style=\"border:none;border-top:1px solid #eee;\"/>" +
                            "<p style=\"color:#555;\">You received this because you follow this question.<br/>--<br/>StackOverflowClone</p>" +
                            "</div>";
            EmailTaskDto dto = new EmailTaskDto(u.getEmail(), subject, personalizedBody);
            boolean offered = emailQueue.offer(dto);
            logger.info("Email notify queued for follower: {} (offered={}) subject='{}'", u.getEmail(), offered, subject);
            if (!offered) {
                try {
                    emailQueue.enqueue(dto); // block if full to avoid dropping
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while enqueuing email task for {}", u.getEmail());
                }
            }
        }
    }
}
