package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.eclipse.collections.api.list.MutableList;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Central DSL for building UI declaratively.
 * <p>
 * Import statically to use the Compose/Flutter-like syntax:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.blueprint.Blueprints.*;
 *
 * Column(() -> {
 *     Label("Title");
 *     Row(8, () -> {
 *         Button("OK", () -> close());
 *         Button("Cancel", () -> cancel());
 *     });
 *     Divider();
 *     ProgressBar(() -> loadingProgress);
 * });
 * }</pre>
 *
 * @see LabelBlueprint
 * @see ButtonBlueprint
 * @see ColumnBlueprint
 * @see RowBlueprint
 */
public final class Blueprints {

    private Blueprints() {}

    // ==================== Text ====================

    public static LabelBlueprint Label(String text) {
        return LabelBlueprint.Label(text);
    }

    public static LabelBlueprint Label(Component text) {
        return LabelBlueprint.Label(text);
    }

    public static LabelBlueprint Label(String text, int color) {
        return LabelBlueprint.Label(text, color);
    }

    // ==================== Image ====================

    public static ImageBlueprint Image(VisualTexture texture) {
        return ImageBlueprint.Image(texture);
    }

    public static ImageBlueprint Image(ResourceLocation location, int width, int height) {
        return ImageBlueprint.Image(location, width, height);
    }

    public static ImageBlueprint EmptyImage() {
        return ImageBlueprint.EmptyImage();
    }

    // ==================== Button ====================

    public static ButtonBlueprint Button(String label) {
        return ButtonBlueprint.Button(label);
    }

    public static ButtonBlueprint Button(String label, Runnable onClick) {
        return ButtonBlueprint.Button(label, onClick);
    }

    public static ButtonBlueprint Button(Component label) {
        return ButtonBlueprint.Button(label);
    }

    public static ButtonBlueprint Button(Component label, Runnable onClick) {
        return ButtonBlueprint.Button(label, onClick);
    }

    // ==================== Progress Bar ====================

    public static ProgressBarBlueprint ProgressBar(DoubleSupplier progressSupplier) {
        return ProgressBarBlueprint.ProgressBar(progressSupplier);
    }

    public static ProgressBarBlueprint ProgressBar(double progress) {
        return ProgressBarBlueprint.ProgressBar(progress);
    }

    // ==================== Slider ====================

    public static SliderBlueprint Slider(double min, double max) {
        return SliderBlueprint.Slider(min, max);
    }

    public static SliderBlueprint Slider(double min, double max, double value) {
        return SliderBlueprint.Slider(min, max, value);
    }

    public static SliderBlueprint Slider(double min, double max, Consumer<Double> onValueChanged) {
        return SliderBlueprint.Slider(min, max, onValueChanged);
    }

    // ==================== Toggle ====================

    public static ToggleBlueprint Toggle(boolean initial) {
        return ToggleBlueprint.Toggle(initial);
    }

    public static ToggleBlueprint Toggle(boolean initial, Consumer<Boolean> onToggle) {
        return ToggleBlueprint.Toggle(initial, onToggle);
    }

    // ==================== Text Field ====================

    public static TextFieldBlueprint TextField(String text) {
        return TextFieldBlueprint.TextField(text);
    }

    public static TextFieldBlueprint TextField(String text, Consumer<String> onTextChanged) {
        return TextFieldBlueprint.TextField(text, onTextChanged);
    }

    // ==================== Layout Containers ====================

    public static ColumnBlueprint Column(Runnable content) {
        return ColumnBlueprint.Column(content);
    }

    public static ColumnBlueprint Column(int spacing, Runnable content) {
        return ColumnBlueprint.Column(spacing, content);
    }

    public static RowBlueprint Row(Runnable content) {
        return RowBlueprint.Row(content);
    }

    public static RowBlueprint Row(int spacing, Runnable content) {
        return RowBlueprint.Row(spacing, content);
    }

    public static BoxBlueprint Box(Runnable content) {
        return BoxBlueprint.Box(content);
    }

    // ==================== Panel ====================

    public static PanelBlueprint Panel(Runnable content) {
        return PanelBlueprint.Panel(content);
    }

    public static PanelBlueprint Panel(String title, Runnable content) {
        return PanelBlueprint.Panel(title, content);
    }

    // ==================== Spacer & Divider ====================

    public static SpacerBlueprint Spacer() {
        return SpacerBlueprint.Spacer();
    }

    public static SpacerBlueprint Spacer(float minLength) {
        return SpacerBlueprint.Spacer(minLength);
    }

    public static SpacerBlueprint FixedSpacer(float size) {
        return SpacerBlueprint.FixedSpacer(size);
    }

    public static DividerBlueprint Divider() {
        return DividerBlueprint.Divider();
    }

    public static DividerBlueprint Divider(int color) {
        return DividerBlueprint.Divider(color);
    }

    // ==================== Internal Blueprint Classes ====================

    public static class FlexBlueprint<T extends Widget> implements Blueprint.Group<CompositeWidget<T>, T> {

        @Override
        public MutableList<? extends Blueprint<T>> children() {
            return null;
        }

        @Override
        public CompositeWidget<T> createWidget(Scene scene, SceneContext context) {
            return null;
        }

        @Override
        public void updateWidget(CompositeWidget<T> widget, Scene scene, SceneContext context) {

        }
    }
}
