package com.hospital.dao;

import com.hospital.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code patients} table (Phase 2.4).
 */
public interface PatientDao {

    /**
     * Persist a new patient (active by default). The generated patient_code
     * must be supplied by the caller (service layer generates it).
     *
     * @return the generated row id
     */
    int create(String patientCode, String fullName, String dateOfBirth, String gender,
               String phone, String email, String address,
               String emergencyContactName, String emergencyContactPhone, String bloodGroup);

    Optional<Patient> findById(int id);

    Optional<Patient> findByPatientCode(String patientCode);

    /** @return every patient ordered by full_name (case-insensitive). */
    List<Patient> findAll();

    void update(int id, String fullName, String dateOfBirth, String gender,
                String phone, String email, String address,
                String emergencyContactName, String emergencyContactPhone, String bloodGroup);

    void updateActiveStatus(int id, boolean active);

    /**
     * Search patients by case-insensitive substring match on patient_code,
     * full_name, phone, or email. If {@code activeFilter} is non-null, also
     * restrict to active/inactive rows.
     */
    List<Patient> search(String query, Boolean activeFilter);

    boolean existsById(int id);

    boolean existsByPatientCode(String patientCode);

    /** @return count of all patient rows (active and inactive). */
    int count();

    /** @return the highest existing numeric suffix of codes like PAT-NNNNNN, or 0 if none. */
    int maxPatientCodeNumber();
}
