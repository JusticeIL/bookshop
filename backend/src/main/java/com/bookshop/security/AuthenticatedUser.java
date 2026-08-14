package com.bookshop.security;

/** Lightweight principal placed in the SecurityContext after JWT validation. */
public record AuthenticatedUser(Long id, String email) {
}
