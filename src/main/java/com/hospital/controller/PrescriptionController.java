package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.MedicalRecord;
import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.model.Role;
import com.hospital.service.MedicalRecordService;
import com.hospital.service.PrescriptionService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.hospital.service.PrescriptionService.PrescriptionItemInput;

/**
 * Prescription Management screen (Phase 5).
 */
public class PrescriptionController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PrescriptionService prescriptionService;
    private final MedicalRecordService medicalRecordService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenUserManagement;
    private Runnable onOpenBilling;
    private Runnable onOpenReports;

    private TableView<Prescription> table;
    private TextField searchField;

    private ComboBox<MedicalRecord> recordCombo;
    private Label patientLabel;
    private Label doctorLabel;
    private DatePicker datePicker;
    private TextArea notesArea;
    private TableView<ItemRow> itemsTable;
    private ObservableList<ItemRow> itemsData;

    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button clearButton;
    private Button viewDetailsButton;
    private Button addItemButton;
    private Button removeItemButton;

    private Prescription editingPrescription;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  MedicalRecordService medicalRecordService,
                                  SceneManager sceneManager) {
        this.prescriptionService = prescriptionService;
        this.medicalRecordService = medicalRecordService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
    public void setOnOpenBilling(Runnable r) { this.onOpenBilling = r; }
    public void setOnOpenReports(Runnable r) { this.onOpenReports = r; }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        Scene scene = new Scene(root, 1350, 780);
        applyCss(scene);
        refreshTable();
        populateEligibleRecords();
        resetForm();
        return scene;
    }

    private Node buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(15, 20, 15, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        Label title = new Label("Prescription Management");
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
        Button records = navBtn("Medical Records", () -> { if (onOpenMedicalRecords != null) onOpenMedicalRecords.run(); });
        Button appts = navBtn("Appointments", () -> { if (onOpenAppointments != null) onOpenAppointments.run(); });
        Button depts = navBtn("Departments", () -> { if (onOpenDepartments != null) onOpenDepartments.run(); });
        depts.setVisible(isAdmin); depts.setManaged(isAdmin);
        Button users = navBtn("User Management", () -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });
        users.setVisible(isAdmin); users.setManaged(isAdmin);

        Label here = new Label("Prescriptions");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));

        Button billingBtn = navBtn("Billing", () -> { if (onOpenBilling != null) onOpenBilling.run(); });
        Button reportsBtn = navBtn("Reports", () -> { if (onOpenReports != null) onOpenReports.run(); });

        sidebar.getChildren().addAll(t, new Separator(), dash, patients, here, records, appts, doctors,
                depts, users, billingBtn, reportsBtn);
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
        searchField.setPromptText("Search patient, code, doctor, medicine...");
        searchField.setPrefWidth(400);
        searchField.textProperty().addListener((o, a, b) -> refreshTable());
        filterRow.getChildren().addAll(searchField);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No prescriptions found."));

        TableColumn<Prescription, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50); idCol.setMaxWidth(70);

        TableColumn<Prescription, Integer> recCol = new TableColumn<>("Record");
        recCol.setCellValueFactory(new PropertyValueFactory<>("medicalRecordId"));
        recCol.setPrefWidth(70);

        TableColumn<Prescription, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("prescriptionDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });

        TableColumn<Prescription, String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));

        TableColumn<Prescription, String> codeCol = new TableColumn<>("Pat. Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        codeCol.setPrefWidth(100);

        TableColumn<Prescription, String> doctorCol = new TableColumn<>("Doctor");
        doctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        TableColumn<Prescription, Integer> cntCol = new TableColumn<>("Medicines");
        cntCol.setCellValueFactory(new PropertyValueFactory<>("itemCount"));
        cntCol.setPrefWidth(80);

        @SuppressWarnings("unchecked")
        TableColumn<Prescription, ?>[] cols = new TableColumn[] { idCol, recCol, dateCol, patientCol, codeCol, doctorCol, cntCol };
        table.getColumns().addAll(cols);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) populateForm(n); });

        HBox actionRow = new HBox(10);
        viewDetailsButton = new Button("View Details");
        viewDetailsButton.getStyleClass().add("secondary-button");
        viewDetailsButton.setDisable(true);
        viewDetailsButton.setOnAction(e -> onViewDetails());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> viewDetailsButton.setDisable(n == null));
        actionRow.getChildren().addAll(viewDetailsButton);

        tablePane.getChildren().addAll(filterRow, table, actionRow);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---------- Form pane ----------
        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(10));
        formPane.setMinWidth(520);
        formPane.setMaxWidth(560);
        formPane.getStyleClass().add("card");

        formTitle = new Label("New Prescription");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);

        form.add(new Label("Medical Record *"), 0, 0);
        recordCombo = new ComboBox<>();
        recordCombo.setMaxWidth(Double.MAX_VALUE);
        recordCombo.setCellFactory(lv -> recordCell());
        recordCombo.setButtonCell(recordCell());
        recordCombo.valueProperty().addListener((o, a, n) -> updatePatientDoctorLabels(n));
        form.add(recordCombo, 1, 0);

        form.add(new Label("Patient"), 0, 1);
        patientLabel = new Label("-");
        patientLabel.getStyleClass().add("info-text");
        form.add(patientLabel, 1, 1);

        form.add(new Label("Doctor"), 0, 2);
        doctorLabel = new Label("-");
        doctorLabel.getStyleClass().add("info-text");
        form.add(doctorLabel, 1, 2);

        form.add(new Label("Prescription Date *"), 0, 3);
        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        form.add(datePicker, 1, 3);

        form.add(new Label("Notes"), 0, 4);
        notesArea = new TextArea();
        notesArea.setPromptText("optional, up to 2000 chars");
        notesArea.setPrefRowCount(2);
        form.add(notesArea, 1, 4);

        for (Node n : new Node[] { recordCombo, datePicker, notesArea }) {
            GridPane.setHgrow(n, Priority.ALWAYS);
        }

        Label itemsLabel = new Label("Medicines *");
        itemsLabel.getStyleClass().add("section-title");

        itemsData = FXCollections.observableArrayList();
        itemsTable = new TableView<>(itemsData);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemsTable.setPrefHeight(200);
        itemsTable.setEditable(true);

        TableColumn<ItemRow, String> medCol = new TableColumn<>("Medicine");
        medCol.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        medCol.setCellFactory(TextFieldTableCell.forTableColumn());
        medCol.setOnEditCommit(e -> e.getRowValue().setMedicineName(e.getNewValue()));

        TableColumn<ItemRow, String> doseCol = new TableColumn<>("Dosage");
        doseCol.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        doseCol.setCellFactory(TextFieldTableCell.forTableColumn());
        doseCol.setOnEditCommit(e -> e.getRowValue().setDosage(e.getNewValue()));

        TableColumn<ItemRow, String> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        freqCol.setCellFactory(TextFieldTableCell.forTableColumn());
        freqCol.setOnEditCommit(e -> e.getRowValue().setFrequency(e.getNewValue()));

        TableColumn<ItemRow, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durCol.setCellFactory(TextFieldTableCell.forTableColumn());
        durCol.setOnEditCommit(e -> e.getRowValue().setDuration(e.getNewValue()));

        TableColumn<ItemRow, String> instrCol = new TableColumn<>("Instructions");
        instrCol.setCellValueFactory(new PropertyValueFactory<>("instructions"));
        instrCol.setCellFactory(TextFieldTableCell.forTableColumn());
        instrCol.setOnEditCommit(e -> e.getRowValue().setInstructions(e.getNewValue()));

        @SuppressWarnings("unchecked")
        TableColumn<ItemRow, ?>[] itemCols = new TableColumn[] { medCol, doseCol, freqCol, durCol, instrCol };
        itemsTable.getColumns().addAll(itemCols);

        HBox itemBtns = new HBox(10);
        addItemButton = new Button("+ Add Medicine");
        addItemButton.getStyleClass().add("secondary-button");
        addItemButton.setOnAction(e -> addEmptyItem());
        removeItemButton = new Button("Remove Selected");
        removeItemButton.getStyleClass().add("danger-button");
        removeItemButton.setOnAction(e -> removeSelectedItem());
        itemBtns.getChildren().addAll(addItemButton, removeItemButton);

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");

        saveButton = new Button("Save Prescription");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        HBox btns = new HBox(10, saveButton, clearButton);
        btns.setAlignment(Pos.CENTER_LEFT);
        btns.setPadding(new Insets(6, 0, 0, 0));

        saveButton.setOnAction(e -> onSave());
        clearButton.setOnAction(e -> resetForm());

        formPane.getChildren().addAll(formTitle, form, itemsLabel, itemsTable, itemBtns, messageLabel, btns);
        content.getChildren().addAll(tablePane, formPane);
        return content;
    }

    private ListCell<MedicalRecord> recordCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(MedicalRecord r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setText(null); return; }
                String date = r.getRecordDate() == null ? "" : r.getRecordDate().format(DATE_FMT);
                setText("MR#" + r.getId() + " — " + date + " — " + r.getPatientName()
                        + " (" + r.getPatientCode() + ") — Dr. " + r.getDoctorName()
                        + " — " + (r.getDiagnosis() == null ? "" : r.getDiagnosis()));
            }
        };
    }

    private void updatePatientDoctorLabels(MedicalRecord r) {
        if (r == null) {
            patientLabel.setText("-");
            doctorLabel.setText("-");
            return;
        }
        patientLabel.setText(r.getPatientName() + " (" + r.getPatientCode() + ")");
        doctorLabel.setText("Dr. " + r.getDoctorName());
    }

    private void populateEligibleRecords() {
        try {
            List<MedicalRecord> eligible = prescriptionService.getEligibleMedicalRecordsForCreate();
            recordCombo.setItems(FXCollections.observableArrayList(eligible));
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshTable() {
        try {
            String q = searchField == null ? null : searchField.getText();
            List<Prescription> list = prescriptionService.searchPrescriptions(q);
            table.getItems().setAll(list);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateForm(Prescription p) {
        this.editingPrescription = p;
        formTitle.setText("Edit Prescription #" + p.getId());
        datePicker.setValue(p.getPrescriptionDate());
        notesArea.setText(p.getNotes() == null ? "" : p.getNotes());

        // The medical record combo shows eligible records for create. For editing,
        // add the existing record and lock the combo so it cannot be changed.
        recordCombo.getItems().clear();
        try {
            MedicalRecord r = medicalRecordService.getRecord(p.getMedicalRecordId());
            recordCombo.getItems().add(r);
            recordCombo.setValue(r);
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
        recordCombo.setDisable(true);

        itemsData.clear();
        try {
            List<PrescriptionItem> its = prescriptionService.getItems(p.getId());
            for (PrescriptionItem it : its) {
                itemsData.add(new ItemRow(it.getMedicineName(), it.getDosage(), it.getFrequency(),
                        it.getDuration(), it.getInstructions()));
            }
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }

        boolean editable = canEdit();
        saveButton.setText("Save Changes");
        saveButton.setDisable(!editable);
        saveButton.setVisible(editable);
        saveButton.setManaged(editable);

        datePicker.setDisable(!editable);
        notesArea.setDisable(!editable);
        itemsTable.setDisable(!editable);
        addItemButton.setDisable(!editable);
        removeItemButton.setDisable(!editable);
        clearMessage();
    }

    private boolean canEdit() {
        Role r = Session.getInstance().getRole();
        return r == Role.ADMIN || r == Role.DOCTOR;
    }

    private void resetForm() {
        this.editingPrescription = null;
        boolean canCreate = canEdit();
        formTitle.setText(canCreate ? "New Prescription" : "Prescriptions");
        datePicker.setValue(LocalDate.now());
        notesArea.clear();
        itemsData.clear();
        addEmptyItem();
        populateEligibleRecords();
        recordCombo.setValue(null);
        recordCombo.setDisable(!canCreate);
        datePicker.setDisable(!canCreate);
        notesArea.setDisable(!canCreate);
        itemsTable.setDisable(!canCreate);
        addItemButton.setDisable(!canCreate);
        removeItemButton.setDisable(!canCreate);
        saveButton.setText("Save Prescription");
        saveButton.setDisable(!canCreate);
        saveButton.setVisible(canCreate);
        saveButton.setManaged(canCreate);
        table.getSelectionModel().clearSelection();
        updatePatientDoctorLabels(null);
        clearMessage();
    }

    private void addEmptyItem() {
        itemsData.add(new ItemRow("", "", "", "", ""));
    }

    private void removeSelectedItem() {
        ItemRow sel = itemsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Select a medicine item to remove.");
            return;
        }
        if (itemsData.size() <= 1) {
            showError("A prescription must have at least one medicine.");
            return;
        }
        itemsData.remove(sel);
        clearMessage();
    }

    private void onSave() {
        clearMessage();
        try {
            MedicalRecord rec = recordCombo.getValue();
            Integer recId = rec == null ? null : rec.getId();
            String date = datePicker.getValue() == null ? null : datePicker.getValue().toString();
            String notes = notesArea.getText();
            List<PrescriptionItemInput> inputs = new ArrayList<>();
            for (ItemRow row : itemsData) {
                inputs.add(new PrescriptionItemInput(row.getMedicineName(), row.getDosage(),
                        row.getFrequency(), row.getDuration(), row.getInstructions()));
            }
            if (editingPrescription == null) {
                Prescription created = prescriptionService.createPrescription(recId, date, notes, inputs);
                showSuccess("Prescription #" + created.getId() + " created for " + created.getPatientName() + ".");
            } else {
                Prescription updated = prescriptionService.updatePrescription(editingPrescription.getId(), date, notes, inputs);
                showSuccess("Prescription #" + updated.getId() + " updated.");
            }
            populateEligibleRecords();
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onViewDetails() {
        Prescription p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        try {
            MedicalRecord r = medicalRecordService.getRecord(p.getMedicalRecordId());
            List<PrescriptionItem> its = prescriptionService.getItems(p.getId());
            StringBuilder body = new StringBuilder();
            body.append("Prescription ID: ").append(p.getId()).append("\n");
            body.append("Medical Record: MR#").append(r.getId()).append(" (").append(r.getDiagnosis()).append(")\n");
            body.append("Patient: ").append(p.getPatientName()).append(" (").append(p.getPatientCode()).append(")\n");
            body.append("Doctor: Dr. ").append(p.getDoctorName()).append("\n");
            body.append("Date: ").append(p.getPrescriptionDate() == null ? "" : p.getPrescriptionDate().format(DATE_FMT)).append("\n");
            body.append("Notes: ").append(p.getNotes() == null ? "(none)" : p.getNotes()).append("\n\n");
            body.append("Medicines:\n");
            int n = 1;
            for (PrescriptionItem it : its) {
                body.append(n++).append(". ").append(it.getMedicineName())
                        .append(" — ").append(it.getDosage())
                        .append(", ").append(it.getFrequency())
                        .append(", for ").append(it.getDuration()).append("\n");
                if (it.getInstructions() != null && !it.getInstructions().isBlank()) {
                    body.append("   Instructions: ").append(it.getInstructions()).append("\n");
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Prescription #" + p.getId());
            alert.setHeaderText("Prescription Details");
            Label content = new Label(body.toString());
            content.setWrapText(true);
            content.setMaxWidth(650);
            alert.getDialogPane().setContent(content);
            alert.getDialogPane().setMinWidth(700);
            alert.showAndWait();
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
        try {
            var cssUrl = getClass().getResource("/com/hospital/css/styles.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}
    }

    /** Editable row model for the medicines table in the form. */
    public static class ItemRow {
        private final StringProperty medicineName = new SimpleStringProperty("");
        private final StringProperty dosage = new SimpleStringProperty("");
        private final StringProperty frequency = new SimpleStringProperty("");
        private final StringProperty duration = new SimpleStringProperty("");
        private final StringProperty instructions = new SimpleStringProperty("");

        public ItemRow() {}
        public ItemRow(String m, String d, String f, String du, String i) {
            setMedicineName(m); setDosage(d); setFrequency(f); setDuration(du); setInstructions(i);
        }
        public StringProperty medicineNameProperty() { return medicineName; }
        public String getMedicineName() { return medicineName.get(); }
        public void setMedicineName(String s) { medicineName.set(s == null ? "" : s); }
        public StringProperty dosageProperty() { return dosage; }
        public String getDosage() { return dosage.get(); }
        public void setDosage(String s) { dosage.set(s == null ? "" : s); }
        public StringProperty frequencyProperty() { return frequency; }
        public String getFrequency() { return frequency.get(); }
        public void setFrequency(String s) { frequency.set(s == null ? "" : s); }
        public StringProperty durationProperty() { return duration; }
        public String getDuration() { return duration.get(); }
        public void setDuration(String s) { duration.set(s == null ? "" : s); }
        public StringProperty instructionsProperty() { return instructions; }
        public String getInstructions() { return instructions.get(); }
        public void setInstructions(String s) { instructions.set(s == null ? "" : s); }
    }
}
