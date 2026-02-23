package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint for {@link DividerWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * VStack(() -> {
 *     Label("Section 1");
 *     Divider();
 *     Label("Section 2");
 * })
 *
 * HStack(() -> {
 *     Label("Left");
 *     Divider().vertical();
 *     Label("Right");
 * })
 * }</pre>
 */
public final class DividerBlueprint implements Blueprint<DividerWidget> {

    private DividerWidget.Orientation orientation = DividerWidget.Orientation.HORIZONTAL;
    private int thickness = 1;
    private VisualTexture texture = new ColorTexture(0xFFAAAAAA);
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private DividerBlueprint() {
    }

    //region dsl entry points

    /**
     * Creates a horizontal divider.
     */
    public static DividerBlueprint Divider() {
        return ScopedReceiver.add(new DividerBlueprint());
    }

    /**
     * Creates a divider with specified color.
     *
     * @param color divider color in ARGB format
     */
    public static DividerBlueprint Divider(int color) {
        return ScopedReceiver.add(new DividerBlueprint().color(color));
    }

    //endregion

    //region builder methods

    public DividerBlueprint horizontal() {
        this.orientation = DividerWidget.Orientation.HORIZONTAL;
        return this;
    }

    public DividerBlueprint vertical() {
        this.orientation = DividerWidget.Orientation.VERTICAL;
        return this;
    }

    public DividerBlueprint orientation(DividerWidget.Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public DividerBlueprint thickness(int thickness) {
        this.thickness = thickness;
        return this;
    }

    public DividerBlueprint color(int color) {
        this.texture = new ColorTexture(color);
        return this;
    }

    public DividerBlueprint texture(VisualTexture texture) {
        this.texture = texture;
        return this;
    }

    public DividerBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public DividerBlueprint style(UIStyle style) {
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
    public DividerWidget createWidget(Scene scene, SceneContext context) {
        return DividerWidget.create();
    }

    @Override
    public void updateWidget(DividerWidget widget, Scene scene, SceneContext context) {
        widget.setOrientation(orientation)
                .setThickness(thickness)
                .setTexture(texture)
                .useStyle(style);
    }

    //endregion
}
