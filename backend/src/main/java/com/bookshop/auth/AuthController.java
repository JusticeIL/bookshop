package com.bookshop.auth;

import com.bookshop.auth.AuthDtos.LoginRequest;
import com.bookshop.auth.AuthDtos.RefreshRequest;
import com.bookshop.auth.AuthDtos.RegisterRequest;
import com.bookshop.auth.AuthDtos.TokenResponse;
import com.bookshop.auth.AuthDtos.UserDto;
import com.bookshop.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account endpoints of the REST API.
 *
 * <p>Sign-in happens here, in the application's own UI: the SPA posts
 * credentials over TLS and receives the same RS256 access token and rotating
 * refresh token the authorization server issues, which are then renewed
 * through {@code POST /api/auth/refresh}, which rotates the refresh token.
 *
 * <p>Both credential endpoints sit behind a per-IP rate limiter.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Rotates the refresh token; the presented one is consumed. */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.currentUser(user.id()));
    }
}
