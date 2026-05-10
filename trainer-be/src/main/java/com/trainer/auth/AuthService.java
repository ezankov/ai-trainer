package com.trainer.auth;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Core authentication service.
 *
 * <p>Handles user registration and login, and implements {@link UserDetailsService}
 * so Spring Security can load users by username during JWT filter processing.
 */
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Lazy JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new user.
     *
     * @param request validated registration payload
     * @return a {@link RegisterResponse} containing the new user's id and username
     * @throws UsernameAlreadyTakenException if the username is already in use
     * @throws EmailAlreadyTakenException    if the email is already in use
     */
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return new RegisterResponse(saved.getId(), saved.getUsername());
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user and issues a JWT.
     *
     * @param request login credentials
     * @return a {@link LoginResponse} containing the signed JWT
     * @throws UsernameNotFoundException if no user exists with the given username (→ 401)
     * @throws BadCredentialsException   if the password does not match (→ 401)
     * @throws DisabledException         if the user account is disabled (→ 401)
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + request.username()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token);
    }

    // -------------------------------------------------------------------------
    // UserDetailsService
    // -------------------------------------------------------------------------

    /**
     * Loads a {@link UserDetails} by username for Spring Security's filter chain.
     *
     * @param username the username to look up
     * @return the matching {@link User} entity
     * @throws UsernameNotFoundException if no user exists with that username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));
    }
}
