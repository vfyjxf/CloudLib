package dev.vfyjxf.cloudlib.ui.inworld;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
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
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
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
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private long tick;

    private int parkCursor = 0;
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
        Matrix4f worldToView = new Matrix4f(event.getModelViewMatrix());
        worldToView.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        this.worldToView = worldToView;
        Matrix4f viewToClip = event.getProjectionMatrix();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        projection = Projection.capture(worldToView, viewToClip, cameraPos, w, h);

        resolvePanels();

        //in-world render pass: face panels only, and only in world presentation
        if (!inspecting) {
            renderFacePanels(event, cameraPos);
        }
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

        for (PanelRuntime runtime : panels.values()) {
            runtime.pointedUv = null;
            runtime.anchorWorld = runtime.spec.anchor().position(level);
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
                //flat projection: every panel becomes floating near its anchor
                resolveFloating(runtime, inspectPlacement(placement));
                runtime.flat = true;
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
                }
            }
        }

        //apply layout so widget bounds are fresh for this frame
        scene.stabilize();
    }

    private static InworldPlacement.Floating inspectPlacement(InworldPlacement original) {
        if (original instanceof InworldPlacement.Floating floating) return floating;
        return new InworldPlacement.Floating(
                FloatingPlacement.rightStart,
                List.of(
                        FloatingMiddlewares.offset(18),
                        FloatingMiddlewares.flip(),
                        FloatingMiddlewares.shift(6),
                        FloatingMiddlewares.hide()
                ));
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

    /** Draws a connector line from each flat panel's edge to its projected anchor point. */
    private void renderLeaderLines(GuiGraphics graphics) {
        for (PanelRuntime runtime : panels.values()) {
            if (!runtime.presented || !runtime.flat || !runtime.widget.visible()) continue;
            if (!runtime.spec.leaderLine()) continue;
            FloatPos anchorPx = runtime.anchorScreen;
            if (anchorPx == null) continue;

            int w = runtime.widget.width();
            int h = runtime.widget.height();
            float px = runtime.widget.screenX;
            float py = runtime.widget.screenY;

            //nearest point on the panel rect to the anchor
            double ex = Math.max(px, Math.min(anchorPx.x, px + w));
            double ey = Math.max(py, Math.min(anchorPx.y, py + h));
            //project the anchor point onto the rect border
            double cx = px + w * 0.5;
            double cy = py + h * 0.5;
            if (ex > px && ex < px + w) {
                ey = anchorPx.y < cy ? py : py + h;
            } else if (ey > py && ey < py + h) {
                ex = anchorPx.x < cx ? px : px + w;
            }
            if (anchorPx.x >= px && anchorPx.x <= px + w && anchorPx.y >= py && anchorPx.y <= py + h) {
                continue; //anchor inside the panel — no line
            }

            int color = runtime.focused() ? InworldTheme.LINE_FOCUSED : InworldTheme.LINE;
            drawLine(graphics, (float) ex, (float) ey, (float) anchorPx.x, (float) anchorPx.y, color);
            //anchor node: small square — fill takes corners, not w/h
            int ax = (int) anchorPx.x;
            int ay = (int) anchorPx.y;
            graphics.fill(ax - 1, ay - 1, ax + 2, ay + 2,
                    runtime.focused() ? InworldTheme.LINE_FOCUSED : InworldTheme.LINE_NODE);
        }
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
