package com.bookshop.security;

import com.bookshop.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

/**
 * Mints the application's OAuth2 tokens for the first-party sign-in flow.
 *
 * <p>The tokens are exactly the ones the authorization server issues: RS256
 * JWTs signed with the same key and published JWK set, carrying the same
 * claims, and registered in the same {@link OAuth2AuthorizationService}. That
 * means the resource server validates them without knowing which flow
 * produced them.
 */
@Service
public class TokenIssuer {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    /** 96 bytes of CSPRNG output, base64url encoded - not guessable. */
    private static final StringKeyGenerator REFRESH_TOKEN_GENERATOR =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private final JwtEncoder jwtEncoder;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final String issuer;

    public TokenIssuer(JwtEncoder jwtEncoder,
                       RegisteredClientRepository registeredClientRepository,
                       OAuth2AuthorizationService authorizationService,
                       @Value("${app.issuer-url}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.issuer = issuer;
    }

    /** Access token, refresh token and lifetime, as returned to the SPA. */
    public record Tokens(String accessToken, String refreshToken, long expiresInSeconds) {
    }

    /** A renewed token pair plus the id of the user it belongs to. */
    public record RefreshResult(Tokens tokens, Long userId) {
    }

    /**
     * Rotates a refresh token: the presented token is consumed and a brand-new
     * pair is issued. Replaying a used token therefore fails.
     *
     * <p>This lives here rather than on the {@code /oauth2/token} endpoint
     * because that endpoint's refresh grant requires client authentication,
     * and a browser-based public client has no secret to authenticate with.
     */
    public RefreshResult refresh(String refreshTokenValue) {
        OAuth2Authorization authorization =
                authorizationService.findByToken(refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);
        if (authorization == null) {
            throw new UnauthorizedException("Your session has expired - please sign in again");
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> stored = authorization.getRefreshToken();
        if (stored == null || !stored.isActive()) {
            authorizationService.remove(authorization);
            throw new UnauthorizedException("Your session has expired - please sign in again");
        }

        Authentication principal = authorization.getAttribute(Principal.class.getName());
        if (principal == null || !(principal.getPrincipal() instanceof BookshopUserDetails user)) {
            authorizationService.remove(authorization);
            throw new UnauthorizedException("Your session has expired - please sign in again");
        }

        // Single use: consume the old authorization before minting the new one.
        authorizationService.remove(authorization);
        return new RefreshResult(issue(user, principal), user.getId());
    }

    public Tokens issue(BookshopUserDetails user, Authentication authentication) {
        RegisteredClient client = registeredClientRepository.findByClientId(AuthorizationServerConfig.CLIENT_ID);
        if (client == null) {
            throw new IllegalStateException("The SPA client is not registered");
        }

        Instant issuedAt = Instant.now();
        Instant accessExpiresAt = issuedAt.plus(ACCESS_TOKEN_TTL);
        Set<String> scopes = client.getScopes();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .audience(java.util.List.of(client.getClientId()))
                .issuedAt(issuedAt)
                .expiresAt(accessExpiresAt)
                .claim("scope", String.join(" ", scopes))
                .claim("uid", user.getId())
                .claim("email", user.getUsername())
                .claim("name", user.getDisplayName())
                .build();

        String tokenValue = jwtEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, tokenValue, issuedAt, accessExpiresAt, scopes);
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                REFRESH_TOKEN_GENERATOR.generateKey(), issuedAt, issuedAt.plus(REFRESH_TOKEN_TTL));

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName(user.getUsername())
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizedScopes(scopes)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                // The refresh grant re-reads this principal to re-issue claims.
                .attribute(Principal.class.getName(), authentication)
                .build();
        authorizationService.save(authorization);

        return new Tokens(tokenValue, refreshToken.getTokenValue(), ACCESS_TOKEN_TTL.toSeconds());
    }
}
