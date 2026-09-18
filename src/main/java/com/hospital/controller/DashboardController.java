package com.hospital.controller;

import com.hospital.model.DashboardStats;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.service.AuthService;
import com.hospital.service.ReportService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import com.hospital.util.UiStyles;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Dashboard with stats cards, quick actions, and navigation (Phase 7).
 */
public class DashboardController {

    private final AuthService authService;
    private final SceneManager sceneManager;
    private final ReportService reportService;

    private Runnable onLogout;
    private Runnable onOpenDepartments;
    private Runnable onOpenUserManagement;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenBilling;
    private Runnable onOpenReports;

    private User currentUser;

    public DashboardController(AuthService authService, ReportService reportService, SceneManager sceneManager) {
        this.authService = authService;
        this.reportService = reportService;
        this.sceneManager = sceneManager;
    }

    public void setOnLogout(Runnable onLogout) { this.onLogout = onLogout; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }
    public void setOnOpenBilling(Runnable r) { this.onOpenBilling = r; }
    public void setOnOpenReports(Runnable r) { this.onOpenReports = r; }

    public void setCurrentUser(User user) { this.currentUser = user; }

    public Scene buildScene() {
        if (currentUser == null) currentUser = Session.getInstance().getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isReceptionist = currentUser != null && currentUser.getRole() == Role.RECEPTIONIST;
        boolean isDoctor = currentUser != null && currentUser.getRole() == Role.DOCTOR;
        boolean isStaff = isAdmin || isReceptionist || isDoctor;
        boolean isAdminOrReceptionist = isAdmin || isReceptionist;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // --- Sidebar ---
        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(16));
        sidebar.setMinWidth(0);
        sidebar.getStyleClass().add("sidebar");

        Label appTitle = new Label("Hospital System");
        appTitle.getStyleClass().add("sidebar-title");

        Label dashboardItem = new Label("Dashboard");
        dashboardItem.setMaxWidth(Double.MAX_VALUE);
        dashboardItem.getStyleClass().addAll("nav-item", "nav-item-active");

        Node patientsItem = navButtonIf("Patients", isStaff, onOpenPatients);
        Node doctorsItem = navButtonIf("Doctors", isAdminOrReceptionist, onOpenDoctors);
        Node departmentsItem = navButtonIf("Departments", isAdmin, onOpenDepartments);
        Node userManagementItem = navButtonIf("User Management", isAdmin, onOpenUserManagement);
        Node appointmentsItem = navButtonIf("Appointments", isStaff, onOpenAppointments);
        Node medicalRecordsItem = navButtonIf("Medical Records", isStaff, onOpenMedicalRecords);
        Node prescriptionsItem = navButtonIf("Prescriptions", isStaff, onOpenPrescriptions);
        Node billingItem = navButtonIf("Billing", isAdminOrReceptionist, onOpenBilling);
        Node reportsItem = navButtonIf("Reports", isStaff, onOpenReports);

        sidebar.getChildren().addAll(
                appTitle, new Separator(), dashboardItem, patientsItem, doctorsItem, departmentsItem,
                userManagementItem, appointmentsItem, medicalRecordsItem, prescriptionsItem, billingItem, reportsItem);

        // --- Top bar ---
        Label pageTitle = new Label("Dashboard");
        pageTitle.getStyleClass().add("page-title");
        String roleText = currentUser != null && currentUser.getRole() != null ? currentUser.getRole().name() : "UNKNOWN";
        String userText = currentUser != null ? currentUser.getUsername() : "unknown";
        Label userInfo = new Label("Logged in as: " + userText + " (" + roleText + ")");
        userInfo.getStyleClass().add("user-info");
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-button");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            currentUser = null;
            if (onLogout != null) onLogout.run();
        });
        VBox topBar = UiStyles.header(pageTitle, userInfo, logoutBtn);

        // --- Content ---
        VBox content = new VBox(20);
        content.setPadding(new Insets(22));
        content.setMinWidth(0);
        content.setAlignment(Pos.TOP_LEFT);

        Label welcome = new Label("Welcome, " + userText + "!");
        welcome.getStyleClass().add("welcome");
        welcome.setWrapText(true);

        DashboardStats stats;
        try {
            stats = reportService.getDashboardStats();
        } catch (Exception e) {
            stats = new DashboardStats(); // fail safe
        }

        FlowPane cards = new FlowPane(15, 15);
        cards.getChildren().addAll(
                statCard("Total Patients", String.valueOf(stats.getTotalPatients())),
                statCard("Active Doctors", String.valueOf(stats.getActiveDoctors())),
                statCard("Today's Appointments", String.valueOf(stats.getTodaysAppointments())),
                statCard("Pending Appointments", String.valueOf(stats.getPendingAppointments())),
                statCard("Completed Appointments", String.valueOf(stats.getCompletedAppointments())),
                statCard("Unpaid Bills", String.valueOf(stats.getUnpaidBills())),
                statCard("Partially Paid Bills", String.valueOf(stats.getPartiallyPaidBills())),
                statCard("Today's Revenue (Paid)", String.format("%,.2f", stats.getTodaysRevenue()))
        );

        Label quickTitle = new Label("Quick Actions");
        quickTitle.getStyleClass().add("section-title");
        FlowPane quick = new FlowPane(10, 10);
        addQuickAction(quick, isDoctor ? "Patients" : "+ Add Patient", isStaff, onOpenPatients);
        addQuickAction(quick, isDoctor ? "Appointments" : "New Appointment", isStaff, onOpenAppointments);
        addQuickAction(quick, "Medical Record", isStaff, onOpenMedicalRecords);
        addQuickAction(quick, "Prescription", isStaff, onOpenPrescriptions);
        addQuickAction(quick, "Billing", isAdminOrReceptionist, onOpenBilling);
        addQuickAction(quick, "Reports", isStaff, onOpenReports);
        addQuickAction(quick, "User Management", isAdmin, onOpenUserManagement);
        addQuickAction(quick, "Departments", isAdmin, onOpenDepartments);

        VBox quickPanel = new VBox(12, quickTitle,
                UiStyles.hint("Open a module to continue your work."), quick);
        quickPanel.getStyleClass().addAll("card", "quick-actions");
        VBox introduction = new VBox(6, welcome,
                UiStyles.hint("Patient care, appointments and daily operations."));
        content.getChildren().addAll(introduction, cards, quickPanel);

        BorderPane inner = new BorderPane();
        inner.setTop(topBar);
        inner.setCenter(UiStyles.scroll(content));
        root.setLeft(UiStyles.sidebar(sidebar));
        root.setCenter(inner);

        Scene scene = new Scene(root, 1200, 750);
        applyCss(scene);
        return scene;
    }

    private Node navButtonIf(String label, boolean visible, Runnable action) {
        Button btn = new Button(label);
        btn.setVisible(visible);
        btn.setManaged(visible);
        btn.getStyleClass().addAll("nav-item", "nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(btn, new Insets(2, 0, 2, 0));
        btn.setOnAction(e -> { if (action != null) action.run(); });
        return btn;
    }

    private void addQuickAction(FlowPane p, String label, boolean enabled, Runnable action) {
        if (!enabled) return;
        Button b = new Button(label);
        b.getStyleClass().add("secondary-button");
        b.setPadding(new Insets(10, 15, 10, 15));
        b.setOnAction(e -> { if (action != null) action.run(); });
        p.getChildren().add(b);
    }

    private VBox statCard(String title, String value) {
        VBox card = new VBox(6);
        card.setMinWidth(180);
        card.setPrefWidth(210);
        card.getStyleClass().addAll("card", "stat-card");
        Label t = new Label(title);
        t.getStyleClass().add("stat-label");
        t.setMinHeight(32);
        t.setWrapText(true);
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        v.setWrapText(true);
        v.setAccessibleText(title + ": " + value);
        if ("0".equals(value) || "0.00".equals(value) || "0,00".equals(value)) {
            v.getStyleClass().add("stat-zero");
        }
        card.getChildren().addAll(t, v);
        return card;
    }

    private void applyCss(Scene scene) {
        UiStyles.apply(scene);
    }
}
