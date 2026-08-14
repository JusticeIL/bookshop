package com.bookshop.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** BCrypt hash of the account password. */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * The users table keeps multi-provider columns (auth_provider, provider_id)
     * for forward compatibility; the application currently supports LOCAL only.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected User() {
        // JPA
    }

    public static User local(String email, String displayName, String passwordHash) {
        User user = new User();
        user.email = email;
        user.displayName = displayName;
        user.passwordHash = passwordHash;
        user.authProvider = AuthProvider.LOCAL;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public String getProviderId() {
        return providerId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
