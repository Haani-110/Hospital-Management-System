package com.hospital.controller;

import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.service.Session;
import com.hospital.service.UserService;
import com.hospital.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * User Management screen (Phase 2.2). Admin-only.
 *
 * Shows all users in a TableView (no password hashes exposed) and provides
 * forms for creating users, editing username/role, changing a password, and
 * activating/deactivating accounts with confirmation dialogs.
 */
public class UserManagementController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserService userService;
    private final SceneManager sceneManager;

    private Runnable onBackToDashboard;
    private Runnable onOpenDepartments;
    private Runnable onOpenDoctors;
    private Runnable onOpenPatients;
    private Runnable onOpenAppointments;
    private Runnable onOpenMedicalRecords;
    private Runnable onOpenPrescriptions;
    private Runnable onOpenBilling;
    private Runnable onOpenReports;

    private TableView<User> table;
    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmField;
    private ComboBox<Role> roleCombo;
    private Label formTitle;
    private Label messageLabel;
    private Button saveButton;
    private Button clearButton;
    private Button changePwdButton;
    private Button toggleActiveButton;

    private User editingUser;

    public UserManagementController(UserService userService, SceneManager sceneManager) {
        this.userService = userService;
        this.sceneManager = sceneManager;
    }

    public void setOnBackToDashboard(Runnable onBackToDashboard) {
        this.onBackToDashboard = onBackToDashboard;
    }

    public void setOnOpenDepartments(Runnable onOpenDepartments) {
        this.onOpenDepartments = onOpenDepartments;
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

        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 1100, 700);
        applyCss(scene);
        refreshTable();
        resetForm();
        return scene;
    }

    // ---------- Top bar ----------

    private Node buildTopBar() {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("topbar");

        Label pageTitle = new Label("User Management");
        pageTitle.getStyleClass().add("page-title");

        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String roleText = Session.getInstance().getRole() != null
                ? Session.getInstance().getRole().name() : "UNKNOWN";
        String userText = Session.getInstance().getUsername() != null
                ? Session.getInstance().getUsername() : "unknown";
        Label userInfo = new Label("Logged in as: " + userText + " (" + roleText + ")");
        userInfo.getStyleClass().add("user-info");

        topBar.getChildren().addAll(backBtn, pageTitle, spacer, userInfo);
        return topBar;
    }

    // ---------- Sidebar ----------

    private Node buildSidebar() {
        Role role = Session.getInstance().getRole();
        boolean isAdmin = role == Role.ADMIN;
        boolean isAdminOrReceptionist = isAdmin || role == Role.RECEPTIONIST;

        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setMinWidth(220);
        sidebar.getStyleClass().add("sidebar");

        Label appTitle = new Label("Hospital System");
        appTitle.getStyleClass().add("sidebar-title");

        Button dashboardBtn = new Button("Dashboard");
        dashboardBtn.getStyleClass().addAll("nav-item", "nav-button");
        dashboardBtn.setMaxWidth(Double.MAX_VALUE);
        dashboardBtn.setOnAction(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        // Departments: enabled button (admin-only by the time this screen is shown).
        Button departmentsBtn = new Button("Departments");
        departmentsBtn.getStyleClass().addAll("nav-item", "nav-button");
        departmentsBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(departmentsBtn, new Insets(2, 0, 2, 0));
        departmentsBtn.setOnAction(e -> {
            if (onOpenDepartments != null) onOpenDepartments.run();
        });

        Label usersItem = new Label("User Management");
        usersItem.setMaxWidth(Double.MAX_VALUE);
        usersItem.getStyleClass().addAll("nav-item", "nav-item-active");
        VBox.setMargin(usersItem, new Insets(2, 0, 2, 0));

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
        departmentsBtn.setVisible(isAdmin);
        departmentsBtn.setManaged(isAdmin);
        usersItem.setVisible(isAdmin);
        usersItem.setManaged(isAdmin);
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
                departmentsBtn,
                usersItem,
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

    // ---------- Content ----------

    private Node buildContent() {
        HBox content = new HBox(20);
        content.setPadding(new Insets(20));

        VBox tablePane = new VBox(10);
        tablePane.setPadding(new Insets(10));
        tablePane.getStyleClass().add("card");
        HBox.setHgrow(tablePane, Priority.ALWAYS);

        Label tableTitle = new Label("Users");
        tableTitle.getStyleClass().add("section-title");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("No users found."));

        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        idCol.setMaxWidth(70);

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, Role> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<User, Boolean> activeCol = new TableColumn<>("Status");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                } else {
                    setText(active ? "Active" : "Inactive");
                }
            }
        });

        TableColumn<User, java.time.LocalDateTime> createdCol = new TableColumn<>("Created At");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(java.time.LocalDateTime t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) setText(null);
                else setText(t.format(DATE_FMT));
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<User, ?>[] cols = new TableColumn[] { idCol, userCol, roleCol, activeCol, createdCol };
        table.getColumns().addAll(cols);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateFormForEdit(newVal);
        });

        tablePane.getChildren().addAll(tableTitle, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox formPane = new VBox(10);
        formPane.setPadding(new Insets(10));
        formPane.setMinWidth(360);
        formPane.setMaxWidth(400);
        formPane.getStyleClass().add("card");

        formTitle = new Label("Create User");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        Label userLabel = new Label("Username *");
        usernameField = new TextField();
        usernameField.setPromptText("e.g. jsmith");

        Label passLabel = new Label("Password *");
        passwordField = new PasswordField();
        passwordField.setPromptText("min 8 characters");

        Label confirmLabel = new Label("Confirm Password *");
        confirmField = new PasswordField();
        confirmField.setPromptText("re-enter password");

        Label roleLabel = new Label("Role *");
        roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(Role.values());
        roleCombo.setValue(Role.DOCTOR);
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        form.add(userLabel, 0, 0);
        form.add(usernameField, 0, 1);
        form.add(passLabel, 0, 2);
        form.add(passwordField, 0, 3);
        form.add(confirmLabel, 0, 4);
        form.add(confirmField, 0, 5);
        form.add(roleLabel, 0, 6);
        form.add(roleCombo, 0, 7);

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().add("error");

        saveButton = new Button("Create User");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);

        changePwdButton = new Button("Change Password");
        changePwdButton.getStyleClass().add("secondary-button");
        changePwdButton.setDisable(true);

        toggleActiveButton = new Button("Deactivate");
        toggleActiveButton.getStyleClass().add("danger-button");
        toggleActiveButton.setDisable(true);

        clearButton = new Button("Clear / New");
        clearButton.getStyleClass().add("secondary-button");

        // Use FlowPane so the 4 buttons wrap within the narrow form pane
        // (≈380px) instead of overflowing horizontally.
        FlowPane buttonRow = new FlowPane(10, 10, saveButton, changePwdButton, toggleActiveButton, clearButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setPrefWrapLength(380);

        saveButton.setOnAction(e -> onSave());
        changePwdButton.setOnAction(e -> onChangePassword());
        toggleActiveButton.setOnAction(e -> onToggleActive());
        clearButton.setOnAction(e -> resetForm());

        formPane.getChildren().addAll(formTitle, form, messageLabel, buttonRow);

        content.getChildren().addAll(tablePane, formPane);
        return content;
    }

    // ---------- Actions ----------

    private void onSave() {
        clearMessage();
        try {
            String username = usernameField.getText();
            Role role = roleCombo.getValue();
            if (editingUser == null) {
                String pw = passwordField.getText();
                String cf = confirmField.getText();
                User created = userService.createUser(username, pw, cf, role);
                showSuccess("User \"" + created.getUsername() + "\" created.");
            } else {
                // When editing, password fields are ignored (use Change Password button).
                // Require both password fields to be empty to avoid confusion; but if the
                // user typed something there, clear it and save only username/role.
                if (!passwordField.getText().isEmpty() || !confirmField.getText().isEmpty()) {
                    showError("Use the 'Change Password' button to change a password. Password fields are ignored when editing.");
                    return;
                }
                User updated = userService.updateUser(editingUser.getId(), username, role);
                showSuccess("User \"" + updated.getUsername() + "\" updated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onChangePassword() {
        clearMessage();
        if (editingUser == null) {
            showError("Select a user first.");
            return;
        }

        DialogResult result = showPasswordDialog();
        if (result == null) return; // cancelled

        try {
            userService.changePassword(editingUser.getId(), result.newPassword, result.confirm);
            showSuccess("Password updated for \"" + editingUser.getUsername() + "\".");
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void onToggleActive() {
        clearMessage();
        if (editingUser == null) {
            showError("Select a user first.");
            return;
        }
        boolean willBeActive = !editingUser.isActive();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(willBeActive ? "Activate user" : "Deactivate user");
        confirm.setHeaderText(willBeActive ? "Activate this user?" : "Deactivate this user?");
        confirm.setContentText(willBeActive
                ? "Are you sure you want to activate the user \"" + editingUser.getUsername() + "\"?"
                : "Are you sure you want to deactivate the user \"" + editingUser.getUsername() + "\"?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            if (willBeActive) {
                userService.activateUser(editingUser.getId());
                showSuccess("User \"" + editingUser.getUsername() + "\" activated.");
            } else {
                userService.deactivateUser(editingUser.getId());
                showSuccess("User \"" + editingUser.getUsername() + "\" deactivated.");
            }
            refreshTable();
            resetForm();
        } catch (ValidationException | AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    // ---------- Helpers ----------

    private void refreshTable() {
        try {
            List<User> all = userService.getAllUsers();
            table.getItems().setAll(all);
        } catch (AuthorizationException | DatabaseException ex) {
            showError(ex.getMessage());
            table.getItems().clear();
        }
    }

    private void populateFormForEdit(User u) {
        this.editingUser = u;
        formTitle.setText("Edit User");
        usernameField.setText(u.getUsername());
        passwordField.clear();
        confirmField.clear();
        passwordField.setPromptText("(unchanged)");
        confirmField.setPromptText("(unchanged)");
        roleCombo.setValue(u.getRole());
        saveButton.setText("Save");

        changePwdButton.setDisable(false);

        User current = Session.getInstance().getCurrentUser();
        boolean isSelf = current != null && current.getId() == u.getId();
        roleCombo.setDisable(isSelf); // prevent changing own role

        if (u.isActive()) {
            toggleActiveButton.setText("Deactivate");
            toggleActiveButton.getStyleClass().remove("primary-button");
            toggleActiveButton.getStyleClass().add("danger-button");
            toggleActiveButton.setDisable(isSelf); // cannot deactivate self
        } else {
            toggleActiveButton.setText("Activate");
            toggleActiveButton.getStyleClass().remove("danger-button");
            toggleActiveButton.getStyleClass().add("primary-button");
            toggleActiveButton.setDisable(false);
        }
        clearMessage();
    }

    private void resetForm() {
        this.editingUser = null;
        formTitle.setText("Create User");
        usernameField.clear();
        passwordField.clear();
        confirmField.clear();
        passwordField.setPromptText("min 8 characters");
        confirmField.setPromptText("re-enter password");
        roleCombo.setValue(Role.DOCTOR);
        roleCombo.setDisable(false);
        saveButton.setText("Create User");
        table.getSelectionModel().clearSelection();
        changePwdButton.setDisable(true);
        toggleActiveButton.setDisable(true);
        toggleActiveButton.setText("Deactivate");
        toggleActiveButton.getStyleClass().remove("primary-button");
        toggleActiveButton.getStyleClass().add("danger-button");
        clearMessage();
    }

    private DialogResult showPasswordDialog() {
        // Build a small dialog using a JavaFX Alert with custom content.
        Alert dlg = new Alert(Alert.AlertType.NONE);
        dlg.setTitle("Change Password");
        dlg.setHeaderText("Set a new password for \"" + editingUser.getUsername() + "\"");

        PasswordField np = new PasswordField();
        np.setPromptText("new password (min 8)");
        PasswordField cp = new PasswordField();
        cp.setPromptText("confirm new password");

        VBox box = new VBox(8, new Label("New Password:"), np, new Label("Confirm Password:"), cp);
        box.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(box);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        // Enable OK only when both fields have content.
        np.textProperty().addListener((o, a, b) -> toggleOk(dlg, np, cp, ButtonType.OK));
        cp.textProperty().addListener((o, a, b) -> toggleOk(dlg, np, cp, ButtonType.OK));

        Optional<ButtonType> pressed = dlg.showAndWait();
        if (pressed.isEmpty() || pressed.get() != ButtonType.OK) return null;

        String newPwd = np.getText();
        String confirm = cp.getText();
        // Basic validation; service validates fully.
        if (newPwd == null || newPwd.length() < 8) {
            showError("Password must be at least 8 characters.");
            return null;
        }
        if (!newPwd.equals(confirm)) {
            showError("Passwords do not match.");
            return null;
        }
        return new DialogResult(newPwd, confirm);
    }

    private void toggleOk(Alert dlg, PasswordField np, PasswordField cp, ButtonType okType) {
        boolean ok = !np.getText().isEmpty() && !cp.getText().isEmpty();
        dlg.getDialogPane().lookupButton(okType).setDisable(!ok);
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
        try {
            var cssUrl = getClass().getResource("/com/hospital/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }

    private record DialogResult(String newPassword, String confirm) { }
}
