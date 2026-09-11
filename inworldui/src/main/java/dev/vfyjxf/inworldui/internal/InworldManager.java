package dev.vfyjxf.inworldui.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPositioning;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUiApi;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.inworldui.InworldKeyMappings;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    public static InworldManager init() {
        if (instance != null) return instance;
        var manager = new InworldManager();
        instance = manager;
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(manager::onClientTick);
        bus.addListener(manager::onLevelStage);
        bus.addListener(manager::onGuiRender);
        bus.addListener(manager::onMouseButton);
        bus.addListener(manager::onMouseScroll);
        bus.addListener(manager::onLoggingOut);
        return manager;
    }

    //region state

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(InworldManager.class);

    private final Minecraft mc = Minecraft.getInstance();

    private final WidgetGroup<Widget> root = new WidgetGroup<>();
    private final Scene scene = new Scene(root);

    private final Map<Object, PanelRuntime> panels = new LinkedHashMap<>();
    private final Map<Object, InworldPanelSpec> imperative = new LinkedHashMap<>();
    private final List<ProviderRegistration> providers = new ArrayList<>();

    private @Nullable PanelRuntime focused;
    private @Nullable PanelRuntime pointed;
    /** Best in-cone interactive panel when nothing is strictly pointed at — the WD2-style "look near it" selection. */
    private @Nullable PanelRuntime softPointed;
    /**
     * Tick of the last manual focus-cycle key press. While fresh (&lt;5s) the
     * pointing pass must not stomp the manually chosen focus — strict aim
     * (pointed != null) still wins and ends manual mode.
     */
    private long manualFocusTick = -1000;
    private @Nullable FloatPos pointedUv;
    /** true when the crosshair actually rests on a panel (vs only its anchor block) */
    private boolean pointedInPanel;

    private boolean inspecting;
    private @Nullable InworldInspectScreen inspectScreen;

    //region trace-mode state (Witness-style drag interaction)
    /** panel currently being traced, non-null for the duration of a session */
    private @Nullable PanelRuntime tracing;
    private @Nullable InworldTraceScreen traceScreen;
    /** trace cursor in content-local px */
    private float traceX, traceY;
    /** accumulated cursor travel — a sub-4px short press falls back to the panel action */
    private float traceMoved;
    private long traceStartTick;
    /** the session is hosted by the inspect screen — no extra screen was opened */
    private boolean traceInspectHosted;
    /** look-assist: ease the camera onto the anchor for the first ticks of a session */
    private float traceYaw, tracePitch;
    private int traceLookTicks;
    //endregion
    /** Set when the inspect screen was closed by ESC while the key is still held — don't reopen until released. */
    private boolean inspectDismissed;
    private boolean pressedConsumed;

    private @Nullable Projection projection;
    private @Nullable Matrix4f worldToView;
    private float framePartialTick;
    private long tick;

    private int parkCursor = 0;
    /** Horizontal cursor inside the off-screen input strip (right of the window). */
    private int stripCursor = 0;
    /** Width of the virtual input strip appended to the scene layout area. */
    private int faceStripWidth = 0;
    /** Reused per-frame collection of presented dock panels awaiting corner layout. */
    private final List<PanelRuntime> dockQueue = new ArrayList<>();
    private final List<PanelRuntime> expandQueue = new ArrayList<>();
    private static final int PARK_BASE = -1_000_000;
    private static final int PARK_STEP = 4096;

    private record ProviderRegistration(InworldProvider provider, int interval, long nextRun) {
    }

    /** Each provider's most recent emission — persisted between its runs so reconcile doesn't drop panels on off-ticks. */
    private final Map<InworldProvider, Map<Object, InworldPanelSpec>> providerPanels = new IdentityHashMap<>();

    //endregion

    private InworldManager() {
        //the root fills the whole layout area (window + input strip) so hitTest
        //bounds-checks pass everywhere — without an explicit size the taffy
        //root measures 0×0 (absolute children are out of flow) and nothing
        //would ever be clickable
        root.useStyle(UIStyles.sizePercent(1f));
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
            if (tracing == runtime) endTrace(false);
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
        while (InworldKeyMappings.focusNext.consumeClick()) focusNext();
        while (InworldKeyMappings.focusPrevious.consumeClick()) focusPrevious();
        while (InworldKeyMappings.interact.consumeClick()) triggerInteract();
        //inspect-hosted traces have no trace screen to drive the look-assist
        if (tracing != null && traceInspectHosted) tickTraceLook();

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
            Widget hit = scene.hitTest(v[0], v[1]);
            LOGGER.info("click press: ptr=({},{}) pointed={} uv={} hit={}",
                    (int) v[0], (int) v[1], pointed, pointedUv, hit);
            //clicks landing anywhere on a pointed panel are swallowed even on
            //dead chrome — otherwise LMB would mine the block under the panel
            boolean consumed = scene.mouseClicked(v[0], v[1], button) || pointedInPanel;
            pressedConsumed = consumed;
            if (consumed) event.setCanceled(true);
        } else if (action == GLFW.GLFW_RELEASE) {
            double[] v = virtualPointer();
            LOGGER.info("click release: ptr=({},{}) hit={}",
                    (int) v[0], (int) v[1], scene.hitTest(v[0], v[1]));
            boolean consumed = scene.mouseReleased(v[0], v[1], button) || pointedInPanel;
            if (consumed || pressedConsumed) event.setCanceled(true);
            pressedConsumed = false;
        }
    }

    private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (inspecting || mc.level == null || mc.screen != null) return;
        if (panels.isEmpty()) return;
        double[] v = virtualPointer();
        if (scene.mouseScrolled(v[0], v[1], event.getScrollDeltaX(), event.getScrollDeltaY())
                || pointedInPanel) {
            event.setCanceled(true);
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (PanelRuntime runtime : panels.values()) {
            root.remove(runtime.widget);
            if (runtime.faceTarget != null) {
                runtime.faceTarget.destroyBuffers();
                runtime.faceTarget = null;
            }
        }
        panels.clear();
        imperative.clear();
        focused = null;
        pointed = null;
        tracing = null;
        traceScreen = null;
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
                if (runtime.faceTarget != null) {
                    runtime.faceTarget.destroyBuffers();
                    runtime.faceTarget = null;
                }
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
        runtime.widget.useStyle(UIStyles.zIndexOf(spec.interactive() ? 0 : -1));
        if (spec.openAnimation() && mc.level != null) {
            runtime.bornAt = mc.level.getGameTime()
                    + mc.getTimer().getGameTimeDeltaPartialTick(true);
            runtime.openScale = 0.25f;
        }
        panels.put(spec.key(), runtime);
        root.addWidget(runtime.widget);
        return runtime;
    }

    private void applySpec(PanelRuntime runtime) {
        runtime.widget.setTitle(runtime.spec.title());
        runtime.widget.setHints(runtime.spec.hints());
        runtime.widget.setInteractive(runtime.spec.interactive());
        //non-interactive panels (entity tags) render behind the chrome
        runtime.widget.useStyle(UIStyles.zIndexOf(runtime.spec.interactive() ? 0 : -1));
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
        stripCursor = 0;
        List<PanelRuntime> docked = dockQueue;
        List<PanelRuntime> expandDeferred = expandQueue;

        for (PanelRuntime runtime : panels.values()) {
            runtime.pointedUv = null;
            runtime.anchorWorld = runtime.spec.anchor().position(level, framePartialTick);
            runtime.anchorScreen = null;
            runtime.presented = false;
            runtime.flat = false;
            runtime.docked = false;
            runtime.smoothMove = false;

            Vec3 anchor = runtime.anchorWorld;
            if (anchor == null || !runtime.widget.visible()) continue;

            runtime.distance = proj.distance(anchor);
            if (runtime.distance > runtime.spec.maxDistance()) continue;

            runtime.anchorScreen = proj.worldToScreen(anchor);

            //off-screen collapse: shrink to an edge indicator instead of
            //presenting the full panel where the target can't be seen.
            //skipped while inspecting — the flat projection is meant to show
            //every panel regardless of facing
            if (runtime.spec.collapsesOffscreen() && !inspecting && anchorOffscreen(runtime.anchorScreen)) {
                runtime.indicator = true;
                runtime.indicatorDir = offscreenDirection(anchor);
                runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
                continue;
            }
            runtime.indicator = false;

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
                    //expand resolves AFTER dock layout — its world spot must
                    //not project onto screen area the flat panels occupy
                    case InworldPlacement.Expand expand -> expandDeferred.add(runtime);
                }
            }
        }

        layoutDocks(docked);
        docked.clear();

        //screen rects the foreground chrome occupies: interactive flat panels
        //and every docked panel (interactive or not — a dock slot is chrome),
        //plus the projected rects of world-space panels resolved so far
        List<Rect2i> occupied = new ArrayList<>();
        for (PanelRuntime r : panels.values()) {
            if (!r.presented || !r.widget.visible()) continue;
            if (r.flat && (r.spec.interactive() || r.docked)) {
                occupied.add(new Rect2i(
                        r.smoothMove ? r.targetX : r.widget.screenX,
                        r.smoothMove ? r.targetY : r.widget.screenY,
                        r.widget.width(), r.widget.height()));
            } else if (!r.flat && worldSpace(r.spec.placement())) {
                Rect2i b = projectedWorldRect(r);
                if (b != null) occupied.add(b);
            }
        }
        for (PanelRuntime r : expandDeferred) {
            resolveExpand(r, (InworldPlacement.Expand) r.spec.placement(), occupied);
            //a shown hologram reserves its own screen rect for the next one
            if (r.presented && !r.flat) {
                Rect2i b = projectedWorldRect(r);
                if (b != null) occupied.add(b);
            }
        }
        expandDeferred.clear();

        resolveConflicts(occupied);
        smoothFlatPositions();

        //open-animation drive (world panels scale their quad instead)
        float now = level.getGameTime() + framePartialTick;
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.bornAt >= 0) {
                float t = (now - runtime.bornAt) / 9f;
                runtime.openScale = t >= 1f ? 1f
                        : 0.25f + 0.75f * easeOutBack(Math.max(t, 0f));
                if (t >= 1f) runtime.bornAt = -1;
            }
            runtime.widget.openScale = runtime.flat ? runtime.openScale : 1f;
            //non-interactive tags shrink with distance so far labels don't hog space
            runtime.widget.distScale = runtime.flat && !runtime.spec.interactive()
                    ? (float) Math.min(1f, Math.max(0.45f, 9.5 / Math.max(runtime.distance, 1)))
                    : 1f;
        }

        //the scene's coordinate space is the window plus a virtual "input
        //strip" to its right where face panels are parked — inside the root's
        //bounds so hitTest reaches them, outside the window so they never draw
        faceStripWidth = stripCursor > 0 ? stripCursor + 32 : 0;
        scene.setLayoutArea(
                mc.getWindow().getGuiScaledWidth() + faceStripWidth,
                mc.getWindow().getGuiScaledHeight());

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
        runtime.smoothMove = true;
        runtime.targetX = (int) result.x();
        runtime.targetY = (int) result.y();
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
        //follow panels track their anchor tightly — no position smoothing,
        //the interpolated anchor already moves smoothly
        runtime.smoothMove = false;
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
        runtime.docked = true;
        runtime.smoothMove = true;
        runtime.dockCorner = dock.corner();
        docked.add(runtime);
    }

    /**
     * Packs docked panels into their screen corners: AUTO picks the quadrant
     * the anchor projects into with a deadband around the center lines so a
     * wandering anchor doesn't keep flapping the panel between corners, panels
     * stack from the corner inward in offer order.
     * <p>
     * Each screen side is one shared vertical budget (top and bottom columns
     * grow toward each other): when a panel no longer fits it is first
     * <em>folded</em> to its chrome strip; when even folded strips overflow the
     * panel is hidden for the frame and counted into the corner's "+N" chip.
     */
    private void layoutDocks(List<PanelRuntime> docked) {
        Arrays.fill(dockTopExtent, 0);
        Arrays.fill(dockOverflow, 0);
        Arrays.fill(dockCursorEnd, 0);
        if (docked.isEmpty()) return;
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int marginX = 8, marginY = 8, gap = 6;
        int budget = H - marginY * 2;
        int[] sideUsed = new int[2];

        for (PanelRuntime runtime : docked) {
            InworldPlacement.DockCorner corner = runtime.dockCorner;
            if (corner == InworldPlacement.DockCorner.AUTO) {
                corner = autoCorner(runtime.anchorScreen, W, H, runtime.lastAutoCorner);
            }
            runtime.lastAutoCorner = corner;
            int side = isLeft(corner) ? 0 : 1;
            boolean top = isTop(corner);
            int w = runtime.widget.width();
            int h = runtime.widget.height();

            runtime.folded = false;
            if (sideUsed[side] + h + gap > budget) {
                int fh = foldHeight(runtime);
                if (sideUsed[side] + fh + gap <= budget) {
                    runtime.folded = true;
                    h = fh;
                } else {
                    //column full even folded — hide this frame, count into "+N"
                    runtime.presented = false;
                    runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
                    dockOverflow[corner.ordinal()]++;
                    continue;
                }
            }
            runtime.widget.setFolded(runtime.folded);

            int slot = dockCursors[corner.ordinal()];
            int x = switch (corner) {
                case TOP_LEFT, BOTTOM_LEFT -> marginX;
                default -> W - marginX - w;
            };
            int y = top ? marginY + slot : H - marginY - h - slot;
            dockCursors[corner.ordinal()] = slot + h + gap;
            sideUsed[side] += h + gap;
            if (corner == InworldPlacement.DockCorner.TOP_LEFT) {
                dockTopExtent[0] = Math.max(dockTopExtent[0], y + h);
            } else if (corner == InworldPlacement.DockCorner.TOP_RIGHT) {
                dockTopExtent[1] = Math.max(dockTopExtent[1], y + h);
            }
            runtime.targetX = x;
            runtime.targetY = y;
        }
        System.arraycopy(dockCursors, 0, dockCursorEnd, 0, dockCursorEnd.length);
        Arrays.fill(dockCursors, 0);
    }

    /** Height of a folded panel: title/hint chrome only, content hidden. */
    private static int foldHeight(PanelRuntime r) {
        int padTop = r.spec.title() != null ? HackerTheme.TITLE_HEIGHT + 2 : HackerTheme.PADDING;
        int padBottom = r.spec.hints().isEmpty() ? HackerTheme.PADDING : HackerTheme.HINT_HEIGHT + 2;
        return padTop + padBottom;
    }

    /**
     * AUTO-corner pick with hysteresis: the anchor has to push a deadband past
     * the screen's center lines before the panel switches sides, so crossing
     * the center doesn't slam the panel to the opposite corner.
     */
    private static InworldPlacement.DockCorner autoCorner(
            @Nullable FloatPos anchor, int W, int H, @Nullable InworldPlacement.DockCorner prev) {
        int db = 72;
        boolean left, top;
        if (anchor == null || prev == null) {
            left = anchor == null || anchor.x < W * 0.5f;
            top = anchor == null || anchor.y < H * 0.5f;
        } else {
            left = anchor.x < W * 0.5f + (isLeft(prev) ? db : -db);
            top = anchor.y < H * 0.5f + (isTop(prev) ? db : -db);
        }
        return top
                ? (left ? InworldPlacement.DockCorner.TOP_LEFT : InworldPlacement.DockCorner.TOP_RIGHT)
                : (left ? InworldPlacement.DockCorner.BOTTOM_LEFT : InworldPlacement.DockCorner.BOTTOM_RIGHT);
    }

    private static boolean isLeft(InworldPlacement.DockCorner c) {
        return c == InworldPlacement.DockCorner.TOP_LEFT || c == InworldPlacement.DockCorner.BOTTOM_LEFT;
    }

    private static boolean isTop(InworldPlacement.DockCorner c) {
        return c == InworldPlacement.DockCorner.TOP_LEFT || c == InworldPlacement.DockCorner.TOP_RIGHT;
    }

    private final int[] dockCursors = new int[InworldPlacement.DockCorner.values().length];
    /** bottom edge (px) of the TOP_LEFT/TOP_RIGHT dock stacks — tag rails start below them */
    private final int[] dockTopExtent = new int[2];
    /** per-corner count of panels that didn't fit even folded — drawn as "+N" chips */
    private final int[] dockOverflow = new int[InworldPlacement.DockCorner.values().length];
    /** per-corner final stack extent from the layout pass — where the overflow chip hangs */
    private final int[] dockCursorEnd = new int[InworldPlacement.DockCorner.values().length];

    /**
     * Screen zoning for non-interactive flat panels (entity tags, passive
     * floats). Docked panels — interactive or not — are chrome and keep their
     * dock slot; they never enter this pass.
     *
     * Occlusion policy is deliberately tolerant: a tag renders behind the
     * foreground chrome, so a partly-covered tag is still readable and keeps
     * sitting on its entity. Only when more than half the tag (or a >10px
     * intrusion) is covered do we look for an escape — the cheapest of the
     * four sides of the dominant blocker within a 48px glide; and only when
     * even that fails <em>and</em> the tag is nearly buried (>72% covered) is
     * it pulled into a side rail — an edge column on the half of the screen
     * its anchor projects into. Everything in between stays put behind the
     * chrome rather than wandering off and losing its anchor.
     */
    private void resolveConflicts(List<Rect2i> occupied) {
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int margin = 8;

        List<PanelRuntime> tags = new ArrayList<>();
        for (PanelRuntime r : panels.values()) {
            if (r.presented && r.flat && !r.spec.interactive() && !r.docked
                    && r.widget.visible()) {
                tags.add(r);
            }
        }
        //stable order: group → coarse distance bucket (2-block steps, so tiny
        //distance wiggles don't reorder) → key — keeps slots from swapping
        tags.sort(Comparator.comparing((PanelRuntime t) -> t.spec.group())
                .thenComparingInt(t -> (int) (t.distance / 2))
                .thenComparing(t -> String.valueOf(t.spec.key())));

        //merge pass: a group shows at most groupLimit members; extras hide
        //and the last visible member carries a "+N" badge
        List<PanelRuntime> visible = new ArrayList<>(tags.size());
        Map<String, Integer> groupIdx = new HashMap<>();
        Map<String, PanelRuntime> groupLast = new HashMap<>();
        Map<String, Integer> groupHidden = new HashMap<>();
        for (PanelRuntime tag : tags) {
            tag.widget.overflow = null;
            String g = tag.spec.group();
            int idx = groupIdx.merge(g, 1, Integer::sum) - 1;
            if (idx < tag.spec.groupLimit()) {
                visible.add(tag);
                groupLast.put(g, tag);
            } else {
                groupHidden.merge(g, 1, Integer::sum);
                tag.presented = false;
                tag.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            }
        }
        for (var e : groupLast.entrySet()) {
            int hidden = groupHidden.getOrDefault(e.getKey(), 0);
            if (hidden > 0) e.getValue().widget.overflow = "+" + hidden;
        }

        List<PanelRuntime> railQueue = new ArrayList<>();
        for (PanelRuntime tag : visible) {
            int w = tag.widget.width();
            int h = tag.widget.height();
            //the tag's home this frame: a follow tag anchors at screenX (just
            //rewritten by resolveFollow), a floating tag at its resolved target
            int ix = (int) Math.max(2, Math.min(W - w - 2,
                    tag.smoothMove ? tag.targetX : tag.widget.screenX));
            int iy = (int) Math.max(2, Math.min(H - h - 2,
                    tag.smoothMove ? tag.targetY : tag.widget.screenY));
            int wx = ix, wy = iy;

            var oc = InworldLayout.occlusion(ix, iy, w, h, occupied);
            if (!oc.acceptable(w, h)) {
                Rect2i blocker = oc.blocker();
                int tol = 10; //px of graze an escape position may keep
                int bx = 0, by = 0, bestCost = Integer.MAX_VALUE, bestDir = -1;
                int[][] candidates = {
                        {ix, blocker.getY() - h + tol},
                        {ix, blocker.getY() + blocker.getHeight() - tol},
                        {blocker.getX() - w + tol, iy},
                        {blocker.getX() + blocker.getWidth() - tol, iy}};
                for (int i = 0; i < candidates.length; i++) {
                    int[] c = candidates[i];
                    int cx = Math.max(2, Math.min(W - w - 2, c[0]));
                    int cy = Math.max(2, Math.min(H - h - 2, c[1]));
                    var co = InworldLayout.occlusion(cx, cy, w, h, occupied);
                    //an escape must land readable — or at least halve the cover
                    if (!co.acceptable(w, h) && co.area() >= oc.area() * 0.55) continue;
                    int cost = Math.abs(cx - ix) + Math.abs(cy - iy)
                            - (i == tag.lastSlideDir ? 14 : 0);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bx = cx;
                        by = cy;
                        bestDir = i;
                    }
                }
                if (bestDir >= 0 && bestCost <= 48) {
                    wx = bx;
                    wy = by;
                    tag.lastSlideDir = bestDir;
                } else if (oc.buried(w, h)) {
                    tag.lastSlideDir = -1;
                    railQueue.add(tag);
                    continue;
                } else {
                    //not worth the trip — stay behind the chrome
                    tag.lastSlideDir = -1;
                }
            }

            //commit: a displaced (or still-gliding-home) tag uses the
            //posX/targetX smoothing so escapes and returns animate; a tag at
            //home snaps tight to its anchor with no lag
            boolean displaced = wx != ix || wy != iy;
            boolean settling = tag.posInit
                    && (Math.abs(tag.posX - ix) > 1.5f || Math.abs(tag.posY - iy) > 1.5f);
            if (displaced || settling) {
                tag.smoothMove = true;
                placeTag(tag, wx, wy);
            } else {
                tag.smoothMove = false;
                tag.posInit = false;
                tag.widget.setScreenPos(ix, iy);
            }
            occupied.add(new Rect2i(wx, wy, w, h));
        }

        //rails: packed columns on the left/right edge, below that side's
        //top dock stack
        int[] railY = {dockTopExtent[0] > 0 ? dockTopExtent[0] + 6 : margin + 16,
                dockTopExtent[1] > 0 ? dockTopExtent[1] + 6 : margin + 16};
        for (PanelRuntime tag : railQueue) {
            int w = tag.widget.width();
            int h = tag.widget.height();
            int side = tag.anchorScreen != null && tag.anchorScreen.x < W * 0.5f ? 0 : 1;
            int x = side == 0 ? margin : W - margin - w;
            int y = railY[side];
            while (firstOverlap(x, y, w, h, occupied) != null && y + h <= H - margin) {
                y += 4;
            }
            if (y + h > H - margin) {
                //rail full — hide the tag this frame
                tag.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
                tag.smoothMove = false;
                continue;
            }
            tag.smoothMove = true; //glide into the rail slot
            placeTag(tag, x, y);
            occupied.add(new Rect2i(x, y, w, h));
            railY[side] = y + h + 4;
        }
    }

    /**
     * Commits a tag's resolved position: non-smoothed panels (follow) write
     * the slot directly; smoothed ones glide into it. The glide start is only
     * re-anchored on entry — resolveFollow rewrites the screen pos to the
     * anchor every frame, so re-anchoring posX here would restart the glide
     * forever and strand the tag mid-flight.
     */
    private void placeTag(PanelRuntime tag, int x, int y) {
        if (tag.smoothMove) {
            if (!tag.posInit) {
                tag.posX = tag.widget.screenX;
                tag.posY = tag.widget.screenY;
                tag.posInit = true;
            }
            tag.targetX = x;
            tag.targetY = y;
        } else {
            tag.widget.setScreenPos(x, y);
            tag.posInit = false;
        }
    }

    private static @Nullable Rect2i firstOverlap(int x, int y, int w, int h, List<Rect2i> rects) {
        for (Rect2i o : rects) {
            if (x < o.getX() + o.getWidth() && x + w > o.getX()
                    && y < o.getY() + o.getHeight() && y + h > o.getY()) {
                return o;
            }
        }
        return null;
    }

    /** Anchor is off the camera view — behind the camera or beyond the viewport edge. */
    private boolean anchorOffscreen(@Nullable FloatPos s) {
        if (s == null) return true;
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        return s.x < 0 || s.y < 0 || s.x >= W || s.y >= H;
    }

    /**
     * Bearing of an off-screen anchor in screen space (x right, y down,
     * normalized). In front of the camera the view-space direction maps
     * straight over; behind the camera we keep the correct side and bias the
     * marker to the bottom edge.
     */
    private FloatPos offscreenDirection(Vec3 anchor) {
        Matrix4f mv = worldToView;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        if (mv != null) {
            Vector4f v = new Vector4f(
                    (float) anchor.x, (float) anchor.y, (float) anchor.z, 1f)
                    .mul(mv); //world→view (already carries -cam translate)
            float sx = v.x(), sy = v.z() < 0 ? -v.y() : 1f;
            double len = Math.hypot(sx, sy);
            if (len > 1e-4) return new FloatPos(sx / len, sy / len);
        }
        //fallback: camera-relative bearing from yaw — forward=(-sin,cos),
        //right=(-cos,-sin) on the xz plane
        Vec3 d = anchor.subtract(cam);
        double yaw = Math.toRadians(mc.gameRenderer.getMainCamera().getYRot());
        double sx = -d.x * Math.cos(yaw) - d.z * Math.sin(yaw);
        double fwd = -d.x * Math.sin(yaw) + d.z * Math.cos(yaw);
        double sy = fwd > 0 ? -d.y : 1.0;
        double len = Math.hypot(sx, sy);
        return len > 1e-4 ? new FloatPos(sx / len, sy / len) : new FloatPos(0, 1);
    }

    /**
     * Draws every collapsed panel's edge indicator: a diamond pinned to the
     * screen border in the anchor's bearing, a short tick pointing outward,
     * and the distance tucked on the inside so it never spills off-screen.
     * Marks landing on the same edge are spread deterministically — order is
     * the stable panel-key order, so nothing churns frame to frame.
     */
    private void renderIndicators(GuiGraphics graphics) {
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int margin = 14;
        float cx = W * 0.5f, cy = H * 0.5f;
        var font = mc.font;

        List<PanelRuntime> collapsed = null;
        for (PanelRuntime r : panels.values()) {
            if (r.indicator && r.indicatorDir != null && r.widget.visible()) {
                if (collapsed == null) collapsed = new ArrayList<>();
                collapsed.add(r);
            }
        }
        if (collapsed == null) return;
        collapsed.sort(Comparator.comparing(r -> String.valueOf(r.spec.key())));

        //walk each bearing from center to the inset border
        List<IndMark> marks = new ArrayList<>(collapsed.size());
        for (PanelRuntime r : collapsed) {
            FloatPos d = r.indicatorDir;
            double tx = Math.abs(d.x) < 1e-4 ? Double.MAX_VALUE
                    : (cx - margin) / Math.abs(d.x);
            double ty = Math.abs(d.y) < 1e-4 ? Double.MAX_VALUE
                    : (cy - margin) / Math.abs(d.y);
            boolean side = tx < ty; //hits a vertical edge before a horizontal one
            double t = Math.min(tx, ty);
            IndMark m = new IndMark();
            m.runtime = r;
            m.dir = d;
            m.edge = side ? (d.x < 0 ? 0 : 1) : (d.y < 0 ? 2 : 3);
            m.px = cx + d.x * t;
            m.py = cy + d.y * t;
            m.tan = side ? m.py : m.px;
            marks.add(m);
        }

        //spread marks sharing an edge with a fixed gap, then recenter the run
        for (int e = 0; e < 4; e++) {
            List<IndMark> g = new ArrayList<>();
            for (IndMark m : marks) if (m.edge == e) g.add(m);
            if (g.size() < 2) continue;
            g.sort(Comparator.comparingDouble(m -> m.tan));
            double lo = margin + 8;
            double hi = (e < 2 ? H : W) - margin - 8;
            double[] tan = new double[g.size()];
            for (int i = 0; i < g.size(); i++) tan[i] = g.get(i).tan;
            InworldLayout.spreadEdgeSlots(tan, lo, hi, 26);
            for (int i = 0; i < g.size(); i++) {
                IndMark m = g.get(i);
                m.tan = tan[i];
                if (e < 2) m.py = m.tan; else m.px = m.tan;
            }
        }

        for (IndMark m : marks) {
            PanelRuntime r = m.runtime;
            FloatPos d = m.dir;
            float px = (float) m.px, py = (float) m.py;
            int color = r.focused() ? HackerTheme.BORDER_FOCUSED : HackerTheme.ACCENT_DIM;
            drawLine(graphics, px, py - 5, px + 5, py, color);
            drawLine(graphics, px + 5, py, px, py + 5, color);
            drawLine(graphics, px, py + 5, px - 5, py, color);
            drawLine(graphics, px - 5, py, px, py - 5, color);
            //bearing tick pointing further outward
            drawLine(graphics, (float) (px + d.x * 6), (float) (py + d.y * 6),
                    (float) (px + d.x * 10), (float) (py + d.y * 10), HackerTheme.ACCENT);
            //distance sits on the inward side so it stays readable on any edge
            String dist = (int) r.distance + "m";
            double ix = px - d.x * 17, iy = py - d.y * 16;
            graphics.drawString(font, dist,
                    (int) (ix - font.width(dist) * 0.5),
                    (int) (iy - font.lineHeight * 0.5),
                    HackerTheme.TEXT_DIM);
        }
    }

    /**
     * Draws a small "+N" chip at the end of each dock column that hid panels
     * this frame — the honest "there's more but it doesn't fit" marker.
     */
    private void renderDockOverflow(GuiGraphics graphics) {
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int marginX = 8, marginY = 8;
        var font = mc.font;
        var corners = InworldPlacement.DockCorner.values();
        for (int c = 0; c < corners.length; c++) {
            int n = dockOverflow[c];
            if (n == 0) continue;
            String s = "+" + n;
            int tw = font.width(s) + 5;
            int x = isLeft(corners[c]) ? marginX : W - marginX - tw;
            int y = isTop(corners[c])
                    ? marginY + dockCursorEnd[c]
                    : H - marginY - 9 - dockCursorEnd[c];
            graphics.fill(x, y, x + tw, y + 9, HackerTheme.BG_FOCUSED);
            //1px accent frame
            graphics.fill(x, y, x + tw, y + 1, HackerTheme.ACCENT_DIM);
            graphics.fill(x, y + 8, x + tw, y + 9, HackerTheme.ACCENT_DIM);
            graphics.fill(x, y, x + 1, y + 9, HackerTheme.ACCENT_DIM);
            graphics.fill(x + tw - 1, y, x + tw, y + 9, HackerTheme.ACCENT_DIM);
            graphics.drawString(font, s, x + 3, y + 1, HackerTheme.ACCENT);
        }
    }

    /** One collapsed panel's edge mark — tangential slot may be adjusted by the de-conflict pass. */
    private static final class IndMark {
        PanelRuntime runtime;
        FloatPos dir;
        int edge;      //0=left 1=right 2=top 3=bottom
        double tan;    //slot coordinate along the edge
        double px, py; //resolved screen position
    }

    /**
     * Screen-space bounding box of a world-space panel's quad. Null when every
     * corner is off-screen or unprojectable.
     */
    private @Nullable Rect2i projectedWorldRect(PanelRuntime r) {
        if (projection == null || r.faceOrigin == null || r.faceU == null || r.faceV == null) {
            return null;
        }
        return quadScreenRect(projection, r.faceOrigin, r.faceU, r.faceV,
                r.widget.width(), r.widget.height());
    }

    /**
     * Expanding exponential smoothing over resolved flat positions — when a
     * panel's slot/corner changes it glides to the new spot instead of
     * teleporting. Newly presented panels snap straight to their target.
     */
    private void smoothFlatPositions() {
        float dt = mc.getTimer().getRealtimeDeltaTicks() / 20f;
        float k = 1f - (float) Math.exp(-dt * 14);
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.presented && runtime.flat && runtime.smoothMove) {
                if (!runtime.posInit) {
                    runtime.posX = runtime.targetX;
                    runtime.posY = runtime.targetY;
                    runtime.posInit = true;
                } else {
                    runtime.posX += (runtime.targetX - runtime.posX) * k;
                    runtime.posY += (runtime.targetY - runtime.posY) * k;
                }
                runtime.widget.setScreenPos(Math.round(runtime.posX), Math.round(runtime.posY));
            } else {
                runtime.posInit = false;
            }
        }
    }

    /**
     * World-space placement for expand panels: scans rings of candidate spots
     * around the anchor for air (the hologram floats beside/above its block),
     * penalizes spots whose screen projection would cover foreground panels,
     * yaw-billboards the panel toward the player, and parks the widget in the
     * input strip so the synthesized pointer can reach it — same pipeline as
     * face panels. When every spot lands on occupied screen area the panel
     * hides rather than overlapping the chrome.
     */
    private void resolveExpand(PanelRuntime runtime, InworldPlacement.Expand expand,
                               List<Rect2i> reserved) {
        Vec3 anchor = runtime.anchorWorld;
        Projection proj = projection;
        if (anchor == null || proj == null || mc.level == null || mc.player == null) {
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }

        double ppb = expand.pixelsPerBlock();
        runtime.facePpb = ppb;
        double s = 1.0 / ppb;
        double pw = runtime.widget.width() * s;
        double ph = runtime.widget.height() * s;

        ClientLevel level = mc.level;
        Vec3 eye = mc.player.getEyePosition(framePartialTick);

        //other world-space panels already occupy these spots
        List<Vec3> occupiedWorld = new ArrayList<>();
        for (PanelRuntime other : panels.values()) {
            if (other != runtime && other.presented && other.expandPos != null) {
                occupiedWorld.add(other.expandPos);
            }
        }

        Vec3 vDown = new Vec3(0, -1, 0).scale(s);
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;
        double bestFrac = 1;
        double[] dys = {1.7, 1.1, 0.5, 2.3, -0.2};
        for (int ring = 0; ring < 4; ring++) {
            double rad = 1.0 + ring * 0.55 + pw * 0.5;
            for (double dy : dys) {
                for (int i = 0; i < 10; i++) {
                    double ang = i * (Math.PI * 2 / 10);
                    Vec3 spot = anchor.add(Math.cos(ang) * rad, dy, Math.sin(ang) * rad);
                    double score = expandSpotScore(level, spot, pw, ph, anchor);
                    for (Vec3 o : occupiedWorld) {
                        if (spot.distanceToSqr(o) < (pw * 0.5 + 0.6) * (pw * 0.5 + 0.6)) {
                            score += 64; //another hologram already there
                        }
                    }
                    //screen-space cost: covering docked/flat panels is the worst outcome
                    double frac = expandScreenOverlap(proj, eye, spot, vDown, s,
                            runtime.widget.width(), runtime.widget.height(), reserved);
                    score += frac * 600;
                    if (frac >= 0.999) score += 300; //unprojectable / fully covered
                    if (score < bestScore) {
                        bestScore = score;
                        best = spot;
                        bestFrac = frac;
                    }
                }
            }
        }

        //stickiness: keep the previously chosen spot unless a clearly better
        //one exists — otherwise the panel would jitter between near-tied spots
        if (runtime.expandPos != null) {
            double cur = expandSpotScore(level, runtime.expandPos, pw, ph, anchor);
            double curFrac = expandScreenOverlap(proj, eye, runtime.expandPos, vDown, s,
                    runtime.widget.width(), runtime.widget.height(), reserved);
            cur += curFrac * 600 + (curFrac >= 0.999 ? 300 : 0);
            if (cur <= bestScore * 1.35 + 1.0) {
                best = runtime.expandPos;
                bestFrac = curFrac;
            }
        }

        //can't show it cleanly → don't show it; hysteresis keeps the
        //show/hide edge from flickering
        double hideAt = runtime.expandHidden ? 0.15 : 0.35;
        if (best == null || bestFrac > hideAt) {
            runtime.expandHidden = true;
            runtime.widget.setScreenPos(PARK_BASE - parkCursor++ * PARK_STEP, 0);
            return;
        }
        runtime.expandHidden = false;
        runtime.presented = true;
        runtime.flat = false;
        runtime.expandPos = best;

        //yaw-billboard toward the player's eye: u×v faces away from the viewer
        //(GUI winding, same convention as face panels)
        Vec3 d = eye.subtract(best);
        double len = Math.hypot(d.x, d.z);
        Vec3 dH = len < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(d.x / len, 0, d.z / len);
        Vec3 u = new Vec3(dH.z, 0, -dH.x);
        runtime.faceU = u.scale(s);
        runtime.faceV = vDown;
        runtime.faceNormal = dH;
        runtime.faceOrigin = best
                .subtract(runtime.faceU.scale(runtime.widget.width() * 0.5))
                .subtract(runtime.faceV.scale(runtime.widget.height() * 0.5));

        int stripX = mc.getWindow().getGuiScaledWidth() + 16 + stripCursor;
        stripCursor += runtime.widget.width() + 16;
        runtime.widget.setScreenPos(stripX, 8);
    }

    /**
     * Fraction (0..1) of a hologram's projected screen rect covered by
     * reserved foreground rects. 1 when the quad can't project at all.
     */
    private static double expandScreenOverlap(Projection proj, Vec3 eye, Vec3 spot,
                                              Vec3 vDown, double s, int wPx, int hPx,
                                              List<Rect2i> reserved) {
        Vec3 d = eye.subtract(spot);
        double len = Math.hypot(d.x, d.z);
        Vec3 dH = len < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(d.x / len, 0, d.z / len);
        Vec3 u = new Vec3(dH.z, 0, -dH.x).scale(s);
        Vec3 o = spot.subtract(u.scale(wPx * 0.5)).subtract(vDown.scale(hPx * 0.5));
        Rect2i rect = quadScreenRect(proj, o, u, vDown, wPx, hPx);
        if (rect == null) return 1;
        double over = 0;
        for (Rect2i r : reserved) {
            int ix = Math.max(0, Math.min(rect.getX() + rect.getWidth(), r.getX() + r.getWidth())
                    - Math.max(rect.getX(), r.getX()));
            int iy = Math.max(0, Math.min(rect.getY() + rect.getHeight(), r.getY() + r.getHeight())
                    - Math.max(rect.getY(), r.getY()));
            over += (double) ix * iy;
        }
        return Math.min(1, over / ((double) rect.getWidth() * rect.getHeight()));
    }

    /** Projects a world quad (origin + u·w + v·h) to its screen bounding rect. */
    private static @Nullable Rect2i quadScreenRect(Projection proj, Vec3 o, Vec3 u, Vec3 v,
                                                   double w, double h) {
        Vec3[] corners = {o, o.add(u.scale(w)), o.add(v.scale(h)),
                o.add(u.scale(w)).add(v.scale(h))};
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        boolean any = false;
        for (Vec3 c : corners) {
            FloatPos s = proj.worldToScreen(c);
            if (s == null) continue;
            any = true;
            minX = Math.min(minX, (int) s.x);
            minY = Math.min(minY, (int) s.y);
            maxX = Math.max(maxX, (int) s.x);
            maxY = Math.max(maxY, (int) s.y);
        }
        return any ? new Rect2i(minX - 4, minY - 4, maxX - minX + 8, maxY - minY + 8) : null;
    }

    /** Prefers spots near the anchor with the panel's bounding volume in air. */
    private static double expandSpotScore(ClientLevel level, Vec3 spot, double pw, double ph, Vec3 anchor) {
        double score = spot.subtract(anchor).lengthSqr();
        var box = new net.minecraft.world.phys.AABB(
                spot.x - pw * 0.5, spot.y - ph * 0.5, spot.z - pw * 0.5,
                spot.x + pw * 0.5, spot.y + ph * 0.5, spot.z + pw * 0.5);
        for (BlockPos b : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.getBlockState(b).isAir()) score += 16;
        }
        return score;
    }

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

        //center of the panel on the face — the geometric offset only needs to
        //cover the panel's own internal z-layering now that the render pass
        //applies a polygon-offset decal bias against the block surface
        Vec3 facePoint = Vec3.atCenterOf(blockPos)
                .add(n.scale(0.5))
                .add(uAxis.scale(face.u() - 0.5))
                .add(vAxis.scale(face.v() - 0.5))
                .add(n.scale(0.001 + s));

        runtime.faceU = uAxis.scale(s);
        runtime.faceV = vAxis.scale(s);
        runtime.faceNormal = n;
        runtime.faceOrigin = facePoint
                .subtract(runtime.faceU.scale(runtime.widget.width() * 0.5))
                .subtract(runtime.faceV.scale(runtime.widget.height() * 0.5));

        //park inside the off-screen input strip: reachable by synthesized
        //pointer coords (slotX + u, slotY + v) but never rendered on screen
        int stripX = mc.getWindow().getGuiScaledWidth() + 16 + stripCursor;
        stripCursor += runtime.widget.width() + 16;
        runtime.widget.setScreenPos(stripX, 8);
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
        pointedInPanel = false;
        softPointed = null;
        if (proj == null || mc.level == null) return;

        Vec3 origin = proj.cameraPos();
        Vec3 dir = proj.crosshairDirection();

        //1. crosshair ray against world-space panels (face + expand)
        double bestT = Double.MAX_VALUE;
        for (PanelRuntime runtime : panels.values()) {
            if (!worldSpace(runtime.spec.placement())) continue;
            if (!runtime.presented || runtime.flat || !runtime.widget.visible()
                    || runtime.faceU == null) continue;
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
        if (pointed != null) pointedInPanel = true;

        //2. crosshair over a flat panel
        if (pointed == null) {
            double cx = mc.getWindow().getGuiScaledWidth() * 0.5;
            double cy = mc.getWindow().getGuiScaledHeight() * 0.5;
            Widget hit = scene.hitTest(cx, cy);
            pointed = panelOf(hit);
            if (pointed != null) pointedInPanel = true;
        }

        //3. crosshair on an anchor block (focus only — clicks fall through to the game)
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
            pointedInPanel = false;
        }

        //4. no exact hit — WD2-style soft focus: the interactive panel whose
        //anchor is nearest the look vector inside a ~30° cone gets selected,
        //so a hotkey press doesn't demand pixel-perfect crosshair aim. Only
        //panels that declared a primary action participate.
        if (pointed == null && !inspecting) {
            softPointed = pickSoftFocus(origin, dir);
        }

        //world mode focus follows pointing — except while a manual cycle is
        //fresh: then the cycled panel keeps focus until the player strictly
        //points at something or the window expires
        if (!inspecting) {
            boolean manual = focused != null && focused.presented
                    && focused.spec.interactive() && tick - manualFocusTick < 100;
            PanelRuntime newFocus;
            if (tracing != null) {
                newFocus = tracing; //a live trace pins focus to its panel
            } else if (pointed != null) {
                newFocus = pointed;
                manualFocusTick = -1000;
            } else if (manual) {
                newFocus = focused;
            } else {
                newFocus = softPointed;
            }
            if (newFocus != focused) {
                focused = newFocus;
                if (focused != null) scene.requestFocus(focused.widget);
            }
        }
    }

    /** Nearest-to-look-axis actionable panel inside the soft-focus cone and range. */
    private @Nullable PanelRuntime pickSoftFocus(Vec3 eye, Vec3 look) {
        PanelRuntime best = null;
        double bestScore = Double.MAX_VALUE;
        for (PanelRuntime r : panels.values()) {
            if (!r.presented || !r.widget.visible() || !r.spec.interactive()) continue;
            //an action or a traceable content both make the panel "activatable"
            if (r.anchorWorld == null || (r.spec.action() == null && r.traceable() == null)) continue;
            if (r.distance > Math.min(r.spec.maxDistance(), InworldLayout.SOFT_FOCUS_RANGE)) continue;
            double score = InworldLayout.softFocusScore(eye, look, r.anchorWorld);
            if (score < 0) continue;
            score += r.distance * 0.01; //angle decides, distance breaks near-ties
            if (score < bestScore) {
                bestScore = score;
                best = r;
            }
        }
        return best;
    }

    /**
     * The interact hotkey. Traceable panels enter a trace session instead of
     * firing immediately — a quick tap still lands on the primary action (see
     * {@link #endTrace}).
     */
    private void triggerInteract() {
        PanelRuntime target = focused;
        if (target == null || !target.presented || !target.spec.interactive()) return;
        if (target.traceable() != null) {
            beginTrace(target);
            return;
        }
        fireAction(target);
    }

    private void fireAction(PanelRuntime target) {
        var action = target.spec.action();
        if (action == null || mc.level == null || mc.player == null) return;
        action.accept(new InworldPanelContext(mc.level, mc.player, target));
    }

    //region trace mode — Witness-style hold-and-drag on the panel surface

    /**
     * Starts a trace session on the panel: locks the camera behind a capture
     * screen (or reuses the inspect screen when already inspecting), feeds the
     * widget cursor positions unprojected onto its surface, and ends with a
     * commit when the interact key is released.
     */
    private void beginTrace(PanelRuntime runtime) {
        InworldTraceable traceable = runtime.traceable();
        if (traceable == null || mc.level == null || mc.player == null) return;
        if (mc.screen != null && !inspecting) return; //a foreign screen owns input

        FloatPos start = initialTracePoint(runtime);
        InworldPanelContext ctx = new InworldPanelContext(mc.level, mc.player, runtime);
        if (!traceable.traceBegin(ctx, (float) start.x, (float) start.y)) {
            fireAction(runtime);
            return;
        }

        tracing = runtime;
        traceX = (float) start.x;
        traceY = (float) start.y;
        traceMoved = 0;
        traceStartTick = tick;
        focused = runtime;
        manualFocusTick = tick;

        //look-assist: ease the view onto the anchor — Witness re-centres the
        //player on the panel; rotating the camera is our equivalent, and it
        //keeps grazing-angle face panels usable during the trace
        if (runtime.anchorWorld != null) {
            Vec3 d = runtime.anchorWorld.subtract(mc.player.getEyePosition());
            traceYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            tracePitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
            traceLookTicks = 10;
        }

        if (inspecting) {
            traceInspectHosted = true; //the inspect screen already captures input
        } else {
            traceInspectHosted = false;
            KeyMapping.releaseAll(); //held walk keys would keep running under the screen
            traceScreen = new InworldTraceScreen(this);
            mc.setScreen(traceScreen);
        }
    }

    /** First cursor position: the aimed-at panel point when pointing, else content center. */
    private FloatPos initialTracePoint(PanelRuntime runtime) {
        Widget content = runtime.widget.content();
        if (pointed == runtime && pointedUv != null) {
            FloatPos off = contentOffset(runtime);
            return new FloatPos(pointedUv.x - off.x, pointedUv.y - off.y);
        }
        return new FloatPos(content.width() * 0.5f, content.height() * 0.5f);
    }

    /**
     * Offset of the content widget's origin inside the chrome's panel pixel
     * space — subtract it from a panel-space point to get content-local px.
     */
    private FloatPos contentOffset(PanelRuntime runtime) {
        FloatPos scene = runtime.widget.content().localToScene(0, 0);
        return new FloatPos((float) (scene.x - runtime.widget.screenX),
                (float) (scene.y - runtime.widget.screenY));
    }

    /** Screen-space cursor → panel pixel space (ray-unprojected for world panels). */
    private @Nullable FloatPos tracePanelPoint(PanelRuntime runtime, double sx, double sy) {
        if (runtime.flat) {
            return new FloatPos((float) (sx - runtime.widget.screenX),
                    (float) (sy - runtime.widget.screenY));
        }
        Projection proj = projection;
        if (proj == null || runtime.faceU == null) return null;
        return Projection.rayPlaneUV(proj.cameraPos(), proj.rayDirection(sx, sy),
                runtime.faceOrigin, runtime.faceU, runtime.faceV, runtime.faceNormal);
    }

    void traceMouseMoved(double sx, double sy) {
        PanelRuntime runtime = tracing;
        if (runtime == null) return;
        if (!runtime.presented) { //panel hid mid-trace (parked/offscreen) — drop the stroke
            endTrace(false);
            return;
        }
        InworldTraceable traceable = runtime.traceable();
        if (traceable == null) {
            endTrace(false);
            return;
        }
        FloatPos px = tracePanelPoint(runtime, sx, sy);
        if (px == null) return; //ray left the plane — keep the last cursor
        FloatPos off = contentOffset(runtime);
        Widget content = runtime.widget.content();
        float cx = (float) Mth.clamp(px.x - off.x, -4, content.width() + 4);
        float cy = (float) Mth.clamp(px.y - off.y, -4, content.height() + 4);
        traceMoved += Math.abs(cx - traceX) + Math.abs(cy - traceY);
        traceX = cx;
        traceY = cy;
        traceable.traceMove(cx, cy);
    }

    /**
     * Ends the session. A committed trace hands the stroke to the widget —
     * except a quick tap (&lt;4px, &lt;6 ticks), which cancels the trace and
     * fires the panel's primary action instead so tap-to-click and
     * hold-to-trace share the interact key.
     */
    void endTrace(boolean commit) {
        PanelRuntime runtime = tracing;
        if (runtime == null) return;
        tracing = null;
        traceInspectHosted = false;
        traceScreen = null;
        InworldTraceable traceable = runtime.traceable();
        if (traceable != null && mc.level != null && mc.player != null) {
            boolean tap = commit && traceMoved < 4f && tick - traceStartTick < 6;
            if (tap) {
                traceable.traceCancel();
                fireAction(runtime);
            } else if (commit) {
                traceable.traceCommit(new InworldPanelContext(mc.level, mc.player, runtime));
            } else {
                traceable.traceCancel();
            }
        }
    }

    boolean traceActive() {
        return tracing != null;
    }

    /** Raw poll of the interact binding — works while the capture screen owns input. */
    boolean traceHeld() {
        KeyMapping key = InworldKeyMappings.interact;
        InputConstants.Key bound = key.key;
        long window = mc.getWindow().getWindow();
        return switch (bound.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> key.isDown();
        };
    }

    /** Look-assist step — called by the trace screen each tick. */
    void tickTraceLook() {
        if (traceLookTicks-- <= 0 || mc.player == null) return;
        mc.player.setYRot(Mth.rotLerp(0.35f, mc.player.getYRot(), traceYaw));
        mc.player.setXRot(Mth.lerp(0.35f, mc.player.getXRot(), tracePitch));
    }

    void onTraceScreenRemoved() {
        traceScreen = null;
        //ESC or a foreign screen took over mid-trace — drop the stroke
        if (tracing != null && !traceInspectHosted) endTrace(false);
    }

    /** GUI render while a trace screen is open — same chrome as the HUD pass. */
    void renderTrace(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderScreenSpace(graphics, mouseX, mouseY, partialTick);
    }

    //endregion

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
        //only interactive panels are worth cycling onto
        List<PanelRuntime> order = panels.values().stream()
                .filter(r -> r.presented && r.spec.interactive())
                .toList();
        if (order.isEmpty()) return;
        int idx = order.indexOf(focused);
        int next = idx < 0
                ? (direction > 0 ? 0 : order.size() - 1)
                : (idx + direction + order.size()) % order.size();
        focus(order.get(next));
        manualFocusTick = tick;
    }

    //endregion

    //region rendering

    /** Face and Expand panels render in world space through the FBO quad path. */
    private static boolean worldSpace(InworldPlacement p) {
        return p instanceof InworldPlacement.Face || p instanceof InworldPlacement.Expand;
    }

    /** Render-to-texture supersampling factor for world-space panels. */
    private static final int FACE_SS = 2;
    /** Dedicated buffer source for FBO passes — flushing the shared level source mid-pass would corrupt the world render. */
    private final MultiBufferSource.BufferSource panelBuffers =
            MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(1 << 18));

    /**
     * Renders face panels in world space during the level stage. Each panel's
     * widget tree is first drawn into an offscreen {@link RenderTarget} with a
     * plain GUI ortho setup, then the texture is blitted onto a single world
     * quad — the quad carries no internal z-layering so nothing inside the
     * panel can z-fight, and the quad itself wins its block surface with a
     * polygon-offset decal bias.
     */
    private void renderFacePanels(RenderLevelStageEvent event, Vec3 cameraPos) {
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        //the textured quad may wind clockwise from the viewing side
        RenderSystem.disableCull();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1f, -4f);
        for (PanelRuntime runtime : panels.values()) {
            if (!worldSpace(runtime.spec.placement())) continue;
            //inspect mode flattens expand panels to docks — nothing world-space to draw
            if (!runtime.presented || runtime.flat || !runtime.widget.visible()
                    || runtime.faceU == null) continue;

            RenderTarget target = faceTarget(runtime);
            renderPanelToTarget(runtime, target, pt);
            drawFaceQuad(runtime, target);
        }
        RenderSystem.polygonOffset(0, 0);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /** Lazily creates/resizes the panel's offscreen target at 2× its gui size. */
    private static RenderTarget faceTarget(PanelRuntime runtime) {
        RenderTarget target = runtime.faceTarget;
        int w = runtime.widget.width() * FACE_SS;
        int h = runtime.widget.height() * FACE_SS;
        if (target == null) {
            target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
            runtime.faceTarget = target;
        }
        if (target.width != w || target.height != h) {
            target.resize(w, h, Minecraft.ON_OSX);
        }
        return target;
    }

    /**
     * Draws the widget tree into the panel's offscreen target using the same
     * ortho + modelView convention vanilla uses for GUI rendering
     * ({@code z = -11000} under a 1000..21000 ortho frustum).
     */
    private void renderPanelToTarget(PanelRuntime runtime, RenderTarget target, float pt) {
        int w = runtime.widget.width();
        int h = runtime.widget.height();

        //save the currently bound FBO + viewport — under Fabulous! graphics the
        //translucent stage renders into a non-main target, so blindly rebinding
        //the main target afterwards would break the level pass
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] prevVp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevVp);

        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        Matrix4f prevProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        mv.translate(0, 0, -11000);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0, w, h, 0, 1000, 21000),
                VertexSorting.ORTHOGRAPHIC_Z);
        try {
            //ortho is 0..w over a w*SS-px viewport — supersampling comes free,
            //no pose scale needed
            GuiGraphics graphics = new GuiGraphics(mc, new PoseStack(), panelBuffers);
            SceneCanvas canvas = SceneCanvas.create(graphics);
            FloatPos uv = runtime == pointed ? pointedUv : null;
            runtime.widget.setFrameState(runtime.focused(), uv != null);
            runtime.widget.render(canvas,
                    uv != null ? (int) uv.x : -1,
                    uv != null ? (int) uv.y : -1, pt);
            canvas.flushBatch();
            //private buffer source — never endBatch() the shared level source mid-pass
            panelBuffers.endBatch();
        } finally {
            target.unbindWrite();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            RenderSystem.viewport(prevVp[0], prevVp[1], prevVp[2], prevVp[3]);
            mv.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(prevProj, VertexSorting.DISTANCE_TO_ORIGIN);
        }
    }

    /** Blits the panel texture onto the face's world-space quad (texture v is flipped). */
    private void drawFaceQuad(PanelRuntime runtime, RenderTarget target) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.enableBlend();

        Matrix4f mat = worldToView != null ? worldToView : new Matrix4f();
        Vec3 o = runtime.faceOrigin;
        Vec3 u = runtime.faceU;
        Vec3 v = runtime.faceV;
        double w = runtime.widget.width(), h = runtime.widget.height();
        Vec3 p10 = o.add(u.scale(w));
        Vec3 p01 = o.add(v.scale(h));
        Vec3 p11 = p10.add(v.scale(h));

        //open animation: scale the quad around its center
        float sc = runtime.openScale;
        if (sc < 0.999f) {
            Vec3 c = o.add(u.scale(w * 0.5)).add(v.scale(h * 0.5));
            o = c.add(o.subtract(c).scale(sc));
            p10 = c.add(p10.subtract(c).scale(sc));
            p01 = c.add(p01.subtract(c).scale(sc));
            p11 = c.add(p11.subtract(c).scale(sc));
        }

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(mat, (float) o.x, (float) o.y, (float) o.z).setUv(0, 1);
        buffer.addVertex(mat, (float) p01.x, (float) p01.y, (float) p01.z).setUv(0, 0);
        buffer.addVertex(mat, (float) p11.x, (float) p11.y, (float) p11.z).setUv(1, 0);
        buffer.addVertex(mat, (float) p10.x, (float) p10.y, (float) p10.z).setUv(1, 1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
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
        Set<Integer> framedEnts = new HashSet<>();
        Set<Integer> hotEnts = new HashSet<>();
        for (PanelRuntime runtime : panels.values()) {
            if (!runtime.presented || !runtime.widget.visible()) continue;
            BlockPos pos = runtime.spec.anchor().blockPos();
            if (pos != null) {
                framed.add(pos);
                if (runtime.focused() || runtime == pointed) hot.add(pos);
            } else if (runtime.spec.anchor() instanceof InworldAnchor.EntityTarget et) {
                framedEnts.add(et.entityId());
                if (runtime.focused() || runtime == pointed) hotEnts.add(et.entityId());
            }
        }
        boolean hasExpand = false;
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.spec.placement() instanceof InworldPlacement.Expand
                    && runtime.presented && !runtime.flat && runtime.widget.visible()
                    && runtime.anchorWorld != null && runtime.faceU != null) {
                hasExpand = true;
                break;
            }
        }
        if (!hasExpand && framed.isEmpty() && framedEnts.isEmpty()) return;

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        //vanilla-style outline pass — the block's real voxel shape and entity
        //hitboxes drawn with RenderType.lines(), same as the crosshair hit
        //outline and the F3+B debug boxes
        PoseStack pose = new PoseStack();
        pose.last().pose().set(worldToView);
        pose.last().normal().set(new Matrix3f(worldToView));
        VertexConsumer lines = panelBuffers.getBuffer(RenderType.lines());
        for (BlockPos pos : framed) {
            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.empty());
            if (shape.isEmpty()) shape = Shapes.block();
            int c = hot.contains(pos) ? HackerTheme.SCAN_SHAPE_HOT : HackerTheme.SCAN_SHAPE;
            emitShape(pose, lines, shape, pos.getX(), pos.getY(), pos.getZ(),
                    red(c), green(c), blue(c), alpha(c));
        }
        for (int id : framedEnts) {
            Entity entity = mc.level.getEntity(id);
            if (entity == null) continue;
            Vec3 p = entity.getPosition(framePartialTick);
            var dims = entity.getDimensions(entity.getPose());
            double hw = dims.width() * 0.5;
            AABB box = new AABB(p.x - hw, p.y, p.z - hw,
                    p.x + hw, p.y + dims.height(), p.z + hw).inflate(0.03);
            int c = hotEnts.contains(id) ? HackerTheme.SCAN_SHAPE_HOT : HackerTheme.SCAN_SHAPE;
            LevelRenderer.renderLineBox(pose, lines, box,
                    red(c), green(c), blue(c), alpha(c));
        }
        panelBuffers.endBatch(RenderType.lines());

        //hacker accents — corner ticks, the top-loop sweep and expand
        //connectors stay on the cheap DEBUG_LINES pass
        double t = (mc.level.getGameTime() + framePartialTick) * 0.9;
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = worldToView;
        for (BlockPos pos : framed) {
            emitScanFrame(buffer, mat, pos, t, hot.contains(pos));
        }
        emitExpandConnectors(buffer, mat);
        var mesh = buffer.build();
        if (mesh != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(mesh);
        }

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private void emitExpandConnectors(BufferBuilder buffer, Matrix4f mat) {
        for (PanelRuntime runtime : panels.values()) {
            if (!(runtime.spec.placement() instanceof InworldPlacement.Expand)) continue;
            //inspect flattens expand panels to docks — no world quad, no connector
            if (!runtime.presented || runtime.flat || !runtime.widget.visible()
                    || runtime.anchorWorld == null || runtime.faceU == null) continue;
            Vec3 a = runtime.anchorWorld;
            Vec3 pb = runtime.faceOrigin
                    .add(runtime.faceU.scale(runtime.widget.width() * 0.5))
                    .add(runtime.faceV.scale(runtime.widget.height()));
            line(buffer, mat,
                    new double[]{a.x, a.y, a.z},
                    new double[]{pb.x, pb.y, pb.z},
                    runtime.focused() || runtime == pointed
                            ? HackerTheme.LINE_FOCUSED : HackerTheme.LINE);
        }
    }

    /** Replica of vanilla's private {@code LevelRenderer.renderShape} — true voxel edges, not AABB slices. */
    private static void emitShape(PoseStack pose, VertexConsumer out, VoxelShape shape,
                                  double x, double y, double z,
                                  float r, float g, float b, float a) {
        PoseStack.Pose p = pose.last();
        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            float nx = (float) (x1 - x0), ny = (float) (y1 - y0), nz = (float) (z1 - z0);
            float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (nl > 1e-6f) {
                nx /= nl;
                ny /= nl;
                nz /= nl;
            }
            out.addVertex(p, (float) (x0 + x), (float) (y0 + y), (float) (z0 + z))
                    .setColor(r, g, b, a).setNormal(p, nx, ny, nz);
            out.addVertex(p, (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .setColor(r, g, b, a).setNormal(p, nx, ny, nz);
        });
    }

    private static float red(int c) {
        return ((c >> 16) & 0xFF) / 255f;
    }

    private static float green(int c) {
        return ((c >> 8) & 0xFF) / 255f;
    }

    private static float blue(int c) {
        return (c & 0xFF) / 255f;
    }

    private static float alpha(int c) {
        return ((c >> 24) & 0xFF) / 255f;
    }

    private static void emitScanFrame(BufferBuilder buffer, Matrix4f mat, BlockPos pos, double t, boolean bright) {
        double e = 0.003;
        double x0 = pos.getX() - e, y0 = pos.getY() - e, z0 = pos.getZ() - e;
        double x1 = pos.getX() + 1 + e, y1 = pos.getY() + 1 + e, z1 = pos.getZ() + 1 + e;
        double[][] c = {
                {x0, y0, z0}, {x1, y0, z0}, {x0, y0, z1}, {x1, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x0, y1, z1}, {x1, y1, z1}
        };

        int tick = bright ? HackerTheme.SCAN_TICK_HOT : HackerTheme.SCAN_TICK;
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
        line(buffer, mat, p0, p1, HackerTheme.SCAN_SWEEP);
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

        SceneCanvas canvas = SceneCanvas.create(graphics);
        for (PanelRuntime runtime : panels.values()) {
            runtime.widget.setFrameState(runtime.focused(), runtime == pointed);
        }

        renderLeaderLines(graphics);
        renderIndicators(graphics);
        renderDockOverflow(graphics);

        canvas.pushViewport(root.viewport());
        root.render(canvas, (int) pointerX, (int) pointerY, partialTick);
        canvas.popViewport();
        canvas.flushBatch();

        renderTooltip(graphics, pointerX, pointerY);
    }

    /** ease-out-back — slight overshoot so expanding panels "pop" into place. */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
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
            if (runtime.widget.screenX < -900_000) continue; //parked/hidden — no line
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

            //adaptive ink: sample the world behind the line's midpoint — a
            //dark core + light halo over bright terrain, bright core + dark
            //halo in the dark. Smoothed per panel so crossing a brightness
            //edge doesn't flicker the line.
            runtime.lineLum += (sampleLineLuminance(ex, ey, from.x, from.y) - runtime.lineLum) * 0.25f;
            boolean brightBg = runtime.lineLum > 0.5f;
            int color = runtime.focused()
                    ? (brightBg ? HackerTheme.LINE_FOCUSED_DARK : HackerTheme.LINE_FOCUSED)
                    : (brightBg ? HackerTheme.LINE_DARK : HackerTheme.LINE);
            int edge = brightBg ? HackerTheme.LINE_EDGE_LIGHT : HackerTheme.LINE_EDGE;
            drawLine(graphics, (float) ex + 1, (float) ey, (float) from.x + 1, (float) from.y, edge);
            drawLine(graphics, (float) ex, (float) ey + 1, (float) from.x, (float) from.y + 1, edge);
            drawLine(graphics, (float) ex, (float) ey, (float) from.x, (float) from.y, color);
            //hollow diamond marking the source the line leads back to
            drawLine(graphics, (float) from.x, (float) from.y - 3.5f, (float) from.x + 3.5f, (float) from.y, color);
            drawLine(graphics, (float) from.x + 3.5f, (float) from.y, (float) from.x, (float) from.y + 3.5f, color);
            drawLine(graphics, (float) from.x, (float) from.y + 3.5f, (float) from.x - 3.5f, (float) from.y, color);
            drawLine(graphics, (float) from.x - 3.5f, (float) from.y, (float) from.x, (float) from.y - 3.5f, color);
        }
    }

    /**
     * Estimated scene luminance (0..1) behind a leader line: a ray through
     * the line's midpoint contributes the hit block's map color; a miss falls
     * back to the sky color at the camera (covers day/night/weather/biome).
     */
    private float sampleLineLuminance(double x0, double y0, double x1, double y1) {
        var level = mc.level;
        var proj = projection;
        var player = mc.player;
        if (level == null || proj == null || player == null) return 0.2f;
        Vec3 eye = proj.cameraPos();
        Vec3 dir = proj.rayDirection((x0 + x1) * 0.5, (y0 + y1) * 0.5);
        var hit = level.clip(new ClipContext(eye, eye.add(dir.scale(48)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            int col = level.getBlockState(hit.getBlockPos())
                    .getMapColor(level, hit.getBlockPos()).col;
            float r = ((col >> 16) & 0xFF) / 255f;
            float g = ((col >> 8) & 0xFF) / 255f;
            float b = (col & 0xFF) / 255f;
            return 0.299f * r + 0.587f * g + 0.114f * b;
        }
        Vec3 sky = level.getSkyColor(eye, framePartialTick);
        return (float) (0.299 * sky.x + 0.587 * sky.y + 0.114 * sky.z);
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
        KeyMapping key = InworldKeyMappings.inspect;
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
        if (tracing != null) {
            traceMouseMoved(x, y);
            return;
        }
        scene.mouseMoved(x, y);
    }

    boolean inspectMouseClicked(double x, double y, int button) {
        if (tracing != null) {
            endTrace(true); //click mid-trace commits, same as releasing V
            return true;
        }
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
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (InworldKeyMappings.interact.isActiveAndMatches(key)) {
            triggerInteract();
            return true;
        }
        if (InworldKeyMappings.focusNext.isActiveAndMatches(key)) {
            focusStep(1);
            return true;
        }
        if (InworldKeyMappings.focusPrevious.isActiveAndMatches(key)) {
            focusStep(-1);
            return true;
        }
        return scene.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean inspectKeyReleased(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (tracing != null && InworldKeyMappings.interact.isActiveAndMatches(key)) {
            endTrace(true);
            return true;
        }
        return scene.keyReleased(keyCode, scanCode, modifiers);
    }

    boolean inspectCharTyped(char codePoint, int modifiers) {
        return scene.charTyped(codePoint, modifiers);
    }

    //endregion
}
