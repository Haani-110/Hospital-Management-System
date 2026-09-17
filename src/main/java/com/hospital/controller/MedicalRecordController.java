package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.MedicalRecord;
import com.hospital.model.Role;
import com.hospital.service.AppointmentService;
import com.hospital.service.MedicalRecordService;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Medical Records screen (Phase 4).
 *
 * Admins and doctors can create and edit records; doctors are restricted to
 * appointments whose doctor profile matches their own user account (see
 * {@link MedicalRecordService}). Receptionists have a read-only view. All
 * mutation restrictions are also enforced at the service layer.
 */
public class MedicalRecordController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicalRecordService medicalRecordService;
    private final AppointmentService appointmentService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenUserManagement;
    private Runnable onOpenPrescriptions;

    private TableView<MedicalRecord> table;
    private TextField searchField;

    private ComboBox<Appointment> appointmentCombo;
    private DatePicker recordDatePicker;
    private TextField diagnosisField;
    private TextArea symptomsArea;
    private TextArea examinationArea;
    private TextArea treatmentArea;

    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button clearButton;
    private Button viewDetailsButton;

    private MedicalRecord editingRecord;

    public MedicalRecordController(MedicalRecordService medicalRecordService,
                                   AppointmentService appointmentService,
                                   SceneManager sceneManager) {
        this.medicalRecordService = medicalRecordService;
        this.appointmentService = appointmentService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        Scene scene = new Scene(root, 1300, 760);
        applyCss(scene);
        refreshTable();
        populateCompletedAppointments();
        resetForm();
        return scene;
    }

    private Node buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(15, 20, 15, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        Label title = new Label("Medical Records");
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

        Button patients = new Button("Patients");
        styleNav(patients);
        patients.setOnAction(e -> { if (onOpenPatients != null) onOpenPatients.run(); });

        Button doctors = new Button("Doctors");
        styleNav(doctors);
        doctors.setOnAction(e -> { if (onOpenDoctors != null) onOpenDoctors.run(); });

        Button depts = new Button("Departments");
        styleNav(depts);
        depts.setVisible(Session.getInstance().getRole() == Role.ADMIN);
        depts.setManaged(Session.getInstance().getRole() == Role.ADMIN);
        depts.setOnAction(e -> { if (onOpenDepartments != null) onOpenDepartments.run(); });

        Button users = new Button("User Management");
        styleNav(users);
        users.setVisible(Session.getInstance().getRole() == Role.ADMIN);
        users.setManaged(Session.getInstance().getRole() == Role.ADMIN);
        users.setOnAction(e -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });

        Button appts = new Button("Appointments");
        styleNav(appts);
        appts.setOnAction(e -> { if (onOpenAppointments != null) onOpenAppointments.run(); });

        Label here = new Label("Medical Records");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));

        Button prescBtn = new Button("Prescriptions");
        styleNav(prescBtn);
        prescBtn.setOnAction(e -> { if (onOpenPrescriptions != null) onOpenPrescriptions.run(); });

        sidebar.getChildren().addAll(t, new Separator(), dash, patients, here, appts, prescBtn, doctors, depts, users);
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
        searchField.setPromptText("Search patient, code, doctor, diagnosis, symptoms...");
        searchField.setPrefWidth(420);
        searchField.textProperty().addListener((o, a, b) -> refreshTable());
        filterRow.getChildren().addAll(searchField);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No medical records found."));

        TableColumn<MedicalRecord, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50); idCol.setMaxWidth(70);

        TableColumn<MedicalRecord, Integer> apptCol = new TableColumn<>("Appt");
        apptCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        apptCol.setPrefWidth(60);

        TableColumn<MedicalRecord, LocalDate> apptDateCol = new TableColumn<>("Date");
        apptDateCol.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        apptDateCol.setCellFactory(col -> dateCell());

        TableColumn<MedicalRecord, LocalTime> apptTimeCol = new TableColumn<>("Time");
        apptTimeCol.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        apptTimeCol.setCellFactory(col -> timeCell());
        apptTimeCol.setPrefWidth(70);

        TableColumn<MedicalRecord, String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));

        TableColumn<MedicalRecord, String> codeCol = new TableColumn<>("Pat. Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        codeCol.setPrefWidth(100);

        TableColumn<MedicalRecord, String> doctorCol = new TableColumn<>("Doctor");
        doctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        TableColumn<MedicalRecord, String> diagCol = new TableColumn<>("Diagnosis");
        diagCol.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));

        TableColumn<MedicalRecord, LocalDate> recDateCol = new TableColumn<>("Record Date");
        recDateCol.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        recDateCol.setCellFactory(col -> dateCell());

        @SuppressWarnings("unchecked")
        TableColumn<MedicalRecord, ?>[] cols = new TableColumn[] {
                idCol, apptCol, apptDateCol, apptTimeCol, patientCol, codeCol, doctorCol, diagCol, recDateCol
        };
        table.getColumns().addAll(cols);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) populateForm(n);
        });

        HBox tableBtnRow = new HBox(10);
        viewDetailsButton = new Button("View Details");
        viewDetailsButton.getStyleClass().add("secondary-button");
        viewDetailsButton.setDisable(true);
        viewDetailsButton.setOnAction(e -> onViewDetails());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) ->
                viewDetailsButton.setDisable(n == null));
        tableBtnRow.getChildren().addAll(viewDetailsButton);

        tablePane.getChildren().addAll(filterRow, table, tableBtnRow);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---------- Form pane ----------
        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(10));
        formPane.setMinWidth(460);
        formPane.setMaxWidth(500);
        formPane.getStyleClass().add("card");

        formTitle = new Label("New Medical Record");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);

        form.add(new Label("Appointment *"), 0, 0);
        appointmentCombo = new ComboBox<>();
        appointmentCombo.setMaxWidth(Double.MAX_VALUE);
        appointmentCombo.setCellFactory(lv -> appointmentCell());
        appointmentCombo.setButtonCell(appointmentCell());
        form.add(appointmentCombo, 1, 0);

        form.add(new Label("Record Date *"), 0, 1);
        recordDatePicker = new DatePicker();
        recordDatePicker.setValue(LocalDate.now());
        form.add(recordDatePicker, 1, 1);

        form.add(new Label("Diagnosis *"), 0, 2);
        diagnosisField = new TextField();
        diagnosisField.setPromptText("required, up to 500 chars");
        form.add(diagnosisField, 1, 2);

        form.add(new Label("Symptoms"), 0, 3);
        symptomsArea = new TextArea();
        symptomsArea.setPromptText("optional, up to 2000 chars");
        symptomsArea.setPrefRowCount(3);
        form.add(symptomsArea, 1, 3);

        form.add(new Label("Examination"), 0, 4);
        examinationArea = new TextArea();
        examinationArea.setPromptText("optional, up to 3000 chars");
        examinationArea.setPrefRowCount(3);
        form.add(examinationArea, 1, 4);

        form.add(new Label("Treatment Notes"), 0, 5);
        treatmentArea = new TextArea();
        treatmentArea.setPromptText("optional, up to 3000 chars");
        treatmentArea.setPrefRowCount(3);
        form.add(treatmentArea, 1, 5);

        for (Node n : new Node[] { appointmentCombo, recordDatePicker, diagnosisField, symptomsArea, examinationArea, treatmentArea }) {
            GridPane.setHgrow(n, Priority.ALWAYS);
        }

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");

        saveButton = new Button("Save Record");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        HBox btns = new HBox(10, saveButton, clearButton);
        btns.setAlignment(Pos.CENTER_LEFT);
        btns.setPadding(new Insets(6, 0, 0, 0));

        saveButton.setOnAction(e -> onSave());
        clearButton.setOnAction(e -> resetForm());

        formPane.getChildren().addAll(formTitle, form, messageLabel, btns);
        content.getChildren().addAll(tablePane, formPane);
        return content;
    }

    private TableCell<MedicalRecord, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        };
    }

    private TableCell<MedicalRecord, LocalTime> timeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalTime t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.format(TIME_FMT));
            }
        };
    }

    private ListCell<Appointment> appointmentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Appointment a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); return; }
                String date = a.getAppointmentDate() == null ? "" : a.getAppointmentDate().format(DATE_FMT);
                String time = a.getAppointmentTime() == null ? "" : a.getAppointmentTime().format(TIME_FMT);
                setText(date + " " + time + " — " + a.getPatientName() + " — Dr. " + a.getDoctorName());
            }
        };
    }

    private void populateCompletedAppointments() {
        try {
            List<Appointment> eligible = medicalRecordService.getCompletedAppointmentsForCreate();
            appointmentCombo.setItems(FXCollections.observableArrayList(eligible));
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshTable() {
        try {
            String q = searchField == null ? null : searchField.getText();
            List<MedicalRecord> list = medicalRecordService.searchRecords(q);
            table.getItems().setAll(list);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateForm(MedicalRecord m) {
        this.editingRecord = m;
        formTitle.setText("Edit Medical Record #" + m.getId());
        recordDatePicker.setValue(m.getRecordDate());
        diagnosisField.setText(m.getDiagnosis() == null ? "" : m.getDiagnosis());
        symptomsArea.setText(m.getSymptoms() == null ? "" : m.getSymptoms());
        examinationArea.setText(m.getExamination() == null ? "" : m.getExamination());
        treatmentArea.setText(m.getTreatmentNotes() == null ? "" : m.getTreatmentNotes());

        // Select the appointment if it is a COMPLETED one visible in the combo;
        // otherwise add it so the user can see the reference.
        selectAppointmentById(m.getAppointmentId());

        boolean editable = canEdit();
        saveButton.setText("Save Changes");
        saveButton.setDisable(!editable);
        saveButton.setVisible(editable);
        saveButton.setManaged(editable);

        appointmentCombo.setDisable(true); // when editing, appointment cannot change
        recordDatePicker.setDisable(!editable);
        diagnosisField.setDisable(!editable);
        symptomsArea.setDisable(!editable);
        examinationArea.setDisable(!editable);
        treatmentArea.setDisable(!editable);
        clearMessage();
    }

    private boolean canEdit() {
        Role r = Session.getInstance().getRole();
        return r == Role.ADMIN || r == Role.DOCTOR;
    }

    private void resetForm() {
        this.editingRecord = null;
        boolean canCreate = canEdit();
        formTitle.setText(canCreate ? "New Medical Record" : "Medical Records");
        recordDatePicker.setValue(LocalDate.now());
        diagnosisField.clear();
        symptomsArea.clear();
        examinationArea.clear();
        treatmentArea.clear();
        appointmentCombo.setValue(null);
        appointmentCombo.setDisable(!canCreate);
        recordDatePicker.setDisable(!canCreate);
        diagnosisField.setDisable(!canCreate);
        symptomsArea.setDisable(!canCreate);
        examinationArea.setDisable(!canCreate);
        treatmentArea.setDisable(!canCreate);
        saveButton.setText("Save Record");
        saveButton.setDisable(!canCreate);
        saveButton.setVisible(canCreate);
        saveButton.setManaged(canCreate);
        table.getSelectionModel().clearSelection();
        clearMessage();
    }

    private void selectAppointmentById(int apptId) {
        for (Appointment a : appointmentCombo.getItems()) {
            if (a.getId() == apptId) { appointmentCombo.setValue(a); return; }
        }
        try {
            Appointment a = appointmentService.getAppointment(apptId);
            appointmentCombo.getItems().add(a);
            appointmentCombo.setValue(a);
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            // leave
        }
    }

    private void onSave() {
        clearMessage();
        try {
            Appointment a = appointmentCombo.getValue();
            Integer apptId = a == null ? null : a.getId();
            String diag = diagnosisField.getText();
            String sym = symptomsArea.getText();
            String exam = examinationArea.getText();
            String treat = treatmentArea.getText();
            String rdate = recordDatePicker.getValue() == null ? null : recordDatePicker.getValue().toString();
            if (editingRecord == null) {
                MedicalRecord created = medicalRecordService.createRecord(apptId, diag, sym, exam, treat, rdate);
                showSuccess("Medical record #" + created.getId() + " created for " + created.getPatientName() + ".");
            } else {
                MedicalRecord updated = medicalRecordService.updateRecord(editingRecord.getId(), diag, sym, exam, treat, rdate);
                showSuccess("Medical record #" + updated.getId() + " updated.");
            }
            populateCompletedAppointments();
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onViewDetails() {
        MedicalRecord m = table.getSelectionModel().getSelectedItem();
        if (m == null) return;

        StringBuilder body = new StringBuilder();
        body.append("Patient: ").append(m.getPatientName()).append(" (").append(m.getPatientCode()).append(")\n");
        body.append("Doctor: Dr. ").append(m.getDoctorName()).append("\n");
        String date = m.getAppointmentDate() == null ? "" : m.getAppointmentDate().format(DATE_FMT);
        String time = m.getAppointmentTime() == null ? "" : m.getAppointmentTime().format(TIME_FMT);
        body.append("Appointment: ").append(date).append(" ").append(time).append(" (id=").append(m.getAppointmentId()).append(")\n");
        body.append("Record date: ").append(m.getRecordDate() == null ? "" : m.getRecordDate().format(DATE_FMT)).append("\n\n");
        body.append("Diagnosis:\n").append(nullSafe(m.getDiagnosis())).append("\n\n");
        body.append("Symptoms:\n").append(nullSafe(m.getSymptoms())).append("\n\n");
        body.append("Examination:\n").append(nullSafe(m.getExamination())).append("\n\n");
        body.append("Treatment Notes:\n").append(nullSafe(m.getTreatmentNotes())).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Medical Record #" + m.getId());
        alert.setHeaderText("Medical Record Details");
        Label content = new Label(body.toString());
        content.setWrapText(true);
        content.setMaxWidth(600);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinWidth(650);
        alert.showAndWait();
    }

    private String nullSafe(String s) { return s == null || s.isEmpty() ? "(not recorded)" : s; }

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
