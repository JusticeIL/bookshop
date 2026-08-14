package com.bookshop.auth;

import com.bookshop.auth.AuthDtos.AuthResponse;
import com.bookshop.auth.AuthDtos.LoginRequest;
import com.bookshop.auth.AuthDtos.ProvidersResponse;
import com.bookshop.auth.AuthDtos.RegisterRequest;
import com.bookshop.auth.AuthDtos.UserDto;
import com.bookshop.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    public AuthController(AuthService authService,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
        this.authService = authService;
        this.clientRegistrations = clientRegistrations;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.currentUser(user.id()));
    }

    /** Lets the SPA render only the social buttons that are actually configured. */
    @GetMapping("/providers")
    public ProvidersResponse providers() {
        List<String> providers = new ArrayList<>();
        ClientRegistrationRepository repository = clientRegistrations.getIfAvailable();
        if (repository instanceof InMemoryClientRegistrationRepository inMemory) {
            for (ClientRegistration registration : inMemory) {
                providers.add(registration.getRegistrationId());
            }
        }
        return new ProvidersResponse(providers);
    }
}
