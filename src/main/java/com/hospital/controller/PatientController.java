package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Patient;
import com.hospital.model.Role;
import com.hospital.service.PatientService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Patient Management screen (Phase 2.4).
 *
 * Admins and receptionists can create/edit/activate/deactivate patients. DOCTOR
 * users can view, search, and filter but cannot mutate.
 */
public class PatientController {

    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PatientService patientService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenUserManagement;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;

    private TableView<Patient> table;
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private Label codeValueLabel;
    private TextField nameField;
    private DatePicker dobPicker;
    private ComboBox<String> genderCombo;
    private TextField phoneField;
    private TextField emailField;
    private TextArea addressArea;
    private TextField emNameField;
    private TextField emPhoneField;
    private ComboBox<String> bgCombo;
    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button toggleActiveButton;
    private Button detailsButton;
    private Button clearButton;

    private Patient editingPatient;

    public PatientController(PatientService patientService, SceneManager sceneManager) {
        this.patientService = patientService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        Scene scene = new Scene(root, 1280, 760);
        applyCss(scene);
        refreshTable();
        resetForm();
        return scene;
    }

    private Node buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(15, 20, 15, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        Label title = new Label("Patient Management");
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

        Button dash = new Button("Dashboard");
        styleNav(dash);
        dash.setOnAction(e -> { if (onBackToDashboard != null) onBackToDashboard.run(); });

        Button depts = new Button("Departments");
        styleNav(depts);
        depts.setOnAction(e -> { if (onOpenDepartments != null) onOpenDepartments.run(); });

        Button doctors = new Button("Doctors");
        styleNav(doctors);
        doctors.setOnAction(e -> { if (onOpenDoctors != null) onOpenDoctors.run(); });

        Button users = new Button("User Management");
        styleNav(users);
        users.setOnAction(e -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });

        Label here = new Label("Patients");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));

        Button apptBtn = new Button("Appointments");
        styleNav(apptBtn);
        apptBtn.setOnAction(e -> { if (onOpenAppointments != null) onOpenAppointments.run(); });

        Button recordsBtn = new Button("Medical Records");
        styleNav(recordsBtn);
        recordsBtn.setOnAction(e -> { if (onOpenMedicalRecords != null) onOpenMedicalRecords.run(); });

        Button prescBtn = new Button("Prescriptions");
        styleNav(prescBtn);
        prescBtn.setOnAction(e -> { if (onOpenPrescriptions != null) onOpenPrescriptions.run(); });

        sidebar.getChildren().addAll(t, new Separator(), dash, here, doctors, apptBtn, recordsBtn,
                prescBtn, depts, users);
        return sidebar;
    }

    private void styleNav(Button b) {
        b.getStyleClass().addAll("nav-item", "nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(b, new Insets(2, 0, 2, 0));
    }

    private Node disabledItem(String label) {
        Label l = new Label(label);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().addAll("nav-item", "nav-item-disabled");
        l.setDisable(true);
        VBox.setMargin(l, new Insets(2, 0, 2, 0));
        return l;
    }

    private Node buildContent() {
        HBox content = new HBox(20);
        content.setPadding(new Insets(20));

        // ---------- Table pane ----------
        VBox tablePane = new VBox(10);
        tablePane.setPadding(new Insets(10));
        tablePane.getStyleClass().add("card");
        HBox.setHgrow(tablePane, Priority.ALWAYS);

        HBox filterRow = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText("Search by code, name, phone, email...");
        searchField.setPrefWidth(360);
        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        statusFilter = new ComboBox<>(FXCollections.observableArrayList("All", "Active", "Inactive"));
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(160);
        statusFilter.valueProperty().addListener((o, a, b) -> refreshTable());

        filterRow.getChildren().addAll(searchField, statusFilter);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No patients found."));

        TableColumn<Patient, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50); idCol.setMaxWidth(70);

        TableColumn<Patient, String> codeCol = new TableColumn<>("Patient Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        codeCol.setPrefWidth(110);

        TableColumn<Patient, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<Patient, LocalDate> dobCol = new TableColumn<>("Date of Birth");
        dobCol.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
        dobCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DOB_FMT));
            }
        });

        TableColumn<Patient, String> genderCol = new TableColumn<>("Gender");
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        genderCol.setPrefWidth(90);

        TableColumn<Patient, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Patient, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Patient, String> bgCol = new TableColumn<>("Blood");
        bgCol.setCellValueFactory(new PropertyValueFactory<>("bloodGroup"));
        bgCol.setPrefWidth(60);

        TableColumn<Patient, Boolean> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        statCol.setPrefWidth(80);
        statCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) setText(null);
                else setText(a ? "Active" : "Inactive");
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<Patient, ?>[] cols = new TableColumn[] { idCol, codeCol, nameCol, dobCol, genderCol, phoneCol, emailCol, bgCol, statCol };
        table.getColumns().addAll(cols);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) populateForm(n); });

        tablePane.getChildren().addAll(filterRow, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---------- Form pane ----------
        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(10));
        formPane.setMinWidth(420);
        formPane.setMaxWidth(460);
        formPane.getStyleClass().add("card");

        formTitle = new Label("Add Patient");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);

        form.add(new Label("Patient Code"), 0, 0);
        codeValueLabel = new Label("(auto-generated)");
        codeValueLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
        form.add(codeValueLabel, 1, 0);

        form.add(new Label("Full Name *"), 0, 1);
        nameField = new TextField(); nameField.setPromptText("e.g. Jane Doe");
        form.add(nameField, 1, 1);

        form.add(new Label("Date of Birth"), 0, 2);
        dobPicker = new DatePicker();
        dobPicker.setPromptText("YYYY-MM-DD");
        form.add(dobPicker, 1, 2);

        form.add(new Label("Gender"), 0, 3);
        genderCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Male", "Female", "Other", "Prefer not to say"));
        genderCombo.setMaxWidth(Double.MAX_VALUE);
        form.add(genderCombo, 1, 3);

        form.add(new Label("Phone"), 0, 4);
        phoneField = new TextField(); phoneField.setPromptText("optional");
        form.add(phoneField, 1, 4);

        form.add(new Label("Email"), 0, 5);
        emailField = new TextField(); emailField.setPromptText("optional");
        form.add(emailField, 1, 5);

        form.add(new Label("Address"), 0, 6);
        addressArea = new TextArea(); addressArea.setPromptText("optional");
        addressArea.setPrefRowCount(2);
        form.add(addressArea, 1, 6);

        form.add(new Label("Emergency Contact Name"), 0, 7);
        emNameField = new TextField(); emNameField.setPromptText("optional");
        form.add(emNameField, 1, 7);

        form.add(new Label("Emergency Contact Phone"), 0, 8);
        emPhoneField = new TextField(); emPhoneField.setPromptText("optional");
        form.add(emPhoneField, 1, 8);

        form.add(new Label("Blood Group"), 0, 9);
        bgCombo = new ComboBox<>(FXCollections.observableArrayList(
                "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        bgCombo.setMaxWidth(Double.MAX_VALUE);
        form.add(bgCombo, 1, 9);

        for (Node n : new Node[] { nameField, dobPicker, genderCombo, phoneField, emailField,
                addressArea, emNameField, emPhoneField, bgCombo }) {
            GridPane.setHgrow(n, Priority.ALWAYS);
        }

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");

        saveButton = new Button("Save");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);

        toggleActiveButton = new Button("Deactivate");
        toggleActiveButton.getStyleClass().add("danger-button");
        toggleActiveButton.setDisable(true);

        detailsButton = new Button("View Details");
        detailsButton.getStyleClass().add("secondary-button");
        detailsButton.setDisable(true);

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        HBox btns = new HBox(10, saveButton, toggleActiveButton, detailsButton, clearButton);
        btns.setAlignment(Pos.CENTER_LEFT);
        btns.setPadding(new Insets(6, 0, 0, 0));

        saveButton.setOnAction(e -> onSave());
        toggleActiveButton.setOnAction(e -> onToggleActive());
        detailsButton.setOnAction(e -> onViewDetails());
        clearButton.setOnAction(e -> resetForm());

        formPane.getChildren().addAll(formTitle, form, messageLabel, btns);
        content.getChildren().addAll(tablePane, formPane);
        return content;
    }

    private void refreshTable() {
        try {
            String q = searchField == null ? null : searchField.getText();
            Boolean activeFilter = null;
            if (statusFilter != null && statusFilter.getValue() != null) {
                switch (statusFilter.getValue()) {
                    case "Active" -> activeFilter = true;
                    case "Inactive" -> activeFilter = false;
                    default -> activeFilter = null;
                }
            }
            List<Patient> list = patientService.searchPatients(q, activeFilter);
            table.getItems().setAll(list);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateForm(Patient p) {
        this.editingPatient = p;
        formTitle.setText("Edit Patient");
        codeValueLabel.setText(p.getPatientCode());
        nameField.setText(p.getFullName());
        dobPicker.setValue(p.getDateOfBirth());
        genderCombo.setValue(p.getGender());
        phoneField.setText(p.getPhone() == null ? "" : p.getPhone());
        emailField.setText(p.getEmail() == null ? "" : p.getEmail());
        addressArea.setText(p.getAddress() == null ? "" : p.getAddress());
        emNameField.setText(p.getEmergencyContactName() == null ? "" : p.getEmergencyContactName());
        emPhoneField.setText(p.getEmergencyContactPhone() == null ? "" : p.getEmergencyContactPhone());
        bgCombo.setValue(p.getBloodGroup());
        saveButton.setText("Save");
        boolean canMutate = canMutate();
        toggleActiveButton.setDisable(!canMutate);
        detailsButton.setDisable(false);
        if (p.isActive()) {
            toggleActiveButton.setText("Deactivate");
            toggleActiveButton.getStyleClass().remove("primary-button");
            toggleActiveButton.getStyleClass().add("danger-button");
        } else {
            toggleActiveButton.setText("Activate");
            toggleActiveButton.getStyleClass().remove("danger-button");
            toggleActiveButton.getStyleClass().add("primary-button");
        }
        clearMessage();
    }

    private void resetForm() {
        this.editingPatient = null;
        boolean canMutate = canMutate();
        formTitle.setText(canMutate ? "Add Patient" : "Patient Details");
        codeValueLabel.setText(canMutate ? "(auto-generated on save)" : "(select a patient)");
        nameField.clear(); dobPicker.setValue(null); genderCombo.setValue(null);
        phoneField.clear(); emailField.clear(); addressArea.clear();
        emNameField.clear(); emPhoneField.clear(); bgCombo.setValue(null);
        saveButton.setText("Add Patient");
        table.getSelectionModel().clearSelection();
        toggleActiveButton.setDisable(true);
        detailsButton.setDisable(true);
        toggleActiveButton.setText("Deactivate");
        toggleActiveButton.getStyleClass().remove("primary-button");
        toggleActiveButton.getStyleClass().add("danger-button");

        nameField.setDisable(!canMutate);
        dobPicker.setDisable(!canMutate);
        genderCombo.setDisable(!canMutate);
        phoneField.setDisable(!canMutate);
        emailField.setDisable(!canMutate);
        addressArea.setDisable(!canMutate);
        emNameField.setDisable(!canMutate);
        emPhoneField.setDisable(!canMutate);
        bgCombo.setDisable(!canMutate);
        saveButton.setDisable(!canMutate);
        saveButton.setVisible(canMutate);
        saveButton.setManaged(canMutate);
        clearMessage();
    }

    private void onSave() {
        clearMessage();
        try {
            String name = nameField.getText();
            String dob;
            if (dobPicker.getValue() != null) {
                dob = dobPicker.getValue().toString();
            } else {
                String editorText = dobPicker.getEditor() == null ? null : dobPicker.getEditor().getText();
                dob = (editorText == null || editorText.isBlank()) ? null : editorText.trim();
            }
            String gender = genderCombo.getValue();
            String phone = phoneField.getText();
            String email = emailField.getText();
            String address = addressArea.getText();
            String emName = emNameField.getText();
            String emPhone = emPhoneField.getText();
            String bg = bgCombo.getValue();
            if (editingPatient == null) {
                Patient created = patientService.createPatient(name, dob, gender, phone, email, address, emName, emPhone, bg);
                showSuccess("Patient " + created.getPatientCode() + " (" + created.getFullName() + ") created.");
            } else {
                Patient updated = patientService.updatePatient(editingPatient.getId(), name, dob, gender, phone, email, address, emName, emPhone, bg);
                showSuccess("Patient " + updated.getPatientCode() + " updated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onToggleActive() {
        clearMessage();
        if (editingPatient == null) { showError("Select a patient first."); return; }
        boolean willBeActive = !editingPatient.isActive();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(willBeActive ? "Activate patient" : "Deactivate patient");
        confirm.setHeaderText(willBeActive ? "Activate this patient?" : "Deactivate this patient?");
        confirm.setContentText(willBeActive
                ? "Are you sure you want to activate \"" + editingPatient.getFullName() + "\" (" + editingPatient.getPatientCode() + ")?"
                : "Are you sure you want to deactivate \"" + editingPatient.getFullName() + "\" (" + editingPatient.getPatientCode() + ")?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;
        try {
            if (willBeActive) {
                patientService.activatePatient(editingPatient.getId());
                showSuccess("Patient activated.");
            } else {
                patientService.deactivatePatient(editingPatient.getId());
                showSuccess("Patient deactivated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onViewDetails() {
        if (editingPatient == null) { showError("Select a patient first."); return; }
        Patient p;
        try {
            p = patientService.getPatient(editingPatient.getId());
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            return;
        }
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Patient Details — " + p.getPatientCode());
        info.setHeaderText(p.getFullName() + " (" + p.getPatientCode() + ")");
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(p.isActive() ? "Active" : "Inactive").append("\n");
        sb.append("DOB: ").append(p.getDateOfBirth() == null ? "—" : p.getDateOfBirth().format(DOB_FMT)).append("\n");
        sb.append("Gender: ").append(p.getGender() == null ? "—" : p.getGender()).append("\n");
        sb.append("Phone: ").append(p.getPhone() == null ? "—" : p.getPhone()).append("\n");
        sb.append("Email: ").append(p.getEmail() == null ? "—" : p.getEmail()).append("\n");
        sb.append("Address: ").append(p.getAddress() == null ? "—" : p.getAddress()).append("\n");
        sb.append("Blood Group: ").append(p.getBloodGroup() == null ? "—" : p.getBloodGroup()).append("\n");
        sb.append("Emergency Contact: ");
        if (p.getEmergencyContactName() == null && p.getEmergencyContactPhone() == null) {
            sb.append("—");
        } else {
            if (p.getEmergencyContactName() != null) sb.append(p.getEmergencyContactName());
            if (p.getEmergencyContactPhone() != null) sb.append(" — ").append(p.getEmergencyContactPhone());
        }
        sb.append("\n");
        info.setContentText(sb.toString());
        info.getDialogPane().setMinWidth(420);
        info.showAndWait();
    }

    private boolean canMutate() {
        Role r = Session.getInstance().getRole();
        return r == Role.ADMIN || r == Role.RECEPTIONIST;
    }

    private void showError(String msg) {
        messageLabel.setText(msg == null ? "An error occurred." : msg);
        messageLabel.getStyleClass().remove("success");
        messageLabel.getStyleClass().add("error");
        messageLabel.setVisible(true); messageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().remove("error");
        messageLabel.getStyleClass().add("success");
        messageLabel.setVisible(true); messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setVisible(false); messageLabel.setManaged(false); messageLabel.setText("");
    }

    private void applyCss(Scene scene) {
        try {
            var cssUrl = getClass().getResource("/com/hospital/css/styles.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}
    }
}
