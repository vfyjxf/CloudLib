package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The mutable state passed through the middleware pipeline.
 * <p>
 * Each middleware receives this state, may modify the coordinates / placement,
 * and may store per-name data that later middleware (or the caller) can read.
 */
public final class FloatingState {

    //region coordinates

    /**
     * Current x-coordinate for the floating element (scene-space).
     */
    private double x;

    /**
     * Current y-coordinate for the floating element (scene-space).
     */
    private double y;

    //endregion

    //region placement

    /**
     * The initial (preferred) placement passed by the caller.
     */
    private final FloatingPlacement initialPlacement;

    /**
     * The stateful resultant placement. FloatingMiddleware such as flip or autoPlacement
     * may change this from the initial value.
     */
    private FloatingPlacement placement;

    //endregion

    //region rects

    /**
     * Bounding rect of the reference element (scene-space).
     */
    private Rect referenceRect;

    /**
     * Bounding rect of the floating element (scene-space).
     */
    private Rect floatingRect;

    /**
     * The clipping boundary rect (scene-space), typically the screen / viewport.
     */
    private Rect boundary;

    //endregion

    //region middleware data

    /**
     * Per-middleware named data storage. Keyed by middleware name.
     */
    private final Map<String, Map<String, Object>> middlewareData = new HashMap<>();

    //endregion

    //region constructor

    public FloatingState(
            double x,
            double y,
            FloatingPlacement initialPlacement,
            FloatingPlacement placement,
            Rect referenceRect,
            Rect floatingRect,
            Rect boundary
    ) {
        this.x = x;
        this.y = y;
        this.initialPlacement = initialPlacement;
        this.placement = placement;
        this.referenceRect = referenceRect;
        this.floatingRect = floatingRect;
        this.boundary = boundary;
    }

    //endregion

    //region getters

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public FloatingPlacement initialPlacement() {
        return initialPlacement;
    }

    public FloatingPlacement placement() {
        return placement;
    }

    public Rect referenceRect() {
        return referenceRect;
    }

    public Rect floatingRect() {
        return floatingRect;
    }

    public Rect boundary() {
        return boundary;
    }

    //endregion

    //region setters

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setPlacement(FloatingPlacement placement) {
        this.placement = placement;
    }

    public void setReferenceRect(Rect referenceRect) {
        this.referenceRect = referenceRect;
    }

    public void setFloatingRect(Rect floatingRect) {
        this.floatingRect = floatingRect;
    }

    //endregion

    //region middleware data access

    /**
     * Stores a value in the named middleware data.
     *
     * @param middlewareName the middleware name (e.g. "offset", "flip")
     * @param key            the data key
     * @param value          the data value
     */
    public void putData(String middlewareName, String key, Object value) {
        middlewareData.computeIfAbsent(middlewareName, k -> new HashMap<>()).put(key, value);
    }

    /**
     * Reads a value from the named middleware data.
     *
     * @param middlewareName the middleware name
     * @param key            the data key
     * @return the value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getData(String middlewareName, String key) {
        Map<String, Object> data = middlewareData.get(middlewareName);
        if (data == null) return null;
        return (T) data.get(key);
    }

    /**
     * @return the entire middleware data map for reading
     */
    public Map<String, Map<String, Object>> allMiddlewareData() {
        return middlewareData;
    }

    //endregion

    //region overflow detection

    /**
     * Detects how much the floating element at its current position overflows
     * the clipping boundary on each side.
     *
     * @param padding the padding to apply to the boundary
     * @return the overflow offsets on each side
     */
    public Insets detectOverflow(int padding) {
        double floatLeft = x;
        double floatTop = y;
        double floatRight = x + floatingRect.width();
        double floatBottom = y + floatingRect.height();

        double boundLeft = boundary.x() + padding;
        double boundTop = boundary.y() + padding;
        double boundRight = boundary.right() - padding;
        double boundBottom = boundary.bottom() - padding;

        return new Insets(
                (int) Math.round(boundTop - floatTop),         // top overflow (positive = overflowing)
                (int) Math.round(floatRight - boundRight),     // right overflow
                (int) Math.round(floatBottom - boundBottom),   // bottom overflow
                (int) Math.round(boundLeft - floatLeft)        // left overflow
        );
    }

    /**
     * Detects overflow with zero padding.
     */
    public Insets detectOverflow() {
        return detectOverflow(0);
    }

    //endregion
}
