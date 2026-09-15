package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.vfyjxf.cloudlib.api.ui.border.BorderPoint;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.internal.ui.inworld.UiSurface;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * One UI surface placed in the world: a logical w×h pixel canvas, a per-frame
 * {@link Placer} resolving its world quad, a {@link Painter} filling the
 * surface texture, and an optional {@link LinesEmitter} for companion
 * line geometry (scan frames, leader lines).
 * <p>
 * The pipeline is: {@code placer → surface FBO → sorted translucent draw}.
 * The panel itself is pure data + callbacks; {@link WorldUiRenderer} drives it.
 */
public final class WorldUiPanel {

    /** Draws the panel's content into its surface. Coordinates are gui px. */
    @FunctionalInterface
    public interface Painter {
        void paint(SceneCanvas canvas, int width, int height, float partialTick);
    }

    /** Resolves the panel's world quad for this frame; null → hidden. */
    @FunctionalInterface
    public interface Placer {
        @Nullable
        QuadBasis place(Frame frame);
    }

    /**
     * Emits POSITION_COLOR line vertices (positions baked through
     * worldToView) — the low-level escape hatch for companion line
     * geometry (scan frames, leader lines).
     */
    @FunctionalInterface
    public interface LinesEmitter {
        void emit(BufferBuilder buffer, Matrix4f worldToView);
    }

    /** Everything a placer needs for one frame. */
    public record Frame(
            Camera camera,
            Vec3 cameraPos,
            Matrix4f worldToView,
            Matrix4f viewToClip,
            float partialTick,
            ClientLevel level,
            /** framebuffer px — for screen-space trace/marker sizing. */
            int viewportW,
            int viewportH) {}

    private int width;
    private int height;
    private int supersample = 2;
    private float opacity = 1f;
    private boolean visible = true;
    /** depth-test against the world (false → hologram-style x-ray). */
    private boolean depthTested = true;
    /** polygon-offset decal bias — set for panels hugging a block face. */
    private boolean decal;

    private Painter painter = (canvas, w, h, pt) -> {};
    private Placer placer = frame -> null;
    private @Nullable LinesEmitter lines;

    /** caller-defined payload slot */
    private @Nullable Object tag;

    // renderer-owned per-frame state
    @Nullable
    QuadBasis basis;

    private @Nullable UiSurface surface;

    public WorldUiPanel(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // region descriptor

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public WorldUiPanel size(int w, int h) {
        this.width = w;
        this.height = h;
        return this;
    }

    /** Supersampling factor for the surface texture (2 = 2× MSAA-like crispness). */
    public int supersample() {
        return supersample;
    }

    public WorldUiPanel supersample(int ss) {
        this.supersample = Math.max(1, ss);
        return this;
    }

    /** Global opacity 0..1 — multiplies every accumulated texel's alpha. */
    public float opacity() {
        return opacity;
    }

    public WorldUiPanel opacity(float opacity) {
        this.opacity = opacity;
        return this;
    }

    public boolean visible() {
        return visible;
    }

    public WorldUiPanel visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean depthTested() {
        return depthTested;
    }

    public WorldUiPanel depthTested(boolean depthTested) {
        this.depthTested = depthTested;
        return this;
    }

    public boolean decal() {
        return decal;
    }

    public WorldUiPanel decal(boolean decal) {
        this.decal = decal;
        return this;
    }

    public Painter painter() {
        return painter;
    }

    public WorldUiPanel painter(Painter painter) {
        this.painter = painter;
        return this;
    }

    public Placer placer() {
        return placer;
    }

    public WorldUiPanel placer(Placer placer) {
        this.placer = placer;
        return this;
    }

    public @Nullable LinesEmitter lines() {
        return lines;
    }

    public WorldUiPanel lines(@Nullable LinesEmitter lines) {
        this.lines = lines;
        return this;
    }

    public @Nullable Object tag() {
        return tag;
    }

    public WorldUiPanel tag(@Nullable Object tag) {
        this.tag = tag;
        return this;
    }

    // endregion

    // region renderer internals

    /** The surface FBO — created lazily on the render thread. */
    UiSurface surface() {
        if (surface == null) surface = new UiSurface();
        return surface;
    }

    /** The surface's color texture object name, or 0 while unallocated — debug/introspection. */
    public int surfaceTextureId() {
        return surface == null ? 0 : surface.colorTextureId();
    }

    /** The quad resolved this frame, or null when hidden. */
    public @Nullable QuadBasis basis() {
        return basis;
    }

    /**
     * The panel's measured border — edge/corner points and attach ports —
     * or null while the panel has no placed quad. This is the connection
     * information a trace (or any caller drawing companion geometry)
     * resolves against.
     */
    public @Nullable QuadBorder border() {
        return QuadBorder.of(this);
    }

    /**
     * Resolves a {@link BorderPoint} spec against the live border —
     * {@code toward} feeds {@code nearest} resolution. Null while hidden.
     */
    public @Nullable WorldPort port(BorderPoint at, @Nullable Vec3 toward) {
        QuadBorder b = border();
        return b == null ? null : b.port(at, toward);
    }

    /** The border port closest to {@code toward}, or null while hidden. */
    public @Nullable WorldPort nearestPort(Vec3 toward) {
        QuadBorder b = border();
        return b == null ? null : b.nearestPort(toward);
    }

    /** Releases the surface's GL objects — safe from any thread. */
    public void close() {
        UiSurface s = surface;
        surface = null;
        if (s != null) s.close();
    }

    // endregion
}
