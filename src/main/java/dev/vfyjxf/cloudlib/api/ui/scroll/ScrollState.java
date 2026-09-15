package dev.vfyjxf.cloudlib.api.ui.scroll;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;

/**
 * Mutable state container for scroll position and configuration.
 * <p>
 * {@code ScrollState} manages scroll offsets, content dimensions, scroll speed,
 * scrollbar visibility, textures, and smooth scrolling. It is designed to be
 * attached to any {@link CompositeWidget CompositeWidget}
 * via {@link ScrollEffect}.
 * <p>
 * Content size is automatically computed from children bounds by default.
 * Use {@link #autoContentSize(boolean)} to disable auto-computation and set
 * content size manually if needed.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ScrollState state = ScrollState.create(ScrollDirection.VERTICAL)
 *     .scrollSpeed(12)
 *     .smooth(true)
 *     .smoothSpeed(0.3f)
 *     .thumbTexture(new ColorTexture(0xFFAAAAAA))
 *     .trackTexture(new ColorTexture(0x80000000));
 * }</pre>
 *
 * @see ScrollEffect
 * @see ScrollDirection
 */
public final class ScrollState {

    //region defaults

    private static final int DEFAULT_SCROLL_SPEED = 10;
    private static final int DEFAULT_SCROLLBAR_WIDTH = 6;
    private static final int DEFAULT_MIN_THUMB_SIZE = 20;
    private static final float DEFAULT_SMOOTH_SPEED = 0.35f;
    private static final float DEFAULT_WHEEL_ACCELERATION_STEP = 0.35f;
    private static final float DEFAULT_WHEEL_ACCELERATION_MAX_MULTIPLIER = 4.0f;
    private static final long DEFAULT_WHEEL_ACCELERATION_RESET_MILLIS = 180L;
    private static final float LONG_CONTENT_WHEEL_ACCELERATION_STEP = 0.45f;
    private static final float LONG_CONTENT_WHEEL_ACCELERATION_MAX_MULTIPLIER = 6.0f;
    private static final long LONG_CONTENT_WHEEL_ACCELERATION_RESET_MILLIS = 220L;
    private static final float DEFAULT_AUTO_SCROLL_DEAD_ZONE = 6.0f;
    private static final float DEFAULT_AUTO_SCROLL_SPEED = 0.35f;
    private static final float DEFAULT_AUTO_SCROLL_MAX_SPEED = 36.0f;
    private static final float SNAP_THRESHOLD = 0.5f;
    private static final VisualTexture DEFAULT_TRACK = new ColorTexture(0x80000000);
    private static final VisualTexture DEFAULT_THUMB = new ColorTexture(0xFFAAAAAA);

    //endregion

    //region scroll state

    private ScrollDirection direction = ScrollDirection.vertical;

    /**
     * The actual (rendered) scroll position, interpolated toward target when smooth scrolling.
     */
    private float scrollX = 0;
    private float scrollY = 0;

    /**
     * The target scroll position that we're scrolling toward.
     * When smooth scrolling is off, actual = target immediately.
     */
    private float targetScrollX = 0;
    private float targetScrollY = 0;

    //endregion

    //region content size

    private int contentWidth = 0;
    private int contentHeight = 0;
    private boolean autoContentSize = true;

    //endregion

    //region config

    private int scrollSpeed = DEFAULT_SCROLL_SPEED;

    //endregion

    //region wheel acceleration

    private boolean wheelAcceleration = false;
    private float wheelAccelerationStep = DEFAULT_WHEEL_ACCELERATION_STEP;
    private float wheelAccelerationMaxMultiplier = DEFAULT_WHEEL_ACCELERATION_MAX_MULTIPLIER;
    private long wheelAccelerationResetMillis = DEFAULT_WHEEL_ACCELERATION_RESET_MILLIS;
    private float wheelAccelerationMultiplier = 1.0f;
    private long wheelAccelerationLastMillis = Long.MIN_VALUE;
    private int wheelAccelerationLastDirectionX = 0;
    private int wheelAccelerationLastDirectionY = 0;

    //endregion

    //region middle mouse auto-scroll

    private boolean middleMouseAutoScroll = false;
    private float autoScrollDeadZone = DEFAULT_AUTO_SCROLL_DEAD_ZONE;
    private float autoScrollSpeed = DEFAULT_AUTO_SCROLL_SPEED;
    private float autoScrollMaxSpeed = DEFAULT_AUTO_SCROLL_MAX_SPEED;

    //endregion

    //region smooth scrolling

    private boolean smooth = true;
    private float smoothSpeed = DEFAULT_SMOOTH_SPEED;

    //endregion

    //region scrollbar config

    private boolean showScrollbar = true;
    private boolean enabled = true;
    private boolean draggable = true;
    private int scrollbarWidth = DEFAULT_SCROLLBAR_WIDTH;
    private int minThumbSize = DEFAULT_MIN_THUMB_SIZE;
    private VisualTexture trackTexture = DEFAULT_TRACK;
    private VisualTexture thumbTexture = DEFAULT_THUMB;
    private int viewportInsetTop = 0;
    private int viewportInsetRight = 0;
    private int viewportInsetBottom = 0;
    private int viewportInsetLeft = 0;

    //endregion

    //region viewport cache (set by ScrollEffect on render)

    private int viewportWidth = 0;
    private int viewportHeight = 0;

    //endregion

    //region factory

    /**
     * Creates a new {@code ScrollState} with default configuration.
     */
    public static ScrollState create() {
        return new ScrollState();
    }

    /**
     * Creates a new {@code ScrollState} with the given direction.
     */
    public static ScrollState create(ScrollDirection direction) {
        return new ScrollState().direction(direction);
    }

    private ScrollState() {
    }

    //endregion

    //region direction

    public ScrollDirection direction() {
        return direction;
    }

    public ScrollState direction(ScrollDirection direction) {
        this.direction = direction;
        return this;
    }

    //endregion

    //region scroll position

    /**
     * Returns the current (rendered) horizontal scroll offset.
     * When smooth scrolling, this is the interpolated value.
     */
    public float scrollX() {
        return scrollX;
    }

    /**
     * Returns the current (rendered) vertical scroll offset.
     * When smooth scrolling, this is the interpolated value.
     */
    public float scrollY() {
        return scrollY;
    }

    /**
     * Returns the target horizontal scroll offset.
     */
    public float targetScrollX() {
        return targetScrollX;
    }

    /**
     * Returns the target vertical scroll offset.
     */
    public float targetScrollY() {
        return targetScrollY;
    }

    /**
     * Sets the target horizontal scroll position.
     * When smooth scrolling is enabled, the actual position will animate toward this value.
     * When smooth scrolling is disabled, the actual position is set immediately.
     *
     * @param scrollX the target scroll X
     * @return this state for chaining
     */
    public ScrollState scrollX(float scrollX) {
        this.targetScrollX = clampX(scrollX);
        if (!smooth) {
            this.scrollX = this.targetScrollX;
        }
        return this;
    }

    /**
     * Sets the target vertical scroll position.
     * When smooth scrolling is enabled, the actual position will animate toward this value.
     * When smooth scrolling is disabled, the actual position is set immediately.
     *
     * @param scrollY the target scroll Y
     * @return this state for chaining
     */
    public ScrollState scrollY(float scrollY) {
        this.targetScrollY = clampY(scrollY);
        if (!smooth) {
            this.scrollY = this.targetScrollY;
        }
        return this;
    }

    /**
     * Scrolls by the given delta amounts, clamped to valid range.
     * Modifies the target position; when smooth scrolling is enabled,
     * the actual position will animate toward the new target.
     *
     * @param deltaX horizontal scroll delta
     * @param deltaY vertical scroll delta
     * @return this state for chaining
     */
    public ScrollState scrollBy(float deltaX, float deltaY) {
        if (direction.allowsHorizontal()) {
            this.targetScrollX = clampX(this.targetScrollX + deltaX);
        }
        if (direction.allowsVertical()) {
            this.targetScrollY = clampY(this.targetScrollY + deltaY);
        }
        if (!smooth) {
            this.scrollX = this.targetScrollX;
            this.scrollY = this.targetScrollY;
        }
        return this;
    }

    /**
     * Resets scroll position to origin (0, 0) immediately (no animation).
     */
    public ScrollState resetScroll() {
        this.scrollX = 0;
        this.scrollY = 0;
        this.targetScrollX = 0;
        this.targetScrollY = 0;
        return this;
    }

    /**
     * Jumps to the given scroll position immediately, bypassing smooth scrolling.
     *
     * @param x the horizontal scroll position
     * @param y the vertical scroll position
     * @return this state for chaining
     */
    public ScrollState jumpTo(float x, float y) {
        this.targetScrollX = clampX(x);
        this.targetScrollY = clampY(y);
        this.scrollX = this.targetScrollX;
        this.scrollY = this.targetScrollY;
        return this;
    }

    //endregion

    //region content size

    /**
     * Returns whether content size is automatically computed from children bounds.
     */
    public boolean autoContentSize() {
        return autoContentSize;
    }

    /**
     * Sets whether content size should be automatically computed from children bounds.
     * <p>
     * When enabled (default), {@link ScrollEffect} calculates the content bounding box
     * from all children's positions and sizes on every render frame.
     * When disabled, you must set content size manually via {@link #contentSize(int, int)}.
     *
     * @param auto true to enable auto-computation, false for manual mode
     * @return this state for chaining
     */
    public ScrollState autoContentSize(boolean auto) {
        this.autoContentSize = auto;
        return this;
    }

    public int contentWidth() {
        return contentWidth;
    }

    public int contentHeight() {
        return contentHeight;
    }

    /**
     * Manually sets the content size. Automatically disables {@link #autoContentSize()}.
     *
     * @param width  the content width
     * @param height the content height
     * @return this state for chaining
     */
    public ScrollState contentSize(int width, int height) {
        this.autoContentSize = false;
        this.contentWidth = width;
        this.contentHeight = height;
        reclamp();
        return this;
    }

    /**
     * Updates the content size. Called internally by {@link ScrollEffect} when
     * {@link #autoContentSize()} is enabled.
     *
     * @param width  the computed content width
     * @param height the computed content height
     */
    void updateContentSize(int width, int height) {
        this.contentWidth = width;
        this.contentHeight = height;
        reclamp();
    }

    //endregion

    //region scroll speed

    public int scrollSpeed() {
        return scrollSpeed;
    }

    public ScrollState scrollSpeed(int speed) {
        this.scrollSpeed = Math.max(1, speed);
        return this;
    }

    //endregion

    //region wheel acceleration

    public boolean wheelAcceleration() {
        return wheelAcceleration;
    }

    public ScrollState wheelAcceleration(boolean enabled) {
        if (this.wheelAcceleration != enabled) {
            resetWheelAcceleration();
        }
        this.wheelAcceleration = enabled;
        return this;
    }

    public float wheelAccelerationStep() {
        return wheelAccelerationStep;
    }

    public ScrollState wheelAccelerationStep(float step) {
        this.wheelAccelerationStep = Math.max(0.0f, step);
        return this;
    }

    public float wheelAccelerationMaxMultiplier() {
        return wheelAccelerationMaxMultiplier;
    }

    public ScrollState wheelAccelerationMaxMultiplier(float multiplier) {
        this.wheelAccelerationMaxMultiplier = Math.max(1.0f, multiplier);
        this.wheelAccelerationMultiplier = Math.min(this.wheelAccelerationMultiplier, this.wheelAccelerationMaxMultiplier);
        return this;
    }

    public long wheelAccelerationResetMillis() {
        return wheelAccelerationResetMillis;
    }

    public ScrollState wheelAccelerationResetMillis(long resetMillis) {
        this.wheelAccelerationResetMillis = Math.max(0L, resetMillis);
        return this;
    }

    /**
     * Enables wheel acceleration tuned for long, dense content such as item grids,
     * recipe databases, and layer lists.
     */
    public ScrollState longContentWheelAcceleration() {
        return wheelAcceleration(true)
                .wheelAccelerationStep(LONG_CONTENT_WHEEL_ACCELERATION_STEP)
                .wheelAccelerationMaxMultiplier(LONG_CONTENT_WHEEL_ACCELERATION_MAX_MULTIPLIER)
                .wheelAccelerationResetMillis(LONG_CONTENT_WHEEL_ACCELERATION_RESET_MILLIS);
    }

    public float currentWheelAccelerationMultiplier() {
        return wheelAccelerationMultiplier;
    }

    public ScrollDelta wheelDelta(double scrollX, double scrollY) {
        return wheelScrollDelta(scrollX, scrollY, System.currentTimeMillis());
    }

    public ScrollDelta wheelDelta(double scrollX, double scrollY, long nowMillis) {
        return wheelScrollDelta(scrollX, scrollY, nowMillis);
    }

    ScrollDelta wheelScrollDelta(double scrollX, double scrollY, long nowMillis) {
        float multiplier = advanceWheelAcceleration(scrollX, scrollY, nowMillis);
        return new ScrollDelta(
                (float) (-scrollX * scrollSpeed * multiplier),
                (float) (-scrollY * scrollSpeed * multiplier)
        );
    }

    float advanceWheelAcceleration(double scrollX, double scrollY, long nowMillis) {
        double magnitude = Math.max(Math.abs(scrollX), Math.abs(scrollY));
        if (!wheelAcceleration || magnitude <= 0.0) {
            resetWheelAcceleration();
            return 1.0f;
        }

        int directionX = sign(scrollX);
        int directionY = sign(scrollY);
        boolean first = wheelAccelerationLastMillis == Long.MIN_VALUE;
        boolean timedOut = !first && (nowMillis < wheelAccelerationLastMillis
                || nowMillis - wheelAccelerationLastMillis > wheelAccelerationResetMillis);
        boolean reversed = isDirectionReversed(directionX, directionY);

        if (first || timedOut || reversed) {
            wheelAccelerationMultiplier = 1.0f;
        } else {
            float step = wheelAccelerationStep * (float) Math.max(1.0, magnitude);
            wheelAccelerationMultiplier = Math.min(wheelAccelerationMaxMultiplier, wheelAccelerationMultiplier + step);
        }

        wheelAccelerationLastMillis = nowMillis;
        if (directionX != 0) wheelAccelerationLastDirectionX = directionX;
        if (directionY != 0) wheelAccelerationLastDirectionY = directionY;
        return wheelAccelerationMultiplier;
    }

    void resetWheelAcceleration() {
        wheelAccelerationMultiplier = 1.0f;
        wheelAccelerationLastMillis = Long.MIN_VALUE;
        wheelAccelerationLastDirectionX = 0;
        wheelAccelerationLastDirectionY = 0;
    }

    private boolean isDirectionReversed(int directionX, int directionY) {
        return (directionX != 0 && wheelAccelerationLastDirectionX != 0 && directionX != wheelAccelerationLastDirectionX)
                || (directionY != 0 && wheelAccelerationLastDirectionY != 0 && directionY != wheelAccelerationLastDirectionY);
    }

    private static int sign(double value) {
        if (value > 0.0) return 1;
        if (value < 0.0) return -1;
        return 0;
    }

    public record ScrollDelta(float x, float y) {
    }

    //endregion

    //region middle mouse auto-scroll

    public boolean middleMouseAutoScroll() {
        return middleMouseAutoScroll;
    }

    public ScrollState middleMouseAutoScroll(boolean enabled) {
        this.middleMouseAutoScroll = enabled;
        return this;
    }

    public float autoScrollDeadZone() {
        return autoScrollDeadZone;
    }

    public ScrollState autoScrollDeadZone(float deadZone) {
        this.autoScrollDeadZone = Math.max(0.0f, deadZone);
        return this;
    }

    public float autoScrollSpeed() {
        return autoScrollSpeed;
    }

    public ScrollState autoScrollSpeed(float speed) {
        this.autoScrollSpeed = Math.max(0.0f, speed);
        return this;
    }

    public float autoScrollMaxSpeed() {
        return autoScrollMaxSpeed;
    }

    public ScrollState autoScrollMaxSpeed(float maxSpeed) {
        this.autoScrollMaxSpeed = Math.max(0.0f, maxSpeed);
        return this;
    }

    float autoScrollDelta(double distanceFromAnchor) {
        double distance = Math.abs(distanceFromAnchor);
        if (distance <= autoScrollDeadZone || autoScrollSpeed <= 0.0f || autoScrollMaxSpeed <= 0.0f) {
            return 0.0f;
        }
        float delta = (float) Math.min(autoScrollMaxSpeed, (distance - autoScrollDeadZone) * autoScrollSpeed);
        return (float) Math.copySign(delta, distanceFromAnchor);
    }

    //endregion

    //region smooth scrolling

    /**
     * Returns whether smooth (animated) scrolling is enabled.
     */
    public boolean smooth() {
        return smooth;
    }

    /**
     * Enables or disables smooth scrolling.
     * <p>
     * When enabled, the actual scroll position interpolates toward the target
     * each frame using exponential easing, producing a fluid motion.
     * When disabled, the scroll position jumps to the target immediately.
     *
     * @param smooth true to enable smooth scrolling
     * @return this state for chaining
     */
    public ScrollState smooth(boolean smooth) {
        this.smooth = smooth;
        if (!smooth) {
            // Snap immediately when disabling
            this.scrollX = this.targetScrollX;
            this.scrollY = this.targetScrollY;
        }
        return this;
    }

    /**
     * Returns the smooth scrolling interpolation speed.
     * Higher values = faster convergence (0.0 - 1.0).
     */
    public float smoothSpeed() {
        return smoothSpeed;
    }

    /**
     * Sets the smooth scrolling interpolation speed.
     * <p>
     * This controls how quickly the scroll position converges to the target.
     * Typical values:
     * <ul>
     *   <li>{@code 0.15f} — slow, very smooth</li>
     *   <li>{@code 0.35f} — default, balanced</li>
     *   <li>{@code 0.6f} — fast, snappy</li>
     *   <li>{@code 1.0f} — instant (effectively disables smooth scrolling)</li>
     * </ul>
     *
     * @param speed the interpolation speed (0.0 - 1.0, exclusive of 0)
     * @return this state for chaining
     */
    public ScrollState smoothSpeed(float speed) {
        this.smoothSpeed = Math.clamp(speed, 0.01f, 1.0f);
        return this;
    }

    /**
     * Advances the smooth scrolling animation by one frame.
     * Called internally by {@link ScrollEffect} during rendering.
     * <p>
     * Uses exponential interpolation: {@code scroll += (target - scroll) * smoothSpeed}.
     * Snaps to target when the difference is less than {@value SNAP_THRESHOLD} pixels.
     */
    void animate() {
        if (!smooth) return;

        if (Math.abs(targetScrollX - scrollX) < SNAP_THRESHOLD) {
            scrollX = targetScrollX;
        } else {
            scrollX += (targetScrollX - scrollX) * smoothSpeed;
        }

        if (Math.abs(targetScrollY - scrollY) < SNAP_THRESHOLD) {
            scrollY = targetScrollY;
        } else {
            scrollY += (targetScrollY - scrollY) * smoothSpeed;
        }
    }

    /**
     * Returns whether the scroll animation is currently in progress
     * (actual position differs from target).
     */
    public boolean isAnimating() {
        return smooth && (Math.abs(targetScrollX - scrollX) >= SNAP_THRESHOLD
                || Math.abs(targetScrollY - scrollY) >= SNAP_THRESHOLD);
    }

    //endregion

    //region scrollbar config

    public boolean showScrollbar() {
        return showScrollbar;
    }

    public ScrollState showScrollbar(boolean show) {
        this.showScrollbar = show;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public ScrollState enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            resetScroll();
            resetWheelAcceleration();
        }
        return this;
    }

    /**
     * Returns whether the scrollbar thumb can be dragged with the mouse.
     */
    public boolean draggable() {
        return draggable;
    }

    /**
     * Enables or disables scrollbar thumb dragging.
     * When enabled, the user can click and drag the scrollbar thumb to scroll.
     * Enabled by default.
     *
     * @param draggable true to allow thumb dragging
     * @return this state for chaining
     */
    public ScrollState draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public int scrollbarWidth() {
        return scrollbarWidth;
    }

    public ScrollState scrollbarWidth(int width) {
        this.scrollbarWidth = Math.max(1, width);
        return this;
    }

    public int minThumbSize() {
        return minThumbSize;
    }

    public ScrollState minThumbSize(int minSize) {
        this.minThumbSize = Math.max(4, minSize);
        return this;
    }

    public VisualTexture trackTexture() {
        return trackTexture;
    }

    /**
     * Sets the scrollbar track (background) texture.
     *
     * @param texture the track texture
     * @return this state for chaining
     */
    public ScrollState trackTexture(VisualTexture texture) {
        this.trackTexture = texture;
        return this;
    }

    public VisualTexture thumbTexture() {
        return thumbTexture;
    }

    /**
     * Sets the scrollbar thumb (draggable handle) texture.
     *
     * @param texture the thumb texture
     * @return this state for chaining
     */
    public ScrollState thumbTexture(VisualTexture texture) {
        this.thumbTexture = texture;
        return this;
    }

    public int viewportInsetTop() {
        return viewportInsetTop;
    }

    public int viewportInsetRight() {
        return viewportInsetRight;
    }

    public int viewportInsetBottom() {
        return viewportInsetBottom;
    }

    public int viewportInsetLeft() {
        return viewportInsetLeft;
    }

    /**
     * Insets the scroll viewport and scrollbar tracks from the widget edges.
     * <p>
     * This is useful when a scrollable widget has a textured frame or border that
     * should remain visually reserved while the inner content scrolls.
     */
    public ScrollState viewportInset(int inset) {
        return viewportInset(inset, inset, inset, inset);
    }

    public ScrollState viewportInset(int vertical, int horizontal) {
        return viewportInset(vertical, horizontal, vertical, horizontal);
    }

    public ScrollState viewportInset(int top, int right, int bottom, int left) {
        this.viewportInsetTop = Math.max(0, top);
        this.viewportInsetRight = Math.max(0, right);
        this.viewportInsetBottom = Math.max(0, bottom);
        this.viewportInsetLeft = Math.max(0, left);
        reclamp();
        return this;
    }

    //endregion

    //region viewport (managed by ScrollEffect)

    /**
     * Updates the viewport dimensions. Called internally by {@link ScrollEffect}.
     */
    void updateViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        reclamp();
    }

    public int viewportWidth() {
        return viewportWidth;
    }

    public int viewportHeight() {
        return viewportHeight;
    }

    //endregion

    //region query

    /**
     * Returns whether vertical scrolling is possible (content taller than viewport).
     */
    public boolean canScrollVertically() {
        return enabled && direction.allowsVertical() && contentHeight > viewportHeight;
    }

    /**
     * Returns whether horizontal scrolling is possible (content wider than viewport).
     */
    public boolean canScrollHorizontally() {
        return enabled && direction.allowsHorizontal() && contentWidth > viewportWidth;
    }

    /**
     * Returns the maximum horizontal scroll offset.
     */
    public int maxScrollX() {
        return Math.max(0, contentWidth - viewportWidth);
    }

    /**
     * Returns the maximum vertical scroll offset.
     */
    public int maxScrollY() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    /**
     * Returns the vertical scroll progress as a value between 0.0 and 1.0,
     * based on the current (rendered) scroll position.
     */
    public float scrollProgressY() {
        int max = maxScrollY();
        return max > 0 ? scrollY / max : 0f;
    }

    /**
     * Returns the horizontal scroll progress as a value between 0.0 and 1.0,
     * based on the current (rendered) scroll position.
     */
    public float scrollProgressX() {
        int max = maxScrollX();
        return max > 0 ? scrollX / max : 0f;
    }

    //endregion

    //region clamping

    private void reclamp() {
        this.targetScrollX = clampX(this.targetScrollX);
        this.targetScrollY = clampY(this.targetScrollY);
        this.scrollX = clampX(this.scrollX);
        this.scrollY = clampY(this.scrollY);
    }

    private float clampX(float x) {
        return Math.clamp(x, 0, Math.max(0, contentWidth - viewportWidth));
    }

    private float clampY(float y) {
        return Math.clamp(y, 0, Math.max(0, contentHeight - viewportHeight));
    }

    //endregion

    @Override
    public String toString() {
        return "ScrollState{" +
                "direction=" + direction +
                ", scroll=(" + scrollX + ", " + scrollY + ")" +
                ", target=(" + targetScrollX + ", " + targetScrollY + ")" +
                ", content=(" + contentWidth + "x" + contentHeight + ")" +
                ", viewport=(" + viewportWidth + "x" + viewportHeight + ")" +
                ", smooth=" + smooth +
                ", enabled=" + enabled +
                '}';
    }
}
