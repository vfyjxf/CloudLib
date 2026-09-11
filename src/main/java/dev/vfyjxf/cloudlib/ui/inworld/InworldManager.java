package dev.vfyjxf.cloudlib.ui.inworld;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.base.host.InworldSceneHost;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPositioning;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUiApi;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.cloudlib.ui.KeyMappings;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The client-side runtime of the in-world UI layer.
 * <p>
 * Owns a single {@link Scene} (host: {@link InworldSceneHost}) whose children are
 * {@link InworldPanelWidget} chrome widgets. Per frame the manager captures the
 * level-render matrices into a {@link Projection}, resolves every panel's
 * anchor → screen position (or face geometry), then renders:
 * <ul>
 *   <li>face panels inside {@code RenderLevelStageEvent} in world space;</li>
 *   <li>floating/follow panels in {@code RenderGuiEvent.Post} in screen space;</li>
 *   <li>everything flattened in the inspect presentation while the inspect key
 *       is held (see {@link InworldInspectScreen}).</li>
 * </ul>
 * Input is unified through the scene's dispatch pipeline — world-mode input
 * uses a virtual pointer (crosshair, or the raycast point on a face panel) so
 * hover/click/focus semantics are identical between presentations.
 */
public final class InworldManager implements InworldUiApi {

    private static @Nullable InworldManager instance;

    public static @Nullable InworldManager instance() {
        return instance;
    }

    /**
     * Registers the in-world key mappings. Must run on the mod bus during
     * client construction — {@link RegisterKeyMappingsEvent} fires before
     * {@code FMLLoadCompleteEvent}, where {@link #init()} runs.
     */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyMappings.inspect);
        event.register(KeyMappings.focusNext);
        event.register(KeyMappings.focusPrevious);
    }

    public static void init() {
        if (instance != null) return;
        var manager = new InworldManager();
        instance = manager;
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(manager::onClientTick);
        bus.addListener(manager::onLevelStage);
        bus.addListener(manager::onGuiRender);
        bus.addListener(manager::onMouseButton);
        bus.addListener(manager::onMouseScroll);
        bus.addListener(manager::onLoggingOut);
    }

    //region state

    private final Minecraft mc = Minecraft.getInstance();

    private final WidgetGroup<Widget> root = new WidgetGroup<>();
    private final Scene scene = new Scene(root);

    private final Map<Object, PanelRuntime> panels = new LinkedHashMap<>();
    private final Map<Object, InworldPanelSpec> imperative = new LinkedHashMap<>();
    private final List<ProviderRegistration> providers = new ArrayList<>();

    private @Nullable PanelRuntime focused;
    private @Nullable PanelRuntime pointed;
    private @Nullable FloatPos pointedUv;

    private boolean inspecting;
    private @Nullable InworldInspectScreen inspectScreen;
    /** Set when the inspect screen was closed by ESC while the key is still held — don't reopen until released. */
    private boolean inspectDismissed;
    private boolean pressedConsumed;

    private @Nullable Projection projection;
    private @Nullable Matrix4f worldToView;
    private float framePartialTick;
    private long tick;

    private int parkCursor = 0;
    /** Reused per-frame collection of presented dock panels awaiting corner layout. */
    private final List<PanelRuntime> dockQueue = new ArrayList<>();
    private static final int PARK_BASE = -1_000_000;
    private static final int PARK_STEP = 4096;

    private record ProviderRegistration(InworldProvider provider, int interval, long nextRun) {
    }

    /** Each provider's most recent emission — persisted between its runs so reconcile doesn't drop panels on off-ticks. */
    private final Map<InworldProvider, Map<Object, InworldPanelSpec>> providerPanels = new IdentityHashMap<>();

    //endregion

    private InworldManager() {
        scene.init();
        scene.mount(SceneContext.create(new InworldSceneHost()));
    }

    //region InworldUiApi

    @Override
    public InworldPanel show(InworldPanelSpec spec) {
        imperative.put(spec.key(), spec);
        PanelRuntime runtime = panels.get(spec.key());
        if (runtime == null) {
            runtime = createPanel(spec);
        } else {
            runtime.spec = spec;
            applySpec(runtime);
        }
        return runtime;
    }

    @Override
    public void close(Object key) {
        imperative.remove(key);
        PanelRuntime runtime = panels.remove(key);
        if (runtime != null) {
            root.remove(runtime.widget);
            if (focused == runtime) focused = null;
            if (pointed == runtime) pointed = null;
        }
    }

    @Override
    public void registerProvider(InworldProvider provider, int intervalTicks) {
        providers.add(new ProviderRegistration(provider, Math.max(1, intervalTicks), 0));
    }

    @Override
    public void unregisterProvider(InworldProvider provider) {
        providers.removeIf(r -> r.provider() == provider);
        if (providerPanels.remove(provider) != null) {
            Map<Object, InworldPanelSpec> wanted = new LinkedHashMap<>(imperative);
            for (Map<Object, InworldPanelSpec> emitted : providerPanels.values()) {
                for (InworldPanelSpec spec : emitted.values()) {
                    wanted.putIfAbsent(spec.key(), spec);
                }
            }
            reconcile(wanted);
        }
    }

    @Override
    public Collection<? extends InworldPanel> panels() {
        return Collections.unmodifiableCollection(panels.values());
    }

    @Override
    public @Nullable InworldPanel panel(Object key) {
        return panels.get(key);
    }

    @Override
    public @Nullable InworldPanel focused() {
        return focused;
    }

    @Override
    public void focus(@Nullable InworldPanel panel) {
        PanelRuntime runtime = panel == null ? null : panels.get(panel.key());
        if (runtime == focused) return;
        focused = runtime;
        if (runtime != null) {
            scene.requestFocus(runtime.widget);
        }
    }

    @Override
    public void focusNext() {
        focusStep(1);
    }

    @Override
    public void focusPrevious() {
        focusStep(-1);
    }

    @Override
    public boolean inspecting() {
        return inspecting;
    }

    @Override
    public List<Rect2i> exclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.presented && runtime.flat) {
                areas.add(new Rect2i(
                        runtime.widget.screenX, runtime.widget.screenY,
                        runtime.widget.width(), runtime.widget.height()));
            }
        }
        return areas;
    }

    @Override
    public Scene scene() {
        return scene;
    }

    //endregion

    //region events

    private void onClientTick(ClientTickEvent.Post event) {
        tick++;
        if (mc.level == null || mc.player == null) return;

        //inspect state machine — raw key polling so it works while the capture screen is open
        boolean held = inspectHeld();
        if (!held) inspectDismissed = false;
        if (held && !inspecting && !inspectDismissed && mc.screen == null) {
            inspecting = true;
            inspectScreen = new InworldInspectScreen(this);
            mc.setScreen(inspectScreen);
        }
        if (inspecting && mc.screen != inspectScreen) {
            inspecting = false;
            inspectScreen = null;
        }

        //focus cycling
        while (KeyMappings.focusNext.consumeClick()) focusNext();
        while (KeyMappings.focusPrevious.consumeClick()) focusPrevious();

        //providers — each provider's last emission is cached; reconcile runs
        //when at least one provider was re-evaluated (or on the first tick so
        //imperative panels created before providers registered still show)
        if (mc.level != null) {
            boolean ran = tick == 1;
            InworldContext ctx = new InworldContext(mc.level, mc.player, mc.gameRenderer.getMainCamera(), currentProjection(), tick);
            for (int i = 0; i < providers.size(); i++) {
                ProviderRegistration reg = providers.get(i);
                if (tick >= reg.nextRun()) {
                    Map<Object, InworldPanelSpec> emitted = new LinkedHashMap<>();
                    reg.provider().provide(ctx, spec -> emitted.putIfAbsent(spec.key(), spec));
                    providerPanels.put(reg.provider(), emitted);
                    providers.set(i, new ProviderRegistration(reg.provider(), reg.interval(), tick + reg.interval()));
                    ran = true;
                }
            }
            if (ran) {
                Map<Object, InworldPanelSpec> wanted = new LinkedHashMap<>(imperative);
                for (Map<Object, InworldPanelSpec> emitted : providerPanels.values()) {
                    for (InworldPanelSpec spec : emitted.values()) {
                        wanted.putIfAbsent(spec.key(), spec);
                    }
                }
                reconcile(wanted);
            }
        }

        scene.tick();
    }

    private void onLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (mc.level == null || mc.player == null) return;
        if (mc.options.hideGui) return;

        //capture this frame's projection. Note: the per-rendertype stages pass
        //no pose stack (getPoseStack() is a fresh identity stack); the real
        //world→view matrix is getModelViewMatrix(), which is camera ROTATION
        //only — the −cameraPos translation happens inside renderSectionLayer.
        Vec3 cameraPos = event.getCamera().getPosition();
        framePartialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Matrix4f worldToView = new Matrix4f(event.getModelViewMatrix());
        worldToView.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        this.worldToView = worldToView;
        Matrix4f viewToClip = event.getProjectionMatrix();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        projection = Projection.capture(worldToView, viewToClip, cameraPos, w, h);

        resolvePanels();

        //in-world render pass: face panels only in world presentation; the
        //scan frame is useful in both (inspect's leader lines end on it)
        if (!inspecting) {
            renderFacePanels(event, cameraPos);
        }
        renderScanFrames();
    }

    private void onGuiRender(RenderGuiEvent.Post event) {
        if (mc.level == null || mc.player == null) return;
        if (inspecting) return;                       // the inspect screen renders instead
        if (mc.screen != null) return;                // compat: a real screen hides in-world ui
        if (mc.options.hideGui || panels.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        //virtual pointer: pointed face uv > screen center
        double vx = mc.getWindow().getGuiScaledWidth() * 0.5;
        double vy = mc.getWindow().getGuiScaledHeight() * 0.5;
        updatePointing();
        if (pointed != null && pointedUv != null) {
            vx = pointed.inputSceneX() + pointedUv.x;
            vy = pointed.inputSceneY() + pointedUv.y;
        }

        renderScreenSpace(graphics, vx, vy, pt);
    }

    private void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (inspecting || mc.level == null || mc.screen != null) return;
        int action = event.getAction();
        int button = event.getButton();
        if (action == GLFW.GLFW_PRESS) {
            double[] v = virtualPointer();
            boolean consumed = scene.mouseClicked(v[0], v[1], button);
            pressedConsumed = consumed;
            if (consumed) event.setCanceled(true);
        } else if (action == GLFW.GLFW_RELEASE) {
            double[] v = virtualPointer();
            boolean consumed = scene.mouseReleased(v[0], v[1], button);
            if (consumed || pressedConsumed) event.setCanceled(true);
            pressedConsumed = false;
        }
    }

    private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (inspecting || mc.level == null || mc.screen != null) return;
        if (panels.isEmpty()) return;
        double[] v = virtualPointer();
        if (scene.mouseScrolled(v[0], v[1], event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (PanelRuntime runtime : panels.values()) {
            root.remove(runtime.widget);
        }
        panels.clear();
        imperative.clear();
        focused = null;
        pointed = null;
        inspecting = false;
        inspectScreen = null;
    }

    //endregion

    //region providers & lifecycle

    private void reconcile(Map<Object, InworldPanelSpec> wanted) {
        //remove panels no longer offered
        var it = panels.values().iterator();
        while (it.hasNext()) {
            PanelRuntime runtime = it.next();
            if (!wanted.containsKey(runtime.key())) {
                it.remove();
                root.remove(runtime.widget);
                if (focused == runtime) focused = null;
                if (pointed == runtime) pointed = null;
            }
        }
        //create missing / refresh surviving
        for (InworldPanelSpec spec : wanted.values()) {
            PanelRuntime runtime = panels.get(spec.key());
            if (runtime == null) {
                createPanel(spec);
            } else if (runtime.spec != spec) {
                runtime.spec = spec;
                applySpec(runtime);
            }
        }
    }

    private PanelRuntime createPanel(InworldPanelSpec spec) {
        PanelRuntime runtime = new PanelRuntime(this, spec);
        InworldPanelContext ctx = new InworldPanelContext(mc.level, mc.player, runtime);
        Widget content = spec.content().apply(ctx);
        runtime.widget = new InworldPanelWidget(runtime, spec, content);
        panels.put(spec.key(), runtime);
        root.addWidget(runtime.widget);
        return runtime;
    }

    private void applySpec(PanelRuntime runtime) {
        runtime.widget.setTitle(runtime.spec.title());
        runtime.widget.setHints(runtime.spec.hints());
        runtime.widget.setInteractive(runtime.spec.interactive());
    }

    //endregion

    //region per-frame resolution

    private @Nullable Projection currentProjection() {
        if (projection == null) {
            //no level render yet — synthesize an identity-ish projection so providers can still run
            Matrix4f identity = new Matrix4f();
            projection = Projection.capture(identity, identity, Vec3.ZERO,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
        return projection;
    }

    /**
     * Resolves every panel's anchor → placement → screen/face geometry and
     * pushes the result into the chrome widget's pending layout position.
     */
    private void resolvePanels() {
        Projection proj = projection;
        ClientLevel level = mc.level;
        if (proj == null || level == null) return;

        parkCursor = 0;
        List<PanelRuntime> docked = dockQueue;

        for (PanelRuntime runtime : panels.values()) {
            runtime.pointedUv = null;
            runtime.anchorWorld = runtime.spec.anchor().position(level, framePartialTick);
            runtime.anchorScreen = null;
            runtime.presented = false;
            runtime.flat = false;

            Vec3 anchor = runtime.anchorWorld;
            if (anchor == null || !runtime.widget.visible()) continue;

            runtime.distance = proj.distance(anchor);
            if (runtime.distance > runtime.spec.maxDistance()) continue;

            runtime.anchorScreen = proj.worldToScreen(anchor);

            InworldPlacement placement = runtime.spec.placement();
            if (inspecting) {
                //flat projection: every panel docks to a screen corner
                resolveDock(runtime, inspectPlacement(placement), docked);
            } else {
                switch (placement) {
                    case InworldPlacement.Face face -> resolveFace(runtime, face);
                    case InworldPlacement.Floating floating -> {
                        resolveFloating(runtime, floating);
                        runtime.flat = true;
                    }
                    case InworldPlacement.Follow follow -> {
                        resolveFollow(runtime, follow);
                        runtime.flat = true;
                    }
                    case InworldPlacement.Dock dock -> resolveDock(runtime, dock, docked);
                }
            }
        }

        layoutDocks(docked);
        docked.clear();

        //apply layout so widget bounds are fresh for this frame
        scene.stabilize();
    }

    /** Inspect flattens every placement into a corner dock. */
    private static InworldPlacement.Dock inspectPlacement(InworldPlacement original) {
        if (original instanceof InworldPlacement.Dock dock) return dock;
        return new InworldPlacement.Dock(InworldPlacement.DockCorner.AUTO);
    }

    private void resolveFloating(PanelRuntime runtime, InworldPlacement.Floating placement) {
        FloatPos anchorPx = runtime.anchorScreen;
        if (anchorPx == null) {
            //anchor off-screen → park far away
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }
        var result = FloatingPositioning.compute(
                new Rect((int) anchorPx.x - 1, (int) anchorPx.y - 1, 2, 2),
                new Rect(0, 0, runtime.widget.width(), runtime.widget.height()),
                new Rect(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()),
                placement.placement(), placement.middlewares()
        );
        if (floatingHidden(result)) {
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }
        runtime.presented = true;
        runtime.widget.setScreenPos((int) result.x(), (int) result.y());
    }

    private static boolean floatingHidden(FloatingPositioning.PositionResult result) {
        Map<String, Object> hide = result.middlewareData().get("hide");
        if (hide == null) return false;
        return Boolean.TRUE.equals(hide.get("referenceHidden"))
                || Boolean.TRUE.equals(hide.get("escaped"));
    }

    private void resolveFollow(PanelRuntime runtime, InworldPlacement.Follow follow) {
        FloatPos anchorPx = runtime.anchorScreen;
        if (anchorPx == null) {
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }
        runtime.presented = true;
        runtime.widget.setScreenPos(
                (int) (anchorPx.x - runtime.widget.width() * 0.5 + follow.offsetX()),
                (int) (anchorPx.y - runtime.widget.height() * 0.5 + follow.offsetY())
        );
    }

    private void resolveDock(PanelRuntime runtime, InworldPlacement.Dock dock, List<PanelRuntime> docked) {
        if (runtime.anchorScreen == null) {
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }
        runtime.presented = true;
        runtime.flat = true;
        runtime.dockCorner = dock.corner();
        docked.add(runtime);
    }

    /**
     * Packs docked panels into their screen corners: AUTO picks the quadrant
     * the anchor projects into, panels stack from the corner inward in offer
     * order, positions are pure screen-space so they never jitter.
     */
    private void layoutDocks(List<PanelRuntime> docked) {
        if (docked.isEmpty()) return;
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int marginX = 8, marginY = 8, gap = 6;

        for (PanelRuntime runtime : docked) {
            InworldPlacement.DockCorner corner = runtime.dockCorner;
            if (corner == InworldPlacement.DockCorner.AUTO) {
                FloatPos anchorPx = runtime.anchorScreen;
                boolean left = anchorPx != null && anchorPx.x < W * 0.5f;
                boolean top = anchorPx == null || anchorPx.y < H * 0.5f;
                corner = top
                        ? (left ? InworldPlacement.DockCorner.TOP_LEFT : InworldPlacement.DockCorner.TOP_RIGHT)
                        : (left ? InworldPlacement.DockCorner.BOTTOM_LEFT : InworldPlacement.DockCorner.BOTTOM_RIGHT);
            }
            int slot = dockCursors[corner.ordinal()];
            int w = runtime.widget.width();
            int h = runtime.widget.height();
            int x = switch (corner) {
                case TOP_LEFT, BOTTOM_LEFT -> marginX;
                default -> W - marginX - w;
            };
            int y = switch (corner) {
                case TOP_LEFT, TOP_RIGHT -> marginY + slot;
                default -> H - marginY - h - slot;
            };
            dockCursors[corner.ordinal()] = slot + h + gap;
            runtime.widget.setScreenPos(x, y);
        }
        Arrays.fill(dockCursors, 0);
    }

    private final int[] dockCursors = new int[InworldPlacement.DockCorner.values().length];

    /**
     * Computes the panel's world-space rect on its face. In world presentation
     * the widget is parked off-screen (it is rendered during the level pass);
     * its scene-space slot doubles as the synthesized pointer coordinate space.
     */
    private void resolveFace(PanelRuntime runtime, InworldPlacement.Face face) {
        var blockPos = runtime.spec.anchor().blockPos();
        if (blockPos == null) {
            //face placement requires a block-bound anchor
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }
        runtime.flat = false;
        runtime.presented = true;

        double s = 1.0 / face.pixelsPerBlock();
        runtime.facePpb = face.pixelsPerBlock();

        Direction dir = face.face();
        Vec3 n = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 uAxis = faceUAxis(dir);
        Vec3 vAxis = faceVAxis(dir);

        //center of the panel on the face, pushed a hair off the surface
        Vec3 facePoint = Vec3.atCenterOf(blockPos)
                .add(n.scale(0.5))
                .add(uAxis.scale(face.u() - 0.5))
                .add(vAxis.scale(face.v() - 0.5))
                .add(n.scale(0.002 + 2 * s));

        runtime.faceU = uAxis.scale(s);
        runtime.faceV = vAxis.scale(s);
        runtime.faceNormal = n;
        runtime.faceOrigin = facePoint
                .subtract(runtime.faceU.scale(runtime.widget.width() * 0.5))
                .subtract(runtime.faceV.scale(runtime.widget.height() * 0.5));

        runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
    }

    /**
     * Panel +x axis in world space for each face (right as seen from outside
     * the face). With v = down and n_col = u×v the basis is right-handed and
     * unmirrored; u×v ends up opposite the face normal — that's expected for
     * GUI-style quads whose front faces the viewer.
     */
    private static Vec3 faceUAxis(Direction face) {
        return switch (face) {
            case NORTH -> new Vec3(-1, 0, 0);
            case SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, 0, 1);
            case EAST -> new Vec3(0, 0, -1);
            case UP -> new Vec3(1, 0, 0);
            case DOWN -> new Vec3(1, 0, 0);
        };
    }

    /** Panel +y axis in world space for each face (down as seen from outside the face). */
    private static Vec3 faceVAxis(Direction face) {
        return switch (face) {
            case NORTH, SOUTH, WEST, EAST -> new Vec3(0, -1, 0);
            case UP -> new Vec3(0, 0, 1);
            case DOWN -> new Vec3(0, 0, -1);
        };
    }

    //endregion

    //region pointing & focus

    /**
     * Recomputes which panel the player is currently pointing at in world mode:
     * face panels via a real 3D raycast, screen-space panels via the scene hit
     * test at the crosshair, or the panel whose anchor block is looked at.
     */
    private void updatePointing() {
        Projection proj = projection;
        pointed = null;
        pointedUv = null;
        if (proj == null || mc.level == null) return;

        Vec3 origin = proj.cameraPos();
        Vec3 dir = proj.crosshairDirection();

        //1. crosshair ray against face panels
        double bestT = Double.MAX_VALUE;
        for (PanelRuntime runtime : panels.values()) {
            if (!(runtime.spec.placement() instanceof InworldPlacement.Face)) continue;
            if (!runtime.presented || !runtime.widget.visible()) continue;
            FloatPos uv = Projection.rayPlane(origin, dir, runtime.faceOrigin,
                    runtime.faceU, runtime.faceV, runtime.faceNormal,
                    runtime.widget.width(), runtime.widget.height());
            if (uv == null) continue;
            double dist = distanceAlongRay(origin, dir, runtime);
            if (dist < bestT) {
                bestT = dist;
                pointed = runtime;
                pointedUv = uv;
            }
        }

        //2. crosshair over a flat panel
        if (pointed == null) {
            double cx = mc.getWindow().getGuiScaledWidth() * 0.5;
            double cy = mc.getWindow().getGuiScaledHeight() * 0.5;
            Widget hit = scene.hitTest(cx, cy);
            pointed = panelOf(hit);
        }

        //3. crosshair on an anchor block
        if (pointed == null && mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            for (PanelRuntime runtime : panels.values()) {
                if (runtime.presented && blockHit.getBlockPos().equals(runtime.anchor().blockPos())) {
                    pointed = runtime;
                    break;
                }
            }
        }

        if (pointed != null && !pointed.spec.interactive()) {
            pointed = null;
            pointedUv = null;
        }

        //world mode focus follows pointing
        if (!inspecting) {
            PanelRuntime newFocus = pointed;
            if (newFocus != focused) {
                focused = newFocus;
                if (focused != null) scene.requestFocus(focused.widget);
            }
        }
    }

    private double distanceAlongRay(Vec3 origin, Vec3 dir, PanelRuntime runtime) {
        Vec3 to = runtime.faceOrigin.subtract(origin);
        double denom = dir.dot(runtime.faceNormal);
        if (Math.abs(denom) < 1e-7) return Double.MAX_VALUE;
        return to.dot(runtime.faceNormal) / denom;
    }

    /** The scene-space position the world-mode pointer currently resolves to. */
    private double[] virtualPointer() {
        updatePointing();
        if (pointed != null && pointedUv != null) {
            return new double[]{pointed.inputSceneX() + pointedUv.x, pointed.inputSceneY() + pointedUv.y};
        }
        return new double[]{
                mc.getWindow().getGuiScaledWidth() * 0.5,
                mc.getWindow().getGuiScaledHeight() * 0.5
        };
    }

    private @Nullable PanelRuntime panelOf(@Nullable Widget widget) {
        Widget current = widget;
        while (current != null) {
            if (current instanceof InworldPanelWidget panel) {
                return panel.runtime;
            }
            current = current.parent();
        }
        return null;
    }

    private void focusStep(int direction) {
        if (panels.isEmpty()) return;
        List<PanelRuntime> order = panels.values().stream().filter(r -> r.presented).toList();
        if (order.isEmpty()) return;
        int idx = order.indexOf(focused);
        int next = idx < 0
                ? (direction > 0 ? 0 : order.size() - 1)
                : (idx + direction + order.size()) % order.size();
        focus(order.get(next));
    }

    //endregion

    //region rendering

    /** Renders face panels in world space during the level stage. */
    private void renderFacePanels(RenderLevelStageEvent event, Vec3 cameraPos) {
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        //vertices are pre-transformed to view space by the widget pose below —
        //the shader still multiplies ProjMat·ModelViewMat, so force ModelView
        //to identity or the leftover rotation applies twice
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        //batched quads may wind clockwise from the viewing side — draw them two-sided
        RenderSystem.disableCull();
        for (PanelRuntime runtime : panels.values()) {
            if (!(runtime.spec.placement() instanceof InworldPlacement.Face)) continue;
            if (!runtime.presented || !runtime.widget.visible()) continue;

            PoseStack pose = new PoseStack();
            //worldToView already contains the −cam translation, so the panel's
            //world-space origin is translated verbatim
            pose.last().pose().set(worldToView != null ? worldToView : event.getModelViewMatrix());
            pose.translate(runtime.faceOrigin.x, runtime.faceOrigin.y, runtime.faceOrigin.z);
            pose.last().pose().mul(faceBasis(runtime));

            GuiGraphics graphics = new GuiGraphics(mc, pose, buffers);
            SceneCanvas canvas = SceneCanvas.create(graphics);
            canvas.preserveDepth();

            FloatPos uv = runtime == pointed ? pointedUv : null;
            runtime.widget.setFrameState(runtime.focused(), uv != null);
            runtime.widget.render(canvas,
                    uv != null ? (int) uv.x : -1,
                    uv != null ? (int) uv.y : -1,
                    event.getPartialTick().getGameTimeDeltaPartialTick(true));
            canvas.flushBatch();
            graphics.flush();
        }
        RenderSystem.enableCull();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    //region scan frame

    /** world-space scan frame corners: which axis deltas each corner owns */
    private static final int[][] SCAN_EDGES = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0},   //bottom loop
            {4, 5}, {5, 7}, {7, 6}, {6, 4},   //top loop
            {0, 4}, {1, 5}, {2, 6}, {3, 7}    //pillars
    };
    private static final int[][] SCAN_CORNERS = new int[8][3];

    static {
        for (int i = 0; i < 8; i++) {
            int x = i & 1, y = (i >> 1) & 1, z = (i >> 2) & 1;
            SCAN_CORNERS[i][0] = (x ^ 1) | (y << 1) | (z << 2); //x-neighbor
            SCAN_CORNERS[i][1] = x | ((y ^ 1) << 1) | (z << 2); //y-neighbor
            SCAN_CORNERS[i][2] = x | (y << 1) | ((z ^ 1) << 2); //z-neighbor
        }
    }

    /**
     * Draws the hacker-style scan frame around every block that currently
     * hosts a presented panel: dim box edges, brighter corner ticks and a
     * bright segment sweeping the top loop. Dedupes shared anchor blocks.
     */
    private void renderScanFrames() {
        if (worldToView == null || mc.level == null) return;
        Set<BlockPos> framed = new HashSet<>();
        Set<BlockPos> hot = new HashSet<>();
        for (PanelRuntime runtime : panels.values()) {
            if (!runtime.presented || !runtime.widget.visible()) continue;
            BlockPos pos = runtime.spec.anchor().blockPos();
            if (pos == null) continue;
            framed.add(pos);
            if (runtime.focused() || runtime == pointed) hot.add(pos);
        }
        if (framed.isEmpty()) return;

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        double t = (mc.level.getGameTime() + framePartialTick) * 0.9;
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = worldToView;
        for (BlockPos pos : framed) {
            emitScanFrame(buffer, mat, pos, t, hot.contains(pos));
        }
        var mesh = buffer.build();
        if (mesh != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(mesh);
        }

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void emitScanFrame(BufferBuilder buffer, Matrix4f mat, BlockPos pos, double t, boolean bright) {
        double e = 0.003;
        double x0 = pos.getX() - e, y0 = pos.getY() - e, z0 = pos.getZ() - e;
        double x1 = pos.getX() + 1 + e, y1 = pos.getY() + 1 + e, z1 = pos.getZ() + 1 + e;
        double[][] c = {
                {x0, y0, z0}, {x1, y0, z0}, {x0, y0, z1}, {x1, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x0, y1, z1}, {x1, y1, z1}
        };

        int edge = bright ? InworldTheme.SCAN_EDGE_HOT : InworldTheme.SCAN_EDGE;
        int tick = bright ? InworldTheme.SCAN_TICK_HOT : InworldTheme.SCAN_TICK;
        for (int[] pair : SCAN_EDGES) {
            line(buffer, mat, c[pair[0]], c[pair[1]], edge);
        }
        //corner ticks: short brighter stubs from each corner along its edges
        double tl = 0.14;
        for (int i = 0; i < 8; i++) {
            for (int nb : SCAN_CORNERS[i]) {
                double[] a = c[i], b = c[nb];
                double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
                line(buffer, mat, a, new double[]{a[0] + dx * tl, a[1] + dy * tl, a[2] + dz * tl}, tick);
            }
        }
        //scan segment sweeping the top loop
        double s = ((t % 4) + 4) % 4;
        int seg = (int) s;
        double f = s - seg;
        double[] a = c[SCAN_EDGES[4 + seg][0]];
        double[] b = c[SCAN_EDGES[4 + seg][1]];
        double len = 0.22;
        double f1 = Math.max(0, f - len);
        double[] p0 = {a[0] + (b[0] - a[0]) * f1, a[1] + (b[1] - a[1]) * f1, a[2] + (b[2] - a[2]) * f1};
        double[] p1 = {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
        line(buffer, mat, p0, p1, InworldTheme.SCAN_SWEEP);
    }

    private static void line(BufferBuilder buffer, Matrix4f mat, double[] a, double[] b, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float bl = (color & 0xFF) / 255f;
        buffer.addVertex(mat, (float) a[0], (float) a[1], (float) a[2]).setColor(r, g, bl, alpha);
        buffer.addVertex(mat, (float) b[0], (float) b[1], (float) b[2]).setColor(r, g, bl, alpha);
    }

    //endregion

    /**
     * Local px → world basis: x→u, y→v, z→u×v (normalized back to 1px depth).
     * u×v points opposite the face normal — same handedness as GUI screen
     * space, so fills/text are unmirrored for a viewer outside the face.
     */
    private static Matrix4f faceBasis(PanelRuntime runtime) {
        Vec3 u = runtime.faceU;
        Vec3 v = runtime.faceV;
        Vec3 w = u.cross(v);
        double len = w.length();
        if (len > 0) w = w.scale(1.0 / len / runtime.facePpb);
        Matrix4f m = new Matrix4f();
        m.m00((float) u.x); m.m10((float) u.y); m.m20((float) u.z);
        m.m01((float) v.x); m.m11((float) v.y); m.m21((float) v.z);
        m.m02((float) w.x); m.m12((float) w.y); m.m22((float) w.z);
        m.m33(1);
        return m;
    }

    /**
     * Renders the flat (screen-space) part of the layer: leader lines first,
     * then the panel tree, then the hover tooltip.
     */
    private void renderScreenSpace(GuiGraphics graphics, double pointerX, double pointerY, float partialTick) {
        scene.mouseMoved(pointerX, pointerY);
        scene.setLayoutArea(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

        SceneCanvas canvas = SceneCanvas.create(graphics);
        for (PanelRuntime runtime : panels.values()) {
            runtime.widget.setFrameState(runtime.focused(), runtime == pointed);
        }

        renderLeaderLines(graphics);

        canvas.pushViewport(root.viewport());
        root.render(canvas, (int) pointerX, (int) pointerY, partialTick);
        canvas.popViewport();
        canvas.flushBatch();

        renderTooltip(graphics, pointerX, pointerY);
    }

    private void renderTooltip(GuiGraphics graphics, double pointerX, double pointerY) {
        Widget hit = scene.hitTest(pointerX, pointerY);
        if (hit == null) return;
        Tooltip tooltip = hit.hoverTooltip((int) (pointerX - hit.absolutePos().x()), (int) (pointerY - hit.absolutePos().y()));
        if (tooltip != null && tooltip.notEmpty()) {
            ScreenUtil.renderTooltip(graphics, tooltip, (int) pointerX, (int) pointerY);
        }
    }

    /** Draws a connector line from each flat panel's edge to its anchor's scan frame. */
    private void renderLeaderLines(GuiGraphics graphics) {
        for (PanelRuntime runtime : panels.values()) {
            if (!runtime.presented || !runtime.flat || !runtime.widget.visible()) continue;
            if (!runtime.spec.leaderLine()) continue;
            FloatPos from = leaderOrigin(runtime);
            if (from == null) continue;

            int w = runtime.widget.width();
            int h = runtime.widget.height();
            float px = runtime.widget.screenX;
            float py = runtime.widget.screenY;

            //nearest point on the panel rect to the origin
            double ex = Math.max(px, Math.min(from.x, px + w));
            double ey = Math.max(py, Math.min(from.y, py + h));
            //project the origin onto the rect border
            double cx = px + w * 0.5;
            double cy = py + h * 0.5;
            if (ex > px && ex < px + w) {
                ey = from.y < cy ? py : py + h;
            } else if (ey > py && ey < py + h) {
                ex = from.x < cx ? px : px + w;
            }
            if (from.x >= px && from.x <= px + w && from.y >= py && from.y <= py + h) {
                continue; //origin inside the panel — no line
            }

            int color = runtime.focused() ? InworldTheme.LINE_FOCUSED : InworldTheme.LINE;
            drawLine(graphics, (float) ex, (float) ey, (float) from.x, (float) from.y, color);
        }
    }

    /**
     * Where a panel's leader line originates: the scan-frame corner nearest the
     * panel for block-bound anchors, else the plain projected anchor point.
     */
    private @Nullable FloatPos leaderOrigin(PanelRuntime runtime) {
        BlockPos pos = runtime.spec.anchor().blockPos();
        Projection proj = projection;
        if (pos == null || proj == null) return runtime.anchorScreen;

        float px = runtime.widget.screenX;
        float py = runtime.widget.screenY;
        int w = runtime.widget.width();
        int h = runtime.widget.height();
        //panel rect center — pick the frame corner closest to it
        double cx = px + w * 0.5;
        double cy = py + h * 0.5;

        double e = 0.003;
        FloatPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3 corner = new Vec3(
                    pos.getX() + ((i & 1) == 0 ? -e : 1 + e),
                    pos.getY() + (((i >> 1) & 1) == 0 ? -e : 1 + e),
                    pos.getZ() + (((i >> 2) & 1) == 0 ? -e : 1 + e));
            FloatPos s = proj.worldToScreen(corner);
            if (s == null) continue;
            double d = (s.x - cx) * (s.x - cx) + (s.y - cy) * (s.y - cy);
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best != null ? best : runtime.anchorScreen;
    }

    private static void drawLine(GuiGraphics graphics, float x0, float y0, float x1, float y1, int color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = graphics.pose().last().pose();
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        buffer.addVertex(mat, x0, y0, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x1, y1, 0).setColor(r, g, b, a);
        var mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    //endregion

    //region inspect presentation

    boolean inspectActive() {
        return inspecting;
    }

    /** The render entry the inspect screen delegates to. */
    void renderInspect(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //in inspect presentation the real cursor points; hover → focus (WD2 style)
        pointed = panelOf(scene.hitTest(mouseX, mouseY));
        if (pointed != null) focus(pointed);
        renderScreenSpace(graphics, mouseX, mouseY, partialTick);
    }

    void onInspectScreenRemoved() {
        inspecting = false;
        inspectScreen = null;
        //closed by ESC/another screen while the key is still held — stay out until released
        if (inspectHeld()) inspectDismissed = true;
    }

    @Override
    public boolean inspectHeld() {
        KeyMapping key = KeyMappings.inspect;
        InputConstants.Key bound = key.key;
        long window = mc.getWindow().getWindow();
        return switch (bound.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> key.isDown();
        };
    }

    //inspect-mode input — forwarded by InworldInspectScreen
    void inspectMouseMoved(double x, double y) {
        scene.mouseMoved(x, y);
    }

    boolean inspectMouseClicked(double x, double y, int button) {
        boolean consumed = scene.mouseClicked(x, y, button);
        Widget hit = scene.hitTest(x, y);
        PanelRuntime panel = panelOf(hit);
        if (panel != null) focus(panel);
        return consumed;
    }

    boolean inspectMouseReleased(double x, double y, int button) {
        return scene.mouseReleased(x, y, button);
    }

    boolean inspectMouseDragged(double x, double y, int button, double dx, double dy) {
        return scene.mouseDragged(x, y, button, dx, dy);
    }

    boolean inspectMouseScrolled(double x, double y, double sx, double sy) {
        return scene.mouseScrolled(x, y, sx, sy);
    }

    boolean inspectKeyPressed(int keyCode, int scanCode, int modifiers) {
        return scene.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean inspectKeyReleased(int keyCode, int scanCode, int modifiers) {
        return scene.keyReleased(keyCode, scanCode, modifiers);
    }

    boolean inspectCharTyped(char codePoint, int modifiers) {
        return scene.charTyped(codePoint, modifiers);
    }

    //endregion
}
