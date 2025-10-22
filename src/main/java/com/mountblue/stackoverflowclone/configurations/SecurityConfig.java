package com.mountblue.stackoverflowclone.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())               // disable CSRF if it's not needed (APIs)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()               // allow every request without auth
                )
                .httpBasic(Customizer.withDefaults());      // can be removed; keeps no login page

        return http.build();
    }
}
