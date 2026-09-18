package com.hospital.service;

import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DoctorDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Doctor;
import com.hospital.model.Role;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Business logic for managing doctor profiles (Phase 2.3).
 *
 * <ul>
 *   <li>ADMIN: full CRUD + activate/deactivate + search.</li>
 *   <li>DOCTOR / RECEPTIONIST: view + search only (no mutations).</li>
 *   <li>Doctors are never physically deleted; use activate/deactivate.</li>
 * </ul>
 */
public class DoctorService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_SPECIALIZATION_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 100;

    // Very permissive sanity-check for emails; not RFC-perfect but rejects garbage.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final DoctorDao doctorDao;
    private final DepartmentDao departmentDao;

    public DoctorService(DoctorDao doctorDao, DepartmentDao departmentDao) {
        this.doctorDao = doctorDao;
        this.departmentDao = departmentDao;
    }

    // ---------- view operations (any authenticated user) ----------

    public List<Doctor> getAllDoctors() {
        requireLoggedIn();
        return doctorDao.findAll();
    }

    public List<Doctor> searchDoctors(String query, Integer departmentId) {
        requireLoggedIn();
        return doctorDao.search(query, departmentId == null ? 0 : departmentId);
    }

    public Doctor getDoctor(int id) {
        requireLoggedIn();
        return doctorDao.findById(id)
                .orElseThrow(() -> new ValidationException("Doctor not found (id=" + id + ")"));
    }

    // ---------- mutations (admin-only) ----------

    public Doctor createDoctor(Integer userId, Integer departmentId, String fullName,
                               String specialization, String phone, String email,
                               double consultationFee) {
        requireAdmin();

        String cleanedName = validateName(fullName);
        String cleanedSpec = validateSpecialization(specialization);
        int deptId = validateDepartment(departmentId);
        String cleanedPhone = validatePhone(phone);
        String cleanedEmail = validateEmail(email);
        double fee = validateFee(consultationFee);
        Integer cleanedUserId = (userId == null || userId <= 0) ? null : userId;

        if (cleanedUserId != null) {
            // Foreign-key integrity is enforced by SQLite too, but provide a friendly error.
            // We don't import UserDao here to keep coupling light; rely on FK constraint + a clear message.
        }

        try {
            int id = doctorDao.create(cleanedUserId, deptId, cleanedName, cleanedSpec,
                    cleanedPhone, cleanedEmail, fee);
            return doctorDao.findById(id).orElseThrow(
                    () -> new DatabaseException("Doctor was created but could not be loaded back."));
        } catch (DatabaseException e) {
            throw e;
        }
    }

    public Doctor updateDoctor(int id, Integer userId, Integer departmentId, String fullName,
                               String specialization, String phone, String email,
                               double consultationFee) {
        requireAdmin();

        if (!doctorDao.existsById(id)) {
            throw new ValidationException("Doctor not found (id=" + id + ")");
        }

        String cleanedName = validateName(fullName);
        String cleanedSpec = validateSpecialization(specialization);
        int deptId = validateDepartment(departmentId);
        String cleanedPhone = validatePhone(phone);
        String cleanedEmail = validateEmail(email);
        double fee = validateFee(consultationFee);
        Integer cleanedUserId = (userId == null || userId <= 0) ? null : userId;

        doctorDao.update(id, cleanedUserId, deptId, cleanedName, cleanedSpec,
                cleanedPhone, cleanedEmail, fee);
        return doctorDao.findById(id).orElseThrow(
                () -> new DatabaseException("Doctor was updated but could not be loaded back."));
    }

    public void deactivateDoctor(int id) {
        requireAdmin();
        Doctor d = doctorDao.findById(id)
                .orElseThrow(() -> new ValidationException("Doctor not found (id=" + id + ")"));
        if (!d.isActive()) {
            throw new ValidationException("This doctor is already inactive.");
        }
        doctorDao.updateActiveStatus(id, false);
    }

    public void activateDoctor(int id) {
        requireAdmin();
        Doctor d = doctorDao.findById(id)
                .orElseThrow(() -> new ValidationException("Doctor not found (id=" + id + ")"));
        if (d.isActive()) {
            throw new ValidationException("This doctor is already active.");
        }
        doctorDao.updateActiveStatus(id, true);
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
            throw new AuthorizationException("Only administrators can manage doctors.");
        }
    }

    // ---------- validation helpers ----------

    private String validateName(String name) {
        if (name == null) throw new ValidationException("Doctor's full name is required.");
        String t = name.trim();
        if (t.isEmpty()) throw new ValidationException("Doctor's full name cannot be empty.");
        if (t.length() > MAX_NAME_LENGTH)
            throw new ValidationException("Doctor name is too long (maximum " + MAX_NAME_LENGTH + " characters).");
        return t;
    }

    private String validateSpecialization(String spec) {
        if (spec == null) throw new ValidationException("Specialization is required.");
        String t = spec.trim();
        if (t.isEmpty()) throw new ValidationException("Specialization cannot be empty.");
        if (t.length() > MAX_SPECIALIZATION_LENGTH)
            throw new ValidationException("Specialization is too long (maximum " + MAX_SPECIALIZATION_LENGTH + " characters).");
        return t;
    }

    private int validateDepartment(Integer departmentId) {
        if (departmentId == null || departmentId <= 0) {
            throw new ValidationException("Department is required.");
        }
        if (departmentDao.findById(departmentId).isEmpty()) {
            throw new ValidationException("Selected department does not exist.");
        }
        return departmentId;
    }

    private String validatePhone(String phone) {
        if (phone == null) return null;
        String t = phone.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_PHONE_LENGTH)
            throw new ValidationException("Phone number is too long (maximum " + MAX_PHONE_LENGTH + " characters).");
        return t;
    }

    private String validateEmail(String email) {
        if (email == null) return null;
        String t = email.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_EMAIL_LENGTH)
            throw new ValidationException("Email is too long (maximum " + MAX_EMAIL_LENGTH + " characters).");
        if (!EMAIL_PATTERN.matcher(t).matches()) {
            throw new ValidationException("Email format is invalid.");
        }
        return t;
    }

    private double validateFee(double fee) {
        if (Double.isNaN(fee) || Double.isInfinite(fee)) {
            throw new ValidationException("Consultation fee must be a valid number.");
        }
        if (fee < 0) {
            throw new ValidationException("Consultation fee cannot be negative.");
        }
        return fee;
    }
}
