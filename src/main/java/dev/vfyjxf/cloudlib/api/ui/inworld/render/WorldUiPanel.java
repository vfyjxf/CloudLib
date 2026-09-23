package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.internal.ui.inworld.UiSurface;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * One UI surface placed in the world: a logical w×h pixel canvas, a per-frame
 * {@link Placer} resolving its world quad, a {@link Painter} filling the
 * surface texture, and an optional {@link LinesEmitter} for companion
 * line geometry (scan frames, leader lines).
 * <p>
 * The pipeline is: {@code placer → surface FBO → sorted translucent draw}.
 * The panel itself is pure data + callbacks; a world UI renderer drives it
 * through the renderer-facing hooks at the bottom of this class.
 * <p>
 * <b>Content versioning (the dirty protocol).</b> By default a panel carries
 * no version and is treated as always dirty: the renderer repaints its
 * surface every frame, exactly like the pre-version pipeline. A panel whose
 * painted content derives fully from observable values may declare a
 * {@link #contentVersion(LongSupplier)}: while the long (and the surface's
 * shape) is unchanged, the renderer skips the repaint entirely (no painter
 * run, no FBO bind, no mip regeneration; the world quad keeps sampling the
 * existing texture). The version must cover <b>every</b> source of visual
 * change the painter reads (live values, animations, partial-tick-driven
 * effects); anything time-dependent belongs on a version-less panel, which
 * repaints per frame. {@link ContentVersions} has adapters for the common
 * shapes (constant, equals-compared record token).
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
        int viewportH
    ) {}

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

    /**
     * The panel's content version, or null — null means always dirty: the
     * renderer repaints the surface every frame (the default, and the exact
     * pre-version behavior). See the class docs for the protocol.
     */
    private @Nullable LongSupplier contentVersion;

    /** caller-defined payload slot */
    private @Nullable Object tag;

    // renderer-owned per-frame state
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

    /**
     * The minimum supersampling factor for the surface texture — the floor
     * the adaptive renderer raises from when the quad is magnified on screen
     * (an explicit value is a lower bound, not a fixed size).
     */
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

    // region content version

    /**
     * The panel's content version supplier, or null when the panel declares
     * none (always dirty — repaint every frame).
     *
     * <p>Renderer-facing: read on the render thread each frame before the
     * surface repaint decision.
     */
    public @Nullable LongSupplier contentVersion() {
        return contentVersion;
    }

    /**
     * Declares the panel's content version — the dirty protocol from the
     * class docs. While the returned long is unchanged (and the surface's
     * shape holds), the renderer skips the surface repaint. {@code null}
     * removes the version and restores always-dirty semantics.
     */
    public WorldUiPanel contentVersion(@Nullable LongSupplier contentVersion) {
        this.contentVersion = contentVersion;
        return this;
    }

    /** A constant content version — content that is fully built at setup and never changes. */
    public WorldUiPanel contentVersion(long version) {
        return contentVersion(ContentVersions.fixed(version));
    }

    /**
     * A content version derived from an equals-compared token — see
     * {@link ContentVersions#of(Supplier)}. Make the token a record over
     * every live value the painter reads. The adapter is stateful (it
     * counts its own observations), so create it once and keep it bound to
     * this panel — a per-frame-recreated adapter degrades to a constant and
     * never reports changes.
     */
    public WorldUiPanel contentVersion(Supplier<?> token) {
        return contentVersion(ContentVersions.of(token));
    }

    // endregion

    // region renderer internals

    /**
     * Repaints the panel's surface with its {@link #painter(Painter)} — the
     * renderer-facing surface entry. The offscreen target and the full GL
     * state save/restore live behind it (see the internal surface); callers
     * only choose the supersample factor and pass the quad's projected size
     * for the mip decision.
     *
     * @param supersample the granted supersample factor for this repaint
     * @param projectedW {@code projectedH} the quad's projected size in
     *     framebuffer pixels (0 when unknown) — mip levels are regenerated
     *     only while the world quad minifies the surface
     */
    public void renderSurface(int supersample, double projectedW, double projectedH, float partialTick) {
        surface().render(width, height, supersample, projectedW, projectedH, painter, partialTick);
    }

    /**
     * The supersample factor the surface was last rendered with — 0 while
     * never rendered. Debug/introspection (texture dump sizing).
     */
    public int surfaceSupersample() {
        return surface == null ? 0 : surface.supersample();
    }

    /** The surface's color texture object name, or 0 while unallocated — debug/introspection. */
    public int surfaceTextureId() {
        return surface == null ? 0 : surface.colorTextureId();
    }

    /** Releases the surface's GL objects — safe from any thread. */
    public void close() {
        UiSurface s = surface;
        surface = null;
        if (s != null) s.close();
    }

    private UiSurface surface() {
        if (surface == null) surface = new UiSurface();
        return surface;
    }

    // endregion
}
