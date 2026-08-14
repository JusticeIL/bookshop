package com.bookshop.auth;

import com.bookshop.auth.AuthDtos.AuthResponse;
import com.bookshop.auth.AuthDtos.LoginRequest;
import com.bookshop.auth.AuthDtos.RegisterRequest;
import com.bookshop.auth.AuthDtos.UserDto;
import com.bookshop.common.BadRequestException;
import com.bookshop.common.NotFoundException;
import com.bookshop.security.JwtService;
import com.bookshop.user.AuthProvider;
import com.bookshop.user.User;
import com.bookshop.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = userRepository.save(User.local(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password())));
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(AuthService::invalidCredentials);
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException(
                    "This account uses %s sign-in - please use that button instead"
                            .formatted(user.getAuthProvider().name().toLowerCase(Locale.ROOT)));
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static BadRequestException invalidCredentials() {
        // Same message for unknown email and wrong password: no account enumeration.
        return new BadRequestException("Invalid email or password");
    }
}
