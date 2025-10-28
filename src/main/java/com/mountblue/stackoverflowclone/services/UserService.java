package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(String name, String email, String password, String confirmPassword) {
        // Normalize inputs
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        // Basic server-side validations
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // Pre-check for duplicates
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered. Please log in instead.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return findByEmail(email).get();
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateReputation(Long userId, int reputationChange) {
        User user = findById(userId);
        user.setReputation(user.getReputation() + reputationChange);
        userRepository.save(user);
    }
}
