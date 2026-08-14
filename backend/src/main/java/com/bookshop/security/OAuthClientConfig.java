package com.bookshop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-rolled OAuth2 client registrations for Google and Facebook.
 *
 * <p>Each provider is registered only when its client id/secret are supplied via
 * environment variables, so the application boots (and local email/password auth
 * keeps working) even before the developer-portal apps are configured. The
 * frontend discovers what is enabled through {@code GET /api/auth/providers}.
 */
@Configuration
public class OAuthClientConfig {

    public static final String GOOGLE = "google";
    public static final String FACEBOOK = "facebook";

    @Bean
    @ConditionalOnExpression(
            "!'${app.oauth.google.client-id:}'.isBlank() || !'${app.oauth.facebook.client-id:}'.isBlank()")
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth.google.client-id:}") String googleClientId,
            @Value("${app.oauth.google.client-secret:}") String googleClientSecret,
            @Value("${app.oauth.facebook.client-id:}") String facebookClientId,
            @Value("${app.oauth.facebook.client-secret:}") String facebookClientSecret) {

        List<ClientRegistration> registrations = new ArrayList<>();
        if (!googleClientId.isBlank()) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder(GOOGLE)
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .build());
        }
        if (!facebookClientId.isBlank()) {
            registrations.add(CommonOAuth2Provider.FACEBOOK.getBuilder(FACEBOOK)
                    .clientId(facebookClientId)
                    .clientSecret(facebookClientSecret)
                    .build());
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
