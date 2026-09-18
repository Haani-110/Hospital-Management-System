package com.hospital.dao;

import com.hospital.model.MedicalRecord;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code medical_records} table (Phase 4).
 */
public interface MedicalRecordDao {

    /**
     * Persist a new medical record and return the generated id.
     */
    int create(int appointmentId, int patientId, int doctorId,
               String diagnosis, String symptoms, String examination,
               String treatmentNotes, String recordDate);

    Optional<MedicalRecord> findById(int id);

    /** @return the medical record attached to the given appointment, if any. */
    Optional<MedicalRecord> findByAppointment(int appointmentId);

    List<MedicalRecord> findAll();

    void update(int id, String diagnosis, String symptoms, String examination,
                String treatmentNotes, String recordDate);

    /**
     * Search medical records with a case-insensitive substring query matched
     * against patient name, patient code, doctor name, diagnosis, or symptoms.
     */
    List<MedicalRecord> search(String query);

    /** @return true if a medical record already exists for the given appointment. */
    boolean existsByAppointment(int appointmentId);

    int count();
}
