package com.hospital.util;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Finite presentation feedback. Entrance requests are prepared before mounting,
 * then played after CSS/layout in the actual application Scene, not a temporary
 * controller Scene or a window-show event. No action handlers or worker threads.
 * -Dhospital.ui.reduceMotion=true applies final states without animation.
 */
public final class UiMotion {
    private static final boolean REDUCED = Boolean.getBoolean("hospital.ui.reduceMotion");
    private static final Duration FEEDBACK = Duration.millis(150);
    private static final Object ANIMATIONS = new Object();
    private static final Object INSTALLED = new Object();
    private static final Object ELEVATION = new Object();
    private static final Object ENTRY = new Object();
    private static final Object SCHEDULED = new Object();

    private enum Entrance { CONTENT, MODULE, DIALOG, INDICATOR }
    private record Entry(Entrance kind, int delay) { }
    private record Scheduled(Scene scene, Runnable pulse) {
        void cancel() { scene.removePostLayoutPulseListener(pulse); }
    }

    private UiMotion() { }

    /** Visit application nodes and graphics, never virtual table cells/skin internals. */
    static void visit(Node node, Consumer<Node> visitor) {
        if (node == null) return;
        visitor.accept(node);
        if (node instanceof ScrollPane scroll) visit(scroll.getContent(), visitor);
        else if (node instanceof SplitPane split) split.getItems().forEach(n -> visit(n, visitor));
        else if (node instanceof DialogPane pane) {
            visit(pane.getHeader(), visitor);
            visit(pane.getContent(), visitor);
            pane.getButtonTypes().forEach(type -> visit(pane.lookupButton(type), visitor));
        } else if (node instanceof Labeled labeled) visit(labeled.getGraphic(), visitor);
        else if (!(node instanceof Control) && node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(n -> visit(n, visitor));
        }
    }

    public static void install(Node root) {
        visit(root, node -> {
            if (node.getProperties().putIfAbsent(INSTALLED, true) != null) return;
            if (node instanceof Button button) button(button);
            if (node instanceof TextInputControl || node instanceof ComboBoxBase<?>) field((Control) node);
            if (node instanceof Label label) message(label);
        });
    }

    private static void button(Button button) {
        button.getStyleClass().add("motion-button");
        ColorAdjust tone = new ColorAdjust();
        DropShadow shadow = new DropShadow(0, Color.TRANSPARENT);
        shadow.setInput(tone);
        button.setEffect(shadow);
        boolean navigation = button.getStyleClass().contains("nav-button");
        Rectangle accent = new Rectangle(3, 18, Color.web("#91d8d5"));
        accent.setArcWidth(3); accent.setArcHeight(3);
        accent.setOpacity(0);
        accent.setMouseTransparent(true);
        if (navigation && button.getGraphic() == null) {
            button.setGraphic(accent);
            button.setGraphicTextGap(10);
        }
        Runnable update = () -> {
            boolean enabled = !button.isDisabled();
            boolean hover = enabled && button.isHover();
            boolean pressed = enabled && button.isPressed();
            double brightness = !enabled ? 0.24 : pressed ? (navigation ? 0.16 : -0.12)
                    : hover ? (navigation ? 0.12 : -0.075) : 0;
            double saturation = enabled ? 0 : -0.85;
            double scale = pressed && !navigation ? 0.975 : 1;
            double radius = hover && !navigation && !pressed ? 7 : 0;
            Color shade = radius > 0 ? Color.web("#173d45", 0.18) : Color.TRANSPARENT;
            double accentOpacity = hover ? 0.85 : 0;
            double accentScale = hover ? 1 : 0.35;
            if (!canAnimate(button)) {
                stop(button, "button");
                tone.setBrightness(brightness); tone.setSaturation(saturation);
                shadow.setRadius(radius); shadow.setOffsetY(radius > 0 ? 2 : 0); shadow.setColor(shade);
                button.setScaleX(scale); button.setScaleY(scale);
                accent.setOpacity(accentOpacity); accent.setScaleY(accentScale);
                return;
            }
            play(button, "button", new Timeline(new KeyFrame(FEEDBACK,
                    new KeyValue(tone.brightnessProperty(), brightness, Interpolator.EASE_OUT),
                    new KeyValue(tone.saturationProperty(), saturation, Interpolator.EASE_OUT),
                    new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_OUT),
                    new KeyValue(shadow.offsetYProperty(), radius > 0 ? 2 : 0, Interpolator.EASE_OUT),
                    new KeyValue(shadow.colorProperty(), shade, Interpolator.EASE_OUT),
                    new KeyValue(button.scaleXProperty(), scale, Interpolator.EASE_OUT),
                    new KeyValue(button.scaleYProperty(), scale, Interpolator.EASE_OUT),
                    new KeyValue(accent.opacityProperty(), accentOpacity, Interpolator.EASE_OUT),
                    new KeyValue(accent.scaleYProperty(), accentScale, Interpolator.EASE_OUT))));
        };
        button.hoverProperty().addListener((obs, old, value) -> update.run());
        button.pressedProperty().addListener((obs, old, value) -> update.run());
        button.disabledProperty().addListener((obs, old, value) -> update.run());
        update.run();
    }

    private static void field(Control control) {
        DropShadow glow = new DropShadow(0, Color.TRANSPARENT);
        control.setEffect(glow);
        Color idleBorder = Color.web("#dae4e6");
        Color focusBorder = Color.web("#176b75");
        ObjectProperty<Color> border = new SimpleObjectProperty<>(idleBorder);
        // Binding keeps CSS pseudo-class recalculation from snapping the animated
        // border straight to its end color on the first focused/hovered frame.
        control.borderProperty().bind(Bindings.createObjectBinding(() -> new Border(new BorderStroke(
                border.get(), BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1))), border));
        Runnable update = () -> {
            boolean focused = !control.isDisabled() && (control.isFocused()
                    || (control instanceof DatePicker picker && picker.getEditor().isFocused()));
            Color color = focused ? Color.web("#277f89", 0.30) : Color.TRANSPARENT;
            Color stroke = focused ? focusBorder
                    : control.isHover() && !control.isDisabled() ? Color.web("#9db5be") : idleBorder;
            double radius = focused ? 8 : 0;
            if (!canAnimate(control)) {
                stop(control, "focus");
                glow.setRadius(radius); glow.setColor(color); border.set(stroke);
                return;
            }
            play(control, "focus", new Timeline(new KeyFrame(FEEDBACK,
                    new KeyValue(glow.radiusProperty(), radius, Interpolator.EASE_OUT),
                    new KeyValue(glow.colorProperty(), color, Interpolator.EASE_OUT),
                    new KeyValue(border, stroke, Interpolator.EASE_OUT))));
        };
        control.focusedProperty().addListener((obs, old, value) -> update.run());
        control.hoverProperty().addListener((obs, old, value) -> update.run());
        control.disabledProperty().addListener((obs, old, value) -> update.run());
        if (control instanceof DatePicker picker) {
            picker.getEditor().focusedProperty().addListener((obs, old, value) -> update.run());
        }
    }

    private static void message(Label label) {
        label.visibleProperty().addListener((obs, old, visible) -> {
            if (!label.getStyleClass().contains("error") && !label.getStyleClass().contains("success")) return;
            stop(label, "message");
            label.setOpacity(1);
            if (visible && canAnimate(label)) {
                FadeTransition fade = new FadeTransition(Duration.millis(180), label);
                fade.setFromValue(0); fade.setToValue(1);
                fade.setInterpolator(Interpolator.EASE_OUT);
                play(label, "message", fade);
            }
        });
    }

    public static void elevate(Node node, boolean hover) {
        if (node.getProperties().putIfAbsent(ELEVATION, true) != null) return;
        DropShadow shadow = new DropShadow(10, Color.web("#173d45", 0.10));
        shadow.setOffsetY(2);
        node.setEffect(shadow);
        if (!hover) return;
        node.hoverProperty().addListener((obs, old, value) -> {
            double radius = value ? 19 : 10;
            double offset = value ? 5 : 2;
            double scale = value ? 1.015 : 1;
            if (!canAnimate(node)) {
                stop(node, "elevation");
                shadow.setRadius(radius); shadow.setOffsetY(offset);
                node.setScaleX(scale); node.setScaleY(scale);
                return;
            }
            play(node, "elevation", new Timeline(new KeyFrame(Duration.millis(170),
                    new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_OUT),
                    new KeyValue(shadow.offsetYProperty(), offset, Interpolator.EASE_OUT),
                    new KeyValue(node.scaleXProperty(), scale, Interpolator.EASE_OUT),
                    new KeyValue(node.scaleYProperty(), scale, Interpolator.EASE_OUT))));
        });
    }

    /** Register a real screen element; the shell starts it after the layout pulse. */
    public static void enter(Node node, int delayMillis, boolean scale) {
        if (!REDUCED) node.getProperties().put(ENTRY,
                new Entry(scale ? Entrance.DIALOG : Entrance.CONTENT, Math.max(0, Math.min(320, delayMillis))));
    }

    public static void indicator(Node line) {
        if (!REDUCED) line.getProperties().put(ENTRY, new Entry(Entrance.INDICATOR, 0));
    }

    public static void prepareModule(Node content) {
        if (!REDUCED) content.getProperties().put(ENTRY, new Entry(Entrance.MODULE, 0));
        prepare(content);
    }

    public static void prepare(Node root) {
        visit(root, node -> {
            Entry entry = (Entry) node.getProperties().get(ENTRY);
            if (entry == null || !node.isVisible() || !node.isManaged()) return;
            node.setOpacity(0);
            if (entry.kind() == Entrance.INDICATOR) node.setScaleY(0.25);
            else if (entry.kind() == Entrance.DIALOG) { node.setScaleX(0.97); node.setScaleY(0.97); }
            else node.setTranslateY(entry.kind() == Entrance.MODULE ? 12 : 10);
        });
    }

    /** One shot in the live Scene, after CSS/layout but before the first render. */
    public static void revealAfterLayout(Node root) {
        if (root == null || REDUCED || root.getScene() == null) return;
        Scheduled previous = (Scheduled) root.getProperties().remove(SCHEDULED);
        if (previous != null) previous.cancel();
        prepare(root);
        Scene scene = root.getScene();
        Runnable pulse = new Runnable() {
            @Override public void run() {
                scene.removePostLayoutPulseListener(this);
                root.getProperties().remove(SCHEDULED);
                if (root.getScene() != scene || !canAnimate(root)) {
                    cancelTree(root);
                    return;
                }
                visit(root, node -> {
                    Entry entry = (Entry) node.getProperties().remove(ENTRY);
                    if (entry == null) return;
                    if (node.isVisible() && node.isManaged()) startEntrance(node, entry);
                    else reset(node);
                });
            }
        };
        root.getProperties().put(SCHEDULED, new Scheduled(scene, pulse));
        scene.addPostLayoutPulseListener(pulse);
        Platform.requestNextPulse();
    }

    private static void startEntrance(Node node, Entry entry) {
        boolean dialog = entry.kind() == Entrance.DIALOG;
        Duration duration = Duration.millis(dialog || entry.kind() == Entrance.INDICATOR ? 200 : 240);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition entrance = new ParallelTransition(fade);
        if (dialog || entry.kind() == Entrance.INDICATOR) {
            ScaleTransition scale = new ScaleTransition(duration, node);
            if (dialog) { scale.setFromX(0.97); scale.setToX(1); }
            scale.setFromY(dialog ? 0.97 : 0.25); scale.setToY(1);
            scale.setInterpolator(Interpolator.EASE_OUT);
            entrance.getChildren().add(scale);
        } else {
            TranslateTransition slide = new TranslateTransition(duration, node);
            slide.setFromY(entry.kind() == Entrance.MODULE ? 12 : 10); slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            entrance.getChildren().add(slide);
        }
        entrance.setDelay(Duration.millis(entry.delay()));
        play(node, "entrance", entrance);
    }

    /** Application-owned alerts only; do not replace onShown/onHidden callbacks. */
    public static void dialog(Alert alert) {
        alert.showingProperty().addListener((obs, old, showing) -> {
            if (showing) {
                enter(alert.getDialogPane(), 0, true);
                revealAfterLayout(alert.getDialogPane());
            } else cancelTree(alert.getDialogPane());
        });
    }

    private static boolean canAnimate(Node node) {
        return !REDUCED && node.getScene() != null && node.getScene().getWindow() != null
                && node.getScene().getWindow().isShowing();
    }

    /** Cancel pending pulses as well as running transitions on outgoing views. */
    public static void cancelTree(Node root) {
        visit(root, node -> {
            Scheduled scheduled = (Scheduled) node.getProperties().remove(SCHEDULED);
            if (scheduled != null) scheduled.cancel();
            boolean entrance = node.getProperties().remove(ENTRY) != null;
            Map<String, Animation> owned = existingAnimations(node);
            if (owned != null) { owned.values().forEach(Animation::stop); owned.clear(); }
            if (entrance || owned != null) reset(node);
        });
    }

    private static void reset(Node node) {
        node.setOpacity(1); node.setTranslateY(0);
        node.setScaleX(1); node.setScaleY(1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Animation> existingAnimations(Node node) {
        return (Map<String, Animation>) node.getProperties().get(ANIMATIONS);
    }

    private static void stop(Node node, String channel) {
        Map<String, Animation> owned = existingAnimations(node);
        if (owned == null) return;
        Animation previous = owned.remove(channel);
        if (previous != null) previous.stop();
    }

    private static void play(Node node, String channel, Animation animation) {
        stop(node, channel);
        Map<String, Animation> owned = existingAnimations(node);
        if (owned == null) {
            owned = new HashMap<>();
            node.getProperties().put(ANIMATIONS, owned);
        }
        owned.put(channel, animation);
        Map<String, Animation> running = owned;
        animation.setOnFinished(event -> running.remove(channel, animation));
        animation.play();
    }
}
