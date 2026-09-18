package com.hospital.dao;

import com.hospital.model.PrescriptionItem;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code prescription_items} table (Phase 5).
 */
public interface PrescriptionItemDao {

    int create(int prescriptionId, String medicineName, String dosage,
               String frequency, String duration, String instructions);

    Optional<PrescriptionItem> findById(int id);

    List<PrescriptionItem> findByPrescriptionId(int prescriptionId);

    void update(int id, String medicineName, String dosage, String frequency,
                String duration, String instructions);

    void delete(int id);

    /** Delete every item belonging to the given prescription. */
    void deleteByPrescriptionId(int prescriptionId);

    int count();

    int countByPrescriptionId(int prescriptionId);
}
