package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartmentDaoImpl implements DepartmentDao {

    private final DatabaseConnection dbConnection;

    public DepartmentDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(String name, String description) {
        final String sql = "INSERT INTO departments (name, description) VALUES (?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            if (description == null) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, description);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated department id");
            }
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DatabaseException("A department with that name already exists: " + name, e);
            }
            throw new DatabaseException("Error creating department: " + name, e);
        }
    }

    @Override
    public Optional<Department> findById(int id) {
        final String sql = "SELECT id, name, description FROM departments WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up department id=" + id, e);
        }
    }

    @Override
    public Optional<Department> findByName(String name) {
        final String sql = "SELECT id, name, description FROM departments WHERE name = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up department name=" + name, e);
        }
    }

    @Override
    public List<Department> findAll() {
        List<Department> result = new ArrayList<>();
        final String sql = "SELECT id, name, description FROM departments ORDER BY name COLLATE NOCASE ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching departments", e);
        }
        return result;
    }

    @Override
    public void update(int id, String name, String description) {
        final String sql = "UPDATE departments SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            if (description == null) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, description);
            }
            ps.setInt(3, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Department not found (id=" + id + ")");
            }
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DatabaseException("A department with that name already exists: " + name, e);
            }
            throw new DatabaseException("Error updating department id=" + id, e);
        }
    }

    @Override
    public void delete(int id) {
        final String sql = "DELETE FROM departments WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            // Do not throw if no row was deleted; "no such department" is handled at service level.
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting department id=" + id, e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM departments";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting departments", e);
        }
    }

    @Override
    public boolean existsByNameIgnoreCase(String name, int excludeId) {
        final String sql;
        if (excludeId > 0) {
            sql = "SELECT COUNT(*) AS c FROM departments WHERE LOWER(name) = LOWER(?) AND id <> ?";
        } else {
            sql = "SELECT COUNT(*) AS c FROM departments WHERE LOWER(name) = LOWER(?)";
        }
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            if (excludeId > 0) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking department name existence", e);
        }
    }

    // ---------- helpers ----------

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setId(rs.getInt("id"));
        d.setName(rs.getString("name"));
        d.setDescription(rs.getString("description"));
        return d;
    }

    private boolean isUniqueViolation(SQLException e) {
        // SQLite returns SQLITE_CONSTRAINT (error code 19) for UNIQUE violations.
        // Different JDBC versions surface the message differently, so check both.
        return e.getErrorCode() == 19
                || (e.getMessage() != null
                    && (e.getMessage().toUpperCase().contains("UNIQUE")
                        || e.getMessage().contains("constraint failed")));
    }
}
