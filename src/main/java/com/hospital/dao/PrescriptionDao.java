package com.hospital.dao;

import com.hospital.model.Prescription;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code prescriptions} table (Phase 5).
 */
public interface PrescriptionDao {

    /**
     * Persist a new prescription (items are inserted separately) and return the generated id.
     */
    int create(int medicalRecordId, int patientId, int doctorId, String prescriptionDate, String notes);

    Optional<Prescription> findById(int id);

    /** @return the prescription attached to the given medical record, if any. */
    Optional<Prescription> findByMedicalRecordId(int medicalRecordId);

    List<Prescription> findAll();

    void update(int id, String prescriptionDate, String notes);

    /**
     * Search prescriptions with a case-insensitive substring match against patient name,
     * patient code, doctor name, or medicine name (joined through items).
     */
    List<Prescription> search(String query);

    boolean existsByMedicalRecord(int medicalRecordId);

    int count();
}
