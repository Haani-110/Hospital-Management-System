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
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Finite, presentation-only feedback. No action handlers, timers, worker threads,
 * loading simulation or application/session state. Set hospital.ui.reduceMotion
 * to true (a JVM property) to apply final visual states without animation.
 */
public final class UiMotion {
    private static final boolean REDUCED = Boolean.getBoolean("hospital.ui.reduceMotion");
    private static final Duration FEEDBACK = Duration.millis(150);
    private static final Object ANIMATIONS = new Object();
    private static final Object INSTALLED = new Object();
    private static final Object ELEVATION = new Object();

    private UiMotion() { }

    /** Traverse application nodes, not the thousands of virtual table/skin nodes. */
    public static void install(Node node) {
        if (node == null || node.getProperties().putIfAbsent(INSTALLED, true) != null) return;
        if (node instanceof Button button) button(button);
        if (node instanceof TextInputControl || node instanceof ComboBoxBase<?>) field((Control) node);
        if (node instanceof Label label) message(label);
        if (node instanceof ScrollPane scroll) {
            install(scroll.getContent());
        } else if (node instanceof SplitPane split) {
            split.getItems().forEach(UiMotion::install);
        } else if (node instanceof DialogPane pane) {
            install(pane.getHeader());
            install(pane.getContent());
            pane.getButtonTypes().forEach(type -> install(pane.lookupButton(type)));
        } else if (!(node instanceof Control) && node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(UiMotion::install);
        }
    }

    private static void button(Button button) {
        button.getStyleClass().add("motion-button");
        ColorAdjust tone = new ColorAdjust();
        button.setEffect(tone);
        boolean navigation = button.getStyleClass().contains("nav-button");
        Runnable update = () -> {
            double brightness = button.isDisabled() ? 0.20
                    : button.isPressed() ? (navigation ? 0.12 : -0.10)
                    : button.isHover() ? (navigation ? 0.075 : -0.04) : 0;
            double saturation = button.isDisabled() ? -0.75 : 0;
            // Backgrounds remain stable in CSS; interpolate their tone instead
            // of snapping CSS colors. Text, focus, default/cancel and click
            // handling are still the original JavaFX Button's responsibility.
            if (!canAnimate(button)) {
                stop(button, "button");
                tone.setBrightness(brightness);
                tone.setSaturation(saturation);
                return;
            }
            play(button, "button", new Timeline(new KeyFrame(FEEDBACK,
                    new KeyValue(tone.brightnessProperty(), brightness, Interpolator.EASE_BOTH),
                    new KeyValue(tone.saturationProperty(), saturation, Interpolator.EASE_BOTH))));
        };
        button.hoverProperty().addListener((obs, old, value) -> update.run());
        button.pressedProperty().addListener((obs, old, value) -> update.run());
        button.disabledProperty().addListener((obs, old, value) -> update.run());
        update.run();
    }

    private static void field(Control control) {
        DropShadow glow = new DropShadow(0, Color.TRANSPARENT);
        control.setEffect(glow);
        Runnable update = () -> {
            boolean focused = control.isFocused()
                    || (control instanceof DatePicker picker && picker.getEditor().isFocused());
            Color color = focused ? Color.web("#277f89", 0.24) : Color.TRANSPARENT;
            double radius = focused ? 7 : 0;
            if (!canAnimate(control)) {
                stop(control, "focus");
                glow.setRadius(radius);
                glow.setColor(color);
                return;
            }
            play(control, "focus", new Timeline(new KeyFrame(FEEDBACK,
                    new KeyValue(glow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                    new KeyValue(glow.colorProperty(), color, Interpolator.EASE_BOTH))));
        };
        control.focusedProperty().addListener((obs, old, value) -> update.run());
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
                fade.setFromValue(0);
                fade.setToValue(1);
                play(label, "message", fade);
            }
        });
    }

    public static void elevate(Node node, boolean hover) {
        if (node.getProperties().putIfAbsent(ELEVATION, true) != null) return;
        DropShadow shadow = new DropShadow(10, Color.web("#173d45", 0.09));
        shadow.setOffsetY(2);
        node.setEffect(shadow);
        if (!hover) return;
        node.hoverProperty().addListener((obs, old, value) -> {
            double radius = value ? 17 : 10;
            double offset = value ? 4 : 2;
            if (!canAnimate(node)) {
                stop(node, "elevation");
                shadow.setRadius(radius);
                shadow.setOffsetY(offset);
                return;
            }
            play(node, "elevation", new Timeline(new KeyFrame(Duration.millis(180),
                    new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                    new KeyValue(shadow.offsetYProperty(), offset, Interpolator.EASE_BOTH))));
        });
    }

    /** Animate once when this node's scene actually becomes visible. */
    public static void enter(Node node, int delayMillis, boolean scale) {
        if (REDUCED || !node.isVisible() || !node.isManaged()) return;
        node.setOpacity(0);
        new Entrance(node, () -> {
            FadeTransition fade = new FadeTransition(Duration.millis(180), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(200), node);
            slide.setFromY(6);
            slide.setToY(0);
            ParallelTransition entrance = new ParallelTransition(fade, slide);
            if (scale) {
                ScaleTransition grow = new ScaleTransition(Duration.millis(200), node);
                grow.setFromX(0.99); grow.setFromY(0.99);
                grow.setToX(1); grow.setToY(1);
                entrance.getChildren().add(grow);
            }
            entrance.setDelay(Duration.millis(Math.max(0, Math.min(50, delayMillis))));
            play(node, "entrance", entrance);
        }).attach();
    }

    /** Only the small indicator grows; the active navigation item never moves. */
    public static void indicator(Node line) {
        if (REDUCED) return;
        new Entrance(line, () -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), line);
            scale.setFromY(0.25);
            scale.setToY(1);
            play(line, "indicator", scale);
        }).attach();
    }

    private static boolean canAnimate(Node node) {
        return !REDUCED && node.getScene() != null && node.getScene().getWindow() != null
                && node.getScene().getWindow().isShowing();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Animation> animations(Node node) {
        Map<String, Animation> running = (Map<String, Animation>) node.getProperties().get(ANIMATIONS);
        if (running == null) {
            running = new HashMap<>();
            node.getProperties().put(ANIMATIONS, running);
            Map<String, Animation> owned = running;
            node.sceneProperty().addListener((obs, old, scene) -> {
                if (scene == null) {
                    owned.values().forEach(Animation::stop);
                    owned.clear();
                    node.setOpacity(1);
                    node.setTranslateY(0);
                    node.setScaleX(1); node.setScaleY(1);
                }
            });
        }
        return running;
    }

    private static void stop(Node node, String channel) {
        Animation previous = animations(node).remove(channel);
        if (previous != null) previous.stop();
    }

    private static void play(Node node, String channel, Animation animation) {
        stop(node, channel);
        animations(node).put(channel, animation);
        animation.setOnFinished(event -> animations(node).remove(channel, animation));
        animation.play();
    }

    /** One-shot listeners are removed as soon as the window is shown. */
    private static final class Entrance {
        private final Node node;
        private final Runnable show;
        private Scene scene;
        private Window window;
        private boolean played;
        private final ChangeListener<Scene> sceneListener = (obs, old, value) -> observeScene();
        private final ChangeListener<Window> windowListener = (obs, old, value) -> observeWindow();
        private final InvalidationListener showingListener = obs -> reveal();

        Entrance(Node node, Runnable show) { this.node = node; this.show = show; }

        void attach() {
            node.sceneProperty().addListener(sceneListener);
            observeScene();
        }

        private void observeScene() {
            if (scene != null) scene.windowProperty().removeListener(windowListener);
            scene = node.getScene();
            if (scene != null) scene.windowProperty().addListener(windowListener);
            observeWindow();
        }

        private void observeWindow() {
            if (window != null) window.showingProperty().removeListener(showingListener);
            window = scene == null ? null : scene.getWindow();
            if (window != null) window.showingProperty().addListener(showingListener);
            reveal();
        }

        private void reveal() {
            if (played || window == null || !window.isShowing()) return;
            played = true;
            node.sceneProperty().removeListener(sceneListener);
            scene.windowProperty().removeListener(windowListener);
            window.showingProperty().removeListener(showingListener);
            show.run();
        }
    }
}
