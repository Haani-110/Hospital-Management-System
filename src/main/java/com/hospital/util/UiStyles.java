package com.hospital.util;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Shared presentation only: stylesheet, sizing, labels and cell decoration.
 * No session state, navigation decisions or business rules belong here.
 */
public final class UiStyles {
    private static final URL CSS = UiStyles.class.getResource("/com/hospital/css/styles.css");

    private UiStyles() { }

    public static void apply(Scene scene) {
        if (CSS != null && !scene.getStylesheets().contains(CSS.toExternalForm())) {
            scene.getStylesheets().add(CSS.toExternalForm());
        }
    }

    public static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMinSize(0, 0);
        scroll.getStyleClass().add("content-scroll");
        return scroll;
    }

    public static ScrollPane sidebar(Node content) {
        ScrollPane scroll = scroll(content);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinWidth(224);
        scroll.setPrefWidth(224);
        scroll.setMaxWidth(224);
        scroll.getStyleClass().add("sidebar-scroll");
        return scroll;
    }

    /** Title and action stay visible; long user names wrap on a separate line. */
    public static VBox header(Label title, Label userInfo, Button action) {
        title.setWrapText(true);
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);
        action.setMinWidth(Region.USE_PREF_SIZE);
        HBox heading = new HBox(16, title, action);
        heading.setAlignment(Pos.CENTER_LEFT);
        userInfo.setWrapText(true);
        userInfo.setMaxWidth(Double.MAX_VALUE);
        VBox header = new VBox(6, heading, userInfo);
        header.getStyleClass().add("topbar");
        return header;
    }

    /** Keep the existing table/form nodes, including their listeners and selection. */
    public static SplitPane workspace(VBox tablePane, VBox formPane) {
        tablePane.setMinWidth(0);
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
        if (value == null || value.isBlank()) return;
        String style = switch (value.toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "COMPLETED", "PAID" -> "status-positive";
            case "SCHEDULED", "UNPAID", "PARTIALLY_PAID" -> "status-pending";
            case "CANCELLED" -> "status-negative";
            case "INACTIVE" -> "status-neutral";
            default -> null;
        };
        if (style != null) cell.getStyleClass().add(style);
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
    }
}
