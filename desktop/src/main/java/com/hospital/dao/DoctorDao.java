package com.hospital.dao;

import com.hospital.model.Doctor;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code doctors} table.
 */
public interface DoctorDao {

    /** Persist a new doctor (active by default) and return the generated id. */
    int create(Integer userId, int departmentId, String fullName, String specialization,
               String phone, String email, double consultationFee);

    Optional<Doctor> findById(int id);

    /** Return every doctor ordered by full_name (case-insensitive). */
    List<Doctor> findAll();

    void update(int id, Integer userId, int departmentId, String fullName, String specialization,
                String phone, String email, double consultationFee);

    void updateActiveStatus(int id, boolean active);

    /**
     * Search doctors by matching a case-insensitive substring against full_name,
     * specialization, phone, or email. If {@code departmentId} > 0, also filter
     * to that department; pass 0 for "all departments".
     */
    List<Doctor> search(String query, int departmentId);

    /** @return true if a doctor row exists with the given id. */
    boolean existsById(int id);

    /** @return the doctor profile linked to the given user id, if any. */
    java.util.Optional<Doctor> findByUserId(int userId);

    int count();
}
