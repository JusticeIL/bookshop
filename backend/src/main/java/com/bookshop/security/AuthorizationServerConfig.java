package com.bookshop.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Self-hosted OAuth2 Authorization Server (Spring Authorization Server -
 * Apache 2.0, running in-process; no third-party or paid identity provider).
 *
 * <p>The SPA is registered as a <b>public client</b> and uses the
 * Authorization Code flow with <b>PKCE</b> (S256) - the grant OAuth 2.1
 * prescribes for browser applications that cannot keep a client secret.
 * Tokens are RS256 JWTs published through a standard JWK Set, which the same
 * application consumes as a Resource Server (see {@link SecurityConfig}).
 *
 * <p>Endpoints exposed (all standard): {@code /oauth2/authorize},
 * {@code /oauth2/token}, {@code /oauth2/jwks},
 * {@code /.well-known/oauth-authorization-server}.
 */
@Configuration
public class AuthorizationServerConfig {

    public static final String CLIENT_ID = "bookshop-spa";

    /**
     * Filter chain for the OAuth2 protocol endpoints. Browser requests that
     * are not yet authenticated are sent to our own sign-in page.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerFilterChain(
            HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(org.springframework.security.config.Customizer.withDefaults());

        http
                // The SPA calls /oauth2/token cross-origin from its own domain.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }

    /**
     * The single first-party client: our React SPA. No client secret (it could
     * not keep one), PKCE mandatory, refresh tokens rotated on every use.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(
            @Value("${app.frontend-url}") String frontendUrl) {
        RegisteredClient spa = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(CLIENT_ID)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(frontendUrl + "/oauth2/callback")
                .redirectUri("http://localhost:5173/oauth2/callback")
                .postLogoutRedirectUri(frontendUrl)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)      // PKCE is mandatory
                        .requireAuthorizationConsent(false) // first-party app: no consent screen
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(24))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)  // rotation on every refresh
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(spa);
    }

    /**
     * Adds the application-specific claims the REST API needs, so protected
     * endpoints can identify the caller straight from the token.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            Object principal = context.getPrincipal().getPrincipal();
            if (principal instanceof BookshopUserDetails user) {
                context.getClaims()
                        .claim("uid", user.getId())
                        .claim("email", user.getUsername())
                        .claim("name", user.getDisplayName());
            }
        };
    }

    /**
     * RSA signing key for issued tokens, generated at startup and published at
     * /oauth2/jwks. Kept in memory deliberately: no key material is committed
     * to the repository or stored in the database. A restart therefore
     * invalidates outstanding tokens and users sign in again.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate the token signing key", ex);
        }
    }

    /** Validates our own tokens using the JWK set above. */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /** Signs tokens minted by the first-party sign-in endpoint (see TokenIssuer). */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Holds issued authorizations so the standard refresh-token grant can renew
     * them. In memory by design: no token material is persisted, and a restart
     * simply requires signing in again.
     */
    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
