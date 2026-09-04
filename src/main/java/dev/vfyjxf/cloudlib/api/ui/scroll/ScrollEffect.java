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
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

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
 * Mouse wheel scrolling, scrollbar dragging, optional wheel acceleration, and optional
 * middle-mouse auto-scroll are handled by the effect. The {@link ScrollState} configures
 * both position and behavior.
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
 * // 3. Optional behavior tuning
 * state.wheelAcceleration(true)
 *      .middleMouseAutoScroll(true);
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

    //region auto-scroll state

    private boolean autoScrolling = false;
    private double autoScrollAnchorX = 0;
    private double autoScrollAnchorY = 0;

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
            if (!state.enabled()) {
                stopAutoScroll();
                return EventDispatch.pass;
            }
            if (input.isMiddleClick()) {
                return handleMiddleMouseAutoScroll(composite, input.mouseX(), input.mouseY());
            }
            if (autoScrolling && input.isMouse()) {
                stopAutoScroll();
                return EventDispatch.consumed;
            }
            if (input.isMouse()) {
                stopScrollbarDrag();
            }
            if (!state.draggable() || !state.showScrollbar() || !input.isLeftClick()) {
                return EventDispatch.pass;
            }
            return handleMousePressed(composite, input.mouseX(), input.mouseY());
        });

        widget.onMouseDragged((input, deltaX, deltaY, context) -> {
            if (!state.enabled()) {
                draggingVertical = false;
                draggingHorizontal = false;
                return EventDispatch.pass;
            }
            if (!draggingVertical && !draggingHorizontal) {
                return EventDispatch.pass;
            }
            if (!input.isLeftClick() || !leftMouseButtonDown()) {
                stopScrollbarDrag();
                return EventDispatch.pass;
            }
            return handleMouseDragged(composite, input.mouseX(), input.mouseY());
        });

        widget.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            if (!state.enabled()) {
                return EventDispatch.pass;
            }
            boolean canScrollX = scrollX != 0 && state.canScrollHorizontally();
            boolean canScrollY = scrollY != 0 && state.canScrollVertically();
            if (!canScrollX && !canScrollY) {
                return EventDispatch.pass;
            }
            ScrollState.ScrollDelta delta = state.wheelScrollDelta(scrollX, scrollY, System.currentTimeMillis());
            state.scrollBy(delta.x(), delta.y());
            return EventDispatch.consumed;
        });


        widget.onMouseReleased((input, context) -> {
            if (!state.enabled()) {
                stopScrollbarDrag();
                stopAutoScroll();
                return EventDispatch.pass;
            }
            if (draggingVertical || draggingHorizontal) {
                stopScrollbarDrag();
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });

        // Intercept the render pipeline to add scroll clipping and scrollbar rendering.
        // We cancel the default render and take full control.
        widget.onRender((canvas, mouseX, mouseY, partialTicks, self, context) -> {
            if (!state.enabled()) {
                return;
            }
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
            ScrollbarTrack track = verticalTrack(width, height, hasHorizontal);
            int thumbHeight = computeVerticalThumbHeight(track.length());
            int maxThumbY = track.length() - thumbHeight;
            int thumbY = track.start() + (int) (state.scrollProgressY() * maxThumbY);

            if (localX >= track.cross() && localX < track.cross() + barWidth && localY >= thumbY && localY < thumbY + thumbHeight) {
                draggingVertical = true;
                dragOffsetY = localY - thumbY;
                return EventDispatch.consumed;
            }

            // Click on track (not thumb) — jump to that position
            if (localX >= track.cross() && localX < track.cross() + barWidth && localY >= track.start() && localY < track.start() + track.length()) {
                float progress = (float) (localY - track.start() - thumbHeight * 0.5) / Math.max(1, track.length() - thumbHeight);
                progress = Math.clamp(progress, 0f, 1f);
                state.jumpTo(state.scrollX(), progress * state.maxScrollY());
                return EventDispatch.consumed;
            }
        }

        // Check horizontal scrollbar thumb
        if (hasHorizontal) {
            ScrollbarTrack track = horizontalTrack(width, height, hasVertical);
            int thumbWidth = computeHorizontalThumbWidth(track.length());
            int maxThumbX = track.length() - thumbWidth;
            int thumbX = track.start() + (int) (state.scrollProgressX() * maxThumbX);

            if (localY >= track.cross() && localY < track.cross() + barWidth && localX >= thumbX && localX < thumbX + thumbWidth) {
                draggingHorizontal = true;
                dragOffsetX = localX - thumbX;
                return EventDispatch.consumed;
            }

            // Click on track — jump
            if (localY >= track.cross() && localY < track.cross() + barWidth && localX >= track.start() && localX < track.start() + track.length()) {
                float progress = (float) (localX - track.start() - thumbWidth * 0.5) / Math.max(1, track.length() - thumbWidth);
                progress = Math.clamp(progress, 0f, 1f);
                state.jumpTo(progress * state.maxScrollX(), state.scrollY());
                return EventDispatch.consumed;
            }
        }

        return EventDispatch.pass;
    }

    private void stopScrollbarDrag() {
        draggingVertical = false;
        draggingHorizontal = false;
        dragOffsetY = 0;
        dragOffsetX = 0;
    }

    private static boolean leftMouseButtonDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
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

        if (draggingVertical && hasVertical) {
            ScrollbarTrack track = verticalTrack(width, height, hasHorizontal);
            int thumbHeight = computeVerticalThumbHeight(track.length());
            int maxThumbY = track.length() - thumbHeight;
            if (maxThumbY > 0) {
                float thumbTop = (float) (localY - dragOffsetY - track.start());
                float progress = Math.clamp(thumbTop / maxThumbY, 0f, 1f);
                state.jumpTo(state.scrollX(), progress * state.maxScrollY());
            }
            return EventDispatch.consumed;
        }

        if (draggingHorizontal && hasHorizontal) {
            ScrollbarTrack track = horizontalTrack(width, height, hasVertical);
            int thumbWidth = computeHorizontalThumbWidth(track.length());
            int maxThumbX = track.length() - thumbWidth;
            if (maxThumbX > 0) {
                float thumbLeft = (float) (localX - dragOffsetX - track.start());
                float progress = Math.clamp(thumbLeft / maxThumbX, 0f, 1f);
                state.jumpTo(progress * state.maxScrollX(), state.scrollY());
            }
            return EventDispatch.consumed;
        }

        return EventDispatch.pass;
    }

    private int computeVerticalThumbHeight(int barHeight) {
        if (barHeight <= 0 || state.contentHeight() <= 0) return Math.max(0, barHeight);
        return Math.min(barHeight, Math.max(state.minThumbSize(), (int) ((float) state.viewportHeight() / state.contentHeight() * barHeight)));
    }

    private int computeHorizontalThumbWidth(int barLength) {
        if (barLength <= 0 || state.contentWidth() <= 0) return Math.max(0, barLength);
        return Math.min(barLength, Math.max(state.minThumbSize(), (int) ((float) state.viewportWidth() / state.contentWidth() * barLength)));
    }

    //endregion

    //region rendering

    private void renderScrollable(SceneCanvas canvas, CompositeWidget<?> composite, int mouseX, int mouseY, float partialTicks) {
        int width = composite.width();
        int height = composite.height();
        Insets insets = effectiveInsets(width, height);
        int viewportWidth = Math.max(0, width - insets.left() - insets.right());
        int viewportHeight = Math.max(0, height - insets.top() - insets.bottom());

        // Update viewport dimensions
        state.updateViewport(viewportWidth, viewportHeight);

        // Auto-compute content size from children bounds
        if (state.autoContentSize()) {
            measureContentSize(composite);
        }

        // Advance smooth scroll animation
        float oldScrollX = state.scrollX();
        float oldScrollY = state.scrollY();
        advanceAutoScroll(mouseX, mouseY);
        state.animate();

        float scrollX = state.scrollX();
        float scrollY = state.scrollY();
        float oldOffsetX = composite.viewport().contentOffsetX();
        float oldOffsetY = composite.viewport().contentOffsetY();

        // Update content offset so coordinate transforms (sceneToLocal, localToScene)
        // and hit testing correctly account for the scroll position.
        // This does NOT move the container — only shifts the children's coordinate space.
        composite.viewport().setContentOffset(scrollX, scrollY);
        if (scrollX != oldScrollX || scrollY != oldScrollY || scrollX != oldOffsetX || scrollY != oldOffsetY) {
            FloatPos sceneMouse = composite.localToScene(mouseX, mouseY);
            requestHoverRefresh(composite, sceneMouse.x, sceneMouse.y);
        }

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

        canvas.pushClip(absX + insets.left(), absY + insets.top(), viewportWidth, viewportHeight);
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
        if (autoScrolling) {
            renderAutoScrollAnchor(canvas, width, height);
        }
    }

    private void requestHoverRefresh(CompositeWidget<?> composite, double mouseX, double mouseY) {
        if (!composite.lifecycle().mounted()) {
            return;
        }
        composite.scene().requestHoverRefresh(mouseX, mouseY);
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
        ScrollbarTrack track = verticalTrack(viewWidth, viewHeight, hasHorizontal);
        if (track.length() <= 0) {
            return;
        }

        // Track
        canvas.texture(state.trackTexture(), track.cross(), track.start(), barWidth, track.length());

        // Thumb
        int thumbHeight = computeVerticalThumbHeight(track.length());
        int maxThumbY = track.length() - thumbHeight;
        int thumbY = track.start() + (int) (state.scrollProgressY() * maxThumbY);

        canvas.texture(state.thumbTexture(), track.cross(), thumbY, barWidth, thumbHeight);
    }

    private void renderHorizontalScrollbar(SceneCanvas canvas, int viewWidth, int viewHeight, boolean hasVertical) {
        int barWidth = state.scrollbarWidth();
        ScrollbarTrack track = horizontalTrack(viewWidth, viewHeight, hasVertical);
        if (track.length() <= 0) {
            return;
        }

        // Track
        canvas.texture(state.trackTexture(), track.start(), track.cross(), track.length(), barWidth);

        // Thumb
        int thumbWidth = computeHorizontalThumbWidth(track.length());
        int maxThumbX = track.length() - thumbWidth;
        int thumbX = track.start() + (int) (state.scrollProgressX() * maxThumbX);

        canvas.texture(state.thumbTexture(), thumbX, track.cross(), thumbWidth, barWidth);
    }

    private ScrollbarTrack verticalTrack(int width, int height, boolean hasHorizontal) {
        Insets insets = effectiveInsets(width, height);
        int barWidth = state.scrollbarWidth();
        int x = Math.max(0, width - insets.right() - barWidth);
        int y = insets.top();
        int reservedBottom = insets.bottom() + (hasHorizontal ? barWidth : 0);
        int length = Math.max(0, height - insets.top() - reservedBottom);
        return new ScrollbarTrack(x, y, length);
    }

    private ScrollbarTrack horizontalTrack(int width, int height, boolean hasVertical) {
        Insets insets = effectiveInsets(width, height);
        int barWidth = state.scrollbarWidth();
        int x = insets.left();
        int y = Math.max(0, height - insets.bottom() - barWidth);
        int reservedRight = insets.right() + (hasVertical ? barWidth : 0);
        int length = Math.max(0, width - insets.left() - reservedRight);
        return new ScrollbarTrack(y, x, length);
    }

    private Insets effectiveInsets(int width, int height) {
        int left = Math.min(state.viewportInsetLeft(), Math.max(0, width));
        int right = Math.min(state.viewportInsetRight(), Math.max(0, width - left));
        int top = Math.min(state.viewportInsetTop(), Math.max(0, height));
        int bottom = Math.min(state.viewportInsetBottom(), Math.max(0, height - top));
        return new Insets(top, right, bottom, left);
    }

    //endregion

    //region middle mouse auto-scroll

    private EventDispatch handleMiddleMouseAutoScroll(CompositeWidget<?> composite, double sceneX, double sceneY) {
        if (autoScrolling) {
            stopAutoScroll();
            return EventDispatch.consumed;
        }
        if (!state.middleMouseAutoScroll()) {
            return EventDispatch.pass;
        }
        FloatPos local = composite.sceneToLocal(sceneX, sceneY);
        return beginAutoScroll(local.x, local.y) ? EventDispatch.consumed : EventDispatch.pass;
    }

    boolean beginAutoScroll(double localX, double localY) {
        if (!state.enabled() || !state.middleMouseAutoScroll() || !canAutoScroll()) {
            return false;
        }
        autoScrolling = true;
        autoScrollAnchorX = localX;
        autoScrollAnchorY = localY;
        return true;
    }

    void advanceAutoScroll(double localMouseX, double localMouseY) {
        if (!autoScrolling) {
            return;
        }
        if (!state.enabled() || !state.middleMouseAutoScroll() || !canAutoScroll()) {
            stopAutoScroll();
            return;
        }

        float deltaX = state.canScrollHorizontally() ? state.autoScrollDelta(localMouseX - autoScrollAnchorX) : 0.0f;
        float deltaY = state.canScrollVertically() ? state.autoScrollDelta(localMouseY - autoScrollAnchorY) : 0.0f;
        if (deltaX != 0.0f || deltaY != 0.0f) {
            state.scrollBy(deltaX, deltaY);
        }
    }

    void stopAutoScroll() {
        autoScrolling = false;
    }

    boolean autoScrolling() {
        return autoScrolling;
    }

    private boolean canAutoScroll() {
        return state.canScrollHorizontally() || state.canScrollVertically();
    }

    private void renderAutoScrollAnchor(SceneCanvas canvas, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int centerX = Math.clamp((int) Math.round(autoScrollAnchorX), 0, Math.max(0, width - 1));
        int centerY = Math.clamp((int) Math.round(autoScrollAnchorY), 0, Math.max(0, height - 1));
        int left = Math.max(0, centerX - 5);
        int right = Math.min(width - 1, centerX + 5);
        int top = Math.max(0, centerY - 5);
        int bottom = Math.min(height - 1, centerY + 5);

        canvas.fill(left, centerY, right - left + 1, 1, 0xCCFFFFFF);
        canvas.fill(centerX, top, 1, bottom - top + 1, 0xCCFFFFFF);
        canvas.fill(left, top, right - left + 1, 1, 0x99000000);
        canvas.fill(left, bottom, right - left + 1, 1, 0x99000000);
        canvas.fill(left, top, 1, bottom - top + 1, 0x99000000);
        canvas.fill(right, top, 1, bottom - top + 1, 0x99000000);
    }

    //endregion

    private record Insets(int top, int right, int bottom, int left) {
    }

    private record ScrollbarTrack(int cross, int start, int length) {
    }
}
