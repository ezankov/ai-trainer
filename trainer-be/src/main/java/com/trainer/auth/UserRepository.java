package com.trainer.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * auth-specific lookup methods used by registration and login flows.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether a user with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a user with that username exists, {@code false} otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether a user with the given email already exists.
     *
     * @param email the email address to check
     * @return {@code true} if a user with that email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
