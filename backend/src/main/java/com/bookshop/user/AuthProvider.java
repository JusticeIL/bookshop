package com.bookshop.user;

/**
 * Identity provider that owns a user account.
 *
 * <p>The application authenticates users locally (email + password) only.
 * The enum and the matching users table columns exist so that a federated
 * provider could be added later without a breaking schema migration.
 */
public enum AuthProvider {
    LOCAL
}
