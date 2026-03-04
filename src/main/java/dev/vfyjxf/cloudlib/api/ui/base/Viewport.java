package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The unified coordinate-space manager for every widget.
 * <p>
 * A {@code Viewport} maps between two coordinate spaces:
 * <ul>
 *   <li>Local space — the widget's own coordinate system (origin at top-left).</li>
 *   <li>Parent space — the coordinate system of the widget's parent (or the scene for the root).</li>
 * </ul>
 * <p>
 * Three concepts are stored as dedicated fields:
 * <ol>
 *   <li>Layout — the widget's position within its parent, managed by the framework.</li>
 *   <li>Content offset — an additional translation for scroll containers.</li>
 *   <li>User transforms — scroll, zoom, rotation, etc.</li>
 * </ol>
 * <p>
 * The full forward matrix (local → parent) is:
 * {@code M = Layout · T(origin) · userTransforms · T(-origin)}
 *
 * @see ViewportTransform
 */
public final class Viewport {

    //region transform pipeline

    /**
     * The layout position, stored as a dedicated field.
     */
    private ViewportTransform.Layout layout = ViewportTransform.layout(0, 0);

    /**
     * Content offset applied between local space and content space (where children reside).
     * Used by scroll containers.
     */
    float contentOffsetX;
    float contentOffsetY;

    /**
     * User transforms (scroll, zoom, rotation, etc.).
     * Layout and contentOffset are stored as dedicated fields, not in this list.
     */
    final List<ViewportTransform> transforms = new ArrayList<>();

    /**
     * Callback invoked whenever cached matrices are invalidated.
     */
    @Nullable Runnable onInvalidate;

    //endregion

    //region dimensions

    /**
     * Size of the visible window (widget-local pixels).
     */
    private int viewportWidth;
    private int viewportHeight;

    /**
     * Size of the full content area (content-space pixels).
     */
    private int contentWidth;
    private int contentHeight;

    //endregion

    //region transform origin

    /**
     * Transform origin expressed as a ratio of the viewport dimensions (0–1).
     * {@code (0, 0)} = top-left (default); {@code (0.5, 0.5)} = center.
     */
    private double originX;
    private double originY;

    //endregion

    //region scale limits

    private double minScale = 0.1;
    private double maxScale = 10.0;

    //endregion

    //region cached matrices

    /**
     * Forward: local → parent.
     */
    @Nullable Matrix3x2f forwardMatrix;

    /**
     * Forward matrix as Matrix4f for GPU use.
     */
    @Nullable Matrix4f forwardMatrix4f;

    /**
     * Inverse: parent → local.
     */
    @Nullable Matrix3x2f inverseMatrix;

    /**
     * View matrix: user transforms only (excludes layout).
     */
    @Nullable Matrix3x2f viewMatrixCache;

    /**
     * View matrix as Matrix4f for GPU use.
     */
    @Nullable Matrix4f viewMatrix4fCache;

    //endregion

    //region factory

    /**
     * Creates a viewport with a default {@code Layout(0, 0)} transform.
     */
    public static Viewport create() {
        return new Viewport();
    }

    /**
     * Creates a viewport with the given visible-window size.
     */
    public static Viewport create(int viewportWidth, int viewportHeight) {
        Viewport vp = new Viewport();
        vp.viewportWidth = viewportWidth;
        vp.viewportHeight = viewportHeight;
        return vp;
    }

    /**
     * Creates a viewport with both viewport and content dimensions.
     */
    public static Viewport create(
            int viewportWidth, int viewportHeight,
            int contentWidth, int contentHeight) {
        Viewport vp = create(viewportWidth, viewportHeight);
        vp.contentWidth = contentWidth;
        vp.contentHeight = contentHeight;
        return vp;
    }

    /**
     * Creates a viewport sized to match the given widget size.
     */
    public static Viewport create(Size viewportSize) {
        return create(viewportSize.width(), viewportSize.height());
    }

    /**
     * Creates a viewport from the given widget size and content size.
     */
    public static Viewport create(Size viewportSize, Size contentSize) {
        return create(viewportSize.width(), viewportSize.height(),
                contentSize.width(), contentSize.height());
    }

    private Viewport() {
        // layout is initialised in-line; transforms list starts empty (user-only)
    }

    //endregion

    //region layout

    /**
     * Returns the layout position.
     */
    public Pos layoutPos() {
        return new Pos(layout.x(), layout.y());
    }

    /**
     * Returns the layout x offset.
     */
    public int layoutX() {
        return layout.x();
    }

    /**
     * Returns the layout y offset.
     */
    public int layoutY() {
        return layout.y();
    }

    /**
     * Updates the layout position. Called by the layout engine.
     *
     * @param x layout x offset in parent space
     * @param y layout y offset in parent space
     */
    @Contract("_,_ -> this")
    public Viewport setLayout(int x, int y) {
        layout = ViewportTransform.layout(x, y);
        invalidate();
        return this;
    }

    /**
     * Updates the layout position.
     *
     * @see #setLayout(int, int)
     */
    @Contract("_ -> this")
    public Viewport setLayout(Pos pos) {
        return setLayout(pos.x(), pos.y());
    }

    //endregion

    //region content offset

    /**
     * @return content offset X (horizontal scroll amount), default 0
     */
    public float contentOffsetX() {
        return contentOffsetX;
    }

    /**
     * @return content offset Y (vertical scroll amount), default 0
     */
    public float contentOffsetY() {
        return contentOffsetY;
    }

    /**
     * Sets the content offset. This shifts the coordinate space that children live in
     * relative to this widget's own local space, without moving the widget itself.
     * <p>
     * Does not invalidate cached matrices (contentOffset is not part of the
     * matrix pipeline) but does fire {@link #onInvalidate} to clear absolute-position caches.
     *
     * @param x horizontal content offset (scroll X)
     * @param y vertical content offset (scroll Y)
     */
    public void setContentOffset(float x, float y) {
        if (this.contentOffsetX == x && this.contentOffsetY == y) return;
        this.contentOffsetX = x;
        this.contentOffsetY = y;
        // Matrices are NOT affected by contentOffset, but children's
        // absolute positions are. Fire the callback directly without
        // clearing cached matrices.
        if (onInvalidate != null) onInvalidate.run();
    }

    //endregion

    //region pipeline access

    /**
     * Returns the full transform pipeline as an unmodifiable list.
     * Index 0 is the layout step; 1+ are user transforms.
     */
    public List<ViewportTransform> transforms() {
        List<ViewportTransform> composed = new ArrayList<>(transforms.size() + 1);
        composed.add(layout);
        composed.addAll(transforms);
        return Collections.unmodifiableList(composed);
    }

    /**
     * Returns the number of transforms in the pipeline (1 layout + n user transforms).
     */
    public int transformCount() {
        return 1 + transforms.size();
    }

    /**
     * Returns the transform at the given pipeline index.
     * Index 0 is the layout step; 1+ are user transforms.
     */
    public ViewportTransform getTransform(int index) {
        return pipelineGet(index);
    }

    /**
     * Replaces the entire user-transform portion of the pipeline.
     * Layout is unaffected (stored separately).
     */
    @Contract("_ -> this")
    public Viewport setUserTransforms(List<ViewportTransform> userTransforms) {
        this.transforms.clear();
        this.transforms.addAll(userTransforms);
        invalidate();
        return this;
    }

    /**
     * Replaces the entire user-transform portion of the pipeline.
     * Layout is unaffected (stored separately).
     */
    @Contract("_ -> this")
    public Viewport setUserTransforms(ViewportTransform... userTransforms) {
        this.transforms.clear();
        Collections.addAll(this.transforms, userTransforms);
        invalidate();
        return this;
    }

    /**
     * Replaces the transform at the given pipeline index.
     * Index 0 updates the layout; use {@link #setLayout(int, int)} for clarity.
     */
    @Contract("_,_ -> this")
    public Viewport setTransform(int index, ViewportTransform transform) {
        pipelineSet(index, transform);
        invalidate();
        return this;
    }

    /**
     * Appends a transform to the end of the pipeline.
     */
    @Contract("_ -> this")
    public Viewport addTransform(ViewportTransform transform) {
        this.transforms.add(transform);
        invalidate();
        return this;
    }

    /**
     * Inserts a user transform at the given pipeline index.
     * Index 0 is the layout step; the first user position is index 1.
     *
     * @param index pipeline index (1-based for user transforms)
     */
    @Contract("_,_ -> this")
    public Viewport addTransform(int index, ViewportTransform transform) {
        if (index < 1) throw new IllegalArgumentException("Cannot insert before the layout step (index 0)");
        this.transforms.add(index - 1, transform);
        invalidate();
        return this;
    }

    /**
     * Removes the transform at the given pipeline index.
     * Index 0 (the layout step) must not be removed.
     *
     * @param index pipeline index (1-based for user transforms)
     * @throws IllegalArgumentException if index is 0
     */
    @Contract("_ -> this")
    public Viewport removeTransform(int index) {
        if (index == 0) throw new IllegalArgumentException("Cannot remove the layout step at index 0");
        this.transforms.remove(index - 1);
        invalidate();
        return this;
    }

    /**
     * Clears all user transforms from the pipeline, keeping only the layout step.
     */
    @Contract("-> this")
    public Viewport clearUserTransforms() {
        this.transforms.clear();
        invalidate();
        return this;
    }

    //endregion

    //region pipeline lookup

    /**
     * Returns the index of the first transform assignable to the given type, or {@code -1}.
     */
    public <T extends ViewportTransform> int indexOf(Class<T> type) {
        if (type.isInstance(layout)) return 0;
        for (int i = 0; i < transforms.size(); i++) {
            if (type.isInstance(transforms.get(i))) return i + 1;
        }
        return -1;
    }

    /**
     * Returns the index of the last transform assignable to the given type, or {@code -1}.
     */
    public <T extends ViewportTransform> int lastIndexOf(Class<T> type) {
        for (int i = transforms.size() - 1; i >= 0; i--) {
            if (type.isInstance(transforms.get(i))) return i + 1;
        }
        if (type.isInstance(layout)) return 0;
        return -1;
    }

    /**
     * Returns the first transform of the given type, or {@code null} if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ViewportTransform> @Nullable T find(Class<T> type) {
        if (type.isInstance(layout)) return (T) layout;
        for (ViewportTransform t : transforms) {
            if (type.isInstance(t)) return (T) t;
        }
        return null;
    }

    //endregion

    //region dimensions

    public int viewportWidth() {
        return viewportWidth;
    }

    public int viewportHeight() {
        return viewportHeight;
    }

    public Size viewportSize() {
        return new Size(viewportWidth, viewportHeight);
    }

    public int contentWidth() {
        return contentWidth;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public Size contentSize() {
        return new Size(contentWidth, contentHeight);
    }

    @Contract("_,_ -> this")
    public Viewport setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        invalidate();
        return this;
    }

    @Contract("_ -> this")
    public Viewport setViewportSize(Size size) {
        return setViewportSize(size.width(), size.height());
    }

    @Contract("_,_ -> this")
    public Viewport setContentSize(int width, int height) {
        this.contentWidth = width;
        this.contentHeight = height;
        invalidate();
        return this;
    }

    @Contract("_ -> this")
    public Viewport setContentSize(Size size) {
        return setContentSize(size.width(), size.height());
    }

    //endregion

    //region origin

    /**
     * Returns the transform origin as a ratio of viewport dimensions.
     */
    public FloatPos origin() {
        return new FloatPos(originX, originY);
    }

    public double originX() {
        return originX;
    }

    public double originY() {
        return originY;
    }

    /**
     * Sets the transform origin as a ratio of the viewport dimensions.
     * {@code (0, 0)} = top-left (default), {@code (0.5, 0.5)} = center.
     * <p>
     * Origin wraps the entire pipeline: the forward matrix becomes
     * {@code T(origin) · pipeline · T(-origin)}. This only affects
     * scale, rotate and affine transforms — translations (including Layout)
     * are transparent to origin.
     */
    @Contract("_,_ -> this")
    public Viewport setOrigin(double ratioX, double ratioY) {
        this.originX = ratioX;
        this.originY = ratioY;
        invalidate();
        return this;
    }

    /**
     * Sets the origin to the center of the viewport.
     */
    @Contract("-> this")
    public Viewport setOriginCenter() {
        return setOrigin(0.5, 0.5);
    }

    /**
     * Sets the origin to the top-left corner (default).
     */
    @Contract("-> this")
    public Viewport setOriginTopLeft() {
        return setOrigin(0.0, 0.0);
    }

    //endregion

    //region scale limits

    public double minScale() {
        return minScale;
    }

    public double maxScale() {
        return maxScale;
    }

    /**
     * Sets the allowed scale range used by {@link #zoomAt}.
     */
    @Contract("_,_ -> this")
    public Viewport setScaleLimits(double min, double max) {
        this.minScale = min;
        this.maxScale = max;
        return this;
    }

    //endregion

    //region coordinate transform — local ↔ parent

    /**
     * Transforms a point from local space to parent space.
     */
    public FloatPos localToParent(double localX, double localY) {
        Matrix3x2f m = forwardMatrix();
        Vector2f v = new Vector2f((float) localX, (float) localY);
        m.transformPosition(v);
        return new FloatPos(v.x, v.y);
    }

    /**
     * Transforms a {@link FloatPos} from local space to parent space.
     */
    public FloatPos localToParent(FloatPos pos) {
        return localToParent(pos.x, pos.y);
    }

    /**
     * Transforms a {@link Pos} from local space to parent space.
     */
    public FloatPos localToParent(Pos pos) {
        return localToParent(pos.x(), pos.y());
    }

    /**
     * Transforms a point from parent space to local space (inverse of {@link #localToParent}).
     */
    public FloatPos parentToLocal(double parentX, double parentY) {
        Matrix3x2f m = inverseMatrix();
        Vector2f v = new Vector2f((float) parentX, (float) parentY);
        m.transformPosition(v);
        return new FloatPos(v.x, v.y);
    }

    /**
     * Transforms a {@link FloatPos} from parent space to local space.
     */
    public FloatPos parentToLocal(FloatPos pos) {
        return parentToLocal(pos.x, pos.y);
    }

    /**
     * Transforms a {@link Pos} from parent space to local space.
     */
    public FloatPos parentToLocal(Pos pos) {
        return parentToLocal(pos.x(), pos.y());
    }

    /**
     * Transforms a delta vector from local space to parent space, ignoring translation.
     */
    public FloatPos localToParentDelta(double dx, double dy) {
        Matrix3x2f m = forwardMatrix();
        float vx = m.m00 * (float) dx + m.m10 * (float) dy;
        float vy = m.m01 * (float) dx + m.m11 * (float) dy;
        return new FloatPos(vx, vy);
    }

    /**
     * Transforms a delta vector from parent space to local space.
     *
     * @return the delta in local space
     */
    public FloatPos parentToLocalDelta(double dx, double dy) {
        Matrix3x2f m = inverseMatrix();
        float vx = m.m00 * (float) dx + m.m10 * (float) dy;
        float vy = m.m01 * (float) dx + m.m11 * (float) dy;
        return new FloatPos(vx, vy);
    }

    //endregion

    //region view matrix (excludes layout)

    /**
     * Builds the view matrix — user transforms only, excludes the layout step.
     * <p>
     * Useful for rendering: the layout position is handled by the widget tree traversal,
     * and the view matrix captures only the extra transforms (scroll, zoom, rotation).
     *
     * @return the view matrix, or identity if only the layout step exists
     */
    public Matrix3x2f viewMatrix() {
        if (viewMatrixCache != null) return new Matrix3x2f(viewMatrixCache);
        Matrix3x2f m = buildViewMatrix();
        viewMatrixCache = m;
        return new Matrix3x2f(m);
    }

    /**
     * Returns the view matrix as a {@link Matrix4f} for GPU use.
     */
    public Matrix4f viewMatrix4f() {
        if (viewMatrix4fCache != null) return new Matrix4f(viewMatrix4fCache);
        Matrix3x2f m = viewMatrix();
        Matrix4f m4 = to4f(m);
        viewMatrix4fCache = m4;
        return new Matrix4f(m4);
    }

    /**
     * Package-private: returns the cached view Matrix4f without defensive copy.
     */
    Matrix4f viewMatrix4fDirect() {
        if (viewMatrix4fCache == null) {
            viewMatrix4f(); // force cache
        }
        return viewMatrix4fCache;
    }

    /**
     * Returns whether the view (non-layout) portion of the pipeline is identity.
     */
    public boolean isViewIdentity() {
        return transforms.isEmpty();
    }

    /**
     * Transforms a point from content space to viewport space (user transforms only, no layout).
     */
    public FloatPos contentToViewport(double localX, double localY) {
        Matrix3x2f m = viewMatrix();
        Vector2f v = new Vector2f((float) localX, (float) localY);
        m.transformPosition(v);
        return new FloatPos(v.x, v.y);
    }

    /**
     * Transforms a point from viewport space to content space (inverse of {@link #contentToViewport}).
     */
    public FloatPos viewportToContent(double viewportX, double viewportY) {
        Matrix3x2f m = viewMatrix();
        Matrix3x2f inv = m.invert(new Matrix3x2f());
        Vector2f v = new Vector2f((float) viewportX, (float) viewportY);
        inv.transformPosition(v);
        return new FloatPos(v.x, v.y);
    }

    //endregion

    //region hit-testing

    /**
     * Tests whether a point in the widget's own local space falls within the
     * viewport bounds (0,0 → viewportWidth×viewportHeight).
     */
    public boolean isInsideViewport(double localX, double localY) {
        return localX >= 0 && localX < viewportWidth
                && localY >= 0 && localY < viewportHeight;
    }

    /**
     * Tests whether a parent-space point hits this widget's visible area.
     */
    public boolean hitTest(double parentX, double parentY) {
        FloatPos local = parentToLocal(parentX, parentY);
        return isInsideViewport(local.x, local.y);
    }

    /**
     * Tests whether a parent-space point hits a local-space rectangle.
     */
    public boolean hitTest(
            double parentX, double parentY,
            int cx, int cy, int cw, int ch) {
        FloatPos local = parentToLocal(parentX, parentY);
        return local.x >= cx && local.x < cx + cw
                && local.y >= cy && local.y < cy + ch;
    }

    /**
     * Tests whether a parent-space point hits a local-space rectangle.
     *
     * @see #hitTest(double, double, int, int, int, int)
     */
    public boolean hitTest(double parentX, double parentY, Rect localRect) {
        return hitTest(parentX, parentY,
                localRect.x(), localRect.y(),
                localRect.width(), localRect.height());
    }

    /**
     * Computes the AABB in parent space for a local-space rect.
     */
    public Rect localRectToParentBounds(int cx, int cy, int cw, int ch) {
        double x0 = cx;
        double y0 = cy;
        double x1 = cx + cw;
        double y1 = cy + ch;

        FloatPos tl = localToParent(x0, y0);
        FloatPos tr = localToParent(x1, y0);
        FloatPos bl = localToParent(x0, y1);
        FloatPos br = localToParent(x1, y1);

        double minX = Math.min(Math.min(tl.x, tr.x), Math.min(bl.x, br.x));
        double minY = Math.min(Math.min(tl.y, tr.y), Math.min(bl.y, br.y));
        double maxX = Math.max(Math.max(tl.x, tr.x), Math.max(bl.x, br.x));
        double maxY = Math.max(Math.max(tl.y, tr.y), Math.max(bl.y, br.y));

        return new Rect(
                (int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY)
        );
    }

    /**
     * Computes the AABB in parent space for a local-space rect.
     *
     * @see #localRectToParentBounds(int, int, int, int)
     */
    public Rect localRectToParentBounds(Rect localRect) {
        return localRectToParentBounds(
                localRect.x(), localRect.y(),
                localRect.width(), localRect.height());
    }

    /**
     * Computes the local-space region currently visible within the viewport.
     */
    public Rect visibleContentBounds() {
        FloatPos tl = viewportToContent(0, 0);
        FloatPos tr = viewportToContent(viewportWidth, 0);
        FloatPos bl = viewportToContent(0, viewportHeight);
        FloatPos br = viewportToContent(viewportWidth, viewportHeight);

        double minX = Math.min(Math.min(tl.x, tr.x), Math.min(bl.x, br.x));
        double minY = Math.min(Math.min(tl.y, tr.y), Math.min(bl.y, br.y));
        double maxX = Math.max(Math.max(tl.x, tr.x), Math.max(bl.x, br.x));
        double maxY = Math.max(Math.max(tl.y, tr.y), Math.max(bl.y, br.y));

        return new Rect(
                (int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY)
        );
    }

    /**
     * Returns the effective (averaged) scale derived from the composed matrix.
     */
    public double effectiveScale() {
        Matrix3x2f m = forwardMatrix();
        double det = m.m00 * m.m11 - m.m01 * m.m10;
        return Math.sqrt(Math.abs(det));
    }

    /**
     * Returns the effective horizontal scale derived from the composed matrix.
     */
    public double effectiveScaleX() {
        Matrix3x2f m = forwardMatrix();
        return Math.sqrt(m.m00 * m.m00 + m.m01 * m.m01);
    }

    /**
     * Returns the effective vertical scale derived from the composed matrix.
     */
    public double effectiveScaleY() {
        Matrix3x2f m = forwardMatrix();
        return Math.sqrt(m.m10 * m.m10 + m.m11 * m.m11);
    }

    //endregion

    //region scrollbar helpers

    /**
     * Horizontal scroll fraction in [0, 1], derived from the view matrix.
     */
    public double scrollFractionX() {
        if (contentWidth <= 0) return 0;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos tr = viewportToContent(viewportWidth, 0);
        double visibleWidth = Math.abs(tr.x - tl.x);
        double maxScroll = Math.max(0, contentWidth - visibleWidth);
        return maxScroll <= 0 ? 0 : clamp(Math.min(tl.x, tr.x) / maxScroll, 0, 1);
    }

    /**
     * Vertical scroll fraction in [0, 1], derived from the view matrix.
     */
    public double scrollFractionY() {
        if (contentHeight <= 0) return 0;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos bl = viewportToContent(0, viewportHeight);
        double visibleHeight = Math.abs(bl.y - tl.y);
        double maxScroll = Math.max(0, contentHeight - visibleHeight);
        return maxScroll <= 0 ? 0 : clamp(Math.min(tl.y, bl.y) / maxScroll, 0, 1);
    }

    /**
     * Horizontal thumb size as a fraction of the track length.
     */
    public double thumbFractionX() {
        if (contentWidth <= 0) return 1.0;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos tr = viewportToContent(viewportWidth, 0);
        double visibleWidth = Math.abs(tr.x - tl.x);
        return Math.min(1.0, visibleWidth / contentWidth);
    }

    /**
     * Vertical thumb size as a fraction of the track length.
     */
    public double thumbFractionY() {
        if (contentHeight <= 0) return 1.0;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos bl = viewportToContent(0, viewportHeight);
        double visibleHeight = Math.abs(bl.y - tl.y);
        return Math.min(1.0, visibleHeight / contentHeight);
    }

    /**
     * Whether the content is scrollable horizontally.
     */
    public boolean canScrollHorizontally() {
        if (contentWidth <= 0) return false;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos tr = viewportToContent(viewportWidth, 0);
        return Math.abs(tr.x - tl.x) < contentWidth;
    }

    /**
     * Whether the content is scrollable vertically.
     */
    public boolean canScrollVertically() {
        if (contentHeight <= 0) return false;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos bl = viewportToContent(0, viewportHeight);
        return Math.abs(bl.y - tl.y) < contentHeight;
    }

    //endregion

    //region pivot adjustment

    /**
     * Adjusts the Translate at the given pipeline index so that
     * the local-space point maps to the given parent-space point.
     */
    @Contract("_,_,_,_,_ -> this")
    public Viewport adjustTranslateForPivot(
            int translateIndex,
            double localX, double localY,
            double parentX, double parentY) {
        float ox = (float) (originX * viewportWidth);
        float oy = (float) (originY * viewportHeight);

        Matrix3x2f before = new Matrix3x2f();
        if (ox != 0 || oy != 0) before.translate(ox, oy);
        for (int i = 0; i < translateIndex; i++) {
            pipelineGet(i).apply(before);
        }

        Matrix3x2f after = new Matrix3x2f();
        int total = pipelineSize();
        for (int i = translateIndex + 1; i < total; i++) {
            pipelineGet(i).apply(after);
        }
        if (ox != 0 || oy != 0) after.translate(-ox, -oy);

        Matrix3x2f beforeInv = before.invert(new Matrix3x2f());
        Vector2f pp = new Vector2f((float) parentX, (float) parentY);
        beforeInv.transformPosition(pp);

        Vector2f lp = new Vector2f((float) localX, (float) localY);
        after.transformPosition(lp);

        double tx = pp.x - lp.x;
        double ty = pp.y - lp.y;

        pipelineSet(translateIndex, ViewportTransform.translate(tx, ty));
        invalidate();
        return this;
    }

    /**
     * Zoom-to-cursor: scales at {@code scaleIndex} and adjusts translation to
     * keep the cursor point stationary.
     */
    @Contract("_,_,_,_,_ -> this")
    public Viewport zoomAt(
            int scaleIndex, int translateIndex,
            double factor,
            double parentX, double parentY) {
        return zoomAt(scaleIndex, translateIndex, factor, factor, parentX, parentY);
    }

    /**
     * Non-uniform variant of {@link #zoomAt(int, int, double, double, double)}.
     */
    @Contract("_,_,_,_,_,_ -> this")
    public Viewport zoomAt(
            int scaleIndex, int translateIndex,
            double factorX, double factorY,
            double parentX, double parentY) {
        FloatPos localPt = parentToLocal(parentX, parentY);

        ViewportTransform.Scale s = (ViewportTransform.Scale) pipelineGet(scaleIndex);
        double newSx = clamp(s.sx() * factorX, minScale, maxScale);
        double newSy = clamp(s.sy() * factorY, minScale, maxScale);
        pipelineSet(scaleIndex, ViewportTransform.scale(newSx, newSy));
        invalidate();

        adjustTranslateForPivot(translateIndex,
                localPt.x, localPt.y, parentX, parentY);
        return this;
    }

    /**
     * Rotate-at-cursor: rotates at {@code rotateIndex} and adjusts Translate
     * at {@code translateIndex} to keep the cursor point stationary.
     */
    @Contract("_,_,_,_,_ -> this")
    public Viewport rotateAt(
            int rotateIndex, int translateIndex,
            double deltaRadians,
            double parentX, double parentY) {
        FloatPos localPt = parentToLocal(parentX, parentY);

        ViewportTransform.Rotate r = (ViewportTransform.Rotate) pipelineGet(rotateIndex);
        double newAngle = normalizeAngle(r.radians() + deltaRadians);
        pipelineSet(rotateIndex, ViewportTransform.rotate(newAngle));
        invalidate();

        adjustTranslateForPivot(translateIndex,
                localPt.x, localPt.y, parentX, parentY);
        return this;
    }

    /**
     * Clamps the Translate at the given index so the visible content stays in bounds.
     */
    @Contract("_ -> this")
    public Viewport clampTranslate(int translateIndex) {
        if (contentWidth <= 0 && contentHeight <= 0) return this;

        ViewportTransform.Translate t =
                (ViewportTransform.Translate) pipelineGet(translateIndex);
        double dx = t.dx();
        double dy = t.dy();

        Rect visible = visibleContentBounds();

        if (visible.x() < 0) {
            dx -= visible.x();
        } else if (contentWidth > 0 && visible.x() + visible.width() > contentWidth) {
            dx -= (visible.x() + visible.width() - contentWidth);
        }

        if (visible.y() < 0) {
            dy -= visible.y();
        } else if (contentHeight > 0 && visible.y() + visible.height() > contentHeight) {
            dy -= (visible.y() + visible.height() - contentHeight);
        }

        if (dx != t.dx() || dy != t.dy()) {
            pipelineSet(translateIndex, ViewportTransform.translate(dx, dy));
            invalidate();
        }
        return this;
    }

    //endregion

    //region matrix access

    /**
     * Returns the forward (local → parent) affine matrix. Defensive copy.
     */
    public Matrix3x2f forwardMatrix() {
        if (forwardMatrix == null) {
            forwardMatrix = buildForwardMatrix();
        }
        return new Matrix3x2f(forwardMatrix);
    }

    /**
     * Package-private: returns the cached forward matrix without defensive copy.
     */
    Matrix3x2f forwardMatrixDirect() {
        if (forwardMatrix == null) {
            forwardMatrix = buildForwardMatrix();
        }
        return forwardMatrix;
    }

    /**
     * Returns the inverse (parent → local) affine matrix.
     */
    public Matrix3x2f inverseMatrix() {
        if (inverseMatrix == null) {
            Matrix3x2f fwd = forwardMatrixDirect();
            inverseMatrix = fwd.invert(new Matrix3x2f());
        }
        return new Matrix3x2f(inverseMatrix);
    }

    /**
     * Package-private: returns the cached inverse matrix without defensive copy.
     */
    Matrix3x2f inverseMatrixDirect() {
        if (inverseMatrix == null) {
            Matrix3x2f fwd = forwardMatrixDirect();
            inverseMatrix = fwd.invert(new Matrix3x2f());
        }
        return inverseMatrix;
    }

    /**
     * Returns the forward transform as a {@link Matrix4f} for GPU rendering.
     */
    public Matrix4f toMatrix4f() {
        if (forwardMatrix4f == null) {
            forwardMatrix4f = to4f(forwardMatrixDirect());
        }
        return new Matrix4f(forwardMatrix4f);
    }

    /**
     * Package-private: returns the cached forward Matrix4f without defensive copy.
     */
    Matrix4f toMatrix4fDirect() {
        if (forwardMatrix4f == null) {
            forwardMatrix4f = to4f(forwardMatrixDirect());
        }
        return forwardMatrix4f;
    }

    /**
     * Returns whether the full composed transform is identity.
     */
    public boolean isIdentity() {
        Matrix3x2f m = forwardMatrixDirect();
        return m.m00 == 1 && m.m01 == 0
                && m.m10 == 0 && m.m11 == 1
                && m.m20 == 0 && m.m21 == 0;
    }

    //endregion

    //region reset / copy

    /**
     * Resets to a clean state: only {@code Layout(0,0)}, origin at top-left.
     */
    @Contract("-> this")
    public Viewport reset() {
        layout = ViewportTransform.layout(0, 0);
        contentOffsetX = 0;
        contentOffsetY = 0;
        transforms.clear();
        originX = 0;
        originY = 0;
        invalidate();
        return this;
    }

    /**
     * Creates a deep copy of this viewport.
     */
    public Viewport copy() {
        Viewport v = new Viewport();
        v.layout = layout; // record is immutable — safe to share
        v.contentOffsetX = contentOffsetX;
        v.contentOffsetY = contentOffsetY;
        v.transforms.addAll(transforms); // records are immutable — shallow copy is safe
        v.viewportWidth = viewportWidth;
        v.viewportHeight = viewportHeight;
        v.contentWidth = contentWidth;
        v.contentHeight = contentHeight;
        v.originX = originX;
        v.originY = originY;
        v.minScale = minScale;
        v.maxScale = maxScale;
        return v;
    }

    //endregion

    //region internal

    void invalidate() {
        forwardMatrix = null;
        forwardMatrix4f = null;
        inverseMatrix = null;
        viewMatrixCache = null;
        viewMatrix4fCache = null;
        if (onInvalidate != null) {
            onInvalidate.run();
        }
    }

    /**
     * Returns the total pipeline size (1 layout + n user transforms).
     */
    private int pipelineSize() {
        return 1 + transforms.size();
    }

    /**
     * Returns the transform at the given pipeline index.
     * Index 0 → layout; 1+ → user transforms.
     */
    private ViewportTransform pipelineGet(int index) {
        return index == 0 ? layout : transforms.get(index - 1);
    }

    /**
     * Sets the transform at the given pipeline index.
     * Index 0 → layout; 1+ → user transforms.
     */
    private void pipelineSet(int index, ViewportTransform transform) {
        if (index == 0) {
            layout = (ViewportTransform.Layout) transform;
        } else {
            transforms.set(index - 1, transform);
        }
    }

    /**
     * Builds the forward (local → parent) affine matrix from the full pipeline.
     */
    private Matrix3x2f buildForwardMatrix() {
        float ox = (float) (originX * viewportWidth);
        float oy = (float) (originY * viewportHeight);

        Matrix3x2f m = new Matrix3x2f();
        // Layout is always first (translations are unaffected by origin wrapping)
        layout.apply(m);

        if (ox != 0 || oy != 0) m.translate(ox, oy);

        for (ViewportTransform t : transforms) {
            t.apply(m);
        }

        if (ox != 0 || oy != 0) m.translate(-ox, -oy);

        return m;
    }

    /**
     * Builds the view matrix (user transforms only, excludes layout).
     */
    private Matrix3x2f buildViewMatrix() {
        Matrix3x2f m = new Matrix3x2f();

        if (transforms.isEmpty()) {
            return m;
        }

        float ox = (float) (originX * viewportWidth);
        float oy = (float) (originY * viewportHeight);

        if (ox != 0 || oy != 0) m.translate(ox, oy);

        for (ViewportTransform t : transforms) {
            t.apply(m);
        }

        if (ox != 0 || oy != 0) m.translate(-ox, -oy);

        return m;
    }

    private static Matrix4f to4f(Matrix3x2f m) {
        return new Matrix4f(
                m.m00, m.m01, 0, 0,
                m.m10, m.m11, 0, 0,
                0, 0, 1, 0,
                m.m20, m.m21, 0, 1
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double normalizeAngle(double radians) {
        radians = radians % (2 * Math.PI);
        if (radians < -Math.PI) radians += 2 * Math.PI;
        if (radians > Math.PI) radians -= 2 * Math.PI;
        return radians;
    }

    //endregion

    @Override
    public String toString() {
        return "Viewport{" +
                "layout=" + layout +
                ", contentOffset=(" + contentOffsetX + ", " + contentOffsetY + ")" +
                ", userTransforms=" + transforms +
                ", viewport=" + viewportWidth + "x" + viewportHeight +
                ", content=" + contentWidth + "x" + contentHeight +
                ", origin=(" + originX + ", " + originY + ")" +
                '}';
    }
}
