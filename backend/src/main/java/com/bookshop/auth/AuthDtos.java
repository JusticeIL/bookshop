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
     * Full name policy: two or more words separated by single spaces, so
     * "Elad Ben David" is as valid as "Gal Rubinstein". Words are letters in
     * any alphabet, with the hyphen as the only permitted non-letter and only
     * between letters - "Jean-Pierre Dupont" passes, "%", digits, underscores,
     * leading/trailing hyphens and double spaces do not.
     *
     * <p>Every field is length-bounded, so an oversized payload is rejected by
     * validation rather than reaching the database.
     */
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank
            @Size(max = 120)
            @Pattern(regexp = "\\p{L}+(?:-\\p{L}+)*(?: \\p{L}+(?:-\\p{L}+)*)+",
                    message = "must be at least two names separated by a space, using letters "
                            + "and hyphens only")
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
