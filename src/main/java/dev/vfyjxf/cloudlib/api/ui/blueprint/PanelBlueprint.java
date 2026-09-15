package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.PanelWidget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Blueprint for {@link PanelWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Panel("Settings", () -> {
 *     Label("Option 1");
 *     Toggle(false);
 * })
 * Panel(() -> { ... })  // Panel without title
 * }</pre>
 */
public final class PanelBlueprint implements Blueprint.Group<PanelWidget, Widget> {

    private final Supplier<List<Blueprint<?>>> childrenSupplier;
    private @Nullable String title;
    private VisualTexture backgroundTexture = new ColorTexture(0xCC222222);
    private VisualTexture borderTexture = new ColorTexture(0xFF555555);
    private int borderWidth = 1;
    private int contentPadding = 4;
    private boolean showTitleBar = true;

    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private PanelBlueprint(@Nullable String title, Supplier<List<Blueprint<?>>> childrenSupplier) {
        this.title = title;
        this.childrenSupplier = childrenSupplier;
    }

    //region dsl entry points

    public static PanelBlueprint Panel(Runnable content) {
        return ScopedReceiver.add(new PanelBlueprint(null, () -> ScopedReceiver.buildChildren(content)));
    }

    public static PanelBlueprint Panel(String title, Runnable content) {
        return ScopedReceiver.add(new PanelBlueprint(title, () -> ScopedReceiver.buildChildren(content)));
    }

    //endregion

    //region builder methods

    public PanelBlueprint title(@Nullable String title) {
        this.title = title;
        return this;
    }

    public PanelBlueprint background(VisualTexture texture) {
        this.backgroundTexture = texture;
        return this;
    }

    public PanelBlueprint backgroundColor(int color) {
        this.backgroundTexture = new ColorTexture(color);
        return this;
    }

    public PanelBlueprint border(VisualTexture texture, int width) {
        this.borderTexture = texture;
        this.borderWidth = width;
        return this;
    }

    public PanelBlueprint borderColor(int color, int width) {
        this.borderTexture = new ColorTexture(color);
        this.borderWidth = width;
        return this;
    }

    public PanelBlueprint contentPadding(int padding) {
        this.contentPadding = padding;
        return this;
    }

    public PanelBlueprint showTitleBar(boolean show) {
        this.showTitleBar = show;
        return this;
    }

    public PanelBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public PanelBlueprint style(UIStyle style) {
        this.style = style;
        return this;
    }

    //endregion

    //region blueprint implementation

    @Override
    public @Nullable Object key() {
        return key;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MutableList<Blueprint<Widget>> children() {
        return (MutableList) Lists.mutable.ofAll(childrenSupplier.get());
    }

    @Override
    public PanelWidget createWidget(Scene scene, SceneContext context) {
        return PanelWidget.create();
    }

    @Override
    public void updateWidget(PanelWidget widget, Scene scene, SceneContext context) {
        widget.setTitle(title)
              .setBackgroundTexture(backgroundTexture)
              .setBorderTexture(borderTexture)
              .setBorderWidth(borderWidth)
              .setContentPadding(contentPadding)
              .setShowTitleBar(showTitleBar)
              .useStyle(style);
    }

    //endregion
}
