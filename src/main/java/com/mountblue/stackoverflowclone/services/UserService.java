package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getUserByUsername(String username){
        return userRepository.findByUsername(username).get();
    }

    public void register(String name, String email, String password, String confirmPassword) {
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));

    userRepository.save(user);
    }
}
