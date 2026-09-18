package com.hospital.service;

import com.hospital.dao.PatientDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Patient;
import com.hospital.model.Role;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Business logic for managing patient records (Phase 2.4).
 *
 * <ul>
 *   <li>ADMIN: full CRUD + activate/deactivate + search.</li>
 *   <li>RECEPTIONIST: full CRUD + activate/deactivate + search (registers patients).</li>
 *   <li>DOCTOR: view + search only.</li>
 *   <li>Patients are never physically deleted; use activate/deactivate.</li>
 * </ul>
 */
public class PatientService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 250;
    private static final int MAX_EMERGENCY_NAME_LENGTH = 100;
    private static final int MAX_EMERGENCY_PHONE_LENGTH = 30;

    private static final Set<String> ALLOWED_GENDERS = Set.of(
            "Male", "Female", "Other", "Prefer not to say"
    );

    private static final Set<String> ALLOWED_BLOOD_GROUPS = Set.of(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    );

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final String CODE_PREFIX = "PAT-";

    private final PatientDao patientDao;

    public PatientService(PatientDao patientDao) {
        this.patientDao = patientDao;
    }

    // ---------- view operations (any authenticated user) ----------

    public List<Patient> getAllPatients() {
        requireLoggedIn();
        return patientDao.findAll();
    }

    public List<Patient> searchPatients(String query, Boolean activeFilter) {
        requireLoggedIn();
        return patientDao.search(query, activeFilter);
    }

    public Patient getPatient(int id) {
        requireLoggedIn();
        return patientDao.findById(id)
                .orElseThrow(() -> new ValidationException("Patient not found (id=" + id + ")"));
    }

    public Patient getPatientByCode(String code) {
        requireLoggedIn();
        if (code == null || code.isBlank()) {
            throw new ValidationException("Patient code is required.");
        }
        return patientDao.findByPatientCode(code.trim())
                .orElseThrow(() -> new ValidationException("Patient not found (code=" + code.trim() + ")"));
    }

    // ---------- mutations (admin or receptionist) ----------

    public Patient createPatient(String fullName, String dateOfBirth, String gender,
                                 String phone, String email, String address,
                                 String emergencyContactName, String emergencyContactPhone,
                                 String bloodGroup) {
        requireCanMutate();

        String cleanedName = validateName(fullName);
        String dob = validateDateOfBirth(dateOfBirth);
        String cleanedGender = validateGender(gender);
        String cleanedPhone = validatePhone(phone);
        String cleanedEmail = validateEmail(email);
        String cleanedAddress = validateAddress(address);
        String cleanedEmName = validateEmergencyName(emergencyContactName);
        String cleanedEmPhone = validateEmergencyPhone(emergencyContactPhone);
        String cleanedBg = validateBloodGroup(bloodGroup);

        String code = generateNextPatientCode();

        try {
            int id = patientDao.create(code, cleanedName, dob, cleanedGender, cleanedPhone, cleanedEmail,
                    cleanedAddress, cleanedEmName, cleanedEmPhone, cleanedBg);
            return patientDao.findById(id).orElseThrow(
                    () -> new DatabaseException("Patient was created but could not be loaded back."));
        } catch (DatabaseException e) {
            if (e.getCause() != null && e.getCause().getMessage() != null
                    && e.getCause().getMessage().toUpperCase().contains("UNIQUE")) {
                throw new DatabaseException("Patient code collision; please retry.", e);
            }
            throw e;
        }
    }

    public Patient updatePatient(int id, String fullName, String dateOfBirth, String gender,
                                 String phone, String email, String address,
                                 String emergencyContactName, String emergencyContactPhone,
                                 String bloodGroup) {
        requireCanMutate();

        if (!patientDao.existsById(id)) {
            throw new ValidationException("Patient not found (id=" + id + ")");
        }

        String cleanedName = validateName(fullName);
        String dob = validateDateOfBirth(dateOfBirth);
        String cleanedGender = validateGender(gender);
        String cleanedPhone = validatePhone(phone);
        String cleanedEmail = validateEmail(email);
        String cleanedAddress = validateAddress(address);
        String cleanedEmName = validateEmergencyName(emergencyContactName);
        String cleanedEmPhone = validateEmergencyPhone(emergencyContactPhone);
        String cleanedBg = validateBloodGroup(bloodGroup);

        patientDao.update(id, cleanedName, dob, cleanedGender, cleanedPhone, cleanedEmail,
                cleanedAddress, cleanedEmName, cleanedEmPhone, cleanedBg);
        return patientDao.findById(id).orElseThrow(
                () -> new DatabaseException("Patient was updated but could not be loaded back."));
    }

    public void deactivatePatient(int id) {
        requireCanMutate();
        Patient p = patientDao.findById(id)
                .orElseThrow(() -> new ValidationException("Patient not found (id=" + id + ")"));
        if (!p.isActive()) {
            throw new ValidationException("This patient is already inactive.");
        }
        patientDao.updateActiveStatus(id, false);
    }

    public void activatePatient(int id) {
        requireCanMutate();
        Patient p = patientDao.findById(id)
                .orElseThrow(() -> new ValidationException("Patient not found (id=" + id + ")"));
        if (p.isActive()) {
            throw new ValidationException("This patient is already active.");
        }
        patientDao.updateActiveStatus(id, true);
    }

    // ---------- authorization ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to perform this action.");
        }
    }

    private void requireCanMutate() {
        requireLoggedIn();
        Role r = Session.getInstance().getRole();
        if (r != Role.ADMIN && r != Role.RECEPTIONIST) {
            throw new AuthorizationException("Only administrators and receptionists can modify patient records.");
        }
    }

    // ---------- code generation ----------

    String generateNextPatientCode() {
        int n = patientDao.maxPatientCodeNumber() + 1;
        return CODE_PREFIX + String.format("%06d", n);
    }

    // ---------- validation helpers ----------

    private String validateName(String name) {
        if (name == null) throw new ValidationException("Patient's full name is required.");
        String t = name.trim();
        if (t.isEmpty()) throw new ValidationException("Patient's full name cannot be empty.");
        if (t.length() > MAX_NAME_LENGTH)
            throw new ValidationException("Patient name is too long (maximum " + MAX_NAME_LENGTH + " characters).");
        return t;
    }

    private String validateDateOfBirth(String dob) {
        if (dob == null) return null;
        String t = dob.trim();
        if (t.isEmpty()) return null;
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date of birth must be a valid date in YYYY-MM-DD format.");
        }
        if (parsed.isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }
        return parsed.toString();
    }

    private String validateGender(String gender) {
        if (gender == null) return null;
        String t = gender.trim();
        if (t.isEmpty()) return null;
        if (!ALLOWED_GENDERS.contains(t)) {
            throw new ValidationException("Gender must be one of: " + String.join(", ", ALLOWED_GENDERS) + ".");
        }
        return t;
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

    private String validateAddress(String address) {
        if (address == null) return null;
        String t = address.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_ADDRESS_LENGTH)
            throw new ValidationException("Address is too long (maximum " + MAX_ADDRESS_LENGTH + " characters).");
        return t;
    }

    private String validateEmergencyName(String name) {
        if (name == null) return null;
        String t = name.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_EMERGENCY_NAME_LENGTH)
            throw new ValidationException("Emergency contact name is too long (maximum " + MAX_EMERGENCY_NAME_LENGTH + " characters).");
        return t;
    }

    private String validateEmergencyPhone(String phone) {
        if (phone == null) return null;
        String t = phone.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_EMERGENCY_PHONE_LENGTH)
            throw new ValidationException("Emergency contact phone is too long (maximum " + MAX_EMERGENCY_PHONE_LENGTH + " characters).");
        return t;
    }

    private String validateBloodGroup(String bg) {
        if (bg == null) return null;
        String t = bg.trim();
        if (t.isEmpty()) return null;
        if (!ALLOWED_BLOOD_GROUPS.contains(t)) {
            throw new ValidationException("Blood group must be one of: " + String.join(", ", ALLOWED_BLOOD_GROUPS) + ".");
        }
        return t;
    }
}
