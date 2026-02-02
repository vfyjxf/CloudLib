package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widgets.LabelWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint for {@link LabelWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Label("Hello World")
 * Label("Colored", 0xFF0000)
 * Label(Component.translatable("key"))
 * }</pre>
 */
public final class LabelBlueprint implements Blueprint<LabelWidget> {

    private final Component text;
    private int color = 0xFFFFFF;
    private boolean shadow = true;
    private LabelWidget.TextAlign align = LabelWidget.TextAlign.LEFT;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private LabelBlueprint(Component text) {
        this.text = text;
    }

    //region dsl entry points

    public static LabelBlueprint Label(String text) {
        return ScopedReceiver.add(new LabelBlueprint(Component.literal(text)));
    }

    public static LabelBlueprint Label(Component text) {
        return ScopedReceiver.add(new LabelBlueprint(text));
    }

    public static LabelBlueprint Label(String text, int color) {
        return ScopedReceiver.add(new LabelBlueprint(Component.literal(text)).color(color));
    }

    //endregion

    //region builder methods

    public LabelBlueprint color(int color) {
        this.color = color;
        return this;
    }

    public LabelBlueprint shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public LabelBlueprint align(LabelWidget.TextAlign align) {
        this.align = align;
        return this;
    }

    public LabelBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public LabelBlueprint style(UIStyle style) {
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
    public LabelWidget createWidget(Scene scene, SceneContext context) {
        return LabelWidget.of(text);
    }

    @Override
    public void updateWidget(LabelWidget widget, Scene scene, SceneContext context) {
        widget.setText(text)
              .setColor(color)
              .setShadow(shadow)
              .setAlign(align)
              .applyStyle(style);
    }

    //endregion
}
