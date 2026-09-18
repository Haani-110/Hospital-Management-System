package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Department;
import com.hospital.model.Doctor;
import com.hospital.model.Role;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Doctor Management screen (Phase 2.3).
 *
 * Admins can create/edit/activate/deactivate doctors. DOCTOR and RECEPTIONIST
 * users can view, search, and filter but cannot mutate.
 */
public class DoctorController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenUserManagement;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenBilling;

    private TableView<Doctor> table;
    private TextField searchField;
    private ComboBox<Department> deptFilter;
    private TextField nameField;
    private TextField specField;
    private TextField phoneField;
    private TextField emailField;
    private TextField feeField;
    private ComboBox<Department> deptCombo;
    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button toggleActiveButton;
    private Button clearButton;

    private Doctor editingDoctor;

    public DoctorController(DoctorService doctorService, DepartmentService departmentService, SceneManager sceneManager) {
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }
    public void setOnOpenBilling(Runnable r) { this.onOpenBilling = r; }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        Scene scene = new Scene(root, 1200, 720);
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
        Label title = new Label("Doctor Management");
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

        Button users = new Button("User Management");
        styleNav(users);
        users.setOnAction(e -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });

        Label here = new Label("Doctor Management");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));

        Button patientsBtn = new Button("Patients");
        styleNav(patientsBtn);
        patientsBtn.setOnAction(e -> { if (onOpenPatients != null) onOpenPatients.run(); });

        Button apptBtn = new Button("Appointments");
        styleNav(apptBtn);
        apptBtn.setOnAction(e -> { if (onOpenAppointments != null) onOpenAppointments.run(); });

        Button recordsBtn = new Button("Medical Records");
        styleNav(recordsBtn);
        recordsBtn.setOnAction(e -> { if (onOpenMedicalRecords != null) onOpenMedicalRecords.run(); });

        Button prescBtn = new Button("Prescriptions");
        styleNav(prescBtn);
        prescBtn.setOnAction(e -> { if (onOpenPrescriptions != null) onOpenPrescriptions.run(); });

        Button billingBtn = new Button("Billing");
        styleNav(billingBtn);
        billingBtn.setOnAction(e -> { if (onOpenBilling != null) onOpenBilling.run(); });

        sidebar.getChildren().addAll(t, new Separator(), dash, patientsBtn, here, apptBtn, recordsBtn,
                prescBtn, billingBtn, depts, users);
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
        searchField.setPromptText("Search name, specialization, phone, email...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        deptFilter = new ComboBox<>();
        deptFilter.setPromptText("All Departments");
        deptFilter.setPrefWidth(220);
        deptFilter.valueProperty().addListener((o, a, b) -> refreshTable());

        filterRow.getChildren().addAll(searchField, deptFilter);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No doctors found."));

        TableColumn<Doctor, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50); idCol.setMaxWidth(70);

        TableColumn<Doctor, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<Doctor, String> specCol = new TableColumn<>("Specialization");
        specCol.setCellValueFactory(new PropertyValueFactory<>("specialization"));

        TableColumn<Doctor, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("departmentName"));

        TableColumn<Doctor, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Doctor, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Doctor, Double> feeCol = new TableColumn<>("Fee");
        feeCol.setCellValueFactory(new PropertyValueFactory<>("consultationFee"));
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        feeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : currency.format(v));
            }
        });

        TableColumn<Doctor, Boolean> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        statCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) setText(null);
                else setText(a ? "Active" : "Inactive");
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<Doctor, ?>[] cols = new TableColumn[] { idCol, nameCol, specCol, deptCol, phoneCol, emailCol, feeCol, statCol };
        table.getColumns().addAll(cols);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) populateForm(n); });

        tablePane.getChildren().addAll(filterRow, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---------- Form pane ----------
        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(10));
        formPane.setMinWidth(360);
        formPane.setMaxWidth(400);
        formPane.getStyleClass().add("card");

        formTitle = new Label("Add Doctor");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.add(new Label("Full Name *"), 0, 0);
        nameField = new TextField(); nameField.setPromptText("e.g. John Smith");
        form.add(nameField, 0, 1);

        form.add(new Label("Specialization *"), 0, 2);
        specField = new TextField(); specField.setPromptText("e.g. Cardiologist");
        form.add(specField, 0, 3);

        form.add(new Label("Department *"), 0, 4);
        deptCombo = new ComboBox<>();
        deptCombo.setMaxWidth(Double.MAX_VALUE);
        form.add(deptCombo, 0, 5);

        // Make department ComboBoxes display department names instead of toString().
        javafx.util.Callback<ListView<Department>, ListCell<Department>> deptCellFactory =
                lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Department item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getName());
                    }
                };
        deptFilter.setCellFactory(deptCellFactory);
        deptFilter.setButtonCell(deptCellFactory.call(null));
        deptCombo.setCellFactory(deptCellFactory);
        deptCombo.setButtonCell(deptCellFactory.call(null));

        form.add(new Label("Phone"), 0, 6);
        phoneField = new TextField(); phoneField.setPromptText("optional");
        form.add(phoneField, 0, 7);

        form.add(new Label("Email"), 0, 8);
        emailField = new TextField(); emailField.setPromptText("optional");
        form.add(emailField, 0, 9);

        form.add(new Label("Consultation Fee *"), 0, 10);
        feeField = new TextField();
        feeField.setPromptText("0.00");
        // Restrict to numeric input with an optional decimal point and up to two decimals.
        UnaryOperator<TextFormatter.Change> feeFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (newText.matches("\\d+(\\.\\d{0,2})?")) return change;
            return null;
        };
        feeField.setTextFormatter(new TextFormatter<>(feeFilter));
        form.add(feeField, 0, 11);

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");

        saveButton = new Button("Add Doctor");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);

        toggleActiveButton = new Button("Deactivate");
        toggleActiveButton.getStyleClass().add("danger-button");
        toggleActiveButton.setDisable(true);

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        HBox btns = new HBox(10, saveButton, toggleActiveButton, clearButton);
        btns.setAlignment(Pos.CENTER_LEFT);

        saveButton.setOnAction(e -> onSave());
        toggleActiveButton.setOnAction(e -> onToggleActive());
        clearButton.setOnAction(e -> resetForm());

        formPane.getChildren().addAll(formTitle, form, messageLabel, btns);
        content.getChildren().addAll(tablePane, formPane);
        populateDepartmentChoices();
        return content;
    }

    private void populateDepartmentChoices() {
        try {
            List<Department> all = departmentService.getAllDepartments();
            Department allItem = new Department();
            allItem.setId(0);
            allItem.setName("All Departments");
            deptFilter.setItems(FXCollections.observableArrayList());
            deptFilter.getItems().add(allItem);
            deptFilter.getItems().addAll(all);
            deptFilter.setValue(allItem);
            deptCombo.setItems(FXCollections.observableArrayList(all));
            if (!all.isEmpty()) deptCombo.setValue(all.get(0));
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshTable() {
        try {
            String q = searchField == null ? null : searchField.getText();
            int deptId = (deptFilter == null || deptFilter.getValue() == null) ? 0 : deptFilter.getValue().getId();
            List<Doctor> list = doctorService.searchDoctors(q, deptId <= 0 ? null : deptId);
            table.getItems().setAll(list);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateForm(Doctor d) {
        this.editingDoctor = d;
        formTitle.setText("Edit Doctor");
        nameField.setText(d.getFullName());
        specField.setText(d.getSpecialization());
        phoneField.setText(d.getPhone() == null ? "" : d.getPhone());
        emailField.setText(d.getEmail() == null ? "" : d.getEmail());
        feeField.setText(String.format(Locale.US, "%.2f", d.getConsultationFee()));
        // select matching department in combo
        for (Department dep : deptCombo.getItems()) {
            if (dep.getId() == d.getDepartmentId()) { deptCombo.setValue(dep); break; }
        }
        saveButton.setText("Save");
        toggleActiveButton.setDisable(!isAdmin());
        if (d.isActive()) {
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
        this.editingDoctor = null;
        formTitle.setText(isAdmin() ? "Add Doctor" : "Doctor Details");
        nameField.clear(); specField.clear(); phoneField.clear(); emailField.clear(); feeField.clear();
        if (!deptCombo.getItems().isEmpty()) deptCombo.setValue(deptCombo.getItems().get(0));
        saveButton.setText("Add Doctor");
        table.getSelectionModel().clearSelection();
        toggleActiveButton.setDisable(true);
        toggleActiveButton.setText("Deactivate");
        toggleActiveButton.getStyleClass().remove("primary-button");
        toggleActiveButton.getStyleClass().add("danger-button");
        boolean admin = isAdmin();
        nameField.setDisable(!admin); specField.setDisable(!admin); phoneField.setDisable(!admin);
        emailField.setDisable(!admin); feeField.setDisable(!admin); deptCombo.setDisable(!admin);
        saveButton.setDisable(!admin); saveButton.setVisible(admin); saveButton.setManaged(admin);
        clearMessage();
    }

    private void onSave() {
        clearMessage();
        try {
            String name = nameField.getText();
            String spec = specField.getText();
            String phone = phoneField.getText();
            String email = emailField.getText();
            Department dept = deptCombo.getValue();
            Integer deptId = dept == null ? null : dept.getId();
            double fee;
            try {
                fee = feeField.getText() == null || feeField.getText().isBlank() ? 0 : Double.parseDouble(feeField.getText().trim());
            } catch (NumberFormatException ex) {
                throw new ValidationException("Consultation fee must be a valid number.");
            }
            if (editingDoctor == null) {
                Doctor created = doctorService.createDoctor(null, deptId, name, spec, phone, email, fee);
                showSuccess("Doctor \"" + created.getFullName() + "\" created.");
            } else {
                Doctor updated = doctorService.updateDoctor(editingDoctor.getId(), null, deptId, name, spec, phone, email, fee);
                showSuccess("Doctor \"" + updated.getFullName() + "\" updated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onToggleActive() {
        clearMessage();
        if (editingDoctor == null) { showError("Select a doctor first."); return; }
        boolean willBeActive = !editingDoctor.isActive();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(willBeActive ? "Activate doctor" : "Deactivate doctor");
        confirm.setHeaderText(willBeActive ? "Activate this doctor?" : "Deactivate this doctor?");
        confirm.setContentText(willBeActive
                ? "Are you sure you want to activate \"" + editingDoctor.getFullName() + "\"?"
                : "Are you sure you want to deactivate \"" + editingDoctor.getFullName() + "\"?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;
        try {
            if (willBeActive) {
                doctorService.activateDoctor(editingDoctor.getId());
                showSuccess("Doctor activated.");
            } else {
                doctorService.deactivateDoctor(editingDoctor.getId());
                showSuccess("Doctor deactivated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private boolean isAdmin() {
        return Session.getInstance().getRole() == Role.ADMIN;
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
