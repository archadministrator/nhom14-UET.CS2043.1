package com.auction.config;

import com.auction.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tách UserDetailsService, AuthenticationProvider, PasswordEncoder ra file riêng.
 * Mục đích: tránh circular dependency giữa SecurityConfig ↔ JwtAuthFilter.
 *
 * Vòng lặp cũ:
 *   JwtAuthFilter  → inject UserDetailsService (bean trong SecurityConfig)
 *   SecurityConfig → inject JwtAuthFilter
 *   => Spring không biết khởi tạo cái nào trước → lỗi circular reference
 *
 * Sau khi tách:
 *   JwtAuthFilter  → inject UserDetailsService (bean trong ApplicationConfig ← không có JwtAuthFilter)
 *   SecurityConfig → inject JwtAuthFilter  ← OK, không còn vòng tròn
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}