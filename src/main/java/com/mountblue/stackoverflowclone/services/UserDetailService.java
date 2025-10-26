package com.mountblue.stackoverflowclone.services;


import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.models.UserPrincipal;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String key = email == null ? null : email.trim().toLowerCase();
        User user = userRepository.findByEmail(key)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new UserPrincipal(user);


    }
}
