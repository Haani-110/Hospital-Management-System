package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Bill;
import com.hospital.model.BillItem;
import com.hospital.model.Patient;
import com.hospital.model.Role;
import com.hospital.service.AppointmentService;
import com.hospital.service.BillingService;
import com.hospital.service.PatientService;
import com.hospital.service.Session;
import com.hospital.util.SceneManager;
import com.hospital.util.UiStyles;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hospital.service.BillingService.BillItemInput;

/**
 * Billing Management screen (Phase 6).
 */
public class BillingController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MONEY_FMT = "%,.2f";

    private final BillingService billingService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenUserManagement;
    private Runnable onOpenReports;

    private TableView<Bill> table;
    private TextField searchField;
    private ComboBox<String> statusFilter;

    private ComboBox<Patient> patientCombo;
    private ComboBox<Appointment> appointmentCombo;
    private DatePicker datePicker;
    private TextField discountField;
    private TextArea notesArea;
    private TableView<ItemRow> itemsTable;
    private ObservableList<ItemRow> itemsData;
    private Label subtotalLabel;
    private Label totalLabel;
    private Label messageLabel;
    private Label formTitle;
    private Button saveButton;
    private Button clearButton;
    private Button markPartialButton;
    private Button markPaidButton;
    private Button cancelBillButton;
    private Button viewDetailsButton;

    private Bill editingBill;

    public BillingController(BillingService billingService,
                             PatientService patientService,
                             AppointmentService appointmentService,
                             SceneManager sceneManager) {
        this.billingService = billingService;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable r) { this.onBackToDashboard = r; }
    public void setOnOpenDepartments(Runnable r) { this.onOpenDepartments = r; }
    public void setOnOpenDoctors(Runnable r) { this.onOpenDoctors = r; }
    public void setOnOpenPatients(Runnable r) { this.onOpenPatients = r; }
    public void setOnOpenAppointments(Runnable r) { this.onOpenAppointments = r; }
    public void setOnOpenMedicalRecords(Runnable r) { this.onOpenMedicalRecords = r; }
    public void setOnOpenPrescriptions(Runnable r) { this.onOpenPrescriptions = r; }
    public void setOnOpenUserManagement(Runnable r) { this.onOpenUserManagement = r; }
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
        resetForm();
        return scene;
    }

    private Node buildTopBar() {
        Label title = new Label("Billing Management");
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

        Button dash = navBtn("Dashboard", () -> { if (onBackToDashboard != null) onBackToDashboard.run(); });
        Button patients = navBtn("Patients", () -> { if (onOpenPatients != null) onOpenPatients.run(); });
        Button doctors = navBtn("Doctors", () -> { if (onOpenDoctors != null) onOpenDoctors.run(); });
        Button appts = navBtn("Appointments", () -> { if (onOpenAppointments != null) onOpenAppointments.run(); });
        Button records = navBtn("Medical Records", () -> { if (onOpenMedicalRecords != null) onOpenMedicalRecords.run(); });
        Button presc = navBtn("Prescriptions", () -> { if (onOpenPrescriptions != null) onOpenPrescriptions.run(); });
        Button depts = navBtn("Departments", () -> { if (onOpenDepartments != null) onOpenDepartments.run(); });
        Button users = navBtn("User Management", () -> { if (onOpenUserManagement != null) onOpenUserManagement.run(); });
        Button reports = navBtn("Reports", () -> { if (onOpenReports != null) onOpenReports.run(); });

        Label here = new Label("Billing");
        here.setMaxWidth(Double.MAX_VALUE);
        here.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(here, new Insets(2, 0, 2, 0));

        // Hidden navigation must not reserve space in the sidebar.
        depts.setVisible(isAdmin);
        depts.setManaged(isAdmin);
        users.setVisible(isAdmin);
        users.setManaged(isAdmin);
        doctors.setVisible(isAdminOrReceptionist);
        doctors.setManaged(isAdminOrReceptionist);
        here.setVisible(isAdminOrReceptionist);
        here.setManaged(isAdminOrReceptionist);

        sidebar.getChildren().addAll(t, new Separator(), dash, patients, appts, here, records, presc, doctors,
                reports, depts, users);
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
        VBox tablePane = new VBox(10);
        tablePane.setPadding(new Insets(16));
        tablePane.getStyleClass().add("card");

        FlowPane filterRow = new FlowPane(8, 8);
        searchField = new TextField();
        searchField.setPromptText("Search bill #, patient, code, doctor, item...");
        searchField.setPrefWidth(400);
        searchField.textProperty().addListener((o, a, b) -> refreshTable());
        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All", Bill.STATUS_UNPAID, Bill.STATUS_PARTIALLY_PAID, Bill.STATUS_PAID, Bill.STATUS_CANCELLED));
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> refreshTable());
        filterRow.getChildren().addAll(searchField, new Label("Status:"), statusFilter);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No bills found."));

        TableColumn<Bill, String> numCol = new TableColumn<>("Bill #");
        numCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBillNumber()));
        numCol.setPrefWidth(100);
        TableColumn<Bill, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getBillDate()));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });
        TableColumn<Bill, String> patCol = new TableColumn<>("Patient");
        patCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPatientName()));
        TableColumn<Bill, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPatientCode()));
        codeCol.setPrefWidth(100);
        TableColumn<Bill, String> docCol = new TableColumn<>("Doctor");
        docCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDoctorName()));
        TableColumn<Bill, BigDecimal> subCol = moneyCol("Subtotal");
        subCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
        TableColumn<Bill, BigDecimal> discCol = moneyCol("Discount");
        discCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDiscount()));
        TableColumn<Bill, BigDecimal> totCol = moneyCol("Total");
        totCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalAmount()));
        TableColumn<Bill, String> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        statCol.setPrefWidth(150);
        statCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value);
                UiStyles.statusCell(this, getText());
            }
        });
        TableColumn<Bill, Integer> itemsCol = new TableColumn<>("Items");
        itemsCol.getStyleClass().add("numeric-column");
        itemsCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getItemCount()).asObject());
        itemsCol.setPrefWidth(60);

        @SuppressWarnings("unchecked")
        TableColumn<Bill, ?>[] cols = new TableColumn[] { numCol, dateCol, patCol, codeCol, docCol, subCol, discCol, totCol, statCol, itemsCol };
        table.getColumns().addAll(cols);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) populateForm(n); });

        FlowPane actionRow = new FlowPane(8, 8);
        viewDetailsButton = new Button("View Details");
        viewDetailsButton.getStyleClass().add("secondary-button");
        viewDetailsButton.setDisable(true);
        viewDetailsButton.setOnAction(e -> onViewDetails());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> viewDetailsButton.setDisable(n == null));

        markPartialButton = new Button("Mark Partially Paid");
        markPartialButton.getStyleClass().add("secondary-button");
        markPartialButton.setDisable(true);
        markPartialButton.setOnAction(e -> onMarkStatus(Bill.STATUS_PARTIALLY_PAID));
        markPaidButton = new Button("Mark Paid");
        markPaidButton.getStyleClass().add("primary-button");
        markPaidButton.setDisable(true);
        markPaidButton.setOnAction(e -> onMarkStatus(Bill.STATUS_PAID));
        cancelBillButton = new Button("Cancel Bill");
        cancelBillButton.getStyleClass().add("danger-button");
        cancelBillButton.setDisable(true);
        cancelBillButton.setOnAction(e -> onCancelBill());
        actionRow.getChildren().addAll(viewDetailsButton, markPartialButton, markPaidButton, cancelBillButton);

        tablePane.getChildren().addAll(filterRow, table, actionRow);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(18));
        formPane.getStyleClass().add("card");

        formTitle = new Label("New Bill");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);

        form.add(new Label("Patient *"), 0, 0);
        patientCombo = new ComboBox<>();
        patientCombo.setMaxWidth(Double.MAX_VALUE);
        patientCombo.setCellFactory(lv -> personCell());
        patientCombo.setButtonCell(personCell());
        patientCombo.valueProperty().addListener((o, a, n) -> {
            refreshAppointmentOptions();
            recalculateTotals();
        });
        form.add(patientCombo, 1, 0);

        form.add(new Label("Appointment (optional)"), 0, 1);
        appointmentCombo = new ComboBox<>();
        appointmentCombo.setMaxWidth(Double.MAX_VALUE);
        appointmentCombo.setCellFactory(lv -> appointmentCell());
        appointmentCombo.setButtonCell(appointmentCell());
        appointmentCombo.setPromptText("None — walk-in / direct charge");
        appointmentCombo.valueProperty().addListener((o, a, n) -> {
            if (n != null && editingBill == null) {
                // Derive patient from appointment to prevent mismatch.
                patientCombo.getItems().stream()
                        .filter(p -> p.getId() == n.getPatientId())
                        .findFirst().ifPresent(p -> patientCombo.setValue(p));
            }
        });
        form.add(appointmentCombo, 1, 1);

        form.add(new Label("Bill Date *"), 0, 2);
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        form.add(datePicker, 1, 2);

        form.add(new Label("Discount"), 0, 3);
        discountField = new TextField("0.00");
        discountField.setMaxWidth(Double.MAX_VALUE);
        discountField.textProperty().addListener((o, a, b) -> recalculateTotals());
        form.add(discountField, 1, 3);

        form.add(new Label("Notes"), 0, 4);
        notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        notesArea.setPromptText("Optional, up to 2000 chars");
        form.add(notesArea, 1, 4);

        Label itemsLabel = new Label("Bill Items");
        itemsLabel.getStyleClass().add("section-title");
        itemsLabel.setPadding(new Insets(8, 0, 0, 0));

        itemsData = FXCollections.observableArrayList();
        itemsTable = new TableView<>(itemsData);
        itemsTable.setPrefHeight(220);
        itemsTable.setMinHeight(180);
        itemsTable.setEditable(true);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ItemRow, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(c -> c.getValue().descriptionProperty());
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e -> {
            e.getRowValue().setDescription(e.getNewValue());
            itemsTable.refresh();
        });
        TableColumn<ItemRow, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.getStyleClass().add("numeric-column");
        qtyCol.setCellValueFactory(c -> c.getValue().quantityProperty());
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        qtyCol.setPrefWidth(60);
        qtyCol.setOnEditCommit(e -> {
            e.getRowValue().setQuantity(e.getNewValue());
            recalculateTotals();
            itemsTable.refresh();
        });
        TableColumn<ItemRow, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.getStyleClass().add("numeric-column");
        priceCol.setCellValueFactory(c -> c.getValue().unitPriceProperty());
        priceCol.setCellFactory(TextFieldTableCell.forTableColumn());
        priceCol.setPrefWidth(100);
        priceCol.setOnEditCommit(e -> {
            e.getRowValue().setUnitPrice(e.getNewValue());
            recalculateTotals();
            itemsTable.refresh();
        });
        TableColumn<ItemRow, String> amtCol = new TableColumn<>("Amount");
        amtCol.getStyleClass().add("numeric-column");
        amtCol.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().computeAmount())));
        amtCol.setPrefWidth(100);
        amtCol.setEditable(false);

        @SuppressWarnings("unchecked")
        TableColumn<ItemRow, ?>[] itemCols = new TableColumn[] { descCol, qtyCol, priceCol, amtCol };
        itemsTable.getColumns().addAll(itemCols);

        FlowPane itemBtns = new FlowPane(8, 8);
        Button addItemButton = new Button("+ Add Item");
        addItemButton.getStyleClass().add("secondary-button");
        addItemButton.setOnAction(e -> {
            itemsData.add(new ItemRow("", "1", "0.00"));
            recalculateTotals();
        });
        Button removeItemButton = new Button("- Remove Item");
        removeItemButton.getStyleClass().add("secondary-button");
        removeItemButton.setOnAction(e -> {
            ItemRow sel = itemsTable.getSelectionModel().getSelectedItem();
            if (sel != null && itemsData.size() > 1) {
                itemsData.remove(sel);
                recalculateTotals();
            } else if (itemsData.size() <= 1) {
                showError("At least one bill item is required.");
            } else {
                showError("Select an item to remove.");
            }
        });
        itemBtns.getChildren().addAll(addItemButton, removeItemButton);

        FlowPane totalsBox = new FlowPane(8, 8);
        totalsBox.setPadding(new Insets(6, 0, 0, 0));
        subtotalLabel = new Label("Subtotal: 0.00");
        totalLabel = new Label("Total: 0.00");
        totalLabel.getStyleClass().add("billing-total");
        totalsBox.getStyleClass().add("summary-strip");
        totalsBox.getChildren().addAll(subtotalLabel, totalLabel);

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        saveButton = new Button("Save Bill");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);
        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");
        FlowPane btns = new FlowPane(8, 8, saveButton, clearButton);
        btns.setAlignment(Pos.CENTER_LEFT);
        btns.setPadding(new Insets(6, 0, 0, 0));
        saveButton.setOnAction(e -> onSave());
        clearButton.setOnAction(e -> resetForm());

        UiStyles.form(form);
        formPane.getChildren().addAll(formTitle, UiStyles.hint("* Required fields"), form,
                itemsLabel, UiStyles.hint("Double-click a cell to edit; press Enter to confirm."),
                UiStyles.tableViewport(itemsTable), itemBtns, totalsBox,
                messageLabel, btns);

        populatePatientOptions();
        return UiStyles.workspace(tablePane, formPane);
    }

    private TableColumn<Bill, BigDecimal> moneyCol(String title) {
        TableColumn<Bill, BigDecimal> col = new TableColumn<>(title);
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format(MONEY_FMT, v));
            }
        });
        col.setPrefWidth(100);
        col.getStyleClass().add("numeric-column");
        return col;
    }

    private ListCell<Patient> personCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(p.getPatientCode() + " — " + p.getFullName());
            }
        };
    }

    private ListCell<Appointment> appointmentCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Appointment a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); return; }
                String date = a.getAppointmentDate() == null ? "" : a.getAppointmentDate().format(DATE_FMT);
                setText("APT#" + a.getId() + " — " + date + " " + a.getAppointmentTime()
                        + " — Dr. " + a.getDoctorName());
            }
        };
    }

    private void populatePatientOptions() {
        try {
            List<Patient> list = patientService.getAllPatients();
            patientCombo.setItems(FXCollections.observableArrayList(list));
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAppointmentOptions() {
        Patient p = patientCombo.getValue();
        if (p == null) {
            appointmentCombo.setItems(FXCollections.observableArrayList());
            appointmentCombo.setValue(null);
            return;
        }
        try {
            List<Appointment> all = appointmentService.getAllAppointments();
            List<Appointment> mine = all.stream()
                    .filter(a -> a.getPatientId() == p.getId())
                    .toList();
            Appointment keep = appointmentCombo.getValue();
            appointmentCombo.setItems(FXCollections.observableArrayList(mine));
            if (keep != null && mine.stream().anyMatch(a -> a.getId() == keep.getId())) {
                appointmentCombo.setValue(keep);
            } else {
                appointmentCombo.setValue(null);
            }
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void recalculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRow r : itemsData) {
            BigDecimal amount = r.computeAmount();
            subtotal = subtotal.add(amount);
        }
        BigDecimal discount = BigDecimal.ZERO;
        try {
            String t = discountField.getText();
            if (t != null && !t.isBlank()) discount = new BigDecimal(t.trim());
        } catch (NumberFormatException ignore) { /* service will catch */ }
        if (discount.signum() < 0) discount = BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        BigDecimal total = subtotal.subtract(discount);
        subtotalLabel.setText(String.format("Subtotal: " + MONEY_FMT, subtotal));
        totalLabel.setText(String.format("Total: " + MONEY_FMT, total));
    }

    private String formatMoney(BigDecimal v) {
        return v == null ? "0.00" : String.format(MONEY_FMT, v);
    }

    private void refreshTable() {
        try {
            String q = searchField == null ? null : searchField.getText();
            String status = (statusFilter == null || "All".equals(statusFilter.getValue()))
                    ? null : statusFilter.getValue();
            List<Bill> list = billingService.searchBills(q, status);
            table.setItems(FXCollections.observableArrayList(list));
            updateActionButtons();
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateActionButtons() {
        Bill b = table.getSelectionModel().getSelectedItem();
        Role role = Session.getInstance().getRole();
        boolean isAdmin = role == Role.ADMIN;
        boolean isReceptionist = role == Role.RECEPTIONIST;
        boolean hasSelection = b != null;
        boolean canMarkPartial = hasSelection && (isAdmin || isReceptionist)
                && Bill.STATUS_UNPAID.equals(b.getStatus());
        boolean canMarkPaid = hasSelection && isAdmin
                && (Bill.STATUS_UNPAID.equals(b.getStatus()) || Bill.STATUS_PARTIALLY_PAID.equals(b.getStatus()));
        boolean canCancel = hasSelection && isAdmin
                && !Bill.STATUS_CANCELLED.equals(b.getStatus()) && !Bill.STATUS_PAID.equals(b.getStatus());
        markPartialButton.setDisable(!canMarkPartial);
        markPaidButton.setDisable(!canMarkPaid);
        cancelBillButton.setDisable(!canCancel);
    }

    private void onSave() {
        clearMessage();
        try {
            Patient p = patientCombo.getValue();
            Integer pid = p == null ? null : p.getId();
            Appointment a = appointmentCombo.getValue();
            Integer aid = a == null ? null : a.getId();
            String date = datePicker.getValue() == null ? null : datePicker.getValue().toString();
            String discount = discountField.getText();
            String notes = notesArea.getText();
            List<BillItemInput> inputs = new ArrayList<>();
            for (ItemRow row : itemsData) {
                inputs.add(new BillItemInput(row.getDescription(), row.getQuantity(), row.getUnitPrice()));
            }
            if (editingBill == null) {
                Bill created = billingService.createBill(pid, aid, date, discount, notes, inputs);
                showSuccess("Bill " + created.getBillNumber() + " created. Total: " + formatMoney(created.getTotalAmount()));
            } else {
                Bill updated = billingService.updateBill(editingBill.getId(), aid, date, discount, notes, inputs);
                showSuccess("Bill " + updated.getBillNumber() + " updated. Total: " + formatMoney(updated.getTotalAmount()));
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onMarkStatus(String target) {
        Bill b = table.getSelectionModel().getSelectedItem();
        if (b == null) return;
        clearMessage();
        try {
            billingService.markStatus(b.getId(), target);
            showSuccess("Bill " + b.getBillNumber() + " marked as " + target + ".");
            refreshTable();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onCancelBill() {
        Bill b = table.getSelectionModel().getSelectedItem();
        if (b == null) return;
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Cancel Bill");
        conf.setHeaderText("Cancel bill " + b.getBillNumber() + "?");
        conf.setContentText("Cancelled bills cannot be edited or changed back. This action preserves the record for history.");
        UiStyles.dialog(conf, true);
        Optional<ButtonType> res = conf.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;
        clearMessage();
        try {
            billingService.markStatus(b.getId(), Bill.STATUS_CANCELLED);
            showSuccess("Bill " + b.getBillNumber() + " has been cancelled.");
            refreshTable();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onViewDetails() {
        Bill b = table.getSelectionModel().getSelectedItem();
        if (b == null) return;
        try {
            List<BillItem> its = billingService.getItems(b.getId());
            StringBuilder body = new StringBuilder();
            body.append("Bill Number: ").append(b.getBillNumber()).append("\n");
            body.append("Date: ").append(b.getBillDate() == null ? "" : b.getBillDate().format(DATE_FMT)).append("\n");
            body.append("Status: ").append(b.getStatus()).append("\n");
            body.append("Patient: ").append(b.getPatientName()).append(" (").append(b.getPatientCode()).append(")\n");
            if (b.getAppointmentId() != null) {
                body.append("Appointment: APT#").append(b.getAppointmentId());
                if (b.getAppointmentDate() != null) body.append(" — ").append(b.getAppointmentDate());
                if (b.getAppointmentTime() != null) body.append(" ").append(b.getAppointmentTime());
                body.append("\n");
            }
            if (b.getDoctorName() != null) body.append("Doctor: Dr. ").append(b.getDoctorName()).append("\n");
            body.append("\nItems:\n");
            int n = 1;
            for (BillItem it : its) {
                body.append(String.format("%d. %s  × %d @ %s = %s%n",
                        n++, it.getDescription(), it.getQuantity(),
                        formatMoney(it.getUnitPrice()), formatMoney(it.getAmount())));
            }
            body.append("\nSubtotal: ").append(formatMoney(b.getSubtotal())).append("\n");
            body.append("Discount: ").append(formatMoney(b.getDiscount())).append("\n");
            body.append("TOTAL: ").append(formatMoney(b.getTotalAmount())).append("\n");
            if (b.getNotes() != null && !b.getNotes().isBlank()) {
                body.append("\nNotes: ").append(b.getNotes()).append("\n");
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(b.getBillNumber());
            alert.setHeaderText("Bill Details");
            Label content = new Label(body.toString());
            content.setWrapText(true);
            content.setMaxWidth(700);
            alert.getDialogPane().setContent(content);
            alert.getDialogPane().setMinWidth(750);
            UiStyles.dialog(alert, false);
            alert.showAndWait();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void populateForm(Bill b) {
        this.editingBill = b;
        formTitle.setText("Edit Bill " + b.getBillNumber());
        datePicker.setValue(b.getBillDate());
        discountField.setText(b.getDiscount() == null ? "0.00" : b.getDiscount().toPlainString());
        notesArea.setText(b.getNotes() == null ? "" : b.getNotes());
        populatePatientOptions();
        patientCombo.getItems().stream()
                .filter(p -> p.getId() == b.getPatientId())
                .findFirst().ifPresent(p -> { patientCombo.setValue(p); refreshAppointmentOptions(); });
        if (b.getAppointmentId() != null) {
            appointmentCombo.getItems().stream()
                    .filter(a -> a.getId() == b.getAppointmentId())
                    .findFirst().ifPresent(a -> appointmentCombo.setValue(a));
        } else {
            appointmentCombo.setValue(null);
        }
        itemsData.clear();
        try {
            List<BillItem> its = billingService.getItems(b.getId());
            for (BillItem it : its) {
                itemsData.add(new ItemRow(it.getDescription(),
                        String.valueOf(it.getQuantity()),
                        it.getUnitPrice() == null ? "0.00" : it.getUnitPrice().toPlainString()));
            }
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }

        boolean cancelled = Bill.STATUS_CANCELLED.equals(b.getStatus());
        boolean editable = canEdit() && !cancelled;
        saveButton.setText("Save Changes");
        saveButton.setDisable(!editable);
        saveButton.setVisible(editable);
        saveButton.setManaged(editable);
        patientCombo.setDisable(true);
        appointmentCombo.setDisable(!editable);
        datePicker.setDisable(!editable);
        discountField.setDisable(!editable);
        notesArea.setDisable(!editable);
        itemsTable.setDisable(!editable);
        recalculateTotals();
        updateActionButtons();
        clearMessage();
    }

    private boolean canEdit() {
        Role r = Session.getInstance().getRole();
        return r == Role.ADMIN || r == Role.RECEPTIONIST;
    }

    private void resetForm() {
        this.editingBill = null;
        boolean canCreate = canEdit();
        formTitle.setText(canCreate ? "New Bill" : "Bills");
        datePicker.setValue(LocalDate.now());
        discountField.setText("0.00");
        notesArea.clear();
        itemsData.clear();
        itemsData.add(new ItemRow("", "1", "0.00"));
        populatePatientOptions();
        refreshAppointmentOptions();
        patientCombo.setValue(null);
        appointmentCombo.setValue(null);
        patientCombo.setDisable(!canCreate);
        appointmentCombo.setDisable(!canCreate);
        datePicker.setDisable(!canCreate);
        discountField.setDisable(!canCreate);
        notesArea.setDisable(!canCreate);
        itemsTable.setDisable(!canCreate);
        saveButton.setText("Save Bill");
        saveButton.setDisable(!canCreate);
        saveButton.setVisible(canCreate);
        saveButton.setManaged(canCreate);
        table.getSelectionModel().clearSelection();
        recalculateTotals();
        updateActionButtons();
        clearMessage();
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

    // ---------- editable item row for JavaFX ----------

    public static class ItemRow {
        private final SimpleStringProperty description;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty unitPrice;

        public ItemRow(String d, String q, String p) {
            this.description = new SimpleStringProperty(d == null ? "" : d);
            this.quantity = new SimpleStringProperty(q == null ? "1" : q);
            this.unitPrice = new SimpleStringProperty(p == null ? "0.00" : p);
        }

        public SimpleStringProperty descriptionProperty() { return description; }
        public String getDescription() { return description.get(); }
        public void setDescription(String s) { description.set(s == null ? "" : s); }

        public SimpleStringProperty quantityProperty() { return quantity; }
        public String getQuantity() { return quantity.get(); }
        public void setQuantity(String s) { quantity.set(s == null ? "1" : s); }

        public SimpleStringProperty unitPriceProperty() { return unitPrice; }
        public String getUnitPrice() { return unitPrice.get(); }
        public void setUnitPrice(String s) { unitPrice.set(s == null ? "0.00" : s); }

        public BigDecimal computeAmount() {
            int qty;
            try { qty = Integer.parseInt(quantity.get().trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
            BigDecimal price;
            try { price = new BigDecimal(unitPrice.get().trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
            if (qty < 0 || price.signum() < 0) return BigDecimal.ZERO;
            return price.multiply(BigDecimal.valueOf(qty)).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}
