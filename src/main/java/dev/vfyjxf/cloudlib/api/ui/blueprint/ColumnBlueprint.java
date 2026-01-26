package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widgets.ColumnWidget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Blueprint for {@link ColumnWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Column(() -> {
 *     Label("Title");
 *     Button("Click", () -> {});
 *     Row(() -> {
 *         Label("Item 1");
 *         Label("Item 2");
 *     });
 * })
 * Column(4, () -> { ... }) // With spacing
 * }</pre>
 */
public final class ColumnBlueprint implements Blueprint.Group<ColumnWidget, Widget> {

    private final Supplier<List<Blueprint<?>>> childrenSupplier;
    private int spacing = 0;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private ColumnBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier) {
        this.childrenSupplier = childrenSupplier;
    }

    // ==================== DSL Entry Points ====================

    public static ColumnBlueprint Column(Runnable content) {
        return ScopedReceiver.add(new ColumnBlueprint(() -> ScopedReceiver.buildChildren(content)));
    }

    public static ColumnBlueprint Column(int spacing, Runnable content) {
        return ScopedReceiver.add(new ColumnBlueprint(() -> ScopedReceiver.buildChildren(content)).spacing(spacing));
    }

    // ==================== Builder Methods ====================

    public ColumnBlueprint spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public ColumnBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public ColumnBlueprint style(UIStyle style) {
        this.style = style;
        return this;
    }

    // ==================== Blueprint Implementation ====================

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
    public ColumnWidget createWidget(Scene scene, SceneContext context) {
        return ColumnWidget.create();
    }

    @Override
    public void updateWidget(ColumnWidget widget, Scene scene, SceneContext context) {
        widget.setSpacing(spacing)
            .applyStyle(style);
    }
}
