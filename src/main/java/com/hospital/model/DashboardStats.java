package com.hospital.model;

import java.math.BigDecimal;

/**
 * Aggregate statistics shown on the Dashboard (Phase 7).
 */
public class DashboardStats {
    private int totalPatients;
    private int activeDoctors;
    private int todaysAppointments;
    private int pendingAppointments;
    private int completedAppointments;
    private int unpaidBills;
    private int partiallyPaidBills;
    private BigDecimal todaysRevenue;

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public int getActiveDoctors() { return activeDoctors; }
    public void setActiveDoctors(int activeDoctors) { this.activeDoctors = activeDoctors; }

    public int getTodaysAppointments() { return todaysAppointments; }
    public void setTodaysAppointments(int todaysAppointments) { this.todaysAppointments = todaysAppointments; }

    public int getPendingAppointments() { return pendingAppointments; }
    public void setPendingAppointments(int pendingAppointments) { this.pendingAppointments = pendingAppointments; }

    public int getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(int completedAppointments) { this.completedAppointments = completedAppointments; }

    public int getUnpaidBills() { return unpaidBills; }
    public void setUnpaidBills(int unpaidBills) { this.unpaidBills = unpaidBills; }

    public int getPartiallyPaidBills() { return partiallyPaidBills; }
    public void setPartiallyPaidBills(int partiallyPaidBills) { this.partiallyPaidBills = partiallyPaidBills; }

    public BigDecimal getTodaysRevenue() { return todaysRevenue; }
    public void setTodaysRevenue(BigDecimal todaysRevenue) { this.todaysRevenue = todaysRevenue; }
}
