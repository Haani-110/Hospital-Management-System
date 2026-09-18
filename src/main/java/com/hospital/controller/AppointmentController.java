package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.model.Role;
import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.PatientService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import com.hospital.util.UiStyles;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Appointment Management screen (Phase 3).
 *
 * Admins and receptionists can create/edit/cancel appointments; admins can also
 * mark them completed. DOCTOR users can view, search, and filter (read-only in
 * this phase; see {@link AppointmentService} for rationale).
 */
public class AppointmentController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenUserManagement;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenBilling;
    private Runnable onOpenReports;

    private TableView<Appointment> table;
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private DatePicker dateFilterPicker;
    private Button clearDateFilterBtn;
    private ComboBox<Doctor> doctorFilter;

    private ComboBox<Patient> patientCombo;
    private ComboBox<Doctor> doctorCombo;
    private DatePicker datePicker;
    private TextField timeField;
    private TextField reasonField;
    private TextArea notesArea;

    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button cancelApptButton;
    private Button completeButton;
    private Button clearButton;

    private Appointment editingAppointment;

    public AppointmentController(AppointmentService appointmentService,
                                 PatientService patientService,
                                 DoctorService doctorService,
                                 SceneManager sceneManager) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }
    public void setOnOpenBilling(Runnable r) { this.onOpenBilling = r; }
    public void setOnOpenReports(Runnable r) { this.onOpenReports = r; }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        BorderPane workspace = new BorderPane();
        workspace.setTop(buildTopBar());
        workspace.setCenter(buildContent());
        root.setLeft(UiStyles.sidebar(buildSidebar()));
        root.setCenter(workspace);
        Scene scene = new Scene(root);
        applyCss(scene);
        refreshTable();
        populateChoices();
        resetForm();
        return scene;
    }

    private Node buildTopBar() {
        Label title = new Label("Appointment Management");
        title.getStyleClass().add("page-title");
        Button back = new Button("← Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> { if (onBackToDashboard != null) onBackToDashboard.run(); });
        String roleText = Session.getInstance().getRole() != null ? Session.getInstance().getRole().name() : "UNKNOWN";
        String userText = Session.getInstance().getUsername() != null ? Session.getInstance().getUsername() : "unknown";
        Label info = new Label("Logged in as: " + userText + " (" + roleText + ")");
        info.getStyleClass().add("user-info");
        return UiStyles.header(title, info, back);
    }

    private Node buildSidebar() {
        Role role = Session.getInstance().getRole();
        boolean isAdmin = role == Role.ADMIN;
        boolean isAdminOrReceptionist = isAdmin || role == Role.RECEPTIONIST;

        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(16));
        sidebar.setMinWidth(0);
        sidebar.getStyleClass().add("sidebar");
        Label t = new Label("Hospital System"); t.getStyleClass().add("sidebar-title");

        Button dash = new Button("Dashboard");
        styleNav(dash);
        dash.setOnAction(e -> { if (onBackToDashboard != null) onBackToDashboard.run(); });

        Button patients = new Button("Patients");
        styleNav(patients);
        patients.setOnAction(e -> { if (onOpenPatients != null) onOpenPatients.run(); });

        Button doctors = new Button("Doctors");
        styleNav(doctors);
        doctors.setOnAction(e -> { if (onOpenDoctors != null) onOpenDoctors.run(); });

        Button depts = new Button("Departments");
        styleNav(depts);
        depts.setOnAction(e -> { if (onOpenDepartments != null) onOpenDepartments.run(); });

        Button users = new Button("User Management");
        styleNav(users);
        users.setOnAction(e -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });

        Label here = new Label("Appointments");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));

        Button recordsBtn = new Button("Medical Records");
        styleNav(recordsBtn);
        recordsBtn.setOnAction(e -> { if (onOpenMedicalRecords != null) onOpenMedicalRecords.run(); });

        Button prescBtn = new Button("Prescriptions");
        styleNav(prescBtn);
        prescBtn.setOnAction(e -> { if (onOpenPrescriptions != null) onOpenPrescriptions.run(); });

        Button billingBtn = new Button("Billing");
        styleNav(billingBtn);
        billingBtn.setOnAction(e -> { if (onOpenBilling != null) onOpenBilling.run(); });

        Button reportsBtn = new Button("Reports");
        styleNav(reportsBtn);
        reportsBtn.setOnAction(e -> { if (onOpenReports != null) onOpenReports.run(); });

        // Hidden navigation must not reserve space in the sidebar.
        depts.setVisible(isAdmin);
        depts.setManaged(isAdmin);
        users.setVisible(isAdmin);
        users.setManaged(isAdmin);
        doctors.setVisible(isAdminOrReceptionist);
        doctors.setManaged(isAdminOrReceptionist);
        billingBtn.setVisible(isAdminOrReceptionist);
        billingBtn.setManaged(isAdminOrReceptionist);

        sidebar.getChildren().addAll(t, new Separator(), dash, patients, here, doctors, recordsBtn,
                prescBtn, billingBtn, depts, users, reportsBtn);
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
        // ---------- Table pane ----------
        VBox tablePane = new VBox(10);
        tablePane.setPadding(new Insets(16));
        tablePane.getStyleClass().add("card");

        FlowPane filterRow = new FlowPane(8, 8);
        searchField = new TextField();
        searchField.setPromptText("Search patient, code, doctor, reason...");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        statusFilter = new ComboBox<>(FXCollections.observableArrayList("All", "Scheduled", "Completed", "Cancelled"));
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(130);
        statusFilter.valueProperty().addListener((o, a, b) -> refreshTable());

        dateFilterPicker = new DatePicker();
        dateFilterPicker.setPromptText("Filter by date");
        dateFilterPicker.setPrefWidth(160);
        dateFilterPicker.valueProperty().addListener((o, a, b) -> refreshTable());
        clearDateFilterBtn = new Button("×");
        clearDateFilterBtn.getStyleClass().add("secondary-button");
        clearDateFilterBtn.setTooltip(new javafx.scene.control.Tooltip("Clear date filter"));
        clearDateFilterBtn.setOnAction(e -> { dateFilterPicker.setValue(null); });

        doctorFilter = new ComboBox<>();
        doctorFilter.setPromptText("All Doctors");
        doctorFilter.setPrefWidth(220);
        doctorFilter.valueProperty().addListener((o, a, b) -> refreshTable());

        filterRow.getChildren().addAll(searchField, statusFilter, dateFilterPicker, clearDateFilterBtn, doctorFilter);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No appointments found."));

        TableColumn<Appointment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50); idCol.setMaxWidth(70);

        TableColumn<Appointment, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });

        Button reportsBtn = new Button("Reports");
        reportsBtn.getStyleClass().addAll("nav-item", "nav-button");
        reportsBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(reportsBtn, new Insets(2, 0, 2, 0));
        reportsBtn.setOnAction(e -> {
            if (onOpenReports != null) onOpenReports.run();
        });


        TableColumn<Appointment, LocalTime> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.format(TIME_FMT));
            }
        });
        timeCol.setPrefWidth(70);

        TableColumn<Appointment, String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));

        TableColumn<Appointment, String> codeCol = new TableColumn<>("Pat. Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        codeCol.setPrefWidth(100);

        TableColumn<Appointment, String> doctorCol = new TableColumn<>("Doctor");
        doctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        TableColumn<Appointment, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value);
                UiStyles.statusCell(this, getText());
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<Appointment, ?>[] cols = new TableColumn[] { idCol, dateCol, timeCol, patientCol, codeCol, doctorCol, reasonCol, statusCol };
        table.getColumns().addAll(cols);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) populateForm(n); });

        tablePane.getChildren().addAll(filterRow, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---------- Form pane ----------
        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(18));
        formPane.getStyleClass().add("card");

        formTitle = new Label("New Appointment");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);

        form.add(new Label("Patient *"), 0, 0);
        patientCombo = new ComboBox<>();
        patientCombo.setMaxWidth(Double.MAX_VALUE);
        patientCombo.setCellFactory(lv -> patientCell());
        patientCombo.setButtonCell(patientCell());
        form.add(patientCombo, 1, 0);

        form.add(new Label("Doctor *"), 0, 1);
        doctorCombo = new ComboBox<>();
        doctorCombo.setMaxWidth(Double.MAX_VALUE);
        doctorCombo.setCellFactory(lv -> doctorCell());
        doctorCombo.setButtonCell(doctorCell());
        form.add(doctorCombo, 1, 1);

        form.add(new Label("Date *"), 0, 2);
        datePicker = new DatePicker();
        datePicker.setPromptText("YYYY-MM-DD");
        // Disable past dates in the date picker for new appointments.
        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                if (d != null && d.isBefore(LocalDate.now())) {
                    setDisable(true);
                    // Disabled-day appearance is supplied by the shared stylesheet.
                }
            }
        });
        form.add(datePicker, 1, 2);

        form.add(new Label("Time *"), 0, 3);
        timeField = new TextField();
        timeField.setPromptText("HH:mm (e.g. 09:30)");
        form.add(timeField, 1, 3);

        form.add(new Label("Reason"), 0, 4);
        reasonField = new TextField(); reasonField.setPromptText("optional");
        form.add(reasonField, 1, 4);

        form.add(new Label("Notes"), 0, 5);
        notesArea = new TextArea(); notesArea.setPromptText("optional");
        notesArea.setPrefRowCount(3);
        form.add(notesArea, 1, 5);

        for (Node n : new Node[] { patientCombo, doctorCombo, datePicker, timeField, reasonField, notesArea }) {
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

        cancelApptButton = new Button("Cancel Appointment");
        cancelApptButton.getStyleClass().add("danger-button");
        cancelApptButton.setDisable(true);

        completeButton = new Button("Mark Completed");
        completeButton.getStyleClass().add("primary-button");
        completeButton.setDisable(true);

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        FlowPane btns = new FlowPane(8, 8, saveButton, cancelApptButton, completeButton, clearButton);
        btns.setAlignment(Pos.CENTER_LEFT);
        btns.setPadding(new Insets(6, 0, 0, 0));

        saveButton.setOnAction(e -> onSave());
        cancelApptButton.setOnAction(e -> onCancel());
        completeButton.setOnAction(e -> onComplete());
        clearButton.setOnAction(e -> resetForm());

        UiStyles.form(form);
        formPane.getChildren().addAll(formTitle, UiStyles.hint("* Required fields"), form, messageLabel, btns);
        return UiStyles.workspace(tablePane, formPane);
    }

    private ListCell<Patient> patientCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(p.getFullName() + " — " + p.getPatientCode());
            }
        };
    }

    private ListCell<Doctor> doctorCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Doctor d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) { setText(null); return; }
                setText("Dr. " + d.getFullName() + " — " + (d.getSpecialization() == null ? "" : d.getSpecialization()));
            }
        };
    }

    private ListCell<Doctor> doctorFilterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Doctor d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) { setText(null); return; }
                setText(d.getFullName() + " — " + (d.getSpecialization() == null ? "" : d.getSpecialization()));
            }
        };
    }

    private void populateChoices() {
        try {
            // Patient combo: only active patients are bookable.
            List<Patient> allPatients = patientService.getAllPatients();
            patientCombo.setItems(FXCollections.observableArrayList(
                    allPatients.stream().filter(Patient::isActive).toList()));
            if (!patientCombo.getItems().isEmpty()) patientCombo.setValue(patientCombo.getItems().get(0));

            // Doctor combo: only active doctors are bookable.
            List<Doctor> allDoctors = doctorService.getAllDoctors();
            List<Doctor> activeDoctors = allDoctors.stream().filter(Doctor::isActive).toList();
            doctorCombo.setItems(FXCollections.observableArrayList(activeDoctors));
            if (!doctorCombo.getItems().isEmpty()) doctorCombo.setValue(doctorCombo.getItems().get(0));

            // Doctor filter: "All Doctors" pseudo-entry (id=0) + all doctors (including inactive, for lookup).
            Doctor allItem = new Doctor();
            allItem.setId(0);
            allItem.setFullName("All Doctors");
            doctorFilter.getItems().clear();
            doctorFilter.getItems().add(allItem);
            doctorFilter.getItems().addAll(allDoctors);
            doctorFilter.setCellFactory(lv -> doctorFilterCell());
            doctorFilter.setButtonCell(doctorFilterCell());
            doctorFilter.setValue(allItem);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshTable() {
        try {
            String q = searchField == null ? null : searchField.getText();
            String status = statusFilter == null ? null : statusFilter.getValue();
            LocalDate df = dateFilterPicker == null ? null : dateFilterPicker.getValue();
            int docId = (doctorFilter == null || doctorFilter.getValue() == null) ? 0 : doctorFilter.getValue().getId();
            List<Appointment> list = appointmentService.searchAppointments(q, status, df, docId <= 0 ? null : docId);
            table.getItems().setAll(list);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateForm(Appointment a) {
        this.editingAppointment = a;
        boolean isScheduled = Appointment.STATUS_SCHEDULED.equals(a.getStatus());
        formTitle.setText(isScheduled ? "Edit Appointment" : "Appointment Details");
        // Select matching patient/doctor in combos (they may be inactive, so look them up; if not present
        // in the bookable list, add them so the user can see the selection).
        selectPatientById(a.getPatientId());
        selectDoctorById(a.getDoctorId());
        datePicker.setValue(a.getAppointmentDate());
        timeField.setText(a.getAppointmentTime() == null ? "" : a.getAppointmentTime().format(TIME_FMT));
        reasonField.setText(a.getReason() == null ? "" : a.getReason());
        notesArea.setText(a.getNotes() == null ? "" : a.getNotes());
        saveButton.setText("Save");
        saveButton.setDisable(!isScheduled || !canEdit());
        saveButton.setVisible(isScheduled && canEdit());
        saveButton.setManaged(isScheduled && canEdit());

        boolean canCancel = isScheduled && canCancel();
        cancelApptButton.setDisable(!canCancel);
        cancelApptButton.setVisible(canCancel);
        cancelApptButton.setManaged(canCancel);

        boolean canComplete = isScheduled && canComplete();
        completeButton.setDisable(!canComplete);
        completeButton.setVisible(canComplete);
        completeButton.setManaged(canComplete);

        // Disable fields when not editable.
        boolean editable = isScheduled && canEdit();
        patientCombo.setDisable(!editable);
        doctorCombo.setDisable(!editable);
        datePicker.setDisable(!editable);
        timeField.setDisable(!editable);
        reasonField.setDisable(!editable);
        notesArea.setDisable(!canEditNotes());
        clearMessage();
    }

    private boolean canEdit() {
        Role r = Session.getInstance().getRole();
        return r == Role.ADMIN || r == Role.RECEPTIONIST;
    }

    private boolean canCancel() {
        Role r = Session.getInstance().getRole();
        return r == Role.ADMIN || r == Role.RECEPTIONIST;
    }

    private boolean canComplete() {
        return Session.getInstance().getRole() == Role.ADMIN;
    }

    private boolean canEditNotes() {
        // Allow any logged-in role to see notes; restrict edits to same as canEdit (admin/receptionist)
        // for this phase to keep lifecycle simple.
        return canEdit();
    }

    private void resetForm() {
        this.editingAppointment = null;
        boolean canCreate = canEdit();
        formTitle.setText(canCreate ? "New Appointment" : "Appointment Details");
        datePicker.setValue(null);
        timeField.clear(); reasonField.clear(); notesArea.clear();
        if (!patientCombo.getItems().isEmpty()) patientCombo.setValue(patientCombo.getItems().get(0));
        if (!doctorCombo.getItems().isEmpty()) doctorCombo.setValue(doctorCombo.getItems().get(0));
        saveButton.setText("Save");
        table.getSelectionModel().clearSelection();
        cancelApptButton.setDisable(true);
        completeButton.setDisable(true);
        cancelApptButton.setVisible(canCancel());
        completeButton.setVisible(canComplete());

        patientCombo.setDisable(!canCreate);
        doctorCombo.setDisable(!canCreate);
        datePicker.setDisable(!canCreate);
        timeField.setDisable(!canCreate);
        reasonField.setDisable(!canCreate);
        notesArea.setDisable(!canCreate);
        saveButton.setDisable(!canCreate);
        saveButton.setVisible(canCreate);
        saveButton.setManaged(canCreate);
        clearMessage();
    }

    private void selectPatientById(int pid) {
        for (Patient p : patientCombo.getItems()) {
            if (p.getId() == pid) { patientCombo.setValue(p); return; }
        }
        // Not in list (inactive patient); look up and add for display.
        try {
            Patient p = patientService.getPatient(pid);
            patientCombo.getItems().add(p);
            patientCombo.setValue(p);
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            // leave selection as-is
        }
    }

    private void selectDoctorById(int did) {
        for (Doctor d : doctorCombo.getItems()) {
            if (d.getId() == did) { doctorCombo.setValue(d); return; }
        }
        try {
            Doctor d = doctorService.getDoctor(did);
            doctorCombo.getItems().add(d);
            doctorCombo.setValue(d);
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            // leave
        }
    }

    private void onSave() {
        clearMessage();
        try {
            Patient p = patientCombo.getValue();
            Doctor d = doctorCombo.getValue();
            Integer pid = p == null ? null : p.getId();
            Integer did = d == null ? null : d.getId();
            String date = datePicker.getValue() == null ? (datePicker.getEditor() == null ? null : datePicker.getEditor().getText()) : datePicker.getValue().toString();
            String time = timeField.getText();
            String reason = reasonField.getText();
            String notes = notesArea.getText();
            if (editingAppointment == null) {
                Appointment created = appointmentService.createAppointment(pid, did, date, time, reason, notes);
                showSuccess("Appointment scheduled (" + created.getId() + ") for " + created.getPatientName() + " with Dr. " + created.getDoctorName() + ".");
            } else {
                Appointment updated = appointmentService.updateAppointment(editingAppointment.getId(), pid, did, date, time, reason, notes);
                showSuccess("Appointment " + updated.getId() + " updated.");
            }
            populateChoices(); // in case new patients/doctors were added elsewhere
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onCancel() {
        if (editingAppointment == null) { showError("Select an appointment first."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel appointment");
        confirm.setHeaderText("Cancel this appointment?");
        confirm.setContentText("Are you sure you want to cancel the appointment for " +
                editingAppointment.getPatientName() + " with Dr. " + editingAppointment.getDoctorName() +
                " on " + editingAppointment.getAppointmentDate() + " at " + editingAppointment.getAppointmentTime() + "?");
        UiStyles.dialog(confirm, true);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;
        try {
            appointmentService.cancelAppointment(editingAppointment.getId());
            showSuccess("Appointment cancelled.");
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onComplete() {
        if (editingAppointment == null) { showError("Select an appointment first."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark appointment completed");
        confirm.setHeaderText("Mark this appointment as completed?");
        confirm.setContentText("Mark the appointment for " + editingAppointment.getPatientName() +
                " with Dr. " + editingAppointment.getDoctorName() + " as completed?");
        UiStyles.dialog(confirm, false);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;
        try {
            appointmentService.completeAppointment(editingAppointment.getId());
            showSuccess("Appointment marked completed.");
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
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
        UiStyles.apply(scene);
    }
}
