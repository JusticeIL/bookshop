package com.bookshop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security for the two faces of this application:
 *
 * <ol>
 *   <li><b>REST API</b> ({@code /api/**}) - a stateless OAuth2
 *       <i>Resource Server</i>. Protected endpoints require an
 *       {@code Authorization: Bearer <access token>} header holding an RS256
 *       JWT issued by this application, validated against its JWK set. No
 *       cookies are used for API calls, so CSRF protection is off there:
 *       an attacker's page cannot make the browser attach a bearer token.</li>
 *   <li><b>Authorization-server sign-in page</b> - session-backed form login
 *       with CSRF protection enabled, used by the {@code /oauth2/authorize}
 *       endpoint.</li>
 * </ol>
 *
 * Both chains emit a strict set of security headers (CSP, HSTS, no framing,
 * no MIME sniffing, minimal referrer and permissions policies).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * The API serves only JSON and never embeds anything, so it can advertise
     * the tightest possible policy: no scripts, no frames, no plugins.
     */
    private static final String API_CSP =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    /**
     * The sign-in page is the one HTML surface: its own styles are inlined,
     * and it posts only to this origin.
     */
    private static final String PAGE_CSP =
            "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                    + "script-src 'self'; object-src 'none'; frame-ancestors 'none'; "
                    + "base-uri 'self'; form-action 'self'";

    private final String frontendUrl;

    public SecurityConfig(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                // No cookie authentication on the API: nothing for CSRF to abuse.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> applyHardening(headers, API_CSP))
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain signInFilterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> applyHardening(headers, PAGE_CSP))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())
                .logout(logout -> logout
                        // Navigated to by the SPA's "Sign out", hence GET.
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessUrl(frontendUrl)
                        .permitAll());
        return http.build();
    }

    /** Response headers applied to every chain. */
    private static void applyHardening(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<HttpSecurity> headers,
            String contentSecurityPolicy) {
        headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.headerValue(
                        XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                // Written directly rather than through the configurer DSL: the
                // header writer API is stable across Spring Security versions.
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "geolocation=(), microphone=(), camera=(), payment=(), usb=()"))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000));
    }

    /**
     * Turns the validated access token into the {@link AuthenticatedUser}
     * principal the controllers expect - using the {@code uid} claim, so no
     * database round-trip is needed and no client-supplied id is ever trusted.
     */
    private static org.springframework.core.convert.converter.Converter<Jwt, AbstractAuthenticationToken>
            jwtAuthenticationConverter() {
        return jwt -> {
            Long userId = jwt.getClaim("uid") instanceof Number number ? number.longValue() : null;
            AuthenticatedUser principal = new AuthenticatedUser(userId, jwt.getClaimAsString("email"));
            return new UsernamePasswordAuthenticationToken(
                    principal, jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Verify the password even for unknown accounts, so response time does
        // not reveal whether an email is registered.
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }

    /** BCrypt with an explicit cost factor (2^12 rounds). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Strict CORS allow-list: only the deployed SPA origin and the local dev
     * server may call the API, and only with the headers/methods it needs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl, "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false); // bearer tokens, never cookies
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
