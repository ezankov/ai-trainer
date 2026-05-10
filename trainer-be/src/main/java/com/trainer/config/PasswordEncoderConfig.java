package com.trainer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Standalone configuration for the {@link PasswordEncoder} bean.
 *
 * <p>Extracted from {@link SecurityConfig} to break the circular dependency:
 * {@code AuthService} → {@code PasswordEncoder} → {@code SecurityConfig}
 * → {@code JwtAuthFilter} → {@code AuthService}.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
