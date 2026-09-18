package com.hospital.service;

import com.hospital.dao.DepartmentDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Department;
import com.hospital.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for managing hospital departments.
 *
 * <p>Validation rules (Phase 2.1):
 * <ul>
 *   <li>Name is required, trimmed, 1-100 characters, unique (case-insensitive).</li>
 *   <li>Description is optional, trimmed, max 255 characters.</li>
 * </ul>
 * Only users with role {@link Role#ADMIN} may create, update, or delete departments.
 * Viewing ({@link #getAllDepartments()}, {@link #getDepartment(int)}) is allowed
 * for any authenticated role (the UI can list departments for everyone), but
 * mutations are strictly enforced here regardless of what the UI shows.
 */
public class DepartmentService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final DepartmentDao departmentDao;

    public DepartmentService(DepartmentDao departmentDao) {
        this.departmentDao = departmentDao;
    }

    // ---------- view operations ----------

    public List<Department> getAllDepartments() {
        requireLoggedIn();
        return departmentDao.findAll();
    }

    public Department getDepartment(int id) {
        requireLoggedIn();
        return departmentDao.findById(id)
                .orElseThrow(() -> new ValidationException("Department not found (id=" + id + ")"));
    }

    // ---------- mutations (admin-only) ----------

    public Department createDepartment(String name, String description) {
        requireAdmin();

        String cleanedName = validateName(name);
        String cleanedDesc = validateDescription(description);

        if (departmentDao.existsByNameIgnoreCase(cleanedName, -1)) {
            throw new ValidationException("A department named \"" + cleanedName + "\" already exists.");
        }

        try {
            int id = departmentDao.create(cleanedName, cleanedDesc);
            return departmentDao.findById(id).orElseThrow(
                    () -> new DatabaseException("Department was created but could not be loaded back."));
        } catch (DatabaseException e) {
            // Surface DAO-level unique violations as validation errors so the UI
            // can show a friendly message.
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new ValidationException("A department named \"" + cleanedName + "\" already exists.");
            }
            throw e;
        }
    }

    public Department updateDepartment(int id, String name, String description) {
        requireAdmin();

        // Ensure department exists before we validate anything else.
        Department existing = departmentDao.findById(id)
                .orElseThrow(() -> new ValidationException("Department not found (id=" + id + ")"));

        String cleanedName = validateName(name);
        String cleanedDesc = validateDescription(description);

        // Case-insensitive duplicate check, excluding the department being updated.
        if (departmentDao.existsByNameIgnoreCase(cleanedName, id)) {
            throw new ValidationException("A department named \"" + cleanedName + "\" already exists.");
        }

        try {
            departmentDao.update(id, cleanedName, cleanedDesc);
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new ValidationException("A department named \"" + cleanedName + "\" already exists.");
            }
            throw e;
        }

        return departmentDao.findById(id).orElseThrow(
                () -> new DatabaseException("Department was updated but could not be loaded back."));
    }

    public void deleteDepartment(int id) {
        requireAdmin();

        Optional<Department> existing = departmentDao.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("Department not found (id=" + id + ")");
        }
        departmentDao.delete(id);
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
            throw new AuthorizationException("Only administrators can manage departments.");
        }
    }

    // ---------- validation helpers ----------

    private String validateName(String name) {
        if (name == null) {
            throw new ValidationException("Department name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Department name cannot be empty.");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    "Department name is too long (maximum " + MAX_NAME_LENGTH + " characters).");
        }
        return trimmed;
    }

    private String validateDescription(String description) {
        if (description == null) return null;
        String trimmed = description.trim();
        if (trimmed.isEmpty()) return null; // store NULL for blank descriptions
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException(
                    "Description is too long (maximum " + MAX_DESCRIPTION_LENGTH + " characters).");
        }
        return trimmed;
    }
}
