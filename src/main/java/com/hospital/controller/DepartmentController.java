package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Department;
import com.hospital.model.Role;
import com.hospital.service.DepartmentService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import com.hospital.util.UiStyles;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Department Management screen.
 *
 * <p>Only ADMIN users can create/edit/delete departments; the service enforces
 * this even if a non-admin somehow reaches this screen. The UI shows the
 * table read-only for everyone, but mutation buttons are only enabled for ADMIN.
 */
public class DepartmentController {

    private final DepartmentService departmentService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenUserManagement;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenBilling;
    private Runnable onOpenReports;

    private TableView<Department> table;
    private TextField nameField;
    private TextArea descriptionArea;
    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button deleteButton;
    private Button clearButton;

    /** The department currently being edited, or {@code null} when adding a new one. */
    private Department editingDepartment;

    public DepartmentController(DepartmentService departmentService, SceneManager sceneManager) {
        this.departmentService = departmentService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable onBackToDashboard) {
        this.onBackToDashboard = onBackToDashboard;
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

    public void setOnOpenPrescriptions(Runnable onOpenPrescriptions) {
        this.onOpenPrescriptions = onOpenPrescriptions;
    }

    public void setOnOpenBilling(Runnable onOpenBilling) {
        this.onOpenBilling = onOpenBilling;
    }

    public void setOnOpenReports(Runnable onOpenReports) {
        this.onOpenReports = onOpenReports;
    }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        BorderPane workspace = new BorderPane();
        workspace.setTop(buildTopBar());
        workspace.setCenter(buildContent());
        root.setLeft(UiStyles.sidebar(buildSidebar()));
        root.setCenter(workspace);

        Scene scene = new Scene(root, 1050, 680);
        applyCss(scene);
        refreshTable();
        resetForm();
        updateButtonsForRole();
        return scene;
    }

    // ---------- Top bar ----------

    private Node buildTopBar() {
        Label pageTitle = new Label("Department Management");
        pageTitle.getStyleClass().add("page-title");

        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        String roleText = Session.getInstance().getRole() != null
                ? Session.getInstance().getRole().name() : "UNKNOWN";
        String userText = Session.getInstance().getUsername() != null
                ? Session.getInstance().getUsername() : "unknown";
        Label userInfo = new Label("Logged in as: " + userText + " (" + roleText + ")");
        userInfo.getStyleClass().add("user-info");

        return UiStyles.header(pageTitle, userInfo, backBtn);
    }

    // ---------- Sidebar (same as dashboard for consistency) ----------

    private Node buildSidebar() {
        Role role = Session.getInstance().getRole();
        boolean isAdmin = role == Role.ADMIN;
        boolean isAdminOrReceptionist = isAdmin || role == Role.RECEPTIONIST;

        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(16));
        sidebar.setMinWidth(0);
        sidebar.getStyleClass().add("sidebar");

        Label appTitle = new Label("Hospital System");
        appTitle.getStyleClass().add("sidebar-title");

        Button dashboardBtn = new Button("Dashboard");
        dashboardBtn.getStyleClass().addAll("nav-item", "nav-button");
        dashboardBtn.setMaxWidth(Double.MAX_VALUE);
        dashboardBtn.setOnAction(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        Label departmentsItem = new Label("Departments");
        departmentsItem.setMaxWidth(Double.MAX_VALUE);
        departmentsItem.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(departmentsItem, new Insets(2, 0, 2, 0));

        // User Management nav button (admin only by construction of this screen)
        Button usersBtn = new Button("User Management");
        usersBtn.getStyleClass().addAll("nav-item", "nav-button");
        usersBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(usersBtn, new Insets(2, 0, 2, 0));
        usersBtn.setOnAction(e -> {
            if (onOpenUserManagement != null) onOpenUserManagement.run();
        });

        Button doctorsBtn = new Button("Doctors");
        doctorsBtn.getStyleClass().addAll("nav-item", "nav-button");
        doctorsBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(doctorsBtn, new Insets(2, 0, 2, 0));
        doctorsBtn.setOnAction(e -> {
            if (onOpenDoctors != null) onOpenDoctors.run();
        });

        Button patientsBtn = new Button("Patients");
        patientsBtn.getStyleClass().addAll("nav-item", "nav-button");
        patientsBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(patientsBtn, new Insets(2, 0, 2, 0));
        patientsBtn.setOnAction(e -> {
            if (onOpenPatients != null) onOpenPatients.run();
        });

        Button apptBtn = new Button("Appointments");
        apptBtn.getStyleClass().addAll("nav-item", "nav-button");
        apptBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(apptBtn, new Insets(2, 0, 2, 0));
        apptBtn.setOnAction(e -> {
            if (onOpenAppointments != null) onOpenAppointments.run();
        });

        Button recordsBtn = new Button("Medical Records");
        recordsBtn.getStyleClass().addAll("nav-item", "nav-button");
        recordsBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(recordsBtn, new Insets(2, 0, 2, 0));
        recordsBtn.setOnAction(e -> {
            if (onOpenMedicalRecords != null) onOpenMedicalRecords.run();
        });

        Button prescBtn = new Button("Prescriptions");
        prescBtn.getStyleClass().addAll("nav-item", "nav-button");
        prescBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(prescBtn, new Insets(2, 0, 2, 0));
        prescBtn.setOnAction(e -> {
            if (onOpenPrescriptions != null) onOpenPrescriptions.run();
        });

        Button billingBtn = new Button("Billing");
        billingBtn.getStyleClass().addAll("nav-item", "nav-button");
        billingBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(billingBtn, new Insets(2, 0, 2, 0));
        billingBtn.setOnAction(e -> {
            if (onOpenBilling != null) onOpenBilling.run();
        });

        Button reportsBtn = new Button("Reports");
        reportsBtn.getStyleClass().addAll("nav-item", "nav-button");
        reportsBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(reportsBtn, new Insets(2, 0, 2, 0));
        reportsBtn.setOnAction(e -> {
            if (onOpenReports != null) onOpenReports.run();
        });

        // Hidden navigation must not reserve space in the sidebar.
        departmentsItem.setVisible(isAdmin);
        departmentsItem.setManaged(isAdmin);
        usersBtn.setVisible(isAdmin);
        usersBtn.setManaged(isAdmin);
        doctorsBtn.setVisible(isAdminOrReceptionist);
        doctorsBtn.setManaged(isAdminOrReceptionist);
        billingBtn.setVisible(isAdminOrReceptionist);
        billingBtn.setManaged(isAdminOrReceptionist);

        sidebar.getChildren().addAll(
                appTitle,
                new Separator(),
                dashboardBtn,
                patientsBtn,
                doctorsBtn,
                departmentsItem,
                usersBtn,
                apptBtn,
                recordsBtn,
                prescBtn,
                billingBtn,
                reportsBtn
        );
        return sidebar;
    }

    private Node disabledSidebarItem(String label) {
        Label item = new Label(label);
        item.setMaxWidth(Double.MAX_VALUE);
        item.getStyleClass().addAll("nav-item", "nav-item-disabled");
        item.setDisable(true);
        VBox.setMargin(item, new Insets(2, 0, 2, 0));
        return item;
    }

    // ---------- Main content ----------

    private Node buildContent() {
        VBox tablePane = new VBox(10);
        tablePane.setPadding(new Insets(16));
        tablePane.getStyleClass().add("card");

        Label tableTitle = new Label("Departments");
        tableTitle.getStyleClass().add("section-title");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No departments found."));

        TableColumn<Department, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        idCol.setMaxWidth(80);

        TableColumn<Department, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Department, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        @SuppressWarnings("unchecked")
        TableColumn<Department, ?>[] cols = new TableColumn[] { idCol, nameCol, descCol };
        table.getColumns().addAll(cols);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFormForEdit(newVal);
            }
        });

        tablePane.getChildren().addAll(tableTitle, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(18));
        formPane.getStyleClass().add("card");

        formTitle = new Label("Add Department");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        Label nameLabel = new Label("Name *");
        nameField = new TextField();
        nameField.setPromptText("e.g. Cardiology");

        Label descLabel = new Label("Description");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional description...");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);

        form.add(nameLabel, 0, 0);
        form.add(nameField, 0, 1);
        form.add(descLabel, 0, 2);
        form.add(descriptionArea, 0, 3);

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");

        saveButton = new Button("Add");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);

        deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        FlowPane buttonRow = new FlowPane(8, 8, saveButton, deleteButton, clearButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        saveButton.setOnAction(e -> onSave());
        deleteButton.setOnAction(e -> onDelete());
        clearButton.setOnAction(e -> resetForm());

        UiStyles.form(form);
        formPane.getChildren().addAll(formTitle, UiStyles.hint("* Required fields"), form, messageLabel, buttonRow);

        return UiStyles.workspace(tablePane, formPane);
    }

    // ---------- Actions ----------

    private void onSave() {
        clearMessage();
        try {
            String name = nameField.getText();
            String desc = descriptionArea.getText();
            if (editingDepartment == null) {
                Department created = departmentService.createDepartment(name, desc);
                showSuccess("Department \"" + created.getName() + "\" created.");
            } else {
                Department updated = departmentService.updateDepartment(editingDepartment.getId(), name, desc);
                showSuccess("Department \"" + updated.getName() + "\" updated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDelete() {
        clearMessage();
        if (editingDepartment == null) {
            showError("Select a department to delete first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete department?");
        confirm.setContentText("Are you sure you want to delete the department \""
                + editingDepartment.getName() + "\"?");
        UiStyles.dialog(confirm, true);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            departmentService.deleteDepartment(editingDepartment.getId());
            showSuccess("Department deleted.");
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    // ---------- Helpers ----------

    private void refreshTable() {
        try {
            List<Department> all = departmentService.getAllDepartments();
            table.getItems().setAll(all);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateFormForEdit(Department d) {
        this.editingDepartment = d;
        formTitle.setText("Edit Department");
        nameField.setText(d.getName());
        descriptionArea.setText(d.getDescription() == null ? "" : d.getDescription());
        saveButton.setText("Save");
        deleteButton.setDisable(!isAdmin());
        deleteButton.setVisible(isAdmin());
        deleteButton.setManaged(isAdmin());
        clearMessage();
    }

    private void resetForm() {
        this.editingDepartment = null;
        formTitle.setText(isAdmin() ? "Add Department" : "View Department");
        nameField.clear();
        descriptionArea.clear();
        saveButton.setText("Add");
        table.getSelectionModel().clearSelection();
        deleteButton.setDisable(true);
        deleteButton.setVisible(isAdmin());
        deleteButton.setManaged(isAdmin());

        // Non-admins can't mutate; disable form + save.
        boolean admin = isAdmin();
        nameField.setDisable(!admin);
        descriptionArea.setDisable(!admin);
        saveButton.setDisable(!admin);
        saveButton.setVisible(admin);
        saveButton.setManaged(admin);
        clearMessage();
    }

    private void updateButtonsForRole() {
        resetForm();
    }

    private boolean isAdmin() {
        return Session.getInstance().getRole() != null
                && Session.getInstance().getRole() == com.hospital.model.Role.ADMIN;
    }

    private void showError(String msg) {
        messageLabel.setText(msg == null ? "An error occurred." : msg);
        messageLabel.getStyleClass().remove("success");
        messageLabel.getStyleClass().add("error");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().remove("error");
        messageLabel.getStyleClass().add("success");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.setText("");
    }

    private void applyCss(Scene scene) {
        UiStyles.apply(scene);
    }
}
