package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Follow;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.FollowRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
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
    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    public FollowService(FollowRepository followRepository, UserRepository userRepository, EmailQueue emailQueue) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.emailQueue = emailQueue;
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
        String link = "/questions/" + questionId;

        String answerAuthorEmail = answer.getAuthor() != null ? answer.getAuthor().getEmail() : null;

        for (User u : followers) {
            if (u == null || u.getEmail() == null) continue;
            if (answerAuthorEmail != null && answerAuthorEmail.equalsIgnoreCase(u.getEmail())) continue;
            String personalizedBody = "Hi " + u.getEmail() + ",\n\n" +
                    "An answer has been submitted to a question you follow.\n" +
                    "Title: " + title + "\n" +
                    "Link: " + link + "\n\n" +
                    "You received this because you follow this question.\n--\nStackOverflowClone";
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
