package com.hospital.dao;

import com.hospital.model.Bill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code bills} table (Phase 6).
 */
public interface BillDao {

    /**
     * Insert a bill row with the given bill number and values. Returns the
     * generated integer id.
     */
    int create(String billNumber, int patientId, Integer appointmentId,
               String billDate, String status, String notes,
               BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount);

    Optional<Bill> findById(int id);

    Optional<Bill> findByBillNumber(String billNumber);

    List<Bill> findAll();

    void update(int id, Integer appointmentId, String billDate, String notes,
                BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount);

    void updateStatus(int id, String status);

    /**
     * Update bill number after insert (used for deterministic BILL-nnnnnn generation).
     */
    void updateBillNumber(int id, String billNumber);

    /**
     * Search bills by an arbitrary query string (bill number, patient, doctor,
     * item description) and optional status filter.
     */
    List<Bill> search(String query, String status);

    int count();
}
