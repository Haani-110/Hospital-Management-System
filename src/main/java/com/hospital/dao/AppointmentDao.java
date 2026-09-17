package com.hospital.dao;

import com.hospital.model.Appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code appointments} table (Phase 3).
 */
public interface AppointmentDao {

    /**
     * Persist a new appointment (default status SCHEDULED) and return the generated id.
     */
    int create(int patientId, int doctorId, String appointmentDate, String appointmentTime,
               String reason, String notes);

    Optional<Appointment> findById(int id);

    /** @return every appointment ordered by date DESC, time ASC. */
    List<Appointment> findAll();

    void update(int id, int patientId, int doctorId, String appointmentDate, String appointmentTime,
                String reason, String notes);

    void updateStatus(int id, String status);

    /**
     * Search appointments with optional filters. All filters are nullable / zero = disabled.
     *
     * @param query       case-insensitive substring match against patient name/code,
     *                    doctor name, or reason
     * @param status      exact status match if non-null (SCHEDULED / COMPLETED / CANCELLED)
     * @param dateFilter  exact date match if non-null
     * @param doctorId    doctor filter if > 0
     */
    List<Appointment> search(String query, String status, LocalDate dateFilter, int doctorId);

    /**
     * Check whether the given doctor has an active (non-CANCELLED) appointment at the
     * specified date and time, optionally excluding a specific appointment id (used when
     * editing to avoid reporting the appointment itself as a conflict).
     */
    boolean hasConflict(int doctorId, String appointmentDate, String appointmentTime, int excludeId);

    int count();
}
