package com.bookshop.auth;

import com.bookshop.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response contracts for the account endpoints of the REST API. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * Full name policy: exactly two words made of letters only (any alphabet),
     * separated by exactly one space - e.g. "Gal Rubinstein".
     *
     * <p>Every field is length-bounded, so an oversized payload is rejected by
     * validation rather than reaching the database.
     */
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank
            @Size(max = 120)
            @Pattern(regexp = "\\p{L}+ \\p{L}+",
                    message = "must be two names, letters only, separated by a single space")
            String fullName) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {
    }

    public record UserDto(Long id, String email, String displayName) {

        public static UserDto from(User user) {
            return new UserDto(user.getId(), user.getEmail(), user.getDisplayName());
        }
    }

    /** OAuth2-shaped token response returned to the SPA after sign-in. */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserDto user) {
    }
}
