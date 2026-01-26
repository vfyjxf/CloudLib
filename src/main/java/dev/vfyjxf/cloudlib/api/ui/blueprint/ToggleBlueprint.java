package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widgets.ToggleWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Blueprint for {@link ToggleWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Toggle(false)
 * Toggle(true, isOn -> System.out.println("Toggled: " + isOn))
 * Toggle(state).colors(0xFF666666, 0xFF00AA00)
 * }</pre>
 */
public final class ToggleBlueprint implements Blueprint<ToggleWidget> {

    private boolean toggled;
    private @Nullable Consumer<Boolean> onToggle;
    private VisualTexture offTexture = new ColorTexture(0xFF666666);
    private VisualTexture onTexture = new ColorTexture(0xFF00AA00);
    private @Nullable VisualTexture hoverTexture;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private ToggleBlueprint(boolean initial) {
        this.toggled = initial;
    }

    // ==================== DSL Entry Points ====================

    public static ToggleBlueprint Toggle(boolean initial) {
        return ScopedReceiver.add(new ToggleBlueprint(initial));
    }

    public static ToggleBlueprint Toggle(boolean initial, Consumer<Boolean> onToggle) {
        return ScopedReceiver.add(new ToggleBlueprint(initial).onToggle(onToggle));
    }

    // ==================== Builder Methods ====================

    public ToggleBlueprint toggled(boolean toggled) {
        this.toggled = toggled;
        return this;
    }

    public ToggleBlueprint onToggle(@Nullable Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }

    public ToggleBlueprint colors(int offColor, int onColor) {
        this.offTexture = new ColorTexture(offColor);
        this.onTexture = new ColorTexture(onColor);
        return this;
    }

    public ToggleBlueprint textures(VisualTexture off, VisualTexture on) {
        this.offTexture = off;
        this.onTexture = on;
        return this;
    }

    public ToggleBlueprint hoverTexture(@Nullable VisualTexture hover) {
        this.hoverTexture = hover;
        return this;
    }

    public ToggleBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public ToggleBlueprint style(UIStyle style) {
        this.style = style;
        return this;
    }

    // ==================== Blueprint Implementation ====================

    @Override
    public @Nullable Object key() {
        return key;
    }

    @Override
    public ToggleWidget createWidget(Scene scene, SceneContext context) {
        return ToggleWidget.create();
    }

    @Override
    public void updateWidget(ToggleWidget widget, Scene scene, SceneContext context) {
        widget.setToggled(toggled)
            .onToggle(onToggle)
            .setTextures(offTexture, onTexture)
            .setHoverTexture(hoverTexture)
            .applyStyle(style);
    }
}
