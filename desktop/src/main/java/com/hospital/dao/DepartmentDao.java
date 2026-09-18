package com.hospital.dao;

import com.hospital.model.Department;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code departments} table.
 * All methods use prepared statements; SQL lives here only.
 */
public interface DepartmentDao {

    /** Persist a new department and return its generated id. */
    int create(String name, String description);

    /** Find a department by its primary key. */
    Optional<Department> findById(int id);

    /**
     * Find a department by name. The caller is responsible for any desired
     * case handling (e.g. passing a lower-cased name for case-insensitive lookup).
     */
    Optional<Department> findByName(String name);

    /** Return every department ordered by name. */
    List<Department> findAll();

    /** Update the name/description of an existing department. */
    void update(int id, String name, String description);

    /** Delete a department by id. No-op if the row does not exist. */
    void delete(int id);

    /** Total number of departments. */
    int count();

    /**
     * Case-insensitive check for whether any department already has the given
     * name, optionally excluding a specific id (for use during updates).
     *
     * @param name     name to look for (comparison is case-insensitive)
     * @param excludeId id to exclude from the check, or {@code -1} to not exclude
     */
    boolean existsByNameIgnoreCase(String name, int excludeId);
}
