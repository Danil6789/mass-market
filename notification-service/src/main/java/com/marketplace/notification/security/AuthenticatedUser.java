package com.marketplace.notification.security;

/**
 * Lightweight principal stored in the SecurityContext for the duration of
 * a single request. Carries the bits the controllers need without dragging
 * Spring Security's {@code UserDetails} boilerplate everywhere.
 */
public record AuthenticatedUser(Long id, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
