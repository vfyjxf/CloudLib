package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widget.SpacerWidget;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint for {@link SpacerWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * HStack(() -> {
 *     Label("Left");
 *     Spacer();        // Flexible spacer
 *     Label("Right");
 * })
 *
 * VStack(() -> {
 *     Label("Top");
 *     Spacer(20);      // Spacer with minimum length
 *     Label("Bottom");
 * })
 * }</pre>
 */
public final class SpacerBlueprint implements Blueprint<SpacerWidget> {

    private float minLength = 0;
    private float flexGrow = 1;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private SpacerBlueprint() {
    }

    //region dsl entry points

    /**
     * Creates a flexible spacer that expands to fill available space.
     */
    public static SpacerBlueprint Spacer() {
        return ScopedReceiver.add(new SpacerBlueprint());
    }

    /**
     * Creates a spacer with a minimum length.
     *
     * @param minLength minimum length in pixels
     */
    public static SpacerBlueprint Spacer(float minLength) {
        return ScopedReceiver.add(new SpacerBlueprint().minLength(minLength));
    }

    /**
     * Creates a fixed-size spacer that doesn't expand.
     *
     * @param size fixed size in pixels
     */
    public static SpacerBlueprint FixedSpacer(float size) {
        return ScopedReceiver.add(new SpacerBlueprint().minLength(size).flexGrow(0));
    }

    //endregion

    //region builder methods

    public SpacerBlueprint minLength(float minLength) {
        this.minLength = minLength;
        return this;
    }

    public SpacerBlueprint flexGrow(float flexGrow) {
        this.flexGrow = flexGrow;
        return this;
    }

    public SpacerBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public SpacerBlueprint style(UIStyle style) {
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
    public SpacerWidget createWidget(Scene scene, SceneContext context) {
        return SpacerWidget.create();
    }

    @Override
    public void updateWidget(SpacerWidget widget, Scene scene, SceneContext context) {
        widget.setMinLength(minLength)
                .setFlexGrow(flexGrow)
                .useStyle(style);
    }

    //endregion
}
