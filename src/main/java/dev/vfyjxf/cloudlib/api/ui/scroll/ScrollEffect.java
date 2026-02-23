package dev.vfyjxf.cloudlib.api.ui.scroll;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.effect.Effect;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.ScrollbarStyleProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.tree.Layout;

/**
 * An {@link Effect} that adds scrolling behavior to any {@link CompositeWidget}.
 * <p>
 * When applied, this effect intercepts the widget's rendering pipeline to:
 * <ul>
 *   <li>Automatically compute content bounds from children (unless {@link ScrollState#autoContentSize(boolean)} is off)</li>
 *   <li>Clip the content area to the widget's bounds</li>
 *   <li>Translate the content by the scroll offset</li>
 *   <li>Drive smooth scroll animation each frame</li>
 *   <li>Render scrollbar overlays when content overflows</li>
 * </ul>
 * <p>
 * Scroll events (mouse wheel, drag) should be handled by the widget itself using
 * its own event listeners. The {@link ScrollState} can be obtained from the effect
 * and used in those listeners to update the scroll position.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // 1. Create scroll state — no need to set content size, it's auto-computed
 * ScrollState state = ScrollState.create(ScrollDirection.VERTICAL)
 *     .scrollSpeed(12)
 *     .smooth(true);
 *
 * // 2. Apply scroll effect to a composite widget
 * WidgetGroup<Widget> list = WidgetGroup.create();
 * list.useEffect(ScrollEffect.of(state));
 *
 * // 3. The widget handles its own scroll events
 * list.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
 *     state.scrollBy(0, (float) (-scrollY * state.scrollSpeed()));
 *     return EventDispatch.consumed;
 * });
 * }</pre>
 *
 * <h3>Scrollbar Customization</h3>
 * Scrollbar appearance is configured through {@link ScrollState}:
 * <pre>{@code
 * state.trackTexture(new ColorTexture(0x40000000))
 *      .thumbTexture(new NineSliceTexture(...))
 *      .scrollbarWidth(8)
 *      .minThumbSize(16);
 * }</pre>
 *
 * @see ScrollState
 * @see ScrollDirection
 */
public final class ScrollEffect implements Effect {

    private final ScrollState state;

    //region drag state

    /**
     * Whether the user is currently dragging the vertical scrollbar thumb.
     */
    private boolean draggingVertical = false;
    /**
     * Whether the user is currently dragging the horizontal scrollbar thumb.
     */
    private boolean draggingHorizontal = false;
    /**
     * The mouse Y offset within the thumb when drag started (vertical).
     */
    private double dragOffsetY = 0;
    /**
     * The mouse X offset within the thumb when drag started (horizontal).
     */
    private double dragOffsetX = 0;

    //endregion

    //region factory

    /**
     * Creates a scroll effect with the given state.
     *
     * @param state the scroll state to use
     * @return a new scroll effect
     */
    public static ScrollEffect of(ScrollState state) {
        return new ScrollEffect(state);
    }

    /**
     * Creates a scroll effect with a new default state for the given direction.
     *
     * @param direction the scroll direction
     * @return a new scroll effect
     */
    public static ScrollEffect of(ScrollDirection direction) {
        return new ScrollEffect(ScrollState.create(direction));
    }

    /**
     * Creates a vertical scroll effect with a new default state.
     *
     * @return a new vertical scroll effect
     */
    public static ScrollEffect vertical() {
        return of(ScrollDirection.vertical);
    }

    /**
     * Creates a horizontal scroll effect with a new default state.
     *
     * @return a new horizontal scroll effect
     */
    public static ScrollEffect horizontal() {
        return of(ScrollDirection.horizontal);
    }

    private ScrollEffect(ScrollState state) {
        this.state = state;
    }

    //endregion

    /**
     * Returns the scroll state managed by this effect.
     */
    public ScrollState state() {
        return state;
    }

    @Override
    public void apply(Widget widget) {
        if (!(widget instanceof CompositeWidget<?> composite)) {
            throw new IllegalArgumentException(
                    "ScrollEffect can only be applied to CompositeWidget, got: " + widget.getClass().getSimpleName()
            );
        }

        // Set overflow: SCROLL on the container so Taffy tracks content overflow correctly
        // and reserves scrollbar space. Also set flex-shrink: 0 on all children so they
        // maintain their natural sizes and overflow the container instead of being compressed.
        ScrollDirection dir = state.direction();
        if (dir.allowsVertical() && dir.allowsHorizontal()) {
            widget.useStyle(UIStyle.of(UIStyles.overflow(Overflow.SCROLL)));
        } else if (dir.allowsVertical()) {
            widget.useStyle(UIStyle.of(UIStyles.overflowY(Overflow.SCROLL)));
        } else {
            widget.useStyle(UIStyle.of(UIStyles.overflowX(Overflow.SCROLL)));
        }

        // Prevent children from shrinking — this is required for scroll to work.
        // Without flex-shrink: 0, Taffy compresses children to fit the viewport,
        // making contentSize == viewportSize and maxScroll == 0.
        UIStyle noShrink = UIStyle.of(UIStyles.flexShrink(0));
        for (Widget child : composite.children()) {
            child.useStyle(noShrink);
        }

        // Also apply flex-shrink: 0 to any children added later
        widget.events().register(WidgetEvent.onChildAdded, (child, context) -> {
            child.useStyle(noShrink);
        });

        // On mount, apply any ScrollbarStyleProperty from the widget's style context
        widget.onMount((scene, context, handle) -> {
            ScrollbarStyleProperty.ScrollbarStyleData styleData = ScrollbarStyleProperty.getFrom(widget);
            if (styleData != null) {
                styleData.applyTo(state);
            }
        });

        // Scrollbar thumb drag support
        widget.onMouseClicked((input, context) -> {
            if (!state.draggable() || !state.showScrollbar() || !input.isLeftClick()) {
                return EventDispatch.pass;
            }
            return handleMousePressed(composite, input.mouseX(), input.mouseY());
        });

        widget.onMouseDragged((input, deltaX, deltaY, context) -> {
            if (!draggingVertical && !draggingHorizontal) {
                return EventDispatch.pass;
            }
            return handleMouseDragged(composite, input.mouseX(), input.mouseY());
        });

        widget.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            state.scrollBy(0, (float) (-scrollY * state.scrollSpeed()));
            return EventDispatch.consumed;
        });


        widget.onMouseReleased((input, context) -> {
            if (draggingVertical || draggingHorizontal) {
                draggingVertical = false;
                draggingHorizontal = false;
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });

        // Intercept the render pipeline to add scroll clipping and scrollbar rendering.
        // We cancel the default render and take full control.
        widget.onRender((canvas, mouseX, mouseY, partialTicks, self, context) -> {
            context.cancel();
            renderScrollable(canvas, composite, mouseX, mouseY, partialTicks);
        });
    }

    //region content measurement

    /**
     * Reads the content size from the Taffy {@link Layout} record.
     * <p>
     * With {@code overflow: SCROLL} set on the container and {@code flex-shrink: 0}
     * on all children, Taffy computes {@link Layout#contentSize()} as the actual
     * bounding extent of all children — which may exceed the container's viewport size.
     * <p>
     * Falls back to iterating children bounds if the layout is not yet available.
     */
    private void measureContentSize(CompositeWidget<?> composite) {
        try {
            Layout layout = composite.layout();
            FloatSize contentSize = layout.contentSize();
            if (contentSize != null && !Float.isNaN(contentSize.width) && !Float.isNaN(contentSize.height)) {
                // contentSize reports the content box extent; add padding/border back
                // since viewport (widget.width/height) includes padding+border
                int contentWidth = (int) Math.ceil(contentSize.width + layout.padding().left + layout.padding().right
                        + layout.border().left + layout.border().right);
                int contentHeight = (int) Math.ceil(contentSize.height + layout.padding().top + layout.padding().bottom
                        + layout.border().top + layout.border().bottom);
                state.updateContentSize(contentWidth, contentHeight);
                return;
            }
        } catch (Exception ignored) {
            // Layout not yet applied — fall through to manual measurement
        }

        // Fallback: measure from children positions
        int maxRight = 0;
        int maxBottom = 0;
        for (Widget child : composite.children()) {
            if (!child.visible()) continue;
            int right = child.posX() + child.width();
            int bottom = child.posY() + child.height();
            if (right > maxRight) maxRight = right;
            if (bottom > maxBottom) maxBottom = bottom;
        }
        state.updateContentSize(maxRight, maxBottom);
    }

    //endregion

    //region scrollbar drag

    /**
     * Handles mouse press — checks if the press is within a scrollbar thumb and starts dragging.
     */
    private EventDispatch handleMousePressed(CompositeWidget<?> composite, double mouseX, double mouseY) {
        int width = composite.width();
        int height = composite.height();

        // Convert scene-space mouse coordinates to widget-local coordinates
        FloatPos local = composite.sceneToLocal(mouseX, mouseY);
        double localX = local.x;
        double localY = local.y;

        boolean hasVertical = state.canScrollVertically();
        boolean hasHorizontal = state.canScrollHorizontally();
        int barWidth = state.scrollbarWidth();

        // Check vertical scrollbar thumb
        if (hasVertical) {
            int barX = width - barWidth;
            int barHeight = height - (hasHorizontal ? barWidth : 0);
            int thumbHeight = computeVerticalThumbHeight(height, barHeight);
            int maxThumbY = barHeight - thumbHeight;
            int thumbY = (int) (state.scrollProgressY() * maxThumbY);

            if (localX >= barX && localX < width && localY >= thumbY && localY < thumbY + thumbHeight) {
                draggingVertical = true;
                dragOffsetY = localY - thumbY;
                return EventDispatch.consumed;
            }

            // Click on track (not thumb) — jump to that position
            if (localX >= barX && localX < width && localY >= 0 && localY < barHeight) {
                float progress = (float) (localY - thumbHeight * 0.5) / (barHeight - thumbHeight);
                progress = Math.clamp(progress, 0f, 1f);
                state.jumpTo(state.scrollX(), progress * state.maxScrollY());
                return EventDispatch.consumed;
            }
        }

        // Check horizontal scrollbar thumb
        if (hasHorizontal) {
            int barY = height - barWidth;
            int barLength = width - (hasVertical ? barWidth : 0);
            int thumbWidth = computeHorizontalThumbWidth(width, barLength);
            int maxThumbX = barLength - thumbWidth;
            int thumbX = (int) (state.scrollProgressX() * maxThumbX);

            if (localY >= barY && localY < height && localX >= thumbX && localX < thumbX + thumbWidth) {
                draggingHorizontal = true;
                dragOffsetX = localX - thumbX;
                return EventDispatch.consumed;
            }

            // Click on track — jump
            if (localY >= barY && localY < height && localX >= 0 && localX < barLength) {
                float progress = (float) (localX - thumbWidth * 0.5) / (barLength - thumbWidth);
                progress = Math.clamp(progress, 0f, 1f);
                state.jumpTo(progress * state.maxScrollX(), state.scrollY());
                return EventDispatch.consumed;
            }
        }

        return EventDispatch.pass;
    }

    /**
     * Handles mouse drag — updates scroll position based on thumb position.
     */
    private EventDispatch handleMouseDragged(CompositeWidget<?> composite, double mouseX, double mouseY) {
        int width = composite.width();
        int height = composite.height();

        // Convert scene-space mouse coordinates to widget-local coordinates
        FloatPos local = composite.sceneToLocal(mouseX, mouseY);
        double localX = local.x;
        double localY = local.y;

        boolean hasVertical = state.canScrollVertically();
        boolean hasHorizontal = state.canScrollHorizontally();
        int barWidth = state.scrollbarWidth();

        if (draggingVertical && hasVertical) {
            int barHeight = height - (hasHorizontal ? barWidth : 0);
            int thumbHeight = computeVerticalThumbHeight(height, barHeight);
            int maxThumbY = barHeight - thumbHeight;
            if (maxThumbY > 0) {
                float thumbTop = (float) (localY - dragOffsetY);
                float progress = Math.clamp(thumbTop / maxThumbY, 0f, 1f);
                state.jumpTo(state.scrollX(), progress * state.maxScrollY());
            }
            return EventDispatch.consumed;
        }

        if (draggingHorizontal && hasHorizontal) {
            int barLength = width - (hasVertical ? barWidth : 0);
            int thumbWidth = computeHorizontalThumbWidth(width, barLength);
            int maxThumbX = barLength - thumbWidth;
            if (maxThumbX > 0) {
                float thumbLeft = (float) (localX - dragOffsetX);
                float progress = Math.clamp(thumbLeft / maxThumbX, 0f, 1f);
                state.jumpTo(progress * state.maxScrollX(), state.scrollY());
            }
            return EventDispatch.consumed;
        }

        return EventDispatch.pass;
    }

    private int computeVerticalThumbHeight(int viewHeight, int barHeight) {
        if (state.contentHeight() <= 0) return barHeight;
        return Math.max(state.minThumbSize(), (int) ((float) viewHeight / state.contentHeight() * barHeight));
    }

    private int computeHorizontalThumbWidth(int viewWidth, int barLength) {
        if (state.contentWidth() <= 0) return barLength;
        return Math.max(state.minThumbSize(), (int) ((float) viewWidth / state.contentWidth() * barLength));
    }

    //endregion

    //region rendering

    private void renderScrollable(SceneCanvas canvas, CompositeWidget<?> composite, int mouseX, int mouseY, float partialTicks) {
        int width = composite.width();
        int height = composite.height();

        // Update viewport dimensions
        state.updateViewport(width, height);

        // Auto-compute content size from children bounds
        if (state.autoContentSize()) {
            measureContentSize(composite);
        }

        // Advance smooth scroll animation
        state.animate();

        float scrollX = state.scrollX();
        float scrollY = state.scrollY();

        // Update content offset so coordinate transforms (sceneToLocal, localToScene)
        // and hit testing correctly account for the scroll position.
        // This does NOT move the container — only shifts the children's coordinate space.
        composite.viewport().setContentOffset(scrollX, scrollY);

        // 1. Render widget background and icon (unscrolled, in container's local space)
        VisualTexture background = composite.style().visualContext().background();
        VisualTexture icon = composite.style().visualContext().icon();
        canvas.texture(background, 0, 0, width, height);
        canvas.texture(icon, 0, 0, width, height);

        // 2. Render children with scroll clipping
        // pushClip needs absolute screen coords
        FloatPos sceneOrigin = composite.localToScene(0, 0);
        int absX = (int) sceneOrigin.x;
        int absY = (int) sceneOrigin.y;

        canvas.pushClip(absX, absY, width, height);
        // Push scroll translation: children are rendered shifted by (-scrollX, -scrollY)
        canvas.pushTransform();
        canvas.translate(-scrollX, -scrollY);
        // Mouse coords in content space = local mouse + scroll offset
        int contentMouseX = (int) (mouseX + scrollX);
        int contentMouseY = (int) (mouseY + scrollY);
        canvas.renderChildren(composite.children(), contentMouseX, contentMouseY, partialTicks);
        canvas.popTransform();
        canvas.popClip();

        // 3. Render scrollbars (unscrolled, on top)
        if (state.showScrollbar()) {
            renderScrollbars(canvas, width, height);
        }
    }

    private void renderScrollbars(SceneCanvas canvas, int viewWidth, int viewHeight) {
        boolean hasVertical = state.canScrollVertically();
        boolean hasHorizontal = state.canScrollHorizontally();

        if (hasVertical) {
            renderVerticalScrollbar(canvas, viewWidth, viewHeight, hasHorizontal);
        }

        if (hasHorizontal) {
            renderHorizontalScrollbar(canvas, viewWidth, viewHeight, hasVertical);
        }
    }

    private void renderVerticalScrollbar(SceneCanvas canvas, int viewWidth, int viewHeight, boolean hasHorizontal) {
        int barWidth = state.scrollbarWidth();
        int barX = viewWidth - barWidth;
        int barHeight = viewHeight - (hasHorizontal ? barWidth : 0);

        // Track
        canvas.texture(state.trackTexture(), barX, 0, barWidth, barHeight);

        // Thumb
        int thumbHeight = Math.max(
                state.minThumbSize(),
                (int) ((float) viewHeight / state.contentHeight() * barHeight)
        );
        int maxThumbY = barHeight - thumbHeight;
        int thumbY = (int) (state.scrollProgressY() * maxThumbY);

        canvas.texture(state.thumbTexture(), barX, thumbY, barWidth, thumbHeight);
    }

    private void renderHorizontalScrollbar(SceneCanvas canvas, int viewWidth, int viewHeight, boolean hasVertical) {
        int barWidth = state.scrollbarWidth();
        int barY = viewHeight - barWidth;
        int barLength = viewWidth - (hasVertical ? barWidth : 0);

        // Track
        canvas.texture(state.trackTexture(), 0, barY, barLength, barWidth);

        // Thumb
        int thumbWidth = Math.max(
                state.minThumbSize(),
                (int) ((float) viewWidth / state.contentWidth() * barLength)
        );
        int maxThumbX = barLength - thumbWidth;
        int thumbX = (int) (state.scrollProgressX() * maxThumbX);

        canvas.texture(state.thumbTexture(), thumbX, barY, thumbWidth, barWidth);
    }

    //endregion
}
