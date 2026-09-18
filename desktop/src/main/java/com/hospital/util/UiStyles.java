package com.hospital.util;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shared presentation only: stylesheet, sizing, labels and cell decoration.
 * No session state, navigation decisions or business rules belong here.
 */
public final class UiStyles {
    private static final URL CSS = UiStyles.class.getResource("/com/hospital/css/styles.css");

    private static final Object STATUS_BADGE = new Object();
    private static final Object EMPTY_STATE = new Object();
    private static final Object RAIL_STATE = new Object();

    private static final class SidebarState {
        private final Map<String, StackPane> slots = new LinkedHashMap<>();
    }

    private UiStyles() { }

    public static void apply(Scene scene) {
        if (CSS != null && !scene.getStylesheets().contains(CSS.toExternalForm())) {
            scene.getStylesheets().add(CSS.toExternalForm());
        }
        // Move the existing presentation nodes, not their callbacks or auth state.
        Node profile = scene.getRoot().lookup("#staff-profile");
        Node user = scene.getRoot().lookup(".user-info");
        if (profile instanceof VBox footer && user != null) {
            moveTo(user, footer);
            Node logout = scene.getRoot().lookup("#portal-logout");
            if (logout instanceof Button button) {
                moveTo(button, footer);
                button.setMaxWidth(Double.MAX_VALUE);
                button.getStyleClass().add("sidebar-logout");
            }
        }
        UiMotion.visit(scene.getRoot(), UiStyles::adaptFlow);
        UiMotion.install(scene.getRoot());
    }

    private static void moveTo(Node node, Pane target) {
        if (node.getParent() == target) return;
        if (node.getParent() instanceof Pane previous) previous.getChildren().remove(node);
        target.getChildren().add(node);
    }

    public static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMinSize(0, 0);
        scroll.getStyleClass().add("content-scroll");
        return scroll;
    }

    public static Node sidebar(Node content) {
        if (!(content instanceof VBox navigation)) return scroll(content);
        List<Node> original = new ArrayList<>(navigation.getChildren());
        navigation.getChildren().clear();
        Label title = original.stream().filter(n -> n.getStyleClass().contains("sidebar-title"))
                .map(n -> (Label) n).findFirst().orElse(new Label("Hospital System"));
        Label subtitle = new Label("CARE & OPERATIONS");
        subtitle.getStyleClass().add("brand-caption");
        VBox brandText = new VBox(3, title, subtitle);
        HBox brand = new HBox(10, medicalMark(), brandText);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("sidebar-brand");

        List<Node> links = original.stream().filter(n -> n.getStyleClass().contains("nav-item")).toList();
        SidebarState state = new SidebarState();
        VBox clinical = new VBox(4);
        VBox operations = new VBox(4);
        VBox administration = new VBox(4);
        for (Node link : links) {
            String name = link instanceof Labeled labeled ? labeled.getText() : "";
            if (link instanceof Label active && link.getStyleClass().contains("nav-item-active")) {
                Rectangle indicator = new Rectangle(3, 18);
                indicator.setArcWidth(3); indicator.setArcHeight(3);
                indicator.getStyleClass().add("nav-indicator");
                active.setGraphic(indicator);
                active.setGraphicTextGap(10);
                if (link.isVisible()) UiMotion.indicator(indicator);
            }
            StackPane slot = new StackPane();
            slot.setMinWidth(0);
            slot.getStyleClass().add("nav-slot");
            setNavigationNode(slot, link);
            state.slots.put("Doctor Management".equals(name) ? "Doctors" : name, slot);
            switch (name) {
                case "Departments", "User Management" -> administration.getChildren().add(slot);
                case "Billing", "Reports" -> operations.getChildren().add(slot);
                case "Dashboard" -> navigation.getChildren().add(slot);
                default -> clinical.getChildren().add(slot);
            }
        }
        navigation.getChildren().addAll(navGroup("CLINICAL WORKSPACE", clinical),
                navGroup("OPERATIONS", operations), navGroup("ADMINISTRATION", administration));
        ScrollPane viewport = scroll(navigation);
        viewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        viewport.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(viewport, Priority.ALWAYS);
        Label session = new Label("STAFF SESSION");
        session.getStyleClass().add("brand-caption");
        VBox profile = new VBox(10, session);
        profile.setId("staff-profile");
        profile.getStyleClass().add("sidebar-profile");
        VBox rail = new VBox(brand, viewport, profile);
        rail.setMinWidth(236); rail.setPrefWidth(236); rail.setMaxWidth(236);
        rail.getStyleClass().add("sidebar-rail");
        rail.getProperties().put(RAIL_STATE, state);
        return rail;
    }

    private static void setNavigationNode(StackPane slot, Node node) {
        slot.visibleProperty().unbind();
        slot.managedProperty().unbind();
        slot.getChildren().setAll(node);
        slot.visibleProperty().bind(node.visibleProperty());
        slot.managedProperty().bind(node.managedProperty());
    }

    /** Keep the mounted rail and its slots. Only active/inactive nodes change.
     * Each replacement retains the incoming controller's original callback.
     * Visibility is copied from the existing role-aware view, never recomputed.
     */
    public static void updateSidebar(Node mounted, Node incoming) {
        SidebarState current = (SidebarState) mounted.getProperties().get(RAIL_STATE);
        SidebarState next = (SidebarState) incoming.getProperties().get(RAIL_STATE);
        for (var entry : current.slots.entrySet()) {
            StackPane slot = entry.getValue();
            StackPane incomingSlot = next.slots.get(entry.getKey());
            Node previous = slot.getChildren().get(0);
            Node candidate = incomingSlot.getChildren().get(0);
            boolean active = candidate.getStyleClass().contains("nav-item-active");
            if (active != previous.getStyleClass().contains("nav-item-active")) {
                UiMotion.cancelTree(previous);
                incomingSlot.visibleProperty().unbind();
                incomingSlot.managedProperty().unbind();
                incomingSlot.getChildren().clear();
                setNavigationNode(slot, candidate);
            } else {
                previous.setVisible(candidate.isVisible());
                previous.setManaged(candidate.isManaged());
                if (active && previous instanceof Labeled label && label.getGraphic() != null) {
                    UiMotion.indicator(label.getGraphic());
                }
            }
        }
        Node oldUser = mounted.lookup(".user-info");
        Node newUser = incoming.lookup(".user-info");
        if (oldUser instanceof Label oldLabel && newUser instanceof Label newLabel) {
            oldLabel.setText(newLabel.getText());
        }
        UiMotion.cancelTree(incoming);
    }

    /** Group headings follow existing node visibility; no role decisions here. */
    private static VBox navGroup(String text, VBox links) {
        Label heading = new Label(text);
        heading.getStyleClass().add("nav-group-title");
        VBox group = new VBox(7, heading, links);
        group.getStyleClass().add("nav-group");
        Observable[] dependencies = links.getChildren().stream().map(Node::visibleProperty).toArray(Observable[]::new);
        group.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> links.getChildren().stream().anyMatch(Node::isVisible), dependencies));
        group.managedProperty().bind(group.visibleProperty());
        return group;
    }

    public static StackPane medicalMark() {
        Rectangle vertical = new Rectangle(6, 20);
        Rectangle horizontal = new Rectangle(20, 6);
        vertical.setArcWidth(2); vertical.setArcHeight(2);
        horizontal.setArcWidth(2); horizontal.setArcHeight(2);
        vertical.getStyleClass().add("medical-cross");
        horizontal.getStyleClass().add("medical-cross");
        StackPane mark = new StackPane(vertical, horizontal);
        mark.setMinSize(34, 34); mark.setPrefSize(34, 34); mark.setMaxSize(34, 34);
        mark.setMouseTransparent(true);
        mark.getStyleClass().add("medical-mark");
        return mark;
    }

    /** Title and action stay visible; long user names wrap on a separate line. */
    public static VBox header(Label title, Label userInfo, Button action) {
        title.setWrapText(true);
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        VBox identity = new VBox(5, title, hint(description(title.getText())));
        identity.setMinWidth(0);
        HBox.setHgrow(identity, Priority.ALWAYS);
        action.setMinWidth(Region.USE_PREF_SIZE);
        if ("Logout".equals(action.getText())) action.setId("portal-logout");
        HBox heading = new HBox(16, identity, action);
        heading.setAlignment(Pos.CENTER_LEFT);
        userInfo.setWrapText(true);
        userInfo.setMinWidth(0);
        userInfo.setMaxWidth(Double.MAX_VALUE);
        // apply() relocates this same label into the rail's staff-session area.
        VBox header = new VBox(6, heading, userInfo);
        header.getStyleClass().add("topbar");
        return header;
    }

    private static String description(String title) {
        return switch (title) {
            case "Department Management" -> "Organize hospital departments and specialties.";
            case "User Management" -> "Manage staff accounts and access assignments.";
            case "Doctor Management" -> "Clinical team directory and consultation details.";
            case "Patient Management" -> "Patient registration, contact information and care history.";
            case "Appointment Management" -> "Patient visits and clinical schedules.";
            case "Medical Records" -> "Review clinical findings, diagnoses and treatment notes.";
            case "Prescription Management", "Prescriptions" -> "Review medication plans and prescription details.";
            case "Billing Management" -> "Manage patient charges, bill items and payment status.";
            case "Reports" -> "Filter hospital activity and review detailed results.";
            default -> "Hospital activity and care coordination at a glance.";
        };
    }

    /** Keep the existing table/form nodes, including their listeners and selection. */
    public static SplitPane workspace(VBox tablePane, VBox formPane) {
        tablePane.setMinWidth(0);
        tablePane.getStyleClass().add("record-list");
        for (Node node : new ArrayList<>(tablePane.getChildren())) {
            if (node instanceof TableView<?> table) {
                int index = tablePane.getChildren().indexOf(table);
                tablePane.getChildren().remove(index);
                ScrollPane viewport = tableViewport(table);
                viewport.setMinHeight(220);
                VBox.setVgrow(viewport, Priority.ALWAYS);
                tablePane.getChildren().add(index, viewport);
            }
        }
        formPane.setMinWidth(0);
        formPane.setMinHeight(Region.USE_PREF_SIZE);
        formPane.setMaxWidth(Double.MAX_VALUE);
        formPane.getStyleClass().add("form-panel");
        ScrollPane list = scroll(tablePane);
        list.setFitToHeight(true);
        ScrollPane editor = scroll(formPane);
        // Keep the split divider from squeezing either pane to an unusable sliver.
        list.setMinWidth(340);
        editor.setMinWidth(300);
        SplitPane split = new SplitPane(list, editor);
        split.getStyleClass().add("workspace");
        split.setMinSize(0, 0);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.52);
        // One breakpoint, not a second layout system. Small desktops stack the
        // panes; each pane remains independently scrollable and resizable.
        split.widthProperty().addListener((obs, oldWidth, width) -> {
            Orientation orientation = width.doubleValue() >= 1000
                    ? Orientation.HORIZONTAL : Orientation.VERTICAL;
            if (split.getOrientation() != orientation) {
                split.setOrientation(orientation);
                split.setDividerPositions(orientation == Orientation.HORIZONTAL ? 0.60 : 0.52);
            }
        });
        return split;
    }

    /** Horizontal scrolling belongs to the table, not its filters or actions. */
    public static ScrollPane tableViewport(TableView<?> table) {
        readableTable(table);
        emptyState(table);
        ScrollPane viewport = scroll(table);
        viewport.setFitToHeight(true);
        viewport.setPrefViewportHeight(table.getPrefHeight() > 0 ? table.getPrefHeight() : 360);
        viewport.setMinHeight(180);
        return viewport;
    }

    /** Keep constrained column resizing, but never compress clinical text to a few pixels. */
    public static void readableTable(TableView<?> table) {
        double width = 2;
        for (TableColumn<?, ?> column : table.getColumns()) {
            double minimum = switch (column.getText()) {
                case "ID", "Appt", "Apt", "Record", "Qty", "Items", "Blood" -> 60;
                case "Name", "Patient", "Doctor", "Username", "Department", "Medicine" -> 150;
                case "Diagnosis", "Treatment", "Description", "Instructions", "Email" -> 180;
                case "Status" -> 150;
                default -> 110;
            };
            minimum = Math.min(minimum, column.getMaxWidth());
            column.setMinWidth(minimum);
            column.setPrefWidth(Math.max(minimum, column.getPrefWidth()));
            width += minimum;
        }
        table.setMinWidth(Math.max(360, width));
    }

    /** Stack existing labels above controls, without replacing any input nodes. */
    public static void form(GridPane form) {
        boolean sideLabels = form.getChildren().stream()
                .anyMatch(n -> GridPane.getColumnIndex(n) != null && GridPane.getColumnIndex(n) > 0);
        var children = new ArrayList<>(form.getChildren());
        if (sideLabels) {
            for (Node node : children) {
                int row = GridPane.getRowIndex(node) == null ? 0 : GridPane.getRowIndex(node);
                int col = GridPane.getColumnIndex(node) == null ? 0 : GridPane.getColumnIndex(node);
                GridPane.setRowIndex(node, row * 2 + col);
                GridPane.setColumnIndex(node, 0);
            }
        }
        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        column.setFillWidth(true);
        form.getColumnConstraints().setAll(column);
        form.getStyleClass().add("form-grid");
        for (Node node : children) {
            if (node instanceof Region region) {
                region.setMinWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
            }
            if (node instanceof TextArea area) area.setWrapText(true);
            if (node instanceof Label label) {
                label.setWrapText(true);
                int row = GridPane.getRowIndex(node) == null ? 0 : GridPane.getRowIndex(node);
                children.stream().filter(n -> n instanceof Control && !(n instanceof Label))
                        .filter(n -> Integer.valueOf(row + 1).equals(GridPane.getRowIndex(n)))
                        .findFirst().ifPresent(label::setLabelFor);
            }
        }
        formSections(form);
    }

    public static VBox field(String text, Control control) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        label.setLabelFor(control);
        control.setMinWidth(0);
        control.setMaxWidth(Double.MAX_VALUE);
        VBox field = new VBox(6, label, control);
        field.setPrefWidth(180);
        field.getStyleClass().add("filter-field");
        return field;
    }

    public static Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("hint");
        return label;
    }

    /** Preserve the status text; color is supplementary, never the only cue. */
    public static void statusCell(TableCell<?, ?> cell, String value) {
        cell.getStyleClass().removeAll("status-positive", "status-pending", "status-negative", "status-neutral");
        if (cell.getGraphic() == cell.getProperties().get(STATUS_BADGE)) cell.setGraphic(null);
        cell.setAccessibleText(null);
        if (value == null || value.isBlank()) return;
        String style = switch (value.toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "COMPLETED", "PAID" -> "status-positive";
            case "SCHEDULED", "UNPAID", "PARTIALLY_PAID" -> "status-pending";
            case "CANCELLED" -> "status-negative";
            case "INACTIVE" -> "status-neutral";
            default -> null;
        };
        if (style != null) {
            Label badge = (Label) cell.getProperties().computeIfAbsent(STATUS_BADGE, key -> new Label());
            badge.setText(value);
            badge.getStyleClass().setAll("label", "status-chip", style);
            badge.setMouseTransparent(true);
            cell.setText(null);
            cell.setGraphic(badge);
            cell.setAccessibleText(value);
        }
    }

    /** Dialogs have their own scene, so they need the stylesheet explicitly. */
    public static void dialog(Alert alert, boolean destructive) {
        DialogPane pane = alert.getDialogPane();
        if (CSS != null && !pane.getStylesheets().contains(CSS.toExternalForm())) {
            pane.getStylesheets().add(CSS.toExternalForm());
        }
        pane.setMinWidth(420);
        pane.setPrefWidth(alert.getAlertType() == Alert.AlertType.INFORMATION ? 640 : 500);
        alert.setResizable(true);
        String kind = destructive ? "CONFIRM ACTION" : switch (alert.getAlertType()) {
            case ERROR -> "ATTENTION REQUIRED";
            case WARNING -> "PLEASE REVIEW";
            case CONFIRMATION -> "CONFIRM DETAILS";
            default -> "HOSPITAL WORKSPACE";
        };
        Label eyebrow = new Label(kind);
        eyebrow.getStyleClass().add("dialog-eyebrow");
        Label title = new Label(alert.getHeaderText() == null ? alert.getTitle() : alert.getHeaderText());
        title.setWrapText(true);
        title.getStyleClass().add("dialog-title");
        VBox titleBlock = new VBox(5, eyebrow, title);
        titleBlock.setMinWidth(0);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        HBox header = new HBox(14, medicalMark(), titleBlock);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-header");
        if (destructive) header.getStyleClass().add("dialog-header-danger");
        pane.setGraphic(null);
        pane.setHeader(header);
        for (ButtonType type : pane.getButtonTypes()) {
            Node button = pane.lookupButton(type);
            boolean affirmative = type.getButtonData() == ButtonBar.ButtonData.OK_DONE
                    || type.getButtonData() == ButtonBar.ButtonData.YES;
            button.getStyleClass().add(affirmative
                    ? (destructive ? "danger-button" : "primary-button") : "secondary-button");
        }
        // Long medical/prescription/billing details must remain readable on a
        // normal laptop display. Keep all text and existing dialog results.
        if (alert.getAlertType() == Alert.AlertType.INFORMATION) {
            Node details = pane.getContent();
            if (details == null && pane.getContentText() != null) {
                Label text = new Label(pane.getContentText());
                text.setWrapText(true);
                details = text;
                pane.setContentText(null);
            }
            if (details != null) {
                if (details instanceof Label label) {
                    label.setMaxWidth(Double.MAX_VALUE);
                    label.setWrapText(true);
                }
                pane.setContent(null);
                ScrollPane body = scroll(details);
                body.setPrefViewportHeight(320);
                body.setMaxHeight(420);
                body.getStyleClass().add("dialog-details");
                pane.setContent(body);
            }
        }
        UiMotion.install(pane);
        UiMotion.dialog(alert);
    }

    private static void formSections(GridPane form) {
        TreeMap<Integer, String> sections = new TreeMap<>();
        List<Node> fields = new ArrayList<>(form.getChildren());
        for (Node node : fields) {
            if (!(node instanceof Label label) || label.getLabelFor() == null) continue;
            String heading = switch (label.getText()) {
                case "Phone" -> "Contact information";
                case "Emergency Contact Name" -> "Emergency contact";
                case "Diagnosis *" -> "Clinical assessment";
                case "Treatment Notes" -> "Care plan";
                case "Date *" -> "Visit schedule";
                case "Password *" -> "Account credentials";
                case "Role *" -> "Access assignment";
                case "Consultation Fee *" -> "Consultation";
                case "Prescription Date *" -> "Prescription details";
                case "Bill Date *" -> "Billing details";
                default -> null;
            };
            if (heading != null) sections.put(GridPane.getRowIndex(node), heading);
        }
        for (Node node : fields) {
            int row = GridPane.getRowIndex(node) == null ? 0 : GridPane.getRowIndex(node);
            GridPane.setRowIndex(node, row + sections.headMap(row, true).size());
        }
        int offset = 0;
        for (var section : sections.entrySet()) {
            Label label = new Label(section.getValue());
            label.getStyleClass().add("form-section");
            label.setMaxWidth(Double.MAX_VALUE);
            form.add(label, 0, section.getKey() + offset++);
        }
    }

    /** Preferred toolbar widths are upper bounds, not a reason to overflow a pane. */
    private static void adaptFlow(Node node) {
        if (!(node instanceof FlowPane flow)) return;
        flow.setMinWidth(0);
        for (Node child : flow.getChildren()) {
            if (!(child instanceof Region region)) continue;
            if (child instanceof Button button) {
                button.setWrapText(true);
                button.setMinWidth(0);
                button.maxWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> flow.getWidth() <= 0 ? Double.MAX_VALUE : Math.max(1,
                                flow.getWidth() - flow.getInsets().getLeft() - flow.getInsets().getRight()),
                        flow.widthProperty(), flow.insetsProperty()));
            } else if (region.getPrefWidth() > 0 && !region.prefWidthProperty().isBound()) {
                double preferred = region.getPrefWidth();
                region.setMinWidth(0);
                region.prefWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> flow.getWidth() <= 0 ? preferred : Math.min(preferred,
                                Math.max(1, flow.getWidth() - flow.getInsets().getLeft() - flow.getInsets().getRight())),
                        flow.widthProperty(), flow.insetsProperty()));
            }
        }
    }

    /** Same presentation nodes at every width; only row/column placement changes. */
    public static GridPane responsiveGrid(int columns, double minimumWidth, Node... cards) {
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setMinWidth(0);
        grid.getChildren().addAll(cards);
        Runnable layout = () -> {
            int count = Math.max(1, Math.min(columns, (int) ((grid.getWidth() + 14) / (minimumWidth + 14))));
            if (grid.getColumnConstraints().size() == count) return;
            grid.getColumnConstraints().clear();
            for (int i = 0; i < count; i++) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(100.0 / count);
                grid.getColumnConstraints().add(column);
            }
            for (int i = 0; i < cards.length; i++) {
                GridPane.setColumnIndex(cards[i], i % count);
                GridPane.setRowIndex(cards[i], i / count);
                GridPane.setHgrow(cards[i], Priority.ALWAYS);
                if (cards[i] instanceof Region region) {
                    region.setMinWidth(0);
                    region.setMaxWidth(Double.MAX_VALUE);
                }
            }
        };
        grid.widthProperty().addListener((obs, old, width) -> layout.run());
        layout.run();
        return grid;
    }

    private static void emptyState(TableView<?> table) {
        if (table.getProperties().putIfAbsent(EMPTY_STATE, true) != null) return;
        String title = table.getPlaceholder() instanceof Label label ? label.getText() : "No items to display";
        String detail = "Try another search or clear the current filters.";
        if (table.isEditable()) detail = "Line items will appear here when available.";
        else if (title.contains("departments") || title.contains("users")) detail = "Registered entries will appear in this list.";
        else if (title.startsWith("Run a report")) {
            title = "No results to display";
            detail = "Adjust the filters or choose another report type.";
        }
        Label heading = new Label(title);
        heading.getStyleClass().add("empty-title");
        heading.setWrapText(true);
        Label explanation = hint(detail);
        VBox empty = new VBox(8, medicalMark(), heading, explanation);
        // Anchor within the initially visible columns, even when a wide table
        // is inside a horizontal viewport. A centered placeholder could be offscreen.
        empty.setAlignment(Pos.CENTER_LEFT);
        empty.setMaxWidth(Double.MAX_VALUE);
        heading.setMaxWidth(300);
        explanation.setMaxWidth(300);
        empty.getStyleClass().add("empty-state");
        table.setPlaceholder(empty);
    }

}
