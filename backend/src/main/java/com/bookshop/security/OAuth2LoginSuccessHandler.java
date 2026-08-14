package com.bookshop.security;

import com.bookshop.user.AuthProvider;
import com.bookshop.user.User;
import com.bookshop.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Completes the OAuth2 handshake: maps the provider profile onto a local user
 * account, issues our own JWT, and hands control back to the SPA.
 *
 * <p>The token travels in the URL <em>fragment</em> (never sent to servers or
 * logged by proxies), where the SPA's /oauth2/redirect route picks it up.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository,
                                     JwtService jwtService,
                                     @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AuthProvider provider = AuthProvider.valueOf(
                token.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT));
        OAuth2User oauthUser = token.getPrincipal();

        String providerId = resolveProviderId(provider, oauthUser);
        String email = resolveEmail(provider, providerId, oauthUser);
        String name = Optional.ofNullable(oauthUser.<String>getAttribute("name")).orElse(email);

        Optional<User> existing = userRepository.findByAuthProviderAndProviderId(provider, providerId);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
        } else if (userRepository.existsByEmailIgnoreCase(email)) {
            // Deliberate decision: never silently merge a social identity into an
            // account registered under another provider (account-takeover vector).
            redirectWithError(response, "email-in-use");
            return;
        } else {
            user = userRepository.save(User.social(email, name, provider, providerId));
        }

        String redirect = frontendUrl + "/oauth2/redirect#token="
                + URLEncoder.encode(jwtService.issueToken(user), StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }

    private String resolveProviderId(AuthProvider provider, OAuth2User oauthUser) {
        // Google exposes the stable id as "sub", Facebook as "id".
        String attribute = provider == AuthProvider.GOOGLE ? "sub" : "id";
        return Optional.ofNullable(oauthUser.<Object>getAttribute(attribute))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("Provider returned no user id"));
    }

    private String resolveEmail(AuthProvider provider, String providerId, OAuth2User oauthUser) {
        // Facebook can legitimately omit the email (phone-registered accounts,
        // declined permission) - fall back to a synthetic, clearly-marked address.
        return Optional.ofNullable(oauthUser.<String>getAttribute("email"))
                .orElse(provider.name().toLowerCase(Locale.ROOT) + "_" + providerId + "@users.noreply.bookshop");
    }

    private void redirectWithError(HttpServletResponse response, String code) throws IOException {
        response.sendRedirect(frontendUrl + "/login?error=" + code);
    }
}
