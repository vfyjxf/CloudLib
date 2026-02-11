package dev.vfyjxf.cloudlib.api.ui.viewport;

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
 *   <li><b>Local space</b> — the widget's own content coordinate system, where children
 *       are laid out and rendering happens (origin {@code (0, 0)} at the widget's top-left).</li>
 *   <li><b>Parent space</b> — the coordinate system of the widget's parent (or the scene
 *       for the root widget).</li>
 * </ul>
 * <p>
 * Every widget has a {@code Viewport}. The pipeline always starts with a
 * {@link ViewportTransform.Layout Layout} transform that encodes the widget's
 * position within its parent — the layout engine updates this step automatically,
 * making position "just another transform in the pipeline". Additional transforms
 * (scroll, zoom, rotation) can be appended after the layout step.
 * <p>
 * The full forward matrix (local → parent) is built as:
 * <pre>{@code
 *   M = T(origin) · transforms[0] · transforms[1] · … · transforms[n-1] · T(-origin)
 * }</pre>
 * where {@code transforms[0]} is always the {@link ViewportTransform.Layout Layout} step.
 * The optional {@linkplain #origin() origin} wrapping causes scale and rotation to
 * operate around the specified point. Translations (including {@code Layout}) are
 * unaffected by origin wrapping because {@code T(o) · T(t) · T(-o) = T(t)}.
 *
 * <h3>Index Convention</h3>
 * <table>
 *   <tr><th>Index 0</th><td>{@code Layout(x, y)} — layout position, managed by the framework</td></tr>
 *   <tr><th>Index 1…n</th><td>User transforms — scroll, scale, rotate, etc.</td></tr>
 * </table>
 *
 * <h3>Typical use cases</h3>
 * <pre>{@code
 * // Plain widget (layout-only, default)
 * // Pipeline: [Layout(x, y)]
 * Viewport vp = Viewport.create();
 *
 * // Scroll panel
 * // Pipeline: [Layout(x, y), Translate(-scrollX, -scrollY)]
 * vp.addTransform(ViewportTransform.translate(0, 0));
 *
 * // Zoomable editor
 * // Pipeline: [Layout(x, y), Scale(1, 1), Translate(0, 0)]
 * vp.addTransform(ViewportTransform.scale(1, 1));
 * vp.addTransform(ViewportTransform.translate(0, 0));
 *
 * // Full transform (rotate + scale around center, then translate)
 * // Pipeline: [Layout(x, y), Rotate(0), Scale(1, 1), Translate(0, 0)]
 * vp.setOriginCenter()
 *   .addTransform(ViewportTransform.rotate(0))
 *   .addTransform(ViewportTransform.scale(1, 1))
 *   .addTransform(ViewportTransform.translate(0, 0));
 * }</pre>
 *
 * <h3>Coordinate Naming Convention</h3>
 * <table>
 *   <tr><th>Method pattern</th><th>Direction</th></tr>
 *   <tr><td>{@code localToParent}</td><td>local → parent (forward matrix)</td></tr>
 *   <tr><td>{@code parentToLocal}</td><td>parent → local (inverse matrix)</td></tr>
 *   <tr><td>{@code localToScene}</td><td>local → scene root (chain through ancestors)</td></tr>
 *   <tr><td>{@code sceneToLocal}</td><td>scene root → local (chain through ancestors)</td></tr>
 * </table>
 *
 * @see ViewportTransform
 * @see dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState
 * @see dev.vfyjxf.cloudlib.api.ui.scroll.ScrollEffect
 * @see dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas
 */
public final class Viewport {

    //region transform pipeline

    /**
     * The ordered transform pipeline. Index 0 is always the {@link ViewportTransform.Layout}.
     * Composed left-to-right via post-multiplication.
     */
    private final List<ViewportTransform> transforms = new ArrayList<>();

    //endregion

    //region dimensions

    /** Size of the visible window (widget-local pixels). */
    private int viewportWidth;
    private int viewportHeight;

    /** Size of the full content area (content-space pixels). */
    private int contentWidth;
    private int contentHeight;

    //endregion

    //region transform origin

    /**
     * Transform origin expressed as a ratio of the viewport dimensions (0–1).
     * {@code (0, 0)} means top-left (the default); {@code (0.5, 0.5)} means center.
     * <p>
     * The origin wraps the <em>entire</em> pipeline:
     * {@code M = T(origin) · pipeline · T(-origin)}.
     * This only affects non-translation transforms (scale, rotate, affine).
     */
    private double originX;
    private double originY;

    //endregion

    //region scale limits

    private double minScale = 0.1;
    private double maxScale = 10.0;

    //endregion

    //region cached matrices

    /** Forward: local → parent. */
    private @Nullable Matrix3x2f forwardMatrix;

    /** Inverse: parent → local. */
    private @Nullable Matrix3x2f inverseMatrix;

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
    public static Viewport create(int viewportWidth, int viewportHeight,
                                  int contentWidth, int contentHeight) {
        Viewport vp = new Viewport();
        vp.viewportWidth = viewportWidth;
        vp.viewportHeight = viewportHeight;
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
        // Every pipeline begins with the layout transform
        transforms.add(ViewportTransform.layout(0, 0));
    }

    //endregion

    //region layout

    /**
     * Returns the layout position from the pipeline's {@link ViewportTransform.Layout} step.
     */
    public Pos layoutPos() {
        ViewportTransform.Layout l = (ViewportTransform.Layout) transforms.get(0);
        return new Pos(l.x(), l.y());
    }

    /**
     * Returns the layout x offset.
     */
    public int layoutX() {
        return ((ViewportTransform.Layout) transforms.get(0)).x();
    }

    /**
     * Returns the layout y offset.
     */
    public int layoutY() {
        return ((ViewportTransform.Layout) transforms.get(0)).y();
    }

    /**
     * Updates the layout position. Called by the layout engine.
     * <p>
     * This replaces the {@code Layout} transform at index 0, invalidating the cache.
     *
     * @param x layout x offset in parent space
     * @param y layout y offset in parent space
     */
    @Contract("_,_ -> this")
    public Viewport setLayout(int x, int y) {
        transforms.set(0, ViewportTransform.layout(x, y));
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

    //region pipeline access

    /**
     * Returns an unmodifiable view of the current transform pipeline.
     * Index 0 is always the {@link ViewportTransform.Layout} step.
     */
    public List<ViewportTransform> transforms() {
        return Collections.unmodifiableList(transforms);
    }

    /**
     * Returns the number of transforms in the pipeline (including the layout step).
     */
    public int transformCount() {
        return transforms.size();
    }

    /**
     * Returns the transform at the given pipeline index.
     * Index 0 is the layout step.
     */
    public ViewportTransform getTransform(int index) {
        return transforms.get(index);
    }

    /**
     * Replaces the entire user-transform portion of the pipeline (indices 1…n).
     * The {@code Layout} at index 0 is preserved.
     */
    @Contract("_ -> this")
    public Viewport setUserTransforms(List<ViewportTransform> userTransforms) {
        ViewportTransform layoutStep = transforms.get(0);
        this.transforms.clear();
        this.transforms.add(layoutStep);
        this.transforms.addAll(userTransforms);
        invalidate();
        return this;
    }

    /**
     * Replaces the entire user-transform portion of the pipeline (indices 1…n).
     * The {@code Layout} at index 0 is preserved.
     */
    @Contract("_ -> this")
    public Viewport setUserTransforms(ViewportTransform... userTransforms) {
        ViewportTransform layoutStep = transforms.get(0);
        this.transforms.clear();
        this.transforms.add(layoutStep);
        Collections.addAll(this.transforms, userTransforms);
        invalidate();
        return this;
    }

    /**
     * Replaces the transform at the given index.
     * Use index 0 only via {@link #setLayout(int, int)}.
     */
    @Contract("_,_ -> this")
    public Viewport setTransform(int index, ViewportTransform transform) {
        this.transforms.set(index, transform);
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
     * Inserts a transform at the given index.
     * Index 0 is reserved for the layout step; use {@code index >= 1}.
     */
    @Contract("_,_ -> this")
    public Viewport addTransform(int index, ViewportTransform transform) {
        this.transforms.add(index, transform);
        invalidate();
        return this;
    }

    /**
     * Removes the transform at the given index.
     * Index 0 (the layout step) must not be removed.
     *
     * @throws IllegalArgumentException if index is 0
     */
    @Contract("_ -> this")
    public Viewport removeTransform(int index) {
        if (index == 0) throw new IllegalArgumentException("Cannot remove the layout step at index 0");
        this.transforms.remove(index);
        invalidate();
        return this;
    }

    /**
     * Clears all user transforms from the pipeline, keeping only the layout step.
     */
    @Contract("-> this")
    public Viewport clearUserTransforms() {
        ViewportTransform layoutStep = transforms.get(0);
        this.transforms.clear();
        this.transforms.add(layoutStep);
        invalidate();
        return this;
    }

    //endregion

    //region pipeline lookup

    /**
     * Returns the index of the first transform assignable to the given type, or {@code -1}.
     * <p>
     * Useful for finding and updating a specific transform step:
     * <pre>{@code
     * int scaleIdx = vp.indexOf(ViewportTransform.Scale.class);
     * if (scaleIdx >= 0) {
     *     ViewportTransform.Scale old = (ViewportTransform.Scale) vp.getTransform(scaleIdx);
     *     vp.setTransform(scaleIdx, ViewportTransform.scale(old.sx() * 1.1, old.sy() * 1.1));
     * }
     * }</pre>
     */
    public <T extends ViewportTransform> int indexOf(Class<T> type) {
        for (int i = 0; i < transforms.size(); i++) {
            if (type.isInstance(transforms.get(i))) return i;
        }
        return -1;
    }

    /**
     * Returns the index of the last transform assignable to the given type, or {@code -1}.
     */
    public <T extends ViewportTransform> int lastIndexOf(Class<T> type) {
        for (int i = transforms.size() - 1; i >= 0; i--) {
            if (type.isInstance(transforms.get(i))) return i;
        }
        return -1;
    }

    /**
     * Returns the first transform of the given type, or {@code null} if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ViewportTransform> @Nullable T find(Class<T> type) {
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
     * Transforms a point from <b>local space</b> to <b>parent space</b>.
     *
     * @param localX x in local space
     * @param localY y in local space
     * @return the corresponding position in parent space
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
     * Transforms a point from <b>parent space</b> to <b>local space</b>.
     * This is the inverse of {@link #localToParent}.
     * <p>
     * Essential for hit-testing: given a mouse position in the parent's coordinate
     * space, compute the corresponding local coordinate.
     *
     * @param parentX x in parent space
     * @param parentY y in parent space
     * @return the corresponding position in local space
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
     * Transforms a <em>delta</em> (direction/distance) vector from local space to parent space.
     * Unlike point transforms, this ignores the translation component.
     *
     * @return the delta in parent space
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
     * Builds the <em>view matrix</em> — the sub-pipeline that excludes the layout step.
     * <p>
     * If the pipeline is {@code [Layout, S, R, T]}, the view matrix is
     * {@code T(origin) · S · R · T · T(-origin)}.
     * <p>
     * This is useful for rendering: the layout position is already handled by the
     * widget tree traversal, and the view matrix captures only the "extra" transforms
     * (scroll, zoom, rotation) applied to the widget's content.
     *
     * @return the view matrix as {@link Matrix3x2f}, or identity if only the layout step exists
     */
    public Matrix3x2f viewMatrix() {
        float ox = (float) (originX * viewportWidth);
        float oy = (float) (originY * viewportHeight);

        Matrix3x2f m = new Matrix3x2f();

        if (transforms.size() <= 1) {
            // Only layout step — view is identity
            return m;
        }

        if (ox != 0 || oy != 0) m.translate(ox, oy);

        for (int i = 1; i < transforms.size(); i++) {
            transforms.get(i).apply(m);
        }

        if (ox != 0 || oy != 0) m.translate(-ox, -oy);

        return m;
    }

    /**
     * Returns the view matrix as a {@link Matrix4f} suitable for use with
     * {@link dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas SceneCanvas}.
     * The Z components are identity.
     */
    public Matrix4f viewMatrix4f() {
        Matrix3x2f m = viewMatrix();
        return new Matrix4f(
                m.m00, m.m01, 0, 0,
                m.m10, m.m11, 0, 0,
                0, 0, 1, 0,
                m.m20, m.m21, 0, 1
        );
    }

    /**
     * Returns whether the view (non-layout) portion of the pipeline is identity.
     * When this is {@code true}, the widget has no extra transforms (no scroll, zoom, rotation).
     */
    public boolean isViewIdentity() {
        if (transforms.size() <= 1) return true;
        Matrix3x2f m = viewMatrix();
        return m.m00 == 1 && m.m01 == 0
               && m.m10 == 0 && m.m11 == 1
               && m.m20 == 0 && m.m21 == 0;
    }

    /**
     * Transforms a point from the widget's <b>local/content space</b> to the widget's
     * <b>viewport space</b> (after user transforms, before layout offset).
     * <p>
     * This applies only the view matrix (indices 1…n), useful for mapping content
     * coordinates to the visible window before layout positioning.
     *
     * @param localX x in local/content space
     * @param localY y in local/content space
     * @return the corresponding position in viewport space (pre-layout)
     */
    public FloatPos contentToViewport(double localX, double localY) {
        Matrix3x2f m = viewMatrix();
        Vector2f v = new Vector2f((float) localX, (float) localY);
        m.transformPosition(v);
        return new FloatPos(v.x, v.y);
    }

    /**
     * Transforms a point from the widget's <b>viewport space</b> to the widget's
     * <b>local/content space</b>.
     * <p>
     * This applies only the inverse of the view matrix (indices 1…n). Essential for
     * hit-testing within a scrolled/zoomed widget: the parent-to-local conversion
     * already accounts for layout, and this further maps through zoom/scroll.
     *
     * @param viewportX x in viewport space
     * @param viewportY y in viewport space
     * @return the corresponding position in local/content space
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
     * The point is inverse-transformed into local space, then checked against
     * the viewport bounds.
     *
     * @param parentX x in parent space (e.g. mouse position)
     * @param parentY y in parent space
     * @return {@code true} if the point maps into the viewport bounds
     */
    public boolean hitTest(double parentX, double parentY) {
        FloatPos local = parentToLocal(parentX, parentY);
        return isInsideViewport(local.x, local.y);
    }

    /**
     * Tests whether a parent-space point hits a local-space rectangle.
     * The point is inverse-transformed into local space via the full pipeline,
     * then checked against the rectangle.
     *
     * @param parentX x in parent space
     * @param parentY y in parent space
     * @param cx      local-space rectangle x
     * @param cy      local-space rectangle y
     * @param cw      local-space rectangle width
     * @param ch      local-space rectangle height
     * @return {@code true} if the point maps into the rectangle
     */
    public boolean hitTest(double parentX, double parentY,
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
     * Computes the axis-aligned bounding box in parent space for a local-space rect.
     * Useful for scissor/clip testing and culling.
     *
     * @param cx local-space rectangle x
     * @param cy local-space rectangle y
     * @param cw local-space rectangle width
     * @param ch local-space rectangle height
     * @return enclosing AABB in parent space (may be larger than the original due to rotation)
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
     * Computes the axis-aligned bounding box in parent space for a local-space rect.
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
     * When the pipeline contains no rotation this is an exact rectangle; otherwise
     * it is the axis-aligned bounding box of the visible region.
     */
    public Rect visibleContentBounds() {
        // Map the four corners of the viewport through the inverse view matrix
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
     * Computed as the geometric mean of the singular values of the 2×2 sub-matrix.
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
     * Returns 0 when content fits entirely.
     * <p>
     * Compatible with {@link dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState#scrollProgressX()}.
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
     * Compatible with {@link dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState#scrollProgressY()}.
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
     * Derived from the view matrix. Returns 1.0 if all content is visible.
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
     * Whether the content is scrollable horizontally (content wider than visible area).
     */
    public boolean canScrollHorizontally() {
        if (contentWidth <= 0) return false;
        FloatPos tl = viewportToContent(0, 0);
        FloatPos tr = viewportToContent(viewportWidth, 0);
        return Math.abs(tr.x - tl.x) < contentWidth;
    }

    /**
     * Whether the content is scrollable vertically (content taller than visible area).
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
     * Adjusts the {@link ViewportTransform.Translate} at the given pipeline index so
     * that the given local-space point maps to the given parent-space point.
     * <p>
     * This is the core algorithm behind zoom-at-cursor and rotate-at-cursor.
     * Given a pipeline split at the translate index:
     * <pre>{@code
     *   M = [T(origin) · t[0]…t[k-1]] · T(tx,ty) · [t[k+1]…t[n-1] · T(-origin)]
     *         ^^^^^^^^^^^^^^^^^^^^^^^^                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     *                before (B)                              after (A)
     *
     *   We need:  M · local = parent
     *   ⟹  (tx, ty) = B⁻¹ · parent − A · local
     * }</pre>
     *
     * @param translateIndex pipeline index of the Translate to adjust
     * @param localX         target local-space x
     * @param localY         target local-space y
     * @param parentX        target parent-space x
     * @param parentY        target parent-space y
     */
    @Contract("_,_,_,_,_ -> this")
    public Viewport adjustTranslateForPivot(int translateIndex,
                                            double localX, double localY,
                                            double parentX, double parentY) {
        float ox = (float) (originX * viewportWidth);
        float oy = (float) (originY * viewportHeight);

        // B = T(origin) · t[0] · … · t[k-1]
        Matrix3x2f before = new Matrix3x2f();
        if (ox != 0 || oy != 0) before.translate(ox, oy);
        for (int i = 0; i < translateIndex; i++) {
            transforms.get(i).apply(before);
        }

        // A = t[k+1] · … · t[n-1] · T(-origin)
        Matrix3x2f after = new Matrix3x2f();
        for (int i = translateIndex + 1; i < transforms.size(); i++) {
            transforms.get(i).apply(after);
        }
        if (ox != 0 || oy != 0) after.translate(-ox, -oy);

        // (tx, ty) = B⁻¹ · parent − A · local
        Matrix3x2f beforeInv = before.invert(new Matrix3x2f());
        Vector2f pp = new Vector2f((float) parentX, (float) parentY);
        beforeInv.transformPosition(pp);

        Vector2f lp = new Vector2f((float) localX, (float) localY);
        after.transformPosition(lp);

        double tx = pp.x - lp.x;
        double ty = pp.y - lp.y;

        transforms.set(translateIndex, ViewportTransform.translate(tx, ty));
        invalidate();
        return this;
    }

    /**
     * Multiplies the {@link ViewportTransform.Scale} at {@code scaleIndex} by a uniform
     * factor, then adjusts the {@link ViewportTransform.Translate} at {@code translateIndex}
     * so that the local-space point under the cursor stays stationary on screen.
     * <p>
     * This is the standard "zoom-to-cursor" operation. The scale is clamped to
     * [{@link #minScale()}, {@link #maxScale()}].
     *
     * @param scaleIndex     pipeline index of the Scale to modify
     * @param translateIndex pipeline index of the Translate to adjust for pivot
     * @param factor         multiplicative zoom factor (&gt;1 = zoom in)
     * @param parentX        x in parent space (e.g. mouse position)
     * @param parentY        y in parent space
     */
    @Contract("_,_,_,_,_ -> this")
    public Viewport zoomAt(int scaleIndex, int translateIndex,
                           double factor,
                           double parentX, double parentY) {
        return zoomAt(scaleIndex, translateIndex, factor, factor, parentX, parentY);
    }

    /**
     * Non-uniform variant of {@link #zoomAt(int, int, double, double, double)}.
     */
    @Contract("_,_,_,_,_,_ -> this")
    public Viewport zoomAt(int scaleIndex, int translateIndex,
                           double factorX, double factorY,
                           double parentX, double parentY) {
        FloatPos localPt = parentToLocal(parentX, parentY);

        ViewportTransform.Scale s = (ViewportTransform.Scale) transforms.get(scaleIndex);
        double newSx = clamp(s.sx() * factorX, minScale, maxScale);
        double newSy = clamp(s.sy() * factorY, minScale, maxScale);
        transforms.set(scaleIndex, ViewportTransform.scale(newSx, newSy));
        invalidate();

        adjustTranslateForPivot(translateIndex,
                localPt.x, localPt.y, parentX, parentY);
        return this;
    }

    /**
     * Rotates the {@link ViewportTransform.Rotate} at {@code rotateIndex} by a delta,
     * then adjusts the {@link ViewportTransform.Translate} at {@code translateIndex}
     * so that the local-space point under the cursor stays stationary on screen.
     *
     * @param rotateIndex    pipeline index of the Rotate to modify
     * @param translateIndex pipeline index of the Translate to adjust for pivot
     * @param deltaRadians   rotation delta in radians
     * @param parentX        x in parent space (e.g. cursor position)
     * @param parentY        y in parent space
     */
    @Contract("_,_,_,_,_ -> this")
    public Viewport rotateAt(int rotateIndex, int translateIndex,
                             double deltaRadians,
                             double parentX, double parentY) {
        FloatPos localPt = parentToLocal(parentX, parentY);

        ViewportTransform.Rotate r = (ViewportTransform.Rotate) transforms.get(rotateIndex);
        double newAngle = normalizeAngle(r.radians() + deltaRadians);
        transforms.set(rotateIndex, ViewportTransform.rotate(newAngle));
        invalidate();

        adjustTranslateForPivot(translateIndex,
                localPt.x, localPt.y, parentX, parentY);
        return this;
    }

    /**
     * Clamps the {@link ViewportTransform.Translate} at the given index so that the
     * visible content region stays within content bounds. This is a best-effort
     * operation — for pipelines with significant rotation, the result is approximate.
     *
     * @param translateIndex pipeline index of the Translate to clamp
     */
    @Contract("_ -> this")
    public Viewport clampTranslate(int translateIndex) {
        if (contentWidth <= 0 && contentHeight <= 0) return this;

        ViewportTransform.Translate t =
                (ViewportTransform.Translate) transforms.get(translateIndex);
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
            transforms.set(translateIndex, ViewportTransform.translate(dx, dy));
            invalidate();
        }
        return this;
    }

    //endregion

    //region matrix access

    /**
     * Returns the forward (local → parent) affine matrix.
     * This is the full pipeline including the layout step.
     * The returned matrix is a defensive copy.
     */
    public Matrix3x2f forwardMatrix() {
        if (forwardMatrix == null) {
            forwardMatrix = buildForwardMatrix();
        }
        return new Matrix3x2f(forwardMatrix);
    }

    /**
     * Returns the inverse (parent → local) affine matrix.
     */
    public Matrix3x2f inverseMatrix() {
        if (inverseMatrix == null) {
            Matrix3x2f fwd = forwardMatrix();
            inverseMatrix = fwd.invert(new Matrix3x2f());
        }
        return new Matrix3x2f(inverseMatrix);
    }

    /**
     * Returns the forward transform as a {@link Matrix4f} suitable for use with
     * {@link dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas SceneCanvas} and
     * {@code GuiGraphics}. The Z components are identity.
     */
    public Matrix4f toMatrix4f() {
        Matrix3x2f m = forwardMatrix();
        return new Matrix4f(
                m.m00, m.m01, 0, 0,
                m.m10, m.m11, 0, 0,
                0, 0, 1, 0,
                m.m20, m.m21, 0, 1
        );
    }

    /**
     * Returns whether the full composed transform is identity.
     * This is only true when the widget is at (0,0) with no extra transforms.
     */
    public boolean isIdentity() {
        Matrix3x2f m = forwardMatrix();
        return m.m00 == 1 && m.m01 == 0
               && m.m10 == 0 && m.m11 == 1
               && m.m20 == 0 && m.m21 == 0;
    }

    //endregion

    //region reset / copy

    /**
     * Resets to a clean state: only the {@code Layout(0, 0)} step, origin at top-left.
     * Dimensions are preserved.
     */
    @Contract("-> this")
    public Viewport reset() {
        transforms.clear();
        transforms.add(ViewportTransform.layout(0, 0));
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
        v.transforms.clear();
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

    private void invalidate() {
        forwardMatrix = null;
        inverseMatrix = null;
    }

    /**
     * Builds the forward (local → parent) affine matrix.
     * <p>
     * Pipeline: {@code M = T(origin) · transforms[0] · … · transforms[n-1] · T(-origin)}
     * <p>
     * JOML's {@link Matrix3x2f} uses post-multiplication: {@code m.op()} appends to
     * the right, so calls are chained in left-to-right order matching the formula.
     */
    private Matrix3x2f buildForwardMatrix() {
        float ox = (float) (originX * viewportWidth);
        float oy = (float) (originY * viewportHeight);

        Matrix3x2f m = new Matrix3x2f();
        if (ox != 0 || oy != 0) m.translate(ox, oy);

        for (ViewportTransform t : transforms) {
            t.apply(m);
        }

        if (ox != 0 || oy != 0) m.translate(-ox, -oy);

        return m;
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
               "transforms=" + transforms +
               ", viewport=" + viewportWidth + "x" + viewportHeight +
               ", content=" + contentWidth + "x" + contentHeight +
               ", origin=(" + originX + ", " + originY + ")" +
               '}';
    }
}
