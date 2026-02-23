package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.*;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widget.BoxWidget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Blueprint for {@link BoxWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Box(() -> {
 *     Image(backgroundTexture);  // Bottom layer
 *     Label("Overlay Text");      // Top layer
 * })
 * }</pre>
 */
public final class BoxBlueprint implements Blueprint.Group<BoxWidget, Widget> {

    private final Supplier<List<Blueprint<?>>> childrenSupplier;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private BoxBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier) {
        this.childrenSupplier = childrenSupplier;
    }

    //region dsl entry points

    public static BoxBlueprint Box(Runnable content) {
        return ScopedReceiver.add(new BoxBlueprint(() -> ScopedReceiver.buildChildren(content)));
    }

    //endregion

    //region builder methods

    public BoxBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public BoxBlueprint style(UIStyle style) {
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
    public BoxWidget createWidget(Scene scene, SceneContext context) {
        return BoxWidget.create();
    }

    @Override
    public void updateWidget(BoxWidget widget, Scene scene, SceneContext context) {
        widget.useStyle(style);
    }

    //endregion
}
