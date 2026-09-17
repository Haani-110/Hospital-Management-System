package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Role;
import com.hospital.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DatabaseConnection dbConnection;

    public UserDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(String username, String passwordHash, Role role) {
        final String sql = "INSERT INTO users (username, password_hash, role, is_active) VALUES (?, ?, ?, 1)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated user id");
            }
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DatabaseException("Username already exists: " + username, e);
            }
            throw new DatabaseException("Error creating user: " + username, e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        final String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up user id=" + id, e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        final String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users WHERE username = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up user: " + username, e);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        final String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users ORDER BY username COLLATE NOCASE ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching users", e);
        }
        return result;
    }

    @Override
    public void updateUser(int id, String username, Role role) {
        final String sql = "UPDATE users SET username = ?, role = ? WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, role.name());
            ps.setInt(3, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("User not found (id=" + id + ")");
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DatabaseException("Username already exists: " + username, e);
            }
            throw new DatabaseException("Error updating user id=" + id, e);
        }
    }

    @Override
    public void updatePassword(int id, String newPasswordHash) {
        final String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("User not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating password for user id=" + id, e);
        }
    }

    @Override
    public void updateActiveStatus(int id, boolean active) {
        final String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("User not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating active status for user id=" + id, e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM users";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting users", e);
        }
    }

    @Override
    public boolean existsByUsernameIgnoreCase(String username, int excludeId) {
        final String sql;
        if (excludeId > 0) {
            sql = "SELECT COUNT(*) AS c FROM users WHERE LOWER(username) = LOWER(?) AND id <> ?";
        } else {
            sql = "SELECT COUNT(*) AS c FROM users WHERE LOWER(username) = LOWER(?)";
        }
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            if (excludeId > 0) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking username existence", e);
        }
    }

    // ---------- helpers ----------

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.fromString(rs.getString("role")));
        u.setActive(rs.getInt("is_active") == 1);
        String createdStr = rs.getString("created_at");
        if (createdStr != null && !createdStr.isBlank()) {
            try {
                u.setCreatedAt(LocalDateTime.parse(createdStr, TS_FMT));
            } catch (Exception ex) {
                u.setCreatedAt(LocalDateTime.now());
            }
        } else {
            u.setCreatedAt(LocalDateTime.now());
        }
        return u;
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getErrorCode() == 19
                || (e.getMessage() != null
                    && (e.getMessage().toUpperCase().contains("UNIQUE")
                        || e.getMessage().contains("constraint failed")));
    }
}
