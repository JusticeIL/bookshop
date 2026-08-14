package com.bookshop.auth;

import com.bookshop.auth.AuthDtos.LoginRequest;
import com.bookshop.auth.AuthDtos.RefreshRequest;
import com.bookshop.auth.AuthDtos.RegisterRequest;
import com.bookshop.auth.AuthDtos.TokenResponse;
import com.bookshop.auth.AuthDtos.UserDto;
import com.bookshop.common.BadRequestException;
import com.bookshop.common.NotFoundException;
import com.bookshop.common.UnauthorizedException;
import com.bookshop.security.BookshopUserDetails;
import com.bookshop.security.TokenIssuer;
import com.bookshop.user.User;
import com.bookshop.user.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    /**
     * A valid-shaped BCrypt hash of a random value. When no account matches the
     * submitted email we still verify the password against this, so a missing
     * account and a wrong password take the same time - closing the timing
     * side channel that would otherwise reveal which emails are registered.
     */
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa8Ub5m4v7yQ1QhO9xkOaVQqRUOD1t2W";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // Email is the login identity and is UNIQUE in the database; normalise
        // to lower case so casing can never split one person into two accounts.
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = userRepository.save(User.local(
                email,
                request.fullName().trim(),
                passwordEncoder.encode(request.password())));
        return issueFor(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(request.email().trim());
        String storedHash = candidate.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), storedHash);

        return candidate
                .filter(ignored -> passwordMatches)
                .map(this::issueFor)
                // Identical response for unknown email and wrong password:
                // no account enumeration.
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    }

    /** Rotates the refresh token and returns a fresh token pair. */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        TokenIssuer.RefreshResult result = tokenIssuer.refresh(request.refreshToken());
        UserDto user = userRepository.findById(result.userId())
                .map(UserDto::from)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return new TokenResponse(
                result.tokens().accessToken(),
                result.tokens().refreshToken(),
                result.tokens().expiresInSeconds(),
                user);
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private TokenResponse issueFor(User user) {
        BookshopUserDetails principal = new BookshopUserDetails(user);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        TokenIssuer.Tokens tokens = tokenIssuer.issue(principal, authentication);
        return new TokenResponse(
                tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds(),
                UserDto.from(user));
    }
}
