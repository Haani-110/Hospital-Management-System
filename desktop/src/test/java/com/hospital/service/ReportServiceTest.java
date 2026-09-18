package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.AppointmentDao;
import com.hospital.dao.AppointmentDaoImpl;
import com.hospital.dao.BillDao;
import com.hospital.dao.BillDaoImpl;
import com.hospital.dao.BillItemDao;
import com.hospital.dao.BillItemDaoImpl;
import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DepartmentDaoImpl;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.DoctorDaoImpl;
import com.hospital.dao.PatientDao;
import com.hospital.dao.PatientDaoImpl;
import com.hospital.dao.ReportDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private DatabaseConnection db;
    private ReportService reportService;
    private DepartmentService departmentService;
    private DoctorService doctorService;
    private PatientService patientService;
    private AppointmentService appointmentService;
    private BillingService billingService;
    private ReportDao reportDao;
    private int appointmentSeq = 0;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();

        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        departmentService = new DepartmentService(deptDao);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        doctorService = new DoctorService(doctorDao, deptDao);
        PatientDao patientDao = new PatientDaoImpl(db);
        patientService = new PatientService(patientDao);
        AppointmentDao appointmentDao = new AppointmentDaoImpl(db);
        appointmentService = new AppointmentService(appointmentDao, patientDao, doctorDao);
        BillDao billDao = new BillDaoImpl(db);
        BillItemDao billItemDao = new BillItemDaoImpl(db);
        billingService = new BillingService(billDao, billItemDao, patientDao, appointmentDao, db);
        reportDao = new ReportDao(db);
        reportService = new ReportService(reportDao);
        appointmentSeq = 0;
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private void login(Role role) {
        User u = new User(999, role.name().toLowerCase(), "hash", role, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(u);
    }

    private Department seedDept(String name) {
        login(Role.ADMIN);
        return departmentService.createDepartment(name, "");
    }

    private Doctor seedDoctor(String name, Department d, String spec, BigDecimal fee) {
        login(Role.ADMIN);
        return doctorService.createDoctor(null, d.getId(), name, spec, "555-9000", "dr@h.com",
                fee.doubleValue());
    }

    private Patient seedPatient(String name) {
        login(Role.RECEPTIONIST);
        return patientService.createPatient(name, "1990-01-01", "Male", "555-1000",
                name.toLowerCase().replace(" ", "") + "@h.com", "addr", "Self", "555-1000", "O+");
    }

    private Appointment seedAppointment(Patient p, Doctor d, LocalDate date, String status) {
        // Auto-generate a non-conflicting time: 10:00, 11:00, 12:00, etc.
        String time = String.format("%02d:00", 10 + (appointmentSeq % 8));
        appointmentSeq++;
        return seedAppointment(p, d, date, time, status);
    }

    private Appointment seedAppointment(Patient p, Doctor d, LocalDate date, String time, String status) {
        login(Role.RECEPTIONIST);
        Appointment a = appointmentService.createAppointment(p.getId(), d.getId(),
                date.toString(), time, "Checkup for " + p.getFullName(), "");
        if ("COMPLETED".equals(status)) {
            login(Role.ADMIN);
            appointmentService.completeAppointment(a.getId());
        } else if ("CANCELLED".equals(status)) {
            // CANCEL is allowed for RECEPTIONIST and ADMIN
            login(Role.RECEPTIONIST);
            appointmentService.cancelAppointment(a.getId());
        }
        return a;
    }

    private Bill seedBill(Patient p, Appointment a, LocalDate date, String status, BigDecimal total) {
        login(Role.RECEPTIONIST);
        var item = new BillingService.BillItemInput("Consultation", "1", "500.00");
        var items = java.util.List.of(item);
        Bill b = billingService.createBill(p.getId(), a != null ? a.getId() : null,
                date.toString(), "50.00", "", items);
        if ("PARTIALLY_PAID".equals(status)) {
            billingService.markStatus(b.getId(), Bill.STATUS_PARTIALLY_PAID);
        } else if ("PAID".equals(status)) {
            login(Role.ADMIN);
            billingService.markStatus(b.getId(), Bill.STATUS_PARTIALLY_PAID);
            billingService.markStatus(b.getId(), Bill.STATUS_PAID);
        } else if ("CANCELLED".equals(status)) {
            login(Role.ADMIN);
            billingService.markStatus(b.getId(), Bill.STATUS_CANCELLED);
        }
        return b;
    }

    // ---------- authorization ----------

    @Test
    void unauthenticatedAccessThrows() {
        assertThrows(AuthorizationException.class, () -> reportService.getDashboardStats());
        assertThrows(AuthorizationException.class, () -> reportService.appointmentReport(null, null, null, null));
    }

    // ---------- dashboard ----------

    @Test
    void dashboardStatsUseSqlAggregates() {
        login(Role.ADMIN);
        Department d = seedDept("Cardio");
        Doctor doc = seedDoctor("Dr. Cardio", d, "Cardiology", new BigDecimal("500"));
        Patient p1 = seedPatient("Alice");
        Patient p2 = seedPatient("Bob");
        LocalDate today = LocalDate.now();
        // Use distinct times to avoid conflict on same doctor/day
        seedAppointment(p1, doc, today, "10:00", "SCHEDULED");
        seedAppointment(p2, doc, today, "11:00", "COMPLETED");
        seedAppointment(p1, doc, today.plusDays(1), "12:00", "SCHEDULED");
        seedBill(p1, null, today, "PAID", new BigDecimal("450.00"));
        seedBill(p2, null, today, "UNPAID", new BigDecimal("450.00"));
        seedBill(p1, null, today, "PARTIALLY_PAID", new BigDecimal("450.00"));

        var s = reportService.getDashboardStats();
        assertEquals(2, s.getTotalPatients());
        assertEquals(1, s.getActiveDoctors());
        assertEquals(2, s.getTodaysAppointments());
        // pending = SCHEDULED appointments (today + tomorrow) = 2
        assertEquals(2, s.getPendingAppointments());
        assertEquals(1, s.getCompletedAppointments());
        assertEquals(1, s.getUnpaidBills());
        assertEquals(1, s.getPartiallyPaidBills());
        assertTrue(s.getTodaysRevenue().compareTo(BigDecimal.ZERO) > 0);
    }

    // ---------- date validation ----------

    @Test
    void dateRangeFromAfterToThrows() {
        login(Role.ADMIN);
        assertThrows(ValidationException.class, () ->
                reportService.appointmentReport("2025-05-10", "2025-05-01", null, null));
    }

    @Test
    void nullDatesAreAllowed() {
        login(Role.ADMIN);
        var res = reportService.appointmentReport(null, null, null, null);
        assertNotNull(res.getRows());
    }

    @Test
    void invalidDateFormatThrows() {
        login(Role.ADMIN);
        assertThrows(ValidationException.class, () ->
                reportService.appointmentReport("not-a-date", null, null, null));
    }

    // ---------- appointment report ----------

    @Test
    void appointmentReportFilters() {
        login(Role.ADMIN);
        Department d = seedDept("Gen");
        Doctor doc = seedDoctor("Dr Gen", d, "GP", new BigDecimal("200"));
        Patient p = seedPatient("Charlie");
        LocalDate today = LocalDate.now();
        // All dates must be today or future to satisfy AppointmentService past-date validation
        // Use distinct times to avoid conflict
        seedAppointment(p, doc, today, "10:00", "SCHEDULED");
        seedAppointment(p, doc, today.plusDays(1), "11:00", "COMPLETED");
        seedAppointment(p, doc, today.plusDays(2), "12:00", "CANCELLED");

        var all = reportService.appointmentReport(null, null, null, null);
        assertEquals(3, all.getRows().size());

        var completed = reportService.appointmentReport(null, null, "COMPLETED", null);
        assertEquals(1, completed.getRows().size());
        assertEquals("COMPLETED", completed.getRows().get(0).get(6));

        var range = reportService.appointmentReport(
                today.toString(), today.toString(), null, null);
        assertEquals(1, range.getRows().size());

        var byName = reportService.appointmentReport(null, null, null, "charlie");
        assertEquals(3, byName.getRows().size());
    }

    // ---------- patient report ----------

    @Test
    void patientReportFilters() {
        login(Role.RECEPTIONIST);
        seedPatient("Diana");
        Patient p2 = seedPatient("Eve");
        patientService.deactivatePatient(p2.getId());

        var all = reportService.patientReport("ALL", null);
        assertEquals(2, all.getRows().size());

        var active = reportService.patientReport("ACTIVE", null);
        assertEquals(1, active.getRows().size());

        var inactive = reportService.patientReport("INACTIVE", null);
        assertEquals(1, inactive.getRows().size());

        var search = reportService.patientReport("ALL", "diana");
        assertEquals(1, search.getRows().size());
    }

    // ---------- doctor report ----------

    @Test
    void doctorReportFilters() {
        login(Role.ADMIN);
        Department d1 = seedDept("Cardio");
        Department d2 = seedDept("Neuro");
        Doctor dr1 = seedDoctor("Dr Heart", d1, "Cardiology", new BigDecimal("500"));
        seedDoctor("Dr Brain", d2, "Neurology", new BigDecimal("700"));
        doctorService.deactivateDoctor(dr1.getId());

        var all = reportService.doctorReport(0, "ALL", null);
        assertEquals(2, all.getRows().size());

        var byDept = reportService.doctorReport(d2.getId(), "ALL", null);
        assertEquals(1, byDept.getRows().size());
        assertEquals("Neuro", byDept.getRows().get(0).get(2));

        var active = reportService.doctorReport(0, "ACTIVE", null);
        assertEquals(1, active.getRows().size());

        var search = reportService.doctorReport(0, "ALL", "brain");
        assertEquals(1, search.getRows().size());
    }

    // ---------- medical record report ----------

    @Test
    void medicalRecordReportFilters() throws Exception {
        login(Role.ADMIN);
        Department d = seedDept("Gen");
        Doctor doc = seedDoctor("Dr G", d, "GP", new BigDecimal("100"));
        Patient p = seedPatient("Frank");
        Appointment a = seedAppointment(p, doc, LocalDate.now(), "COMPLETED");
        com.hospital.dao.MedicalRecordDao mrDao = new com.hospital.dao.MedicalRecordDaoImpl(db);
        MedicalRecordService mrService = new MedicalRecordService(mrDao, new AppointmentDaoImpl(db), new DoctorDaoImpl(db));
        login(Role.ADMIN);
        mrService.createRecord(a.getId(), "Flu", null, null, "Rest and fluids", LocalDate.now().toString());

        var all = reportService.medicalRecordReport(null, null, null);
        assertEquals(1, all.getRows().size());

        var bySearch = reportService.medicalRecordReport(null, null, "flu");
        assertEquals(1, bySearch.getRows().size());
    }

    // ---------- prescription report ----------

    @Test
    void prescriptionReportFilters() throws Exception {
        login(Role.ADMIN);
        Department d = seedDept("Gen");
        Doctor doc = seedDoctor("Dr G", d, "GP", new BigDecimal("100"));
        Patient p = seedPatient("Grace");
        Appointment a = seedAppointment(p, doc, LocalDate.now(), "COMPLETED");
        com.hospital.dao.MedicalRecordDao mrDao = new com.hospital.dao.MedicalRecordDaoImpl(db);
        MedicalRecordService mrService = new MedicalRecordService(mrDao, new AppointmentDaoImpl(db), new DoctorDaoImpl(db));
        login(Role.ADMIN);
        MedicalRecord mr = mrService.createRecord(a.getId(), "Cold", null, null, "Rest", LocalDate.now().toString());

        com.hospital.dao.PrescriptionDao prDao = new com.hospital.dao.PrescriptionDaoImpl(db);
        com.hospital.dao.PrescriptionItemDao piDao = new com.hospital.dao.PrescriptionItemDaoImpl(db);
        PrescriptionService prService = new PrescriptionService(prDao, piDao, mrDao, new DoctorDaoImpl(db), db);
        var prItem = new PrescriptionService.PrescriptionItemInput("Paracetamol", "500mg", "2x daily", "5 days", "Take with food");
        login(Role.ADMIN);
        prService.createPrescription(mr.getId(), LocalDate.now().toString(), null, java.util.List.of(prItem));

        var all = reportService.prescriptionReport(null, null, null);
        assertEquals(1, all.getRows().size());
        assertTrue(all.getRows().get(0).get(5).toString().contains("Paracetamol"));

        var byMed = reportService.prescriptionReport(null, null, "paracetamol");
        assertEquals(1, byMed.getRows().size());
    }

    // ---------- billing report ----------

    @Test
    void billingReportAggregatesWithoutDoubleCounting() {
        login(Role.ADMIN);
        Patient p = seedPatient("Henry");
        LocalDate today = LocalDate.now();
        Bill b = seedBill(p, null, today, "PAID", new BigDecimal("450.00"));

        var all = reportService.billingReport(null, null, null, null);
        assertEquals(1, all.getRows().size());
        assertEquals(1, all.getCount());
        assertTrue(all.getTotal().compareTo(BigDecimal.ZERO) > 0);

        var paid = reportService.billingReport(null, null, "PAID", null);
        assertEquals(1, paid.getCount());

        var unpaid = reportService.billingReport(null, null, "UNPAID", null);
        assertEquals(0, unpaid.getCount());

        var byNumber = reportService.billingReport(null, null, null, b.getBillNumber());
        assertEquals(1, byNumber.getRows().size());
    }

    @Test
    void emptySearchReturnsAll() {
        login(Role.ADMIN);
        seedPatient("Ivan");
        seedPatient("Jane");
        var all = reportService.patientReport("ALL", "");
        assertEquals(2, all.getRows().size());
    }
}
