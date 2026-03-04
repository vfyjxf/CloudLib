package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.*;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widget.RowWidget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Blueprint for {@link RowWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Row(() -> {
 *     Label("Left");
 *     Spacer();
 *     Label("Right");
 * })
 * Row(8, () -> { ... }) // With spacing
 * }</pre>
 */
public final class RowBlueprint implements Blueprint.Group<RowWidget, Widget> {

    private final Supplier<List<Blueprint<?>>> childrenSupplier;
    private int spacing = 0;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private RowBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier) {
        this.childrenSupplier = childrenSupplier;
    }

    //region dsl entry points

    public static RowBlueprint Row(Runnable content) {
        return ScopedReceiver.add(new RowBlueprint(() -> ScopedReceiver.buildChildren(content)));
    }

    public static RowBlueprint Row(int spacing, Runnable content) {
        return ScopedReceiver.add(new RowBlueprint(() -> ScopedReceiver.buildChildren(content)).spacing(spacing));
    }

    //endregion

    //region builder methods

    public RowBlueprint spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public RowBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public RowBlueprint style(UIStyle style) {
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
    public RowWidget createWidget(Scene scene, SceneContext context) {
        return RowWidget.create();
    }

    @Override
    public void updateWidget(RowWidget widget, Scene scene, SceneContext context) {
        widget.setSpacing(spacing)
              .useStyle(style);
    }

    //endregion
}
