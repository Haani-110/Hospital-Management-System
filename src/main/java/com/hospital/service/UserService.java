package com.hospital.service;

import com.hospital.dao.UserDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.util.PasswordUtil;

import java.util.List;

/**
 * Business logic for managing user accounts (Phase 2.2).
 *
 * <ul>
 *   <li>Only {@link Role#ADMIN} may create/edit/activate/deactivate users or
 *       change passwords of other accounts.</li>
 *   <li>The currently logged-in ADMIN cannot deactivate themselves or change
 *       their own role away from ADMIN (prevents accidental lockout).</li>
 *   <li>Usernames are required, trimmed, max 50 chars, unique case-insensitively.</li>
 *   <li>Passwords are always hashed via {@link PasswordUtil}; plaintext is never stored.</li>
 * </ul>
 * Users are never physically deleted — accounts are activated/deactivated.
 */
public class UserService {

    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    // ---------- view operations ----------

    public List<User> getAllUsers() {
        requireLoggedIn();
        return userDao.findAll();
    }

    public User getUser(int id) {
        requireLoggedIn();
        return userDao.findById(id)
                .orElseThrow(() -> new ValidationException("User not found (id=" + id + ")"));
    }

    // ---------- mutations (admin-only) ----------

    public User createUser(String username, String rawPassword, String confirmPassword, Role role) {
        requireAdmin();

        String cleanedUsername = validateUsername(username);
        Role cleanedRole = validateRole(role);
        validateNewPassword(rawPassword, confirmPassword);

        if (userDao.existsByUsernameIgnoreCase(cleanedUsername, -1)) {
            throw new ValidationException("Username \"" + cleanedUsername + "\" is already in use.");
        }

        String hash = PasswordUtil.hash(rawPassword);
        try {
            int id = userDao.create(cleanedUsername, hash, cleanedRole);
            return userDao.findById(id).orElseThrow(
                    () -> new DatabaseException("User was created but could not be loaded back."));
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new ValidationException("Username \"" + cleanedUsername + "\" is already in use.");
            }
            throw e;
        }
    }

    public User updateUser(int id, String username, Role role) {
        requireAdmin();

        User existing = userDao.findById(id)
                .orElseThrow(() -> new ValidationException("User not found (id=" + id + ")"));

        String cleanedUsername = validateUsername(username);
        Role cleanedRole = validateRole(role);

        // Rule: the currently logged-in admin cannot change their own role away from ADMIN.
        User current = Session.getInstance().getCurrentUser();
        if (current != null && current.getId() == id) {
            if (cleanedRole != Role.ADMIN) {
                throw new ValidationException("You cannot change your own administrator role.");
            }
        }

        if (userDao.existsByUsernameIgnoreCase(cleanedUsername, id)) {
            throw new ValidationException("Username \"" + cleanedUsername + "\" is already in use.");
        }

        try {
            userDao.updateUser(id, cleanedUsername, cleanedRole);
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new ValidationException("Username \"" + cleanedUsername + "\" is already in use.");
            }
            throw e;
        }

        // If the admin is editing their own username, keep the session in sync.
        if (current != null && current.getId() == id) {
            current.setUsername(cleanedUsername);
            current.setRole(cleanedRole);
        }

        return userDao.findById(id).orElseThrow(
                () -> new DatabaseException("User was updated but could not be loaded back."));
    }

    public void changePassword(int id, String newPassword, String confirmPassword) {
        requireAdmin();

        userDao.findById(id)
                .orElseThrow(() -> new ValidationException("User not found (id=" + id + ")"));

        validateNewPassword(newPassword, confirmPassword);
        String hash = PasswordUtil.hash(newPassword);
        userDao.updatePassword(id, hash);
    }

    public void deactivateUser(int id) {
        requireAdmin();

        User existing = userDao.findById(id)
                .orElseThrow(() -> new ValidationException("User not found (id=" + id + ")"));

        User current = Session.getInstance().getCurrentUser();
        if (current != null && current.getId() == id) {
            throw new ValidationException("You cannot deactivate your current account.");
        }
        if (!existing.isActive()) {
            throw new ValidationException("This account is already inactive.");
        }
        userDao.updateActiveStatus(id, false);
    }

    public void activateUser(int id) {
        requireAdmin();

        User existing = userDao.findById(id)
                .orElseThrow(() -> new ValidationException("User not found (id=" + id + ")"));

        if (existing.isActive()) {
            throw new ValidationException("This account is already active.");
        }
        userDao.updateActiveStatus(id, true);
    }

    // ---------- authorization ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to perform this action.");
        }
    }

    private void requireAdmin() {
        requireLoggedIn();
        if (Session.getInstance().getRole() != Role.ADMIN) {
            throw new AuthorizationException("Only administrators can manage users.");
        }
    }

    // ---------- validation helpers ----------

    private String validateUsername(String username) {
        if (username == null) {
            throw new ValidationException("Username is required.");
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException(
                    "Username is too long (maximum " + MAX_USERNAME_LENGTH + " characters).");
        }
        return trimmed;
    }

    private Role validateRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        return role;
    }

    private void validateNewPassword(String password, String confirm) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (confirm == null || !password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
    }
}
