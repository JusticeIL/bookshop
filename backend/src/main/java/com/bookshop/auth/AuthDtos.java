package com.bookshop.auth;

import com.bookshop.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request/response contracts for the auth API. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String displayName) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record UserDto(Long id, String email, String displayName, String authProvider) {

        public static UserDto from(User user) {
            return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(),
                    user.getAuthProvider().name());
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }

    /** Which social providers are configured; drives button rendering in the SPA. */
    public record ProvidersResponse(List<String> providers) {
    }
}
