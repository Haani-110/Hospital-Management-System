package com.hospital.model;

import java.time.LocalDateTime;

/**
 * Model representing a doctor's professional profile.
 *
 * A Doctor record is separate from a login {@link User} account. A doctor may
 * (optionally) be linked to a user_id for login purposes; otherwise the record
 * is just a profile managed by administrators.
 */
public class Doctor {
    private int id;
    private Integer userId;            // nullable - no login account required
    private int departmentId;
    private String fullName;
    private String specialization;
    private String phone;
    private String email;
    private double consultationFee;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient (joined) fields - populated by DAO for convenience in the UI.
    private String departmentName;
    private String username;

    public Doctor() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
