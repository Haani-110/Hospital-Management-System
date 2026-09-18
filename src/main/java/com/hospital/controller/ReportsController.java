package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Role;
import com.hospital.service.DepartmentService;
import com.hospital.service.ReportService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reports screen (Phase 7) offering appointment, patient, doctor, medical
 * record, prescription, and billing reports.
 */
public class ReportsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportService reportService;
    private final DepartmentService departmentService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenBilling;
    private Runnable onOpenUserManagement;

    private ComboBox<String> reportType;
    private TextField searchField;
    private DatePicker fromPicker;
    private DatePicker toPicker;
    private ComboBox<String> statusFilter;
    private ComboBox<String> activeFilter;
    private ComboBox<DepartmentOption> deptFilter;
    private TableView<List<Object>> table;
    private Label summaryLabel;
    private Label messageLabel;

    public ReportsController(ReportService reportService, DepartmentService departmentService,
                             SceneManager sceneManager) {
        this.reportService = reportService;
        this.departmentService = departmentService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }
    public void setOnOpenBilling(Runnable r) { this.onOpenBilling = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        Scene scene = new Scene(root, 1400, 800);
        applyCss(scene);
        return scene;
    }

    private Node buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(15, 20, 15, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        Label title = new Label("Reports");
        title.getStyleClass().add("page-title");
        Button back = new Button("← Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> { if (onBackToDashboard != null) onBackToDashboard.run(); });
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String roleText = Session.getInstance().getRole() != null ? Session.getInstance().getRole().name() : "UNKNOWN";
        String userText = Session.getInstance().getUsername() != null ? Session.getInstance().getUsername() : "unknown";
        Label info = new Label("Logged in as: " + userText + " (" + roleText + ")");
        info.getStyleClass().add("user-info");
        bar.getChildren().addAll(back, title, spacer, info);
        return bar;
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setMinWidth(220);
        sidebar.getStyleClass().add("sidebar");
        Label t = new Label("Hospital System"); t.getStyleClass().add("sidebar-title");
        boolean isAdmin = Session.getInstance().getRole() == Role.ADMIN;
        Button dash = navBtn("Dashboard", () -> { if (onBackToDashboard != null) onBackToDashboard.run(); });
        Button patients = navBtn("Patients", () -> { if (onOpenPatients != null) onOpenPatients.run(); });
        Button doctors = navBtn("Doctors", () -> { if (onOpenDoctors != null) onOpenDoctors.run(); });
        Button appts = navBtn("Appointments", () -> { if (onOpenAppointments != null) onOpenAppointments.run(); });
        Button records = navBtn("Medical Records", () -> { if (onOpenMedicalRecords != null) onOpenMedicalRecords.run(); });
        Button presc = navBtn("Prescriptions", () -> { if (onOpenPrescriptions != null) onOpenPrescriptions.run(); });
        Button billing = navBtn("Billing", () -> { if (onOpenBilling != null) onOpenBilling.run(); });
        Button depts = navBtn("Departments", () -> { if (onOpenDepartments != null) onOpenDepartments.run(); });
        depts.setVisible(isAdmin); depts.setManaged(isAdmin);
        Button users = navBtn("User Management", () -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });
        users.setVisible(isAdmin); users.setManaged(isAdmin);
        Label here = new Label("Reports");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));
        sidebar.getChildren().addAll(t, new Separator(), dash, patients, appts, here, records, presc, billing, doctors,
                depts, users);
        return sidebar;
    }

    private Button navBtn(String label, Runnable action) {
        Button b = new Button(label);
        b.getStyleClass().addAll("nav-item", "nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(b, new Insets(2, 0, 2, 0));
        b.setOnAction(e -> action.run());
        return b;
    }

    private Node buildContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        HBox topRow = new HBox(10);
        reportType = new ComboBox<>(FXCollections.observableArrayList(
                "Appointments", "Patients", "Doctors", "Medical Records", "Prescriptions", "Billing"));
        reportType.setValue("Appointments");
        reportType.setOnAction(e -> onReportTypeChanged());
        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setPrefWidth(300);
        topRow.getChildren().addAll(new Label("Report:"), reportType, new Label("Search:"), searchField);

        GridPane filterRow = new GridPane();
        filterRow.setHgap(10); filterRow.setVgap(6);
        fromPicker = new DatePicker();
        toPicker = new DatePicker();
        statusFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "SCHEDULED", "COMPLETED", "CANCELLED",
                "UNPAID", "PARTIALLY_PAID", "PAID"));
        statusFilter.setValue("ALL");
        activeFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "ACTIVE", "INACTIVE"));
        activeFilter.setValue("ALL");
        deptFilter = new ComboBox<>();
        reloadDepartments();
        Button apply = new Button("Apply"); apply.getStyleClass().add("primary-button");
        apply.setOnAction(e -> runReport());
        Button clear = new Button("Clear"); clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearFilters());

        filterRow.add(new Label("From:"), 0, 0);
        filterRow.add(fromPicker, 1, 0);
        filterRow.add(new Label("To:"), 2, 0);
        filterRow.add(toPicker, 3, 0);
        filterRow.add(new Label("Status:"), 4, 0);
        filterRow.add(statusFilter, 5, 0);
        filterRow.add(new Label("Active:"), 6, 0);
        filterRow.add(activeFilter, 7, 0);
        filterRow.add(new Label("Department:"), 8, 0);
        filterRow.add(deptFilter, 9, 0);
        filterRow.add(apply, 10, 0);
        filterRow.add(clear, 11, 0);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Run a report to see results."));
        VBox.setVgrow(table, Priority.ALWAYS);

        summaryLabel = new Label("");
        summaryLabel.getStyleClass().add("info-text");

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false); messageLabel.setManaged(false);

        content.getChildren().addAll(topRow, filterRow, table, summaryLabel, messageLabel);

        searchField.textProperty().addListener((o, a, b) -> runReport());
        reportType.setOnAction(e -> { onReportTypeChanged(); runReport(); });
        onReportTypeChanged();
        runReport();
        return content;
    }

    private void reloadDepartments() {
        deptFilter.getItems().clear();
        DepartmentOption all = new DepartmentOption(0, "All Departments");
        deptFilter.getItems().add(all);
        deptFilter.setValue(all);
        try {
            for (var d : departmentService.getAllDepartments()) {
                deptFilter.getItems().add(new DepartmentOption(d.getId(), d.getName()));
            }
        } catch (AuthorizationException | DatabaseException ex) {
            // silently degrade; dept filter will just have "All".
        }
    }

    private void onReportTypeChanged() {
        String type = reportType.getValue();
        boolean dates = "Appointments".equals(type) || "Medical Records".equals(type)
                || "Prescriptions".equals(type) || "Billing".equals(type);
        fromPicker.setDisable(!dates);
        toPicker.setDisable(!dates);
        boolean status = "Appointments".equals(type) || "Billing".equals(type);
        statusFilter.setDisable(!status);
        if ("Billing".equals(type)) {
            statusFilter.setItems(FXCollections.observableArrayList("ALL", "UNPAID", "PARTIALLY_PAID", "PAID", "CANCELLED"));
        } else {
            statusFilter.setItems(FXCollections.observableArrayList("ALL", "SCHEDULED", "COMPLETED", "CANCELLED"));
        }
        statusFilter.setValue("ALL");
        boolean active = "Patients".equals(type) || "Doctors".equals(type);
        activeFilter.setDisable(!active);
        activeFilter.setValue("ALL");
        boolean dept = "Doctors".equals(type);
        deptFilter.setDisable(!dept);
    }

    private void clearFilters() {
        searchField.clear();
        fromPicker.setValue(null);
        toPicker.setValue(null);
        statusFilter.setValue("ALL");
        activeFilter.setValue("ALL");
        deptFilter.setValue(deptFilter.getItems().get(0));
        runReport();
    }

    private void runReport() {
        clearMessage();
        try {
            String from = fromPicker.getValue() == null ? null : fromPicker.getValue().toString();
            String to = toPicker.getValue() == null ? null : toPicker.getValue().toString();
            String search = searchField.getText();
            String type = reportType.getValue();
            ReportService.ReportResult res;
            switch (type) {
                case "Patients" -> res = reportService.patientReport(activeFilter.getValue(), search);
                case "Doctors" -> {
                    int deptId = deptFilter.getValue() == null ? 0 : deptFilter.getValue().id;
                    res = reportService.doctorReport(deptId, activeFilter.getValue(), search);
                }
                case "Medical Records" -> res = reportService.medicalRecordReport(from, to, search);
                case "Prescriptions" -> res = reportService.prescriptionReport(from, to, search);
                case "Billing" -> {
                    ReportService.BillingReportResult br = reportService.billingReport(
                            from, to, statusFilter.getValue(), search);
                    renderTable(br.getColumns(), br.getRows());
                    summaryLabel.setText(String.format("Bills: %d   Subtotal: %,.2f   Discount: %,.2f   Total: %,.2f",
                            br.getCount(), br.getSubtotal(), br.getDiscount(), br.getTotal()));
                    return;
                }
                default -> res = reportService.appointmentReport(from, to, statusFilter.getValue(), search);
            }
            renderTable(res.getColumns(), res.getRows());
            summaryLabel.setText("Rows: " + res.getRows().size());
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void renderTable(List<String> columns, List<List<Object>> rows) {
        table.getColumns().clear();
        for (int i = 0; i < columns.size(); i++) {
            final int idx = i;
            TableColumn<List<Object>, Object> col = new TableColumn<>(columns.get(i));
            col.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().get(idx)));
            col.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
                @Override protected void updateItem(Object o, boolean empty) {
                    super.updateItem(o, empty);
                    if (empty || o == null) setText(null);
                    else if (o instanceof BigDecimal bd) setText(String.format("%,.2f", bd));
                    else if (o instanceof java.sql.Date d) setText(d.toString());
                    else setText(o.toString());
                }
            });
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(rows));
    }

    private void showError(String msg) {
        messageLabel.setText(msg == null ? "An error occurred." : msg);
        messageLabel.getStyleClass().add("error");
        messageLabel.setVisible(true); messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setVisible(false); messageLabel.setManaged(false); messageLabel.setText("");
    }

    private void applyCss(Scene scene) {
        try {
            String css = getClass().getResource("/com/hospital/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignore) {}
    }

    private static class DepartmentOption {
        final int id; final String name;
        DepartmentOption(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}
