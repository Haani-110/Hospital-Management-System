package com.hospital.controller;

import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.service.AuthService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Dashboard shell. Sidebar items are placeholders except:
 * <ul>
 *   <li>"Dashboard" – shows this view (active)</li>
 *   <li>"Departments" – opens Department Management (ADMIN only)</li>
 *   <li>"Logout" button (top bar) returns to login</li>
 * </ul>
 * Other future modules remain disabled placeholders.
 */
public class DashboardController {

    private final AuthService authService;
    private final SceneManager sceneManager;

    private Runnable onLogout;
    private Runnable onOpenDepartments;
    private Runnable onOpenUserManagement;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;

    private User currentUser;

    public DashboardController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    public void setOnOpenDepartments(Runnable onOpenDepartments) {
        this.onOpenDepartments = onOpenDepartments;
    }

    public void setOnOpenUserManagement(Runnable onOpenUserManagement) {
        this.onOpenUserManagement = onOpenUserManagement;
    }

    public void setOnOpenDoctors(Runnable onOpenDoctors) {
        this.onOpenDoctors = onOpenDoctors;
    }

    public void setOnOpenPatients(Runnable onOpenPatients) {
        this.onOpenPatients = onOpenPatients;
    }

    public void setOnOpenAppointments(Runnable onOpenAppointments) {
        this.onOpenAppointments = onOpenAppointments;
    }

    public void setOnOpenMedicalRecords(Runnable onOpenMedicalRecords) {
        this.onOpenMedicalRecords = onOpenMedicalRecords;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public Scene buildScene() {
        if (currentUser == null) {
            currentUser = Session.getInstance().getCurrentUser();
        }
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // --- Sidebar ---
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setMinWidth(220);
        sidebar.getStyleClass().add("sidebar");

        Label appTitle = new Label("Hospital System");
        appTitle.getStyleClass().add("sidebar-title");

        // "Dashboard" is the active item.
        Node dashboardItem = navLabel("Dashboard", true, false);

        // Departments: button (styled like a nav item) when admin, otherwise a disabled label.
        Node departmentsItem;
        if (isAdmin) {
            Button btn = new Button("Departments");
            btn.getStyleClass().addAll("nav-item", "nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (onOpenDepartments != null) onOpenDepartments.run();
            });
            VBox.setMargin(btn, new Insets(2, 0, 2, 0));
            departmentsItem = btn;
        } else {
            departmentsItem = navLabel("Departments", false, true);
        }

        // User Management: admin-only nav button.
        Node userManagementItem;
        if (isAdmin) {
            Button btn = new Button("User Management");
            btn.getStyleClass().addAll("nav-item", "nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (onOpenUserManagement != null) onOpenUserManagement.run();
            });
            VBox.setMargin(btn, new Insets(2, 0, 2, 0));
            userManagementItem = btn;
        } else {
            userManagementItem = navLabel("User Management", false, true);
        }

        // Doctor Management: admin-only.
        Node doctorsItem;
        if (isAdmin) {
            Button btn = new Button("Doctors");
            btn.getStyleClass().addAll("nav-item", "nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (onOpenDoctors != null) onOpenDoctors.run();
            });
            VBox.setMargin(btn, new Insets(2, 0, 2, 0));
            doctorsItem = btn;
        } else {
            doctorsItem = navLabel("Doctors", false, true);
        }

        // Patient Management: any authenticated user can view (the service
        // enforces mutation restrictions). Show an enabled button for everyone
        // logged in since DOCTORs need view/search access and RECEPTIONISTs
        // have full CRUD alongside ADMIN.
        boolean isLoggedIn = currentUser != null && currentUser.getRole() != null;
        Node patientsItem;
        if (isLoggedIn) {
            Button btn = new Button("Patients");
            btn.getStyleClass().addAll("nav-item", "nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (onOpenPatients != null) onOpenPatients.run();
            });
            VBox.setMargin(btn, new Insets(2, 0, 2, 0));
            patientsItem = btn;
        } else {
            patientsItem = navLabel("Patients", false, true);
        }

        // Appointments: available to every authenticated user (service enforces
        // mutation permissions by role).
        Node appointmentsItem;
        if (isLoggedIn) {
            Button btn = new Button("Appointments");
            btn.getStyleClass().addAll("nav-item", "nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (onOpenAppointments != null) onOpenAppointments.run();
            });
            VBox.setMargin(btn, new Insets(2, 0, 2, 0));
            appointmentsItem = btn;
        } else {
            appointmentsItem = navLabel("Appointments", false, true);
        }

        // Medical Records: available to every authenticated user (service enforces
        // mutation permissions by role).
        Node medicalRecordsItem;
        if (isLoggedIn) {
            Button btn = new Button("Medical Records");
            btn.getStyleClass().addAll("nav-item", "nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (onOpenMedicalRecords != null) onOpenMedicalRecords.run();
            });
            VBox.setMargin(btn, new Insets(2, 0, 2, 0));
            medicalRecordsItem = btn;
        } else {
            medicalRecordsItem = navLabel("Medical Records", false, true);
        }

        sidebar.getChildren().addAll(
                appTitle,
                new Separator(),
                dashboardItem,
                patientsItem,
                doctorsItem,
                departmentsItem,
                userManagementItem,
                appointmentsItem,
                medicalRecordsItem,
                navLabel("Prescriptions", false, true),
                navLabel("Billing", false, true)
        );

        // --- Top bar ---
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("topbar");

        Label pageTitle = new Label("Dashboard");
        pageTitle.getStyleClass().add("page-title");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String roleText = currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole().name() : "UNKNOWN";
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

        topBar.getChildren().addAll(pageTitle, spacer, userInfo, logoutBtn);

        // --- Content area ---
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_LEFT);

        Label welcome = new Label("Welcome, " + userText + "!");
        welcome.getStyleClass().add("welcome");

        StringBuilder infoText = new StringBuilder("You are logged into the Hospital Management System.\n");
        if (isAdmin) {
            infoText.append("As an administrator you can manage departments and user accounts from the sidebar.");
        } else {
            infoText.append("Additional modules (Patients, Appointments, etc.) will be added in future phases.");
        }
        Label info = new Label(infoText.toString());
        info.setWrapText(true);
        info.getStyleClass().add("info-text");

        content.getChildren().addAll(welcome, info);

        BorderPane inner = new BorderPane();
        inner.setTop(topBar);
        inner.setCenter(content);

        root.setLeft(sidebar);
        root.setCenter(inner);

        Scene scene = new Scene(root, 1000, 650);
        applyCss(scene);
        return scene;
    }

    private Node navLabel(String label, boolean active, boolean disabled) {
        Label item = new Label(label);
        item.setMaxWidth(Double.MAX_VALUE);
        item.getStyleClass().add("nav-item");
        if (active) item.getStyleClass().add("nav-item-active");
        if (disabled) {
            item.setDisable(true);
            item.getStyleClass().add("nav-item-disabled");
        }
        VBox.setMargin(item, new Insets(2, 0, 2, 0));
        return item;
    }

    private void applyCss(Scene scene) {
        try {
            var cssUrl = getClass().getResource("/com/hospital/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }
}
