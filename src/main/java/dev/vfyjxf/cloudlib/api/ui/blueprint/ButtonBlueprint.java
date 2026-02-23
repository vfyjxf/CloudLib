package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint for {@link ButtonWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Button("Click Me", () -> System.out.println("Clicked!"))
 * Button(Component.translatable("button.label"), this::handleClick)
 *     .colors(0xFF555555, 0xFF777777, 0xFF333333)
 * }</pre>
 */
public final class ButtonBlueprint implements Blueprint<ButtonWidget> {

    private final Component label;
    private @Nullable Runnable onClick;
    private boolean enabled = true;

    private VisualTexture normalTexture = new ColorTexture(0xFF555555);
    private VisualTexture hoverTexture = new ColorTexture(0xFF777777);
    private VisualTexture pressedTexture = new ColorTexture(0xFF333333);
    private @Nullable VisualTexture iconTexture;
    private int textColor = 0xFFFFFF;

    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private ButtonBlueprint(Component label) {
        this.label = label;
    }

    //region dsl entry points

    public static ButtonBlueprint Button(String label) {
        return ScopedReceiver.add(new ButtonBlueprint(Component.literal(label)));
    }

    public static ButtonBlueprint Button(String label, Runnable onClick) {
        return ScopedReceiver.add(new ButtonBlueprint(Component.literal(label)).onClick(onClick));
    }

    public static ButtonBlueprint Button(Component label) {
        return ScopedReceiver.add(new ButtonBlueprint(label));
    }

    public static ButtonBlueprint Button(Component label, Runnable onClick) {
        return ScopedReceiver.add(new ButtonBlueprint(label).onClick(onClick));
    }

    //endregion

    //region builder methods

    public ButtonBlueprint onClick(@Nullable Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    public ButtonBlueprint enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ButtonBlueprint colors(int normal, int hover, int pressed) {
        this.normalTexture = new ColorTexture(normal);
        this.hoverTexture = new ColorTexture(hover);
        this.pressedTexture = new ColorTexture(pressed);
        return this;
    }

    public ButtonBlueprint textures(VisualTexture normal, VisualTexture hover, VisualTexture pressed) {
        this.normalTexture = normal;
        this.hoverTexture = hover;
        this.pressedTexture = pressed;
        return this;
    }

    public ButtonBlueprint icon(@Nullable VisualTexture icon) {
        this.iconTexture = icon;
        return this;
    }

    public ButtonBlueprint textColor(int color) {
        this.textColor = color;
        return this;
    }

    public ButtonBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public ButtonBlueprint style(UIStyle style) {
        this.style = style;
        return this;
    }

    //endregion

    //region blueprint implementation

    @Override
    public @Nullable Object key() {
        return key;
    }

    @Override
    public ButtonWidget createWidget(Scene scene, SceneContext context) {
        return ButtonWidget.of(label);
    }

    @Override
    public void updateWidget(ButtonWidget widget, Scene scene, SceneContext context) {
        widget.setLabel(label)
                .onClick(onClick)
                .setEnabled(enabled)
                .setTextures(normalTexture, hoverTexture, pressedTexture)
                .setIconTexture(iconTexture)
                .setTextColor(textColor)
                .useStyle(style);
    }

    //endregion
}
