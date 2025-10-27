package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Follow;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.FollowRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
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
}
