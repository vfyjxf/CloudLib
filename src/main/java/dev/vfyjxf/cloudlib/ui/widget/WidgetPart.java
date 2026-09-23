package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.layout.LayoutScope;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A named sub-part of a compound widget — a real node in the widget tree, so a
 * stylesheet can paint it through {@code ::part(name)}.
 * <p>
 * Three rules keep a part from changing its owner:
 * <ul>
 *   <li>it is absolutely positioned, so taffy never flows it and the owner's
 *       measured size stays whatever it was — the owner hands it a rect
 *       ({@code bounds}) instead of laying it out;</li>
 *   <li>it answers to the owner's type tag ({@link #styleTag()}), so the
 *       documented compound form {@code slider::part(handle)} matches. The
 *       flip side: a rule styling the host itself ({@code slider { … }}) also
 *       matches its parts;</li>
 *   <li>it mirrors the owner's state surface ({@link #styleStates()}), so
 *       {@code slider:hover::part(handle)} follows the compound.</li>
 * </ul>
 * The rect is read at layout time and again right before the owner renders its
 * children, so a geometry that changes every frame (a progress bar's supplier)
 * stays live without forcing a scene relayout.
 */
class WidgetPart extends Widget {

    private final Widget owner;
    private final @Nullable Supplier<Rect> bounds;
    private final @Nullable Supplier<@Nullable VisualTexture> fallback;
    private @Nullable Rect applied;

    /**
     * @param owner    the compound widget this part belongs to
     * @param name     the {@code ::part(name)} name, or null for a repeated
     *                 child that a theme addresses by class or tag instead
     * @param bounds   the rect the owner wants, re-read on every pass, or null
     *                 for a part whose geometry comes from an override of
     *                 {@link #resolveBounds()}
     * @param fallback the code texture used when the theme paints no background
     */
    WidgetPart(
        Widget owner,
        @Nullable String name,
        @Nullable Supplier<Rect> bounds,
        @Nullable Supplier<@Nullable VisualTexture> fallback
    ) {
        this.owner = owner;
        this.bounds = bounds;
        this.fallback = fallback;
        stylePart(name);
        setInteractive(false);
        useStyle(UIStyle.of(UIStyles.positionAbsolute()));
        onLayout(this::resolveLayout);
    }

    // region geometry

    /**
     * The rect this part should occupy — read at layout time and again right
     * before the owner renders its children. The default reads the owner's
     * supplier; a compound with more parts than roles (a segmented bar) overrides
     * it to derive each part's rect from its own state.
     */
    protected Rect resolveBounds() {
        return bounds != null ? bounds.get() : Rect.empty;
    }

    private void resolveLayout(Widget widget, LayoutScope scope) {
        Rect rect = resolveBounds();
        applied = rect;
        scope.setBounds(rect.x(), rect.y(), rect.width(), rect.height());
    }

    /**
     * Applies the owner's current rect when it moved since the last pass — the
     * same writes {@code applyLayout} performs, so a viewport set here and a
     * viewport set there never disagree.
     */
    void syncBounds() {
        Rect rect = resolveBounds();
        if (rect.equals(applied)) {
            return;
        }
        applied = rect;
        viewport().setLayout(rect.x(), rect.y());
        viewport().setViewportSize(rect.width(), rect.height());
        setSize(rect.width(), rect.height());
    }

    /** Re-applies every part's owner-computed bounds before the owner renders them. */
    static void syncAll(CompositeWidget<?> owner) {
        for (Widget child : owner.children()) {
            if (child instanceof WidgetPart part) {
                part.syncBounds();
            }
        }
    }

    /** Marks every part of {@code owner} style-dirty — the state mirror runs both ways. */
    static void markStyleDirtyAll(CompositeWidget<?> owner) {
        for (Widget child : owner.children()) {
            child.markStyleDirty();
        }
    }

    // endregion

    // region selector surface

    @Override
    public String styleTag() {
        return owner.styleTag();
    }

    @Override
    public Set<String> styleStates() {
        Set<String> states = new HashSet<>(owner.styleStates());
        if (hovered()) {
            states.add("hovered");
            states.add("hover");
        }
        if (hasFocus()) {
            states.add("focused");
        }
        return states;
    }

    // endregion

    // region rendering

    /** The themed background when the theme paints this part, else the owner's code texture. */
    protected @Nullable VisualTexture texture() {
        VisualTexture themed = style().visualContext().background();
        if (themed != null && !themed.isEmpty()) {
            return themed;
        }
        return fallback != null ? fallback.get() : null;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        VisualTexture texture = texture();
        if (texture == null || texture.isEmpty()) {
            return;
        }
        // an empty part (a zero-progress fill) paints nothing at all
        if (width() <= 0 || height() <= 0) {
            return;
        }
        canvas.texture(texture, 0, 0, width(), height());
    }

    // endregion
}
