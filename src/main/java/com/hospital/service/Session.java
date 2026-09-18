package com.hospital.service;

import com.hospital.model.Role;
import com.hospital.model.User;

/**
 * Very simple in-memory session that stores the currently authenticated user.
 * Phase 1 keeps this intentionally lightweight (no tokens, no HTTP).
 */
public class Session {
    private static Session instance;

    private User currentUser;

    private Session() {
    }

    public static synchronized Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String getUsername() {
        return currentUser == null ? null : currentUser.getUsername();
    }

    public Role getRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    public void clear() {
        this.currentUser = null;
    }

    /** Reset the singleton, primarily useful in tests. */
    public static void reset() {
        instance = null;
    }
}
