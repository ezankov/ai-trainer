package com.trainer.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "Username may only contain letters, digits, underscores, and hyphens")
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 255)
        String password
) {}
