package com.hospital.dao;

import com.hospital.model.BillItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code bill_items} table (Phase 6).
 */
public interface BillItemDao {

    int create(int billId, String description, int quantity,
               BigDecimal unitPrice, BigDecimal amount);

    Optional<BillItem> findById(int id);

    List<BillItem> findByBillId(int billId);

    void update(int id, String description, int quantity,
                BigDecimal unitPrice, BigDecimal amount);

    void delete(int id);

    /** Delete every item belonging to the given bill. */
    void deleteAllByBillId(int billId);

    int count();
}
