package dev.vfyjxf.nimbusprojection.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.floating.AvoidRectsMiddleware;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddleware;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPositioning;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelChannel;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.SplitPlan;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDragAcceptor;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.cloudlib.ui.sync.ContainerContents;
import dev.vfyjxf.cloudlib.util.ContainerScan;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.NimbusKeyMappings;
import dev.vfyjxf.nimbusprojection.NimbusProjection;
import dev.vfyjxf.nimbusprojection.api.NimbusClient;
import dev.vfyjxf.nimbusprojection.api.panel.Decay;
import dev.vfyjxf.nimbusprojection.api.panel.GroupRole;
import dev.vfyjxf.nimbusprojection.api.panel.PanelGroup;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.policy.FocusContext;
import dev.vfyjxf.nimbusprojection.api.policy.FocusPolicy;
import dev.vfyjxf.nimbusprojection.api.policy.SuspendContext;
import dev.vfyjxf.nimbusprojection.api.policy.SuspendPolicy;
import dev.vfyjxf.nimbusprojection.api.policy.SuspendVerdict;
import dev.vfyjxf.nimbusprojection.api.presentation.FlattenContext;
import dev.vfyjxf.nimbusprojection.api.presentation.FlattenedGeometry;
import dev.vfyjxf.nimbusprojection.api.presentation.PanelGeometry;
import dev.vfyjxf.nimbusprojection.api.presentation.PresentationDriver;
import dev.vfyjxf.nimbusprojection.api.presentation.SolveContext;
import dev.vfyjxf.nimbusprojection.api.provider.PanelProvider;
import dev.vfyjxf.nimbusprojection.api.provider.PanelSink;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderContext;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderOptions;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceInfo;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceKind;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelView;
import dev.vfyjxf.nimbusprojection.api.sync.SharedViewContext;
import dev.vfyjxf.nimbusprojection.feature.inventory.InventoryFeature;
import dev.vfyjxf.nimbusprojection.internal.section.SectionContents;
import dev.vfyjxf.nimbusprojection.network.PanelChannelPayload;
import dev.vfyjxf.nimbusprojection.network.PresenceReportPayload;
import dev.vfyjxf.nimbusprojection.network.SharedPanelSpawnPayload;
import dev.vfyjxf.nimbusprojection.network.WorldDragPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.UUID;

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
public final class InworldManager implements NimbusClient {

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

    // region state

    private static final Logger logger = LoggerFactory.getLogger(InworldManager.class);

    private final Minecraft mc = Minecraft.getInstance();

    private final WidgetGroup<Widget> root = new WidgetGroup<>();
    private final Scene scene = new Scene(root);

    private final Map<PanelKey, PanelRuntime> panels = new LinkedHashMap<>();
    private final Map<PanelKey, PanelSpec> imperative = new LinkedHashMap<>();
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

    // region trace-mode state (Witness-style drag interaction)
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
    /**
     * Frozen projection frame captured at traceBegin — every cursor pixel maps
     * through this snapshot so a still mouse maps to a still cursor no matter
     * what the live camera does (the camera can't move while the capture
     * screen is open anyway; the snapshot just makes that guarantee explicit).
     */
    private @Nullable Projection traceProj;
    /** frozen panel basis for world-space panels (o/u/v/n) */
    private Vec3 traceO, traceU, traceV, traceN;
    /** frozen screen rect for flat panels */
    private float tracePanelX, tracePanelY;
    // endregion
    /** Set when the inspect screen was closed by ESC while the key is still held — don't reopen until released. */
    private boolean inspectDismissed;

    private boolean pressedConsumed;
    /** mouse button currently held on the scene (world mode), -1 = none — feeds mouseDragged while held */
    private int heldSceneButton = -1;

    private double heldPtrX, heldPtrY;

    // region world-drag state (world-as-UI item transfer)
    /**
     * Active drag session, non-null while the player holds an item pulled out
     * of a {@link dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable} panel.
     * The carried stack is a preview copy — the real stack stays in its slot
     * until the server commits the {@link WorldDragPayload}.
     */
    private @Nullable WorldDrag dragSession;

    private @Nullable PanelRuntime dragPanel;
    /** session was started inside the inspect screen — targets come from the cursor ray, not the crosshair */
    private boolean dragInspectHosted;
    /** last cursor position while inspect-hosting a drag (scene coords) */
    private double dragCursorX, dragCursorY;

    private final DragTrail dragTrail = new DragTrail();
    /** container under the crosshair right now (may not be in the trail yet) */
    private @Nullable BlockPos dragTarget;
    /** world-space point the carried stack hovers at (ray hit or air position) */
    private @Nullable Vec3 dragHold;
    /** cosmetic item flights spawned at commit — from release point to targets */
    private final List<FlyingStack> flying = new ArrayList<>();
    /** target pos → flash age for the landing highlight */
    private final Map<BlockPos, Integer> landFlash = new LinkedHashMap<>();
    // endregion

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
    private final List<PanelRuntime> floatingQueue = new ArrayList<>();
    private static final int parkBase = -1_000_000;
    private static final int parkStep = 4096;
    /** V pressed on an engaged panel — resolves to close-or-pointer on release. */
    private @Nullable PanelRuntime interactArm;

    private long interactArmTick;
    private boolean interactArmUsed;
    /** ~10° sweep cone for drag target acquisition — the ray only has to
     *  pass near a container for the trail to collect it */
    private static final double dragSweepCos = Math.cos(Math.toRadians(10));

    private record ProviderRegistration(PanelProvider provider, int interval, ProviderOptions options, long nextRun) {}

    /** Each provider's most recent emission — persisted between its runs so reconcile doesn't drop panels on off-ticks. */
    private final Map<PanelProvider, Map<PanelKey, PanelSpec>> providerPanels = new IdentityHashMap<>();
    /** Groups offered this pass — members reconcile into panels, roles tracked per key. */
    private final Map<PanelProvider, Map<PanelKey, PanelGroup>> providerGroups = new IdentityHashMap<>();

    /** Registered presentation drivers by type id; built-in types resolve through the internal solver. */
    private final Map<ResourceLocation, PresentationDriver<?>> presentationDrivers = new HashMap<>();
    /** Client-side materializers for server-declared shared panels. */
    private final Map<ResourceLocation, SharedViewRegistration<?>> sharedViews = new HashMap<>();

    private record SharedViewRegistration<P extends CustomPacketPayload>(
            StreamCodec<? super RegistryFriendlyByteBuf, P> codec, SharedPanelView<P> factory) {}

    /** Remote-player presence fed by the network layer — read-only here. */
    private final Map<PanelKey, List<PresenceInfo>> presences = new HashMap<>();
    /** Keys emitted by sharedDomain providers in the latest provider pass —
     *  presence relays only for these. */
    private final Set<PanelKey> sharedDomainKeys = new HashSet<>();

    private List<PresenceReportPayload.Entry> lastPresenceReport = List.of();

    // endregion

    private InworldManager() {
        // the root fills the whole layout area (window + input strip) so hitTest
        // bounds-checks pass everywhere — without an explicit size the taffy
        // root measures 0×0 (absolute children are out of flow) and nothing
        // would ever be clickable
        root.useStyle(UIStyles.sizePercent(1f));
        // in-world panels wear the nimbus dark theme unless a caller re-pins
        scene.setTheme(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "dark"));
        scene.init();
        scene.mount(SceneContext.create(new InworldSceneHost()));
    }

    // region NimbusClient

    @Override
    public InworldPanel open(PanelSpec spec) {
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
    public void close(PanelKey key) {
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
    public void registerProvider(PanelProvider provider, int intervalTicks, ProviderOptions options) {
        providers.add(new ProviderRegistration(provider, Math.max(1, intervalTicks), options, 0));
    }

    @Override
    public void unregisterProvider(PanelProvider provider) {
        providers.removeIf(r -> r.provider() == provider);
        providerGroups.remove(provider);
        if (providerPanels.remove(provider) != null) {
            Map<PanelKey, PanelSpec> wanted = new LinkedHashMap<>(imperative);
            for (Map<PanelKey, PanelSpec> emitted : providerPanels.values()) {
                for (PanelSpec spec : emitted.values()) {
                    wanted.putIfAbsent(spec.key(), spec);
                }
            }
            reconcile(wanted);
        }
    }

    @Override
    public void registerPresentation(PresentationDriver<?> driver) {
        presentationDrivers.put(driver.presentationId(), driver);
    }

    @Override
    public <P extends CustomPacketPayload> void registerView(
            ResourceLocation view,
            CustomPacketPayload.Type<P> type,
            StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            SharedPanelView<P> factory) {
        // the codec doubles as a channel type so the spec's data payload and
        // later update() payloads resolve through the same wire registry
        PanelChannelPayload.registerChannelType(type, (StreamCodec<RegistryFriendlyByteBuf, P>) codec);
        sharedViews.put(view, new SharedViewRegistration<>(codec, factory));
    }

    /**
     * A shared panel spawned for this client: the registered view
     * materializes a {@link PanelSpec}, which joins the imperative set under
     * the shared key — presence relays because shared keys are a shared
     * domain by definition.
     */
    public void onSharedSpawn(SharedPanelSpawnPayload spawn) {
        if (mc.level == null || !mc.level.dimension().equals(spawn.dimension())) return;
        SharedViewRegistration<?> reg = sharedViews.get(spawn.view());
        if (reg == null) {
            NimbusProjection.logger.warn("No shared view registered for {}", spawn.view());
            return;
        }
        spawnWith(reg, spawn);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <P extends CustomPacketPayload> void spawnWith(
            SharedViewRegistration<P> reg, SharedPanelSpawnPayload spawn) {
        SharedViewContext<P> ctx = new SharedViewContext<>(
                mc.level, mc.player, spawn.key(), spawn.anchor(), spawn.presentation(), (P) spawn.payload());
        PanelSpec spec = reg.factory().open(ctx);
        if (spec == null || !spec.key().equals(spawn.key())) {
            NimbusProjection.logger.warn(
                    "Shared view {} returned a spec not keyed {} — spawn rejected", spawn.view(), spawn.key());
            return;
        }
        spec.interactive(spawn.canInteract());
        imperative.put(spec.key(), spec);
        PanelRuntime runtime = createPanel(spec);
        runtime.shared = true;
    }

    /** Server revoked a shared panel — drop the local materialization. */
    public void onSharedRemove(PanelKey key) {
        close(key);
    }

    @Override
    public Collection<PresenceInfo> presence() {
        return presences.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public Collection<PresenceInfo> presence(PanelKey key) {
        return presences.getOrDefault(key, List.of());
    }

    /**
     * Clientbound channel delivery: the network layer hands a server→client
     * message to the target panel's declared {@code channelHandler}.
     */
    public void onChannelMessage(PanelKey key, CustomPacketPayload payload) {
        PanelRuntime runtime = panels.get(key);
        if (runtime == null || runtime.spec.channelHandler() == null) return;
        runtime.spec.channelHandler().receive(new InworldPanelContext(mc.level, mc.player, runtime), payload);
    }

    /**
     * Publishes the local player's presence over shared-domain keys — the
     * relay that lets other clients render ghost affordances and lets the
     * server tier each watcher's sync. Sent only when the set changes.
     */
    private void publishPresence() {
        if (mc.getConnection() == null) return;
        List<PresenceReportPayload.Entry> report = new ArrayList<>();
        for (PanelRuntime r : panels.values()) {
            if (!r.shared) continue;
            PresenceKind kind = null;
            if (dragSession != null && dragPanel == r) {
                // dragging on a player-anchored panel would expose the carried
                // stack — remote viewers see the engagement, not the payload
                kind = playerAnchored(r) ? PresenceKind.engaged : PresenceKind.dragging;
            } else if (tracing == r) kind = PresenceKind.tracing;
            else if (r.engaged) kind = PresenceKind.engaged;
            else if (focused == r) kind = PresenceKind.watching;
            if (kind != null) report.add(new PresenceReportPayload.Entry(r.spec.key(), kind));
        }
        if (!report.equals(lastPresenceReport)) {
            lastPresenceReport = report;
            PacketDistributor.sendToServer(new PresenceReportPayload(report));
        }
    }

    /** True when the panel's anchor is an entity that resolves to a player. */
    private boolean playerAnchored(PanelRuntime runtime) {
        if (!(runtime.spec.anchor() instanceof InworldAnchor.EntityTarget target)) return false;
        return mc.level != null && mc.level.getEntity(target.entityId()) instanceof Player;
    }

    /**
     * Server relayed a remote player's presence report — merge it into the
     * ghost view: the player's previous entries are replaced by the new set.
     */
    public void onPresence(UUID playerId, List<PresenceInfo> infos) {
        if (mc.player != null && playerId.equals(mc.player.getUUID())) return;
        for (List<PresenceInfo> list : presences.values()) {
            list.removeIf(i -> i.playerId().equals(playerId));
        }
        for (PresenceInfo info : infos) {
            presences.computeIfAbsent(info.panelKey(), k -> new ArrayList<>()).add(info);
        }
    }

    @Override
    public Collection<? extends InworldPanel> panels() {
        return Collections.unmodifiableCollection(panels.values());
    }

    @Override
    public @Nullable InworldPanel panel(PanelKey key) {
        return panels.get(key);
    }

    @Override
    public @Nullable InworldPanel focused() {
        return focused;
    }

    /**
     * Moves the focus to a specific panel — internal driver for keynav and
     * soft-focus; not part of the client API surface.
     */
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
    public void pin(PanelKey key) {
        PanelRuntime runtime = panels.get(key);
        if (runtime != null) runtime.userPinned = true;
    }

    @Override
    public void unpin(PanelKey key) {
        PanelRuntime runtime = panels.get(key);
        if (runtime != null) runtime.userPinned = false;
    }

    @Override
    public boolean pinned(PanelKey key) {
        PanelRuntime runtime = panels.get(key);
        return runtime != null && runtime.userPinned;
    }

    @Override
    public List<Rect2i> exclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.presented && runtime.flat) {
                areas.add(new Rect2i(
                        runtime.widget.screenX,
                        runtime.widget.screenY,
                        runtime.widget.width(),
                        runtime.widget.height()));
            }
        }
        return areas;
    }

    @Override
    public Scene scene() {
        return scene;
    }

    // endregion

    // region events

    private void onClientTick(ClientTickEvent.Post event) {
        tick++;
        if (mc.level == null || mc.player == null) return;

        // inspect state machine — raw key polling so it works while the capture screen is open
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

        // focus cycling — while a screen (inspect/trace) owns input, keypresses
        // are routed through inspectKeyPressed; still drain the click counters
        // here so they don't fire a second time when the screen closes
        boolean keysViaScreen = inspecting || mc.screen != null;
        while (NimbusKeyMappings.focusNext.consumeClick()) {
            if (!keysViaScreen) focusNext();
        }
        while (NimbusKeyMappings.focusPrevious.consumeClick()) {
            if (!keysViaScreen) focusPrevious();
        }
        while (NimbusKeyMappings.interact.consumeClick()) {
            if (!keysViaScreen && dragSession == null) triggerInteract();
        }
        while (NimbusKeyMappings.inventory.consumeClick()) {
            if (!keysViaScreen) InventoryFeature.toggle();
        }
        while (NimbusKeyMappings.pin.consumeClick()) {
            if (!keysViaScreen && focused != null) togglePin(focused.key());
        }
        tickInteractArm();

        tickWorldDrag();
        tickSceneDrag();
        tickDecay();

        // providers — each provider's last emission is cached; reconcile runs
        // when at least one provider was re-evaluated (or on the first tick so
        // imperative panels created before providers registered still show)
        if (mc.level != null) {
            boolean ran = tick == 1;
            ProviderContext ctx = new ProviderContext(
                    mc.level, mc.player, mc.gameRenderer.getMainCamera(), currentProjection(), tick);
            for (int i = 0; i < providers.size(); i++) {
                ProviderRegistration reg = providers.get(i);
                if (tick >= reg.nextRun()) {
                    Map<PanelKey, PanelSpec> emitted = new LinkedHashMap<>();
                    Map<PanelKey, PanelGroup> groups = new LinkedHashMap<>();
                    reg.provider().provide(ctx, new PanelSink() {
                        @Override
                        public void offer(PanelSpec spec) {
                            emitted.putIfAbsent(spec.key(), spec);
                        }

                        @Override
                        public void offerGroup(PanelGroup group) {
                            groups.putIfAbsent(group.key(), group);
                            for (PanelGroup.Member member : group.members()) {
                                PanelSpec spec = member.spec();
                                if (spec.groupKey() == null) spec.groupKey(group.key());
                                if (spec.groupRole() == null) spec.groupRole(member.role());
                                emitted.putIfAbsent(spec.key(), spec);
                            }
                        }
                    });
                    providerPanels.put(reg.provider(), emitted);
                    providerGroups.put(reg.provider(), groups);
                    providers.set(
                            i,
                            new ProviderRegistration(
                                    reg.provider(), reg.interval(), reg.options(), tick + reg.interval()));
                    ran = true;
                }
            }
            if (ran) {
                Map<PanelKey, PanelSpec> wanted = new LinkedHashMap<>(imperative);
                sharedDomainKeys.clear();
                for (int i = 0; i < providers.size(); i++) {
                    ProviderRegistration reg = providers.get(i);
                    if (!reg.options().sharedDomain()) continue;
                    Map<PanelKey, PanelSpec> emitted = providerPanels.get(reg.provider());
                    if (emitted != null) sharedDomainKeys.addAll(emitted.keySet());
                }
                for (Map<PanelKey, PanelSpec> emitted : providerPanels.values()) {
                    for (PanelSpec spec : emitted.values()) {
                        wanted.putIfAbsent(spec.key(), spec);
                    }
                }
                reconcile(wanted);
                for (PanelRuntime r : panels.values()) {
                    r.shared = sharedDomainKeys.contains(r.spec.key());
                }
            }
        }

        publishPresence();
        InventoryFeature.tick(this);
        scene.tick();
    }

    private void onLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (mc.level == null || mc.player == null) return;
        if (mc.options.hideGui) return;

        // capture this frame's projection. Note: the per-rendertype stages pass
        // no pose stack (getPoseStack() is a fresh identity stack); the real
        // world→view matrix is getModelViewMatrix(), which is camera ROTATION
        // only — the −cameraPos translation happens inside renderSectionLayer.
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

        // the stage fires inside renderSectionLayer(translucent) while the
        // translucent rendertype is still set up — under Fabulous! that's the
        // translucent target, otherwise the main target. Capture it now: the
        // sprite endBatch below rebinds the main target via output shards, not
        // the FBO this stage was actually entered with.
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        // item sprites go straight into the scene target BEFORE the OIT pass —
        // they ride vanilla rendertype output shards that fight a foreign bound
        // framebuffer, so they can never draw inside the accumulation pass
        renderWorldDragSprites();
        // rebind the real stage target in case endBatch's output shards left
        // main bound instead (Fabulous!)
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);

        // in-world render pass: face panels only in world presentation; the
        // scan frame is useful in both (inspect's leader lines end on it).
        // All of it routes through the weighted-blended OIT pass when the scene
        // depth texture can be borrowed — otherwise the draws fall through to
        // the same direct path they always used.
        int sceneDepth = OitTarget.querySceneDepth(prevFbo);
        boolean oitOk = sceneDepth != 0
                && NimbusShaders.oitReady()
                && oit.ensureSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        oitActive = oitOk;
        oitDraws = 0;
        try {
            if (oitOk) {
                oit.beginAccum(prevFbo, sceneDepth);
            }
            try {
                if (!inspecting) {
                    renderFacePanels(event, cameraPos);
                }
                renderScanFrames();
                renderWorldDragFrames();
            } finally {
                // restores prevFbo, the viewport and ambient blend/depth state
                // even when a draw throws mid-pass
                if (oitOk) {
                    oit.endAccum();
                }
                oitActive = false;
            }
            if (oitOk && oitDraws > 0) {
                oit.resolve(prevFbo);
                // the pass drew with depth writes off — stamp the panel quads'
                // depth so later translucent draws can't punch through them
                stampPanelDepth();
            }
        } finally {
            oitActive = false;
        }
    }

    private void onGuiRender(RenderGuiEvent.Post event) {
        if (mc.level == null || mc.player == null) return;
        if (inspecting) return; // the inspect screen renders instead
        if (mc.screen != null) return; // compat: a real screen hides in-world ui
        if (mc.options.hideGui || panels.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        // virtual pointer: a pointed face's uv, otherwise parked off-screen —
        // the panel tree must not hover under a bare crosshair
        double vx = -10_000, vy = -10_000;
        updatePointing();
        if (pointed != null && pointedUv != null) {
            vx = pointed.inputSceneX() + pointedUv.x;
            vy = pointed.inputSceneY() + pointedUv.y;
        }

        renderScreenSpace(graphics, vx, vy, pt);
        renderDragOverlay(graphics);
        renderEngageChip(graphics);
    }

    /**
     * The "press to open" affordance over a dormant target's anchor — the
     * only thing a gated panel shows before it is engaged: a small key chip
     * floating just above the anchor's screen position.
     */
    private void renderEngageChip(GuiGraphics graphics) {
        PanelRuntime target = focused != null && isDormant(focused)
                ? focused
                : (pointed != null && isDormant(pointed) ? pointed : null);
        boolean closing = false;
        if (target == null) {
            // an engaged, closable panel under the crosshair gets the same
            // affordance mirrored: [V]× toggles it shut
            PanelRuntime cand = focused != null ? focused : pointed;
            if (cand != null && cand.engaged && cand.spec.requiresEngage()) {
                target = cand;
                closing = true;
            }
        }
        if (target == null || target.anchorScreen == null) return;
        float heat = target.focusHeat;
        if (heat <= 0.05f) return;
        // one key toggles both ways — the chip style carries the state:
        // hollow = press to open, solid = press to close
        String key = "[" + NimbusKeyMappings.interact.getTranslatedKeyMessage().getString() + "]";
        var font = mc.font;
        int tw = font.width(key) + 6;
        int x = (int) Math.round(target.anchorScreen.x - tw * 0.5);
        int y = (int) Math.round(target.anchorScreen.y) - 24;
        // the anchor may project onto committed chrome (e.g. a dock column) —
        // hop the chip above whatever covers it so the affordance stays
        // readable instead of stamping over another panel
        for (int pass = 0; pass < 3; pass++) {
            boolean moved = false;
            for (Rect2i r : frameOccupied) {
                if (x + tw <= r.getX()
                        || x >= r.getX() + r.getWidth()
                        || y + 10 <= r.getY()
                        || y >= r.getY() + r.getHeight()) continue;
                y = r.getY() - 12;
                moved = true;
            }
            if (!moved) break;
        }
        x = Math.max(2, Math.min(x, mc.getWindow().getGuiScaledWidth() - tw - 2));
        y = Math.max(2, y);
        if (closing) {
            // solid chip: the open panel is under the crosshair — V closes it
            graphics.fill(x, y, x + tw, y + 10, scaleAlpha(HackerTheme.accent, heat * 0.92f));
            graphics.drawString(font, key, x + 3, y + 1, scaleAlpha(HackerTheme.lineDark, heat));
        } else {
            // hollow chip: dormant target — V opens it
            graphics.fill(x, y, x + tw, y + 10, scaleAlpha(HackerTheme.bgFocused, heat));
            graphics.fill(x, y, x + tw, y + 1, scaleAlpha(HackerTheme.accentDim, heat));
            graphics.fill(x, y + 9, x + tw, y + 10, scaleAlpha(HackerTheme.accentDim, heat));
            graphics.fill(x, y, x + 1, y + 10, scaleAlpha(HackerTheme.accentDim, heat));
            graphics.fill(x + tw - 1, y, x + tw, y + 10, scaleAlpha(HackerTheme.accentDim, heat));
            graphics.drawString(font, key, x + 3, y + 1, scaleAlpha(HackerTheme.accent, heat));
        }
    }

    /** ARGB with its alpha channel rescaled — drives fade-by-heat rendering. */
    private static int scaleAlpha(int argb, float s) {
        int a = Math.min(255, Math.round(((argb >>> 24) & 0xFF) * s));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (inspecting || mc.level == null || mc.screen != null) return;
        int action = event.getAction();
        int button = event.getButton();
        if (action == GLFW.GLFW_PRESS) {
            // no interaction key held — the press belongs to the world
            // (mine/attack/use); panels stay clickable only while V is held
            // or a gesture is already in flight
            if (!panelPointerLive()) return;
            interactArmUsed =
                    true; // a mouse press during an armed V hold means it was pointer intent, not a tap-to-close
            double[] v = virtualPointer();
            Widget hit = scene.hitTest(v[0], v[1]);
            logger.info(
                    "click press: ptr=({},{}) pointed={} uv={} hit={}",
                    (int) v[0],
                    (int) v[1],
                    pointed,
                    pointedUv,
                    hit);
            // world-as-UI: a press on a draggable widget leaves the panel and
            // becomes a world-targeted drag — checked before normal clicking so
            // the button stays held for the whole gesture. A live session
            // already owns the gesture; extra presses are swallowed.
            WorldDraggable src = dragSession == null ? WorldDraggable.find(hit) : null;
            if (src != null) {
                PanelRuntime srcPanel = panelOf((Widget) src);
                if (srcPanel != null) {
                    WorldDrag drag = src.beginWorldDrag(
                            new InworldPanelContext(mc.level, mc.player, srcPanel), v[0], v[1], button);
                    if (drag != null) {
                        dragSession = drag;
                        dragPanel = srcPanel;
                        dragTrail.clear();
                        dragTarget = null;
                        pressedConsumed = true;
                        event.setCanceled(true);
                        return;
                    }
                }
            }
            // clicks landing anywhere on a pointed panel are swallowed even on
            // dead chrome — otherwise LMB would mine the block under the panel
            boolean consumed = scene.mouseClicked(v[0], v[1], button) || pointedInPanel;
            pressedConsumed = consumed;
            if (consumed) {
                heldSceneButton = button; // held on the scene — world-mode drags feed mouseDragged per frame
                heldPtrX = v[0];
                heldPtrY = v[1];
                event.setCanceled(true);
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (dragSession != null) {
                // releasing a drag commits (targets/throw) or cancels (back onto
                // a panel). "Over a panel" must mean a real panel — hitTest can
                // return the window-filling root over empty space, which would
                // cancel every release.
                double[] v = virtualPointer();
                boolean overPanel = panelOf(scene.hitTest(v[0], v[1])) != null;
                commitWorldDrag(overPanel);
                event.setCanceled(true);
                pressedConsumed = false;
                return;
            }
            double[] v = virtualPointer();
            logger.info("click release: ptr=({},{}) hit={}", (int) v[0], (int) v[1], scene.hitTest(v[0], v[1]));
            boolean consumed = scene.mouseReleased(v[0], v[1], button) || pointedInPanel;
            if (consumed || pressedConsumed) event.setCanceled(true);
            pressedConsumed = false;
            if (heldSceneButton == button) heldSceneButton = -1;
        }
    }

    /**
     * While a button is held on the scene in world mode, forward a synthetic
     * mouseDragged each tick — the virtual pointer is the crosshair, so for
     * world-space panels looking around scrubs the drag (e.g. sliders on a
     * face panel); flat panels get a stationary pointer, matching inspect.
     */
    private void tickSceneDrag() {
        if (heldSceneButton < 0) return;
        if (inspecting || mc.screen != null || mc.level == null) {
            heldSceneButton = -1;
            return;
        }
        double[] v = virtualPointer();
        scene.mouseDragged(v[0], v[1], heldSceneButton, v[0] - heldPtrX, v[1] - heldPtrY);
        heldPtrX = v[0];
        heldPtrY = v[1];
    }

    // region world-drag (world-as-UI item transfer)

    /** A world-drag session is in flight — providers can key off this to keep the source panel alive. */
    public boolean dragActive() {
        return dragSession != null;
    }

    /** A block position counts as a drop target when it exposes an item-handler capability. */
    private boolean isDragTarget(BlockPos pos) {
        return mc.level != null && mc.level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
    }

    /**
     * Per-tick drag housekeeping: age flights/flashes, acquire the container
     * under the crosshair into the trail and park the carried stack at the
     * ray hit point. The vanilla {@code hitResult} is reused — it is the same
     * reach ray vanilla uses for block interaction, so "what you point at" is
     * exactly what the drag sees.
     */
    private void tickWorldDrag() {
        flying.removeIf(fs -> {
            if (fs.tick()) return false;
            landFlash.put(BlockPos.containing(fs.to), 0);
            return true;
        });
        landFlash.values().removeIf(age -> age + 1 > FlyingStack.flashTicks);
        landFlash.replaceAll((p, age) -> age + 1);

        if (dragSession == null) return;
        if (mc.player == null
                || mc.level == null
                || dragPanel == null
                || !panels.containsValue(dragPanel)
                || (dragInspectHosted ? !inspecting : (inspecting || mc.screen != null))) {
            cancelWorldDrag();
            return;
        }

        // target acquisition: world mode reuses the vanilla crosshair pick;
        // inspect mode casts the cursor's ray through the live projection
        // (mc.hitResult is useless there — it always follows screen center).
        // Either way a near-miss still collects the container — the sweep only
        // has to pass within a small cone of it, not dead-center it.
        Vec3 eye = mc.player.getEyePosition();
        Vec3 dir;
        HitResult hit;
        double reach = mc.player.blockInteractionRange();
        if (dragInspectHosted) {
            if (projection == null) return;
            // cursor over a panel must not "see through" the UI to a container
            // behind it — the panel surface owns that pixel while dragging
            if (panelOf(scene.hitTest(dragCursorX, dragCursorY)) != null) {
                dragTarget = null;
                dragTrail.leave();
                return;
            }
            dir = projection.rayDirection(dragCursorX, dragCursorY);
            hit = mc.level.clip(new ClipContext(
                    eye, eye.add(dir.scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        } else {
            dir = mc.player.getLookAngle();
            hit = mc.hitResult;
        }
        BlockPos pos = null;
        if (hit instanceof BlockHitResult bhr && bhr.getType() != HitResult.Type.MISS) {
            BlockPos hitPos = bhr.getBlockPos();
            if (isDragTarget(hitPos)) pos = hitPos;
            dragHold = bhr.getLocation();
        } else {
            dragHold = eye.add(dir.scale(2.4));
        }
        if (pos == null) {
            pos = ContainerScan.nearest(mc.level, mc.player, eye, dir, reach, dragSweepCos);
        }
        if (pos != null) {
            dragTarget = pos;
            dragTrail.offer(pos);
        } else {
            dragTarget = null;
            dragTrail.leave();
        }
    }

    /**
     * Release handling for a live drag. {@code overScene} means the pointer
     * came back up onto a panel — that cancels silently; otherwise the
     * gathered trail (or a bare throw) is committed to the server.
     */
    private void commitWorldDrag(boolean overScene) {
        WorldDrag drag = dragSession;
        PanelRuntime panel = dragPanel;
        List<BlockPos> trail = dragTrail.targets();
        Vec3 hold = dragHold;
        dragSession = null;
        dragPanel = null;
        dragTarget = null;
        dragTrail.clear();
        if (drag == null) return;
        if (overScene) {
            // release over a panel: a WorldDragAcceptor on the target claims
            // the stack (panel-to-panel transfer); anything else cancels
            double[] v = virtualPointer();
            Widget hit = scene.hitTest(v[0], v[1]);
            WorldDragAcceptor acceptor = WorldDragAcceptor.find(hit);
            PanelRuntime target = panelOf(hit);
            // same-panel drops reach the acceptor too — slot-merge/reorder
            // is a drop semantics the widget may want
            if (acceptor != null && target != null) {
                acceptor.acceptWorldDrag(drag, new InworldPanelContext(mc.level, mc.player, target), v[0], v[1]);
            }
            return;
        }

        List<BlockPos> targets =
                panel != null ? drag.commitTargets(trail, new InworldPanelContext(mc.level, mc.player, panel)) : trail;
        int mode = targets.isEmpty()
                ? (drag.wholeStack() ? WorldDragPayload.throwStack : WorldDragPayload.throwOne)
                : (drag.wholeStack() ? WorldDragPayload.insertEven : WorldDragPayload.insertOne);
        Vec3 look = mc.player.getLookAngle();
        PacketDistributor.sendToServer(new WorldDragPayload(
                drag.sourceSlot(), mode, targets, look, SectionTarget.of(drag.sourceContainer(), drag.sourceEntity())));

        // the commit just mutated the source (and every target) server-side —
        // drop the throttle so their next watch() re-queries immediately
        // instead of showing a stale slot until the repoll
        SectionTarget sourceTarget = SectionTarget.of(drag.sourceContainer(), drag.sourceEntity());
        if (sourceTarget != null) {
            if (sourceTarget.pos() != null) ContainerContents.invalidate(sourceTarget.pos());
            SectionContents.invalidate(sourceTarget);
        }
        for (BlockPos t : targets) {
            ContainerContents.invalidate(t);
            SectionContents.invalidate(SectionTarget.of(t));
        }

        // cosmetic fly-outs: one sprite per non-zero share, release point →
        // target top-center. Throw mode needs none — the real ItemEntity spawns.
        if (!targets.isEmpty() && hold != null) {
            int[] shares = drag.wholeStack()
                    ? SplitPlan.evenly(drag.carried().getCount(), targets.size())
                    : SplitPlan.oneEach(drag.carried().getCount(), targets.size());
            for (int i = 0; i < targets.size(); i++) {
                if (shares[i] <= 0) continue;
                Vec3 to = Vec3.atCenterOf(targets.get(i)).add(0, 0.55, 0);
                flying.add(new FlyingStack(drag.carried().copyWithCount(shares[i]), hold, to));
            }
        }
    }

    private void cancelWorldDrag() {
        dragSession = null;
        dragPanel = null;
        dragInspectHosted = false;
        dragTarget = null;
        dragTrail.clear();
    }

    // endregion

    private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (inspecting || mc.level == null || mc.screen != null) return;
        if (panels.isEmpty()) return;
        double[] v = virtualPointer();
        if (scene.mouseScrolled(v[0], v[1], event.getScrollDeltaX(), event.getScrollDeltaY()) || pointedInPanel) {
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
        traceProj = null;
        inspecting = false;
        inspectScreen = null;
        cancelWorldDrag();
        flying.clear();
        landFlash.clear();
        oit.close();
        ContainerContents.clear();
    }

    // endregion

    // region providers & lifecycle

    /**
     * Transient panels: a spec-declared {@link Decay} counts down from
     * creation — lingerOnHover freezes the clock while the panel holds the
     * player's attention, the tail of the window fades the panel out, expiry
     * closes it.
     */
    private void tickDecay() {
        if (mc.level == null) return;
        for (PanelRuntime r : panels.values()) {
            Decay decay = r.spec.decay();
            if (decay == null || r.userPinned) continue;
            if (decay.lingerOnHover() && (focused == r || pointed == r || r.engaged || tracing == r)) {
                r.bornTick = tick;
                continue;
            }
            long age = tick - r.bornTick;
            if (age >= decay.ttlTicks()) {
                close(r.key());
                continue;
            }
            int remaining = (int) (decay.ttlTicks() - age);
            if (decay.fadeTicks() > 0 && remaining < decay.fadeTicks()) {
                r.openScale = Math.min(r.openScale, remaining / (float) decay.fadeTicks());
            }
        }
    }

    private void reconcile(Map<PanelKey, PanelSpec> wanted) {
        // remove panels no longer offered
        var it = panels.values().iterator();
        while (it.hasNext()) {
            PanelRuntime runtime = it.next();
            // a pinned panel outlives its offer — the pin is the explicit
            // intent to keep it; it still closes on suspend-close/unpin
            if (!wanted.containsKey(runtime.key()) && !runtime.userPinned) {
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
        // create missing / refresh surviving
        for (PanelSpec spec : wanted.values()) {
            PanelRuntime runtime = panels.get(spec.key());
            if (runtime == null) {
                createPanel(spec);
            } else if (runtime.spec != spec) {
                runtime.spec = spec;
                applySpec(runtime);
            }
        }
    }

    private PanelRuntime createPanel(PanelSpec spec) {
        PanelRuntime runtime = new PanelRuntime(this, spec);
        PanelChannel channel = payload -> PacketDistributor.sendToServer(new PanelChannelPayload(spec.key(), payload));
        InworldPanelContext ctx = new InworldPanelContext(mc.level, mc.player, runtime, channel);
        runtime.bornTick = tick;
        Widget content = spec.content().apply(ctx);
        runtime.widget = new InworldPanelWidget(runtime, spec, content);
        runtime.widget.useStyle(UIStyles.zIndex(spec.interactive() ? 0 : -1));
        if (spec.openAnimation() && mc.level != null) {
            runtime.bornAt = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true);
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
        // non-interactive panels (entity tags) render behind the chrome
        runtime.widget.useStyle(UIStyles.zIndex(runtime.spec.interactive() ? 0 : -1));
    }

    // endregion

    // region per-frame resolution

    private @Nullable Projection currentProjection() {
        if (projection == null) {
            // no level render yet — synthesize an identity-ish projection so providers can still run
            Matrix4f identity = new Matrix4f();
            projection = Projection.capture(
                    identity,
                    identity,
                    Vec3.ZERO,
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight());
        }
        return projection;
    }

    /**
     * Resolves every panel's anchor → placement → screen/face geometry and
     * pushes the result into the chrome widget's pending layout position.
     */
    /**
     * Custom presentation kinds: the driver registered for
     * {@link Presentation#type()} produces this frame's geometry — a world
     * quad renders like a face panel, a screen rect like a flat panel.
     * Unregistered kinds simply don't present.
     */
    @SuppressWarnings("unchecked")
    private void resolveCustom(PanelRuntime runtime, Presentation presentation) {
        PresentationDriver<Presentation> driver =
                (PresentationDriver<Presentation>) presentationDrivers.get(presentation.type());
        if (driver == null) {
            runtime.presented = false;
            return;
        }
        PanelGeometry geometry = driver.resolve(new SolveContext<>(
                presentation,
                runtime,
                mc.level,
                mc.gameRenderer.getMainCamera(),
                projection,
                runtime.anchorWorld == null ? Vec3.ZERO : runtime.anchorWorld,
                List.of(),
                framePartialTick));
        runtime.presented = true;
        switch (geometry) {
            case PanelGeometry.WorldQuad quad -> {
                runtime.flat = false;
                runtime.faceOrigin = quad.center().subtract(quad.axisU()).subtract(quad.axisV());
                runtime.faceU = quad.axisU().scale(2);
                runtime.faceV = quad.axisV().scale(2);
                runtime.faceNormal = quad.axisU().cross(quad.axisV()).normalize();
                runtime.facePpb = quad.pixelsPerBlock();
                runtime.widget.setScreenPos(parkBase, 0);
            }
            case PanelGeometry.ScreenRect rect -> {
                runtime.flat = true;
                runtime.smoothMove = true;
                runtime.targetX = rect.pos().x();
                runtime.targetY = rect.pos().y();
            }
        }
    }

    private @Nullable ClientLevel lastLevel;
    private final List<PanelKey> suspendCloseQueue = new ArrayList<>();

    private void resolvePanels() {
        Projection proj = projection;
        ClientLevel level = mc.level;
        if (proj == null || level == null) return;

        parkCursor = 0;
        stripCursor = 0;
        List<PanelRuntime> docked = dockQueue;
        List<PanelRuntime> expandDeferred = expandQueue;
        List<PanelRuntime> floatingDeferred = floatingQueue;

        // group membership: secondary panels present only while some member of
        // their group is engaged — collect the live group keys once per pass
        Set<PanelKey> engagedGroups = new HashSet<>();
        for (PanelRuntime r : panels.values()) {
            if (r.groupKey != null && r.engaged) engagedGroups.add(r.groupKey);
        }

        boolean dimensionChanged = lastLevel != level;
        lastLevel = level;
        // the inspect screen IS the projection surface — it doesn't count as
        // "a screen open" for suspension; every other screen (esc'd, real
        // menus) still suspends world panels
        boolean screenOpen = mc.screen != null && mc.screen != inspectScreen;
        boolean paused = mc.isPaused();
        List<PanelKey> suspendCloses = suspendCloseQueue;
        suspendCloses.clear();

        for (PanelRuntime runtime : panels.values()) {
            runtime.pointedUv = null;
            runtime.anchorWorld = runtime.spec.anchor().position(level, framePartialTick);
            runtime.anchorScreen = null;
            runtime.presented = false;
            runtime.flat = false;
            runtime.docked = false;
            runtime.smoothMove = false;

            // suspend policy: per-spec lifecycle verdict — close removes the
            // panel outright, suspend keeps it alive but unpresented
            SuspendVerdict verdict = (runtime.spec.suspendPolicy() != null
                            ? runtime.spec.suspendPolicy()
                            : SuspendPolicy.standard())
                    .evaluate(new SuspendContext(
                            level,
                            mc.player,
                            runtime,
                            runtime.anchorWorld != null,
                            screenOpen,
                            paused,
                            dimensionChanged));
            // a pinned panel doesn't close on a dead anchor — it keeps the
            // card up ("signal lost") until unpinned; a dimension change or
            // an explicit policy still closes it outright
            if (verdict == SuspendVerdict.close && !(runtime.userPinned && !dimensionChanged)) {
                suspendCloses.add(runtime.key());
                continue;
            }
            if (verdict == SuspendVerdict.suspend) continue;

            Vec3 anchor = runtime.anchorWorld;
            if (anchor == null || !runtime.widget.visible()) {
                if (runtime.userPinned) {
                    // dead anchor: dock at the stale hint position so the
                    // "signal lost" card stays discoverable
                    runtime.anchorScreen = new FloatPos(
                            Math.clamp(runtime.widget.screenX, 0, mc.getWindow().getGuiScaledWidth()),
                            Math.clamp(runtime.widget.screenY, 0, mc.getWindow().getGuiScaledHeight()));
                    resolveDock(runtime, inspectPlacement(runtime.spec.presentation()), docked);
                }
                continue;
            }

            // secondary group members stay hidden until their group wakes up
            if (runtime.groupRole == GroupRole.secondary
                    && (runtime.groupKey == null || !engagedGroups.contains(runtime.groupKey))) {
                runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
                continue;
            }

            runtime.distance = proj.distance(anchor);
            if (runtime.distance > runtime.spec.maxDistance() && !runtime.userPinned) continue;

            runtime.anchorScreen = proj.worldToScreen(anchor);

            // engagement lifecycle: an engaged panel stays open while it is
            // pointed at, soft-focused, hosting a session, or inspect is flat —
            // after losing all of those it folds away after a short grace
            if (runtime.engaged) tickEngagement(runtime);

            // engagement gate: an on-demand panel stays dormant — it tracks
            // the anchor so the scan frame/key chip and the targeting math
            // still work, but presents no chrome until the interact key
            // expands it. Watch-Dogs-style: the world isn't wallpapered with
            // ui until the player asks for it.
            if (isDormant(runtime) && !runtime.userPinned) {
                runtime.indicator = false;
                runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
                continue;
            }

            // off-screen collapse: shrink to an edge indicator instead of
            // presenting the full panel where the target can't be seen.
            // skipped while inspecting — the flat projection is meant to show
            // every panel regardless of facing
            if (runtime.spec.hint().collapsesOffscreen()
                    && !inspecting
                    && !runtime.userPinned
                    && anchorOffscreen(runtime.anchorScreen)) {
                runtime.indicator = true;
                runtime.indicatorDir = offscreenDirection(anchor);
                runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
                continue;
            }
            runtime.indicator = false;

            Presentation placement = runtime.spec.presentation();

            // InspectOnly panels have no world form at all — outside inspect
            // they stay parked (presented=false), the dormant-style anchor
            // tracking keeps the scan-frame affordance working when the flag
            // asks for it
            if (!inspecting && placement instanceof Presentation.InspectOnly) {
                runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
                continue;
            }

            // floating-on-idle: the resting form is a screen card tracking
            // the anchor's projection (Jade-style), not a corner dock — the
            // declared world presentation only appears once V pins the
            // panel in (engaged). Skipped under inspect: the flat projection
            // owns placement while the screen is up.
            if (runtime.spec.floatingOnIdle() && !runtime.engaged && !inspecting) {
                placement = Presentation.floating();
            }

            // engage-expansion: a dormant Face/Follow panel's engaged form is
            // the world hologram — the glance affordance becomes the expand
            // anchor. A pinned panel keeps its dock instead — the pin wins.
            if (runtime.engaged
                    && !runtime.userPinned
                    && (placement instanceof Presentation.Face || placement instanceof Presentation.Follow)) {
                placement = Presentation.expand();
            }
            runtime.effective = placement;
            if (runtime.userPinned || (inspecting && inspectScopeContains(runtime))) {
                // a custom driver's inspect policy decides its flat form;
                // builtins always dock
                if (inspecting && !runtime.userPinned
                        && inspectByDriver(runtime, placement, proj)) {
                    continue;
                }
                // pinned and flattened panels both dock to a screen corner
                resolveDock(runtime, inspectPlacement(placement), docked);
            } else {
                switch (placement) {
                    case Presentation.Face face -> resolveFace(runtime, face);
                    // floating resolves after dock layout so the panels can
                    // steer clear of committed chrome instead of covering it
                    case Presentation.Floating floating -> floatingDeferred.add(runtime);
                    case Presentation.Follow follow -> {
                        resolveFollow(runtime, follow);
                        runtime.flat = true;
                    }
                    case Presentation.Dock dock -> resolveDock(runtime, dock, docked);
                    // expand resolves AFTER dock layout — its world spot must
                    // not project onto screen area the flat panels occupy
                    case Presentation.Expand expand -> expandDeferred.add(runtime);
                    // custom presentation kinds resolve through their registered driver
                    default -> resolveCustom(runtime, placement);
                }
            }
        }
        for (PanelKey k : suspendCloses) close(k);

        layoutDocks(docked);
        // dockQueue is cleared after the floating pass — degraded floats
        // append to it and a second solve packs them into the same corners
        int dockCount = docked.size();

        // screen rects the foreground chrome occupies: every docked panel
        // (interactive or not — a dock slot is chrome), interactive flat
        // panels resolved so far, and projected world-space panels
        List<Rect2i> occupied = new ArrayList<>();
        List<Rect> obstacles = new ArrayList<>();
        for (PanelRuntime r : panels.values()) {
            if (!r.presented || !r.widget.visible()) continue;
            if (r.flat && (r.spec.interactive() || r.docked)) {
                Rect2i rect = new Rect2i(
                        r.smoothMove ? r.targetX : r.widget.screenX,
                        r.smoothMove ? r.targetY : r.widget.screenY,
                        r.widget.width(),
                        r.widget.height());
                occupied.add(rect);
                obstacles.add(new Rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
            } else if (!r.flat && worldSpace(r.spec.presentation())) {
                Rect2i b = projectedWorldRect(r);
                if (b != null) {
                    occupied.add(b);
                    obstacles.add(new Rect(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
                }
            }
        }

        for (PanelRuntime r : floatingDeferred) {
            int before = docked.size();
            resolveFloating(r, (Presentation.Floating) r.effective, obstacles, docked);
            if (docked.size() != before) continue; // degraded into the dock queue
            // each resolved floating panel becomes an obstacle for the next —
            // two panels sharing an anchor side can't stack on each other
            if (r.presented && r.widget.visible()) {
                r.flat = true;
                Rect2i rect = new Rect2i(r.targetX, r.targetY, r.widget.width(), r.widget.height());
                occupied.add(rect);
                obstacles.add(new Rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
            }
        }
        floatingDeferred.clear();

        if (docked.size() > dockCount) {
            // late dock arrivals: the solve is deterministic and order-stable,
            // so a second pass leaves earlier panels exactly where they were
            // and packs the newcomers into the remaining corner budget
            layoutDocks(docked);
            for (int i = dockCount; i < docked.size(); i++) {
                PanelRuntime r = docked.get(i);
                if (!r.presented) continue;
                Rect2i rect = new Rect2i(r.targetX, r.targetY, r.widget.width(), r.widget.height());
                occupied.add(rect);
                obstacles.add(new Rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
            }
        }
        docked.clear();

        for (PanelRuntime r : expandDeferred) {
            resolveExpand(r, (Presentation.Expand) r.effective, occupied);
            // a shown hologram reserves its own screen rect for the next one
            if (r.presented && !r.flat) {
                Rect2i b = projectedWorldRect(r);
                if (b != null) occupied.add(b);
            }
        }
        expandDeferred.clear();

        resolveConflicts(occupied);
        frameOccupied = occupied;
        smoothFlatPositions();

        // open-animation drive (world panels scale their quad instead)
        float now = level.getGameTime() + framePartialTick;
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.bornAt >= 0) {
                float t = (now - runtime.bornAt) / 9f;
                runtime.openScale = t >= 1f ? 1f : 0.25f + 0.75f * easeOutBack(Math.max(t, 0f));
                if (t >= 1f) runtime.bornAt = -1;
            }
            runtime.widget.openScale = runtime.flat ? runtime.openScale : 1f;
            // non-interactive tags shrink with distance so far labels don't hog space
            runtime.widget.distScale = runtime.flat && !runtime.spec.interactive()
                    ? (float) Math.min(1f, Math.max(0.45f, 9.5 / Math.max(runtime.distance, 1)))
                    : 1f;
        }

        // the scene's coordinate space is the window plus a virtual "input
        // strip" to its right where face panels are parked — inside the root's
        // bounds so hitTest reaches them, outside the window so they never draw
        faceStripWidth = stripCursor > 0 ? stripCursor + 32 : 0;
        scene.setLayoutArea(
                mc.getWindow().getGuiScaledWidth() + faceStripWidth,
                mc.getWindow().getGuiScaledHeight());

        // apply layout so widget bounds are fresh for this frame
        scene.stabilize();
    }

    /** Inspect flattens every placement into a corner dock. */
    private static Presentation.Dock inspectPlacement(Presentation original) {
        if (original instanceof Presentation.Dock dock) return dock;
        return new Presentation.Dock(Presentation.DockCorner.auto);
    }

    /**
     * Whether the panel joins the inspect flat projection under the
     * configured {@code inspect.scope}. {@link Presentation.InspectOnly}
     * panels always join — the flat layer is their only form.
     */
    private boolean inspectScopeContains(PanelRuntime r) {
        if (r.spec.presentation() instanceof Presentation.InspectOnly) return true;
        return switch (NimbusConfig.inspectScope()) {
            case all -> true;
            case focused -> r == focused || sameGroup(r, focused);
            case focusAndPinned -> r.userPinned || r == focused || sameGroup(r, focused);
        };
    }

    private static boolean sameGroup(PanelRuntime r, @Nullable PanelRuntime other) {
        return other != null && r.groupKey != null && r.groupKey.equals(other.groupKey);
    }

    /**
     * Lets a custom presentation's driver decide its inspect form:
     * {@code hidden} parks the panel, {@code custom} places the driver
     * {@linkplain PresentationDriver#flatten flattened} rect directly.
     * Returns true when the driver took over (no dock slot wanted).
     */
    @SuppressWarnings("unchecked")
    private boolean inspectByDriver(PanelRuntime runtime, Presentation placement, Projection proj) {
        PresentationDriver<Presentation> driver =
                (PresentationDriver<Presentation>) presentationDrivers.get(placement.type());
        if (driver == null) return false;
        return switch (driver.inspectPolicy()) {
            case hidden -> {
                runtime.widget.setScreenPos(parkBase, 0);
                yield true;
            }
            case custom -> {
                FlattenedGeometry flat = driver.flatten(new FlattenContext<>(
                        placement,
                        runtime,
                        proj,
                        runtime.anchorScreen != null ? runtime.anchorScreen : new FloatPos(0, 0),
                        new Size(runtime.widget.width(), runtime.widget.height())));
                runtime.presented = true;
                runtime.flat = true;
                runtime.smoothMove = true;
                runtime.targetX = (int) flat.pos().x();
                runtime.targetY = (int) flat.pos().y();
                yield true;
            }
            case flatten -> false;
        };
    }

    private void resolveFloating(
            PanelRuntime runtime, Presentation.Floating placement, List<Rect> obstacles, List<PanelRuntime> docked) {
        FloatPos anchorPx = runtime.anchorScreen;
        if (anchorPx == null) {
            // anchor behind the camera — parked it would be invisible, so it
            // degrades into a dock column where it stays discoverable for the
            // rest of its engagement window
            dockDegrade(runtime, docked);
            return;
        }
        // a pinned panel (live trace) freezes in place — retargeting it now
        // would slide the surface out from under the stroke
        if (runtime.pinned) {
            runtime.presented = true;
            runtime.flat = true;
            runtime.smoothMove = false;
            return;
        }
        // measure against the remembered full size while folded — a folded
        // dock strip must not squeak back out as a float just because its
        // collapsed bounds happen to fit somewhere
        int fw = runtime.widget.width();
        int fh = runtime.widget.height();
        if (runtime.widget.folded) {
            fh = Math.max(fh, runtime.unfoldedHeight);
            fw = Math.max(fw, runtime.unfoldedWidth);
        } else {
            runtime.unfoldedHeight = fh;
            runtime.unfoldedWidth = fw;
        }
        // the spec's middleware chain runs first, then an internal avoid pass
        // keeps the panel clear of already-committed chrome (docks, face
        // projections, earlier floating panels). The avoid is skipped while the
        // panel carries focus — never slide out from under the cursor.
        List<FloatingMiddleware> chain = new ArrayList<>(placement.middlewares());
        if (!runtime.focused() && runtime != pointed) {
            chain.add(AvoidRectsMiddleware.create(() -> obstacles, 4));
        }
        var result = FloatingPositioning.compute(
                new Rect((int) anchorPx.x - 1, (int) anchorPx.y - 1, 2, 2),
                new Rect(0, 0, fw, fh),
                new Rect(
                        0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()),
                placement.placement(),
                chain);
        if (floatingHidden(result)) {
            // viewport escape: clamp the full panel back inside the screen —
            // slight anchor occlusion beats losing the content; only when the
            // panel is genuinely taller/wider than the viewport does it degrade
            // to a folded chrome strip. Never parks outright.
            clampOntoScreen(runtime, anchorPx, obstacles, docked);
            return;
        }
        if (blockedByChrome((int) result.x(), (int) result.y(), fw, fh, obstacles)) {
            // the push-budget ran out — typically a sandwich between chrome the
            // local escape can't clear. Rather than accept the overlap the
            // panel joins the dock columns: they own the real degrade ladder
            // (fold → hide → +N chip) instead of stacking onto other panels
            dockDegrade(runtime, docked);
            return;
        }
        runtime.widget.setFolded(false);
        runtime.presented = true;
        runtime.smoothMove = true;
        // retarget deadband — sub-2px target churn from anchor/projection noise
        // keeps the panel gliding forever; small deltas just keep the old target
        if (!runtime.posInit || InworldLayout.retarget(runtime.targetX, runtime.targetY, result.x(), result.y(), 2)) {
            runtime.targetX = (int) result.x();
            runtime.targetY = (int) result.y();
        }
    }

    /**
     * Hand a floating panel to the dock queue — the solver's shared column
     * budget folds, then hides + counts it into the corner chip, which is a
     * strictly better degrade than overlapping committed chrome.
     */
    private void dockDegrade(PanelRuntime runtime, List<PanelRuntime> docked) {
        runtime.presented = true;
        runtime.flat = true;
        runtime.docked = true;
        runtime.smoothMove = true;
        runtime.dockCorner = Presentation.DockCorner.auto;
        docked.add(runtime);
    }

    /**
     * Full content size: a folded widget's bounds are collapsed to the chrome
     * strip, so geometry that must describe the <em>real</em> panel (world
     * quads, fold budgets) measures the remembered unfolded size instead.
     */
    private static int fullW(PanelRuntime r) {
        return r.widget.folded ? Math.max(r.widget.width(), r.unfoldedWidth) : r.widget.width();
    }

    private static int fullH(PanelRuntime r) {
        return r.widget.folded ? Math.max(r.widget.height(), r.unfoldedHeight) : r.widget.height();
    }

    /**
     * True when the rect covers any committed chrome past a graze — a thin
     * edge clip is fine (tolerance beats jitter), a real area overlap means
     * the placement failed and the panel should degrade instead.
     */
    private static boolean blockedByChrome(int x, int y, int w, int h, List<Rect> obstacles) {
        Rect self = new Rect(x, y, w, h);
        for (Rect ob : obstacles) {
            Rect in = self.intersection(ob);
            if (in.width() > 6 && in.height() > 6 && in.width() * in.height() > (double) w * h * 0.10) return true;
        }
        return false;
    }

    private static boolean floatingHidden(FloatingPositioning.PositionResult result) {
        Map<String, Object> hide = result.middlewareData().get("hide");
        if (hide == null) return false;
        return Boolean.TRUE.equals(hide.get("referenceHidden")) || Boolean.TRUE.equals(hide.get("escaped"));
    }

    /**
     * Degrade a floating panel whose placement escaped the viewport, in two
     * steps: first clamp the <em>full</em> panel just inside the screen near
     * the anchor — covering a bit of the anchor is far better than losing the
     * content — and only when the panel is genuinely larger than the viewport
     * does it fold to a chrome strip. Either way the panel stays discoverable
     * and resolves back the moment the middleware chain fits again.
     */
    private void clampOntoScreen(
            PanelRuntime runtime, FloatPos anchorPx, List<Rect> obstacles, List<PanelRuntime> docked) {
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int fw = runtime.widget.width();
        // folded panels collapsed — compare the remembered full height or the
        // oversized check would flap fold→unfold→fold every other frame
        int fh = runtime.widget.height();
        if (runtime.widget.folded) {
            fh = Math.max(fh, runtime.unfoldedHeight);
            fw = Math.max(fw, runtime.unfoldedWidth);
        } else {
            runtime.unfoldedHeight = fh;
            runtime.unfoldedWidth = fw;
        }
        boolean oversized = fw > W - 4 || fh > H - 4;
        runtime.widget.setFolded(oversized);
        if (oversized) {
            fw = runtime.widget.width();
            fh = foldHeight(runtime);
        }
        // hug the anchor horizontally, prefer sitting above it; every axis is
        // clamped so the panel can never leak off-screen
        int tx = (int) Math.max(2, Math.min(W - fw - 2, anchorPx.x - fw * 0.5));
        int ty = (int) Math.max(2, Math.min(H - fh - 2, anchorPx.y - fh - 10));
        if (blockedByChrome(tx, ty, fw, fh, obstacles)) {
            // clamping back inside the viewport landed on committed chrome —
            // the dock degrade is strictly better than covering another panel
            dockDegrade(runtime, docked);
            return;
        }
        runtime.presented = true;
        runtime.flat = true;
        runtime.smoothMove = true;
        if (!runtime.posInit || InworldLayout.retarget(runtime.targetX, runtime.targetY, tx, ty, 2)) {
            runtime.targetX = tx;
            runtime.targetY = ty;
        }
    }

    private void resolveFollow(PanelRuntime runtime, Presentation.Follow follow) {
        FloatPos anchorPx = runtime.anchorScreen;
        if (anchorPx == null) {
            runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
            return;
        }
        runtime.presented = true;
        runtime.widget.setFolded(false);
        // follow panels track their anchor tightly — no position smoothing,
        // the interpolated anchor already moves smoothly
        runtime.smoothMove = false;
        // a pinned panel freezes where it is — the anchor keeps moving but the
        // surface under a live trace must not
        if (runtime.pinned) return;
        runtime.widget.setScreenPos((int) (anchorPx.x - fullW(runtime) * 0.5 + follow.offsetX()), (int)
                (anchorPx.y - fullH(runtime) * 0.5 + follow.offsetY()));
    }

    private void resolveDock(PanelRuntime runtime, Presentation.Dock dock, List<PanelRuntime> docked) {
        if (runtime.anchorScreen == null) {
            runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
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
     * Packs docked panels into their screen corners via {@link DockLayout} —
     * the solver is pure; this method only maps runtime state in and out.
     */
    private void layoutDocks(List<PanelRuntime> docked) {
        Arrays.fill(dockTopExtent, 0);
        Arrays.fill(dockOverflow, 0);
        Arrays.fill(dockCursorEnd, 0);
        if (docked.isEmpty()) return;
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();

        List<DockLayout.Item> items = new ArrayList<>(docked.size());
        for (PanelRuntime runtime : docked) {
            FloatPos a = runtime.anchorScreen;
            // a folded panel's bounds collapsed to the chrome strip — budget
            // against the remembered full size so the fold decision is stable
            int h = runtime.widget.height();
            int w = runtime.widget.width();
            if (runtime.widget.folded) {
                h = Math.max(h, runtime.unfoldedHeight);
                w = Math.max(w, runtime.unfoldedWidth);
            } else {
                runtime.unfoldedHeight = h;
                runtime.unfoldedWidth = w;
            }
            items.add(new DockLayout.Item(
                    runtime.dockCorner,
                    w,
                    h,
                    foldHeight(runtime),
                    a != null ? a.x : Double.NaN,
                    a != null ? a.y : Double.NaN,
                    runtime.lastAutoCorner));
        }
        DockLayout.Result result = DockLayout.solve(items, W, H);
        for (int i = 0; i < docked.size(); i++) {
            PanelRuntime runtime = docked.get(i);
            DockLayout.Item item = items.get(i);
            runtime.lastAutoCorner = item.resolved;
            runtime.folded = item.folded;
            if (item.hidden) {
                runtime.presented = false;
                runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
                continue;
            }
            runtime.widget.setFolded(item.folded);
            runtime.targetX = item.x;
            runtime.targetY = item.y;
        }
        System.arraycopy(result.overflow, 0, dockOverflow, 0, dockOverflow.length);
        System.arraycopy(result.topExtent, 0, dockTopExtent, 0, dockTopExtent.length);
        System.arraycopy(result.cursorEnd, 0, dockCursorEnd, 0, dockCursorEnd.length);
    }

    /** Height of a folded panel: title/hint chrome only, content hidden. */
    private static int foldHeight(PanelRuntime r) {
        int padTop = r.spec.title() != null ? HackerTheme.titleHeight + 2 : HackerTheme.padding;
        int padBottom = r.spec.hints().isEmpty() ? HackerTheme.padding : HackerTheme.hintHeight + 2;
        return padTop + padBottom;
    }

    /** bottom edge (px) of the topLeft/topRight dock stacks — tag rails start below them */
    private final int[] dockTopExtent = new int[2];
    /** per-corner count of panels that didn't fit even folded — drawn as "+N" chips */
    private final int[] dockOverflow = new int[Presentation.DockCorner.values().length];
    /** per-corner final stack extent from the layout pass — where the overflow chip hangs */
    private final int[] dockCursorEnd = new int[Presentation.DockCorner.values().length];
    /** last frame's committed chrome rects — the engage chip steers above them */
    private List<Rect2i> frameOccupied = List.of();

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

        List<PanelRuntime> tags = new ArrayList<>();
        for (PanelRuntime r : panels.values()) {
            if (r.presented && r.flat && !r.spec.interactive() && !r.docked && r.widget.visible()) {
                tags.add(r);
            }
        }
        // stable order: group → coarse distance bucket (2-block steps, so tiny
        // distance wiggles don't reorder) → key — keeps slots from swapping
        tags.sort(Comparator.comparing((PanelRuntime t) -> t.spec.hint().zone())
                .thenComparingInt(t -> (int) (t.distance / 2))
                .thenComparing(t -> String.valueOf(t.spec.key())));

        // merge pass: a group shows at most groupLimit members; extras hide
        // and the last visible member carries a "+N" badge
        List<PanelRuntime> visible = new ArrayList<>(tags.size());
        Map<String, Integer> groupIdx = new HashMap<>();
        Map<String, PanelRuntime> groupLast = new HashMap<>();
        Map<String, Integer> groupHidden = new HashMap<>();
        for (PanelRuntime tag : tags) {
            tag.widget.overflow = null;
            String g = tag.spec.hint().zone();
            int idx = groupIdx.merge(g, 1, Integer::sum) - 1;
            if (idx < tag.spec.hint().zoneLimit()) {
                visible.add(tag);
                groupLast.put(g, tag);
            } else {
                groupHidden.merge(g, 1, Integer::sum);
                tag.presented = false;
                tag.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
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
            // the tag's home this frame: a follow tag anchors at screenX (just
            // rewritten by resolveFollow), a floating tag at its resolved target
            int ix = (int) Math.max(2, Math.min(W - w - 2, tag.smoothMove ? tag.targetX : tag.widget.screenX));
            int iy = (int) Math.max(2, Math.min(H - h - 2, tag.smoothMove ? tag.targetY : tag.widget.screenY));

            TagFlow.Result res = TagFlow.resolve(ix, iy, w, h, occupied, tag.lastSlideDir, W, H);
            tag.lastSlideDir = res.slideDir();
            int wx = res.x(), wy = res.y();
            if (res.outcome() == TagFlow.Outcome.rail) {
                railQueue.add(tag);
                continue;
            }

            // commit: a displaced (or still-gliding-home) tag uses the
            // posX/targetX smoothing so escapes and returns animate; a tag at
            // home snaps tight to its anchor with no lag
            boolean displaced = res.outcome() == TagFlow.Outcome.slided;
            boolean settling = tag.posInit && (Math.abs(tag.posX - ix) > 1.5f || Math.abs(tag.posY - iy) > 1.5f);
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

        // rails: packed columns on the left/right edge, below that side's
        // top dock stack
        TagFlow.Rails rails = new TagFlow.Rails(W, H, dockTopExtent[0], dockTopExtent[1], occupied);
        for (PanelRuntime tag : railQueue) {
            int w = tag.widget.width();
            int h = tag.widget.height();
            int side = rails.side(tag.anchorScreen != null ? tag.anchorScreen.x : Double.NaN);
            int[] slot = rails.claim(side, w, h);
            if (slot == null) {
                // rail full — hide the tag this frame
                tag.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
                tag.smoothMove = false;
                continue;
            }
            tag.smoothMove = true; // glide into the rail slot
            placeTag(tag, slot[0], slot[1]);
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
            Vector4f v = new Vector4f((float) anchor.x, (float) anchor.y, (float) anchor.z, 1f)
                    .mul(mv); // world→view (already carries -cam translate)
            float sx = v.x(), sy = v.z() < 0 ? -v.y() : 1f;
            double len = Math.hypot(sx, sy);
            if (len > 1e-4) return new FloatPos(sx / len, sy / len);
        }
        // fallback: camera-relative bearing from yaw — forward=(-sin,cos),
        // right=(-cos,-sin) on the xz plane
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

        // walk each bearing from center to the inset border
        List<IndMark> marks = new ArrayList<>(collapsed.size());
        for (PanelRuntime r : collapsed) {
            FloatPos d = r.indicatorDir;
            double tx = Math.abs(d.x) < 1e-4 ? Double.MAX_VALUE : (cx - margin) / Math.abs(d.x);
            double ty = Math.abs(d.y) < 1e-4 ? Double.MAX_VALUE : (cy - margin) / Math.abs(d.y);
            boolean side = tx < ty; // hits a vertical edge before a horizontal one
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

        // spread marks sharing an edge with a fixed gap, then recenter the run
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
                if (e < 2) m.py = m.tan;
                else m.px = m.tan;
            }
        }

        for (IndMark m : marks) {
            PanelRuntime r = m.runtime;
            FloatPos d = m.dir;
            float px = (float) m.px, py = (float) m.py;
            int color = r.focused() ? HackerTheme.borderFocused : HackerTheme.accentDim;
            drawLine(graphics, px, py - 5, px + 5, py, color);
            drawLine(graphics, px + 5, py, px, py + 5, color);
            drawLine(graphics, px, py + 5, px - 5, py, color);
            drawLine(graphics, px - 5, py, px, py - 5, color);
            // bearing tick pointing further outward
            drawLine(
                    graphics,
                    (float) (px + d.x * 6),
                    (float) (py + d.y * 6),
                    (float) (px + d.x * 10),
                    (float) (py + d.y * 10),
                    HackerTheme.accent);
            // distance sits on the inward side so it stays readable on any edge
            String dist = (int) r.distance + "m";
            double ix = px - d.x * 17, iy = py - d.y * 16;
            graphics.drawString(
                    font,
                    dist,
                    (int) (ix - font.width(dist) * 0.5),
                    (int) (iy - font.lineHeight * 0.5),
                    HackerTheme.textDim);
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
        var corners = Presentation.DockCorner.values();
        for (int c = 0; c < corners.length; c++) {
            int n = dockOverflow[c];
            if (n == 0) continue;
            String s = "+" + n;
            int tw = font.width(s) + 5;
            int x = DockLayout.isLeft(corners[c]) ? marginX : W - marginX - tw;
            int y = DockLayout.isTop(corners[c]) ? marginY + dockCursorEnd[c] : H - marginY - 9 - dockCursorEnd[c];
            graphics.fill(x, y, x + tw, y + 9, HackerTheme.bgFocused);
            // 1px accent frame
            graphics.fill(x, y, x + tw, y + 1, HackerTheme.accentDim);
            graphics.fill(x, y + 8, x + tw, y + 9, HackerTheme.accentDim);
            graphics.fill(x, y, x + 1, y + 9, HackerTheme.accentDim);
            graphics.fill(x + tw - 1, y, x + tw, y + 9, HackerTheme.accentDim);
            graphics.drawString(font, s, x + 3, y + 1, HackerTheme.accent);
        }
    }

    /** One collapsed panel's edge mark — tangential slot may be adjusted by the de-conflict pass. */
    private static final class IndMark {
        PanelRuntime runtime;
        FloatPos dir;
        int edge; // 0=left 1=right 2=top 3=bottom
        double tan; // slot coordinate along the edge
        double px, py; // resolved screen position
    }

    /**
     * Screen-space bounding box of a world-space panel's quad. Null when every
     * corner is off-screen or unprojectable.
     */
    private @Nullable Rect2i projectedWorldRect(PanelRuntime r) {
        if (projection == null || r.faceOrigin == null || r.faceU == null || r.faceV == null) {
            return null;
        }
        return quadScreenRect(projection, r.faceOrigin, r.faceU, r.faceV, r.widget.width(), r.widget.height());
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
            if (runtime.pinned) continue; // a live trace froze this panel
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
    private void resolveExpand(PanelRuntime runtime, Presentation.Expand expand, List<Rect2i> reserved) {
        Vec3 anchor = runtime.anchorWorld;
        Projection proj = projection;
        if (anchor == null || proj == null || mc.level == null || mc.player == null) {
            runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
            return;
        }

        // holograms never fold — a dock-strip fold from an earlier frame must
        // not shrink the world quad or linger on the projected surface
        int pwPx = fullW(runtime);
        int phPx = fullH(runtime);
        runtime.widget.setFolded(false);

        double ppb = expand.pixelsPerBlock();
        runtime.facePpb = ppb;
        double s = 1.0 / ppb;
        double pw = pwPx * s;
        double ph = phPx * s;

        ClientLevel level = mc.level;
        Vec3 eye = mc.player.getEyePosition(framePartialTick);

        // a live trace froze this hologram: keep last frame's spot and basis
        // untouched so the frozen ray-plane mapping in TraceMap stays exact
        if (runtime.pinned && runtime.expandPos != null && runtime.faceU != null) {
            runtime.presented = true;
            runtime.flat = false;
            int px = mc.getWindow().getGuiScaledWidth() + 16 + stripCursor;
            stripCursor += runtime.widget.width() + 16;
            runtime.widget.setScreenPos(px, 8);
            return;
        }

        // other world-space panels already occupy these spots
        List<Vec3> occupiedWorld = new ArrayList<>();
        for (PanelRuntime other : panels.values()) {
            if (other != runtime && other.presented && other.expandPos != null) {
                occupiedWorld.add(other.expandPos);
            }
        }

        Vec3 vDown = new Vec3(0, -1, 0).scale(s);
        Vec3 best = null;
        double bestFrac = 1;

        // fast path: a spot that is still clear stays — the ring scan's scores
        // are noisy (screen coverage flips as the view moves) and near-tied
        // spots flipping every frame is what makes the hologram wander
        if (runtime.expandPos != null && !runtime.expandHidden) {
            boolean contested = false;
            for (Vec3 o : occupiedWorld) {
                if (runtime.expandPos.distanceToSqr(o) < (pw * 0.5 + 0.6) * (pw * 0.5 + 0.6)) {
                    contested = true;
                    break;
                }
            }
            if (!contested) {
                double curFrac = expandScreenOverlap(proj, eye, runtime.expandPos, vDown, s, pwPx, phPx, reserved);
                double cur = expandSpotScore(level, runtime.expandPos, pw, ph, anchor)
                        + curFrac * 600
                        + (curFrac >= 0.999 ? 300 : 0);
                if (ExpandPlacer.keepSpot(false, curFrac, cur)) {
                    best = runtime.expandPos;
                    bestFrac = curFrac;
                }
            }
        }

        if (best == null) {
            double bestScore = Double.MAX_VALUE;
            // hug the anchor: the hologram reads as the block's UI, drifting
            // several blocks away severs that read — start just clear of it
            double[] dys = {0.9, 0.55, 0.25, 1.35, -0.15};
            for (int ring = 0; ring < 4; ring++) {
                double rad = 0.65 + ring * 0.5 + pw * 0.5;
                for (double dy : dys) {
                    for (int i = 0; i < 10; i++) {
                        double ang = i * (Math.PI * 2 / 10);
                        Vec3 spot = anchor.add(Math.cos(ang) * rad, dy, Math.sin(ang) * rad);
                        double score = expandSpotScore(level, spot, pw, ph, anchor);
                        for (Vec3 o : occupiedWorld) {
                            if (spot.distanceToSqr(o) < (pw * 0.5 + 0.6) * (pw * 0.5 + 0.6)) {
                                score += 64; // another hologram already there
                            }
                        }
                        // screen-space cost: covering docked/flat panels is the worst outcome
                        double frac = expandScreenOverlap(proj, eye, spot, vDown, s, pwPx, phPx, reserved);
                        score += frac * 600;
                        if (frac >= 0.999) score += 300; // unprojectable / fully covered
                        if (score < bestScore) {
                            bestScore = score;
                            best = spot;
                            bestFrac = frac;
                        }
                    }
                }
            }

            // hysteresis: the current spot only loses when the alternative is
            // clearly better — an absolute margin, not a relative one, so the
            // 600-weighted coverage term can't flip the choice on a coin toss
            if (runtime.expandPos != null) {
                double curFrac = expandScreenOverlap(proj, eye, runtime.expandPos, vDown, s, pwPx, phPx, reserved);
                double cur = expandSpotScore(level, runtime.expandPos, pw, ph, anchor)
                        + curFrac * 600
                        + (curFrac >= 0.999 ? 300 : 0);
                if (ExpandPlacer.preferCurrent(cur, bestScore)) {
                    best = runtime.expandPos;
                    bestFrac = curFrac;
                }
            }
        }

        // can't show it cleanly → don't show it; hysteresis keeps the
        // show/hide edge from flickering
        if (best == null || ExpandPlacer.shouldHide(bestFrac, runtime.expandHidden)) {
            runtime.expandHidden = true;
            runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
            return;
        }
        runtime.expandHidden = false;
        runtime.presented = true;
        runtime.flat = false;

        // glide toward the chosen spot instead of teleporting — a relocating
        // hologram reads as motion, a teleporting one reads as a bug
        if (runtime.expandPos == null) {
            runtime.expandPos = best;
        } else {
            runtime.expandPos =
                    runtime.expandPos.add(best.subtract(runtime.expandPos).scale(0.25));
            if (runtime.expandPos.distanceToSqr(best) < 0.0025) runtime.expandPos = best;
        }
        best = runtime.expandPos;

        // yaw-billboard toward the player's eye: u×v faces away from the viewer
        // (GUI winding, same convention as face panels)
        Vec3 d = eye.subtract(best);
        double len = Math.hypot(d.x, d.z);
        Vec3 dH = len < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(d.x / len, 0, d.z / len);
        Vec3 u = new Vec3(dH.z, 0, -dH.x);
        runtime.faceU = u.scale(s);
        runtime.faceV = vDown;
        runtime.faceNormal = dH;
        runtime.faceOrigin = best.subtract(runtime.faceU.scale(pwPx * 0.5)).subtract(runtime.faceV.scale(phPx * 0.5));

        int stripX = mc.getWindow().getGuiScaledWidth() + 16 + stripCursor;
        stripCursor += pwPx + 16;
        runtime.widget.setScreenPos(stripX, 8);
    }

    /**
     * Fraction (0..1) of a hologram's projected screen rect covered by
     * reserved foreground rects. 1 when the quad can't project at all.
     */
    private static double expandScreenOverlap(
            Projection proj, Vec3 eye, Vec3 spot, Vec3 vDown, double s, int wPx, int hPx, List<Rect2i> reserved) {
        Vec3 d = eye.subtract(spot);
        double len = Math.hypot(d.x, d.z);
        Vec3 dH = len < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(d.x / len, 0, d.z / len);
        Vec3 u = new Vec3(dH.z, 0, -dH.x).scale(s);
        Vec3 o = spot.subtract(u.scale(wPx * 0.5)).subtract(vDown.scale(hPx * 0.5));
        Rect2i rect = quadScreenRect(proj, o, u, vDown, wPx, hPx);
        if (rect == null) return 1;
        double over = 0;
        for (Rect2i r : reserved) {
            int ix = Math.max(
                    0,
                    Math.min(rect.getX() + rect.getWidth(), r.getX() + r.getWidth()) - Math.max(rect.getX(), r.getX()));
            int iy = Math.max(
                    0,
                    Math.min(rect.getY() + rect.getHeight(), r.getY() + r.getHeight())
                            - Math.max(rect.getY(), r.getY()));
            over += (double) ix * iy;
        }
        return Math.min(1, over / ((double) rect.getWidth() * rect.getHeight()));
    }

    /** Projects a world quad (origin + u·w + v·h) to its screen bounding rect. */
    private static @Nullable Rect2i quadScreenRect(Projection proj, Vec3 o, Vec3 u, Vec3 v, double w, double h) {
        Vec3[] corners = {
            o, o.add(u.scale(w)), o.add(v.scale(h)), o.add(u.scale(w)).add(v.scale(h))
        };
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
        var box = new AABB(
                spot.x - pw * 0.5,
                spot.y - ph * 0.5,
                spot.z - pw * 0.5,
                spot.x + pw * 0.5,
                spot.y + ph * 0.5,
                spot.z + pw * 0.5);
        for (BlockPos b : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.getBlockState(b).isAir()) score += 16;
        }
        return score;
    }

    /**
     * Computes the panel's world-space rect on its face. In world presentation
     * the widget is parked off-screen (it is rendered during the level pass);
     * its scene-space slot doubles as the synthesized pointer coordinate space.
     */
    private void resolveFace(PanelRuntime runtime, Presentation.Face face) {
        var blockPos = runtime.spec.anchor().blockPos();
        if (blockPos == null) {
            // face placement requires a block-bound anchor
            runtime.widget.setScreenPos(parkBase - parkCursor++ * parkStep, 0);
            return;
        }
        runtime.flat = false;
        runtime.presented = true;
        // face panels never fold — a dock-strip fold from an earlier frame
        // (inspect projection, corner budget) must not linger into world space
        runtime.widget.setFolded(false);

        double s = 1.0 / face.pixelsPerBlock();
        runtime.facePpb = face.pixelsPerBlock();

        Direction dir = face.face();
        Vec3 n = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 uAxis = faceUAxis(dir);
        Vec3 vAxis = faceVAxis(dir);

        // center of the panel on the face — the geometric offset only needs to
        // cover the panel's own internal z-layering now that the render pass
        // applies a polygon-offset decal bias against the block surface
        Vec3 facePoint = Vec3.atCenterOf(blockPos)
                .add(n.scale(0.5))
                .add(uAxis.scale(face.u() - 0.5))
                .add(vAxis.scale(face.v() - 0.5))
                .add(n.scale(0.001 + s));

        runtime.faceU = uAxis.scale(s);
        runtime.faceV = vAxis.scale(s);
        runtime.faceNormal = n;
        runtime.faceOrigin = facePoint
                .subtract(runtime.faceU.scale(fullW(runtime) * 0.5))
                .subtract(runtime.faceV.scale(fullH(runtime) * 0.5));

        // park inside the off-screen input strip: reachable by synthesized
        // pointer coords (slotX + u, slotY + v) but never rendered on screen
        int stripX = mc.getWindow().getGuiScaledWidth() + 16 + stripCursor;
        stripCursor += fullW(runtime) + 16;
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

    // endregion

    // region pointing & focus

    /**
     * Whether the world-mode pointer is allowed into panel surfaces this
     * frame. Plain crosshair movement must never hover or press a panel —
     * the world stays directly interactive until the player takes hold of
     * the UI by holding the interact key (inspect supplies its own cursor
     * path). A gesture already in flight keeps its pointer until release.
     */
    private boolean panelPointerLive() {
        return NimbusKeyMappings.interact.isDown() || heldSceneButton >= 0 || dragSession != null;
    }

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

        // 1. crosshair ray against world-space panels (face + expand).
        // Pointing is free — a panel under the crosshair IS pointed, no
        // interact-key hold needed; V only routes clicks into the surface.
        {
            double bestT = Double.MAX_VALUE;
            for (PanelRuntime runtime : panels.values()) {
                if (!worldSpace(runtime.spec.presentation())) continue;
                if (!runtime.presented || runtime.flat || !runtime.widget.visible() || runtime.faceU == null) continue;
                FloatPos uv = Projection.rayPlane(
                        origin,
                        dir,
                        runtime.faceOrigin,
                        runtime.faceU,
                        runtime.faceV,
                        runtime.faceNormal,
                        runtime.widget.width(),
                        runtime.widget.height());
                if (uv == null) continue;
                double dist = distanceAlongRay(origin, dir, runtime);
                if (dist < bestT) {
                    bestT = dist;
                    pointed = runtime;
                    pointedUv = uv;
                }
            }
            if (pointed != null) pointedInPanel = true;

            // 2. crosshair over a flat panel — same free-pointing rule: the
            // card is its own hit region, hovering it selects it for V-pin
            if (pointed == null) {
                double cx = mc.getWindow().getGuiScaledWidth() * 0.5;
                double cy = mc.getWindow().getGuiScaledHeight() * 0.5;
                Widget hit = scene.hitTest(cx, cy);
                PanelRuntime hitPanel = panelOf(hit);
                if (hitPanel != null) {
                    pointed = hitPanel;
                    pointedInPanel = true;
                }
            }
        }

        // 3. crosshair on an anchor block (focus only — clicks fall through to
        // the game). Dormant panels count: aiming at a dormant anchor is what
        // lights up its engage affordance.
        if (pointed == null
                && mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            for (PanelRuntime runtime : panels.values()) {
                if (runtime.anchorWorld != null
                        && blockHit.getBlockPos().equals(runtime.anchor().blockPos())) {
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

        // 4. no exact hit — WD2-style soft focus: the interactive panel whose
        // anchor is nearest the look vector inside a ~30° cone gets selected,
        // so a hotkey press doesn't demand pixel-perfect crosshair aim. Only
        // panels that declared a primary action participate.
        if (pointed == null && !inspecting) {
            softPointed = pickSoftFocus(origin, dir);
        }

        // world mode focus follows pointing — except while a manual cycle is
        // fresh: then the cycled panel keeps focus until the player strictly
        // points at something or the window expires
        if (!inspecting) {
            boolean manual =
                    focused != null && focused.presented && focused.spec.interactive() && tick - manualFocusTick < 100;
            PanelRuntime newFocus;
            if (tracing != null) {
                newFocus = tracing; // a live trace pins focus to its panel
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

    /**
     * Nearest-to-look-axis actionable panel inside the soft-focus cone and
     * range. The incumbent focus keeps its seat until a challenger scores
     * clearly better — without the margin the selection flaps back and forth
     * whenever two anchors sit at similar angles.
     */
    private @Nullable PanelRuntime pickSoftFocus(Vec3 eye, Vec3 look) {
        PanelRuntime best = null;
        double bestScore = Double.MAX_VALUE;
        double incumbentScore = -1;
        for (PanelRuntime r : panels.values()) {
            boolean dormant = isDormant(r);
            if (!dormant && (!r.presented || !r.widget.visible() || !r.spec.interactive())) continue;
            // every interactive panel is engageable — dormant → open,
            // presented → expand/close, action/traceable own the verb first
            if (r.anchorWorld == null) continue;
            if (r.distance > Math.min(r.spec.maxDistance(), InworldLayout.softFocusRange)) continue;
            double score;
            FocusPolicy policy = r.spec.focusPolicy();
            if (policy != null) {
                // custom policy: higher-is-better, UNFOCUSABLE excludes —
                // mapped onto the internal lower-is-better ordering
                Vec3 toAnchor = r.anchorWorld.subtract(eye);
                double angleCos = toAnchor.lengthSqr() < 1e-9 ? 1.0 : look.dot(toAnchor.normalize());
                double policyScore = policy.score(new FocusContext(
                        r,
                        eye,
                        look,
                        r.anchorWorld,
                        angleCos,
                        r.distance,
                        pointed == r,
                        r == focused,
                        framePartialTick));
                if (policyScore == FocusPolicy.UNFOCUSABLE) continue;
                score = -policyScore;
            } else {
                score = InworldLayout.softFocusScore(eye, look, r.anchorWorld);
                if (score < 0) continue;
                score += r.distance * 0.01; // angle decides, distance breaks near-ties
            }
            if (r == focused) incumbentScore = score;
            if (score < bestScore) {
                bestScore = score;
                best = r;
            }
        }
        // hysteresis: the incumbent must be beaten by ~35% score to lose focus
        if (incumbentScore >= 0 && best != null && best != focused && incumbentScore < bestScore * 1.5 + 0.004) {
            return focused;
        }
        return best;
    }

    /**
     * Dormant = gated by on-demand presentation and not yet engaged. A
     * dormant panel still tracks its anchor (scan frame, key chip, soft
     * focus) but presents no chrome.
     */
    private static boolean isDormant(PanelRuntime r) {
        return r.spec.requiresEngage() && !r.engaged;
    }

    /**
     * Per-frame lifecycle of an engaged panel: it holds while the player is
     * still engaged with it — pointing at it, soft-focused on its anchor,
     * hosting a trace/drag, or the inspect projection is up — and releases
     * after the configured grace ticks with none of those.
     */
    private void tickEngagement(PanelRuntime runtime) {
        // a floatingOnIdle panel's engaged form IS the pin — it stays world-anchored
        // until a V tap toggles it off, not just while the player keeps looking
        boolean held =
                inspecting || tracing == runtime || dragPanel == runtime || pointed == runtime || focused == runtime
                        || (runtime.spec.floatingOnIdle() && runtime.engaged);
        if (held) {
            runtime.engageIdleSince = -1;
            return;
        }
        if (runtime.engageIdleSince < 0) runtime.engageIdleSince = tick;
        if (tick - runtime.engageIdleSince > NimbusConfig.engageGraceTicks()) {
            runtime.engaged = false;
        }
    }

    /**
     * Expands a panel into its engaged form. Engagements are no longer
     * exclusive — the player can keep several panels pinned to the world at
     * once, and each releases on its own grace/tap-off.
     */
    private void engage(PanelRuntime runtime) {
        runtime.engaged = true;
        runtime.engageIdleSince = -1;
        if (mc.level != null) {
            runtime.bornAt = mc.level.getGameTime() + framePartialTick;
        }
        focused = runtime;
        scene.requestFocus(runtime.widget);
    }

    /**
     * Whether the panel with the given key is currently engaged — providers
     * use it to keep offering a spec whose anchor is no longer under the
     * crosshair (the panel stays alive while it is being used).
     */
    public boolean engaged(PanelKey key) {
        PanelRuntime r = panels.get(key);
        return r != null && r.engaged;
    }

    /**
     * The interact hotkey — a toggle on the focus-selected target. A dormant
     * panel engages (expands) on press; an already-engaged panel arms a
     * pending tap that resolves on release — quick tap closes it, a hold
     * becomes pointer intent instead (see {@link #tickInteractArm}). A
     * traceable panel still starts its trace on press; a tap there also
     * closes ({@link #endTrace}). Panels that were not opened by V
     * (non-{@code onDemand}) keep firing their primary action on press.
     */
    private void triggerInteract() {
        PanelRuntime target = focused;
        if (target == null || !target.spec.interactive()) return;
        if (isDormant(target)) {
            engage(target);
            return;
        }
        if (!target.presented) return;
        if (target.traceable() != null) {
            beginTrace(target);
            return;
        }
        if (target.engaged) {
            interactArm = target;
            interactArmTick = tick;
            interactArmUsed = false;
            return;
        }
        if (target.spec.action() != null) {
            fireAction(target);
            return;
        }
        // no other verb owns the key — engage toggles the panel's expanded
        // form (e.g. an always-on Face tag expanding to the world hologram)
        engage(target);
    }

    /** Closes a V-opened panel — it drops back to dormant on the next resolve. */
    private void disengage(PanelRuntime runtime) {
        runtime.engaged = false;
        runtime.engageIdleSince = -1;
    }

    /**
     * Tap-vs-hold resolution for the interact key on an engaged panel: a
     * release within the configured tap window that didn't touch a widget
     * toggles the panel shut; a hold outlives the window and the press stays
     * pointer intent (or the mouse was used — also not a tap).
     */
    private void tickInteractArm() {
        PanelRuntime armed = interactArm;
        if (armed == null) return;
        int tapTicks = NimbusConfig.interactTapTicks();
        if (interactHeld()) {
            if (tick - interactArmTick > tapTicks) interactArm = null;
            return;
        }
        interactArm = null;
        if (tick - interactArmTick <= tapTicks && !interactArmUsed && armed.engaged) {
            disengage(armed);
        }
    }

    private void fireAction(PanelRuntime target) {
        var action = target.spec.action();
        if (action == null || mc.level == null || mc.player == null) return;
        action.accept(new InworldPanelContext(mc.level, mc.player, target));
    }

    // region trace mode — Witness-style hold-and-drag on the panel surface

    /**
     * Starts a trace session on the panel: locks the camera behind a capture
     * screen (or reuses the inspect screen when already inspecting), feeds the
     * widget cursor positions unprojected onto its surface, and ends with a
     * commit when the interact key is released.
     */
    private void beginTrace(PanelRuntime runtime) {
        InworldTraceable traceable = runtime.traceable();
        if (traceable == null || mc.level == null || mc.player == null) return;
        if (tracing != null) return; // already in a session — a re-entrant begin would wipe the stroke
        if (mc.screen != null && !inspecting) return; // a foreign screen owns input

        FloatPos start = initialTracePoint(runtime);
        InworldPanelContext ctx = new InworldPanelContext(mc.level, mc.player, runtime);

        // freeze the mapping inputs first: the camera frame and the panel's
        // basis (or flat rect). The cursor snap below needs the frozen basis,
        // and from here until commit a still mouse maps to a still cursor.
        traceProj = projection;
        if (runtime.flat) {
            tracePanelX = runtime.widget.screenX;
            tracePanelY = runtime.widget.screenY;
            traceO = traceU = traceV = traceN = null;
        } else {
            traceO = runtime.faceOrigin;
            traceU = runtime.faceU;
            traceV = runtime.faceV;
            traceN = runtime.faceNormal;
        }

        // Witness start-node semantics: the widget may declare a canonical
        // start point — the cursor (and the session) begins there
        FloatPos snap = traceable.traceCursorStart();
        if (snap != null) start = snap;

        if (!traceable.traceBegin(ctx, (float) start.x, (float) start.y)) {
            traceProj = null;
            fireAction(runtime);
            return;
        }

        tracing = runtime;
        runtime.pinned = true;
        traceX = (float) start.x;
        traceY = (float) start.y;
        traceMoved = 0;
        traceStartTick = tick;
        focused = runtime;
        manualFocusTick = tick;

        if (inspecting) {
            traceInspectHosted = true; // the inspect screen already captures input
        } else {
            traceInspectHosted = false;
            KeyMapping.releaseAll(); // held walk keys would keep running under the screen
            traceScreen = new InworldTraceScreen(this);
            mc.setScreen(traceScreen);
        }

        // physical cursor follows the logical snap — otherwise the next real
        // mouse event would map back to where the press landed and the stroke
        // would visibly jump off the start node
        if (snap != null) warpCursorTo(runtime, snap);
    }

    /**
     * Moves the OS cursor onto the screen point that maps to {@code contentPt}
     * under the frozen trace basis. GLFW delivers the position as a normal
     * mouse-move event, so the trace mapping stays self-consistent.
     */
    private void warpCursorTo(PanelRuntime runtime, FloatPos contentPt) {
        FloatPos off = contentOffset(runtime);
        double u = off.x + contentPt.x;
        double v = off.y + contentPt.y;
        FloatPos screen;
        if (runtime.flat) {
            screen = new FloatPos(tracePanelX + u, tracePanelY + v);
        } else {
            if (traceProj == null || traceU == null) return;
            Vec3 world = traceO.add(traceU.scale(u)).add(traceV.scale(v));
            screen = traceProj.worldToScreen(world);
            if (screen == null) return;
        }
        double sf = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), screen.x * sf, screen.y * sf);
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
        return new FloatPos((float) (scene.x - runtime.widget.screenX), (float) (scene.y - runtime.widget.screenY));
    }

    /**
     * Screen-space cursor → panel pixel space against the frozen trace state —
     * never the live basis, so panel animation/camera drift can't move the
     * cursor under a still mouse.
     */
    private @Nullable FloatPos tracePanelPoint(PanelRuntime runtime, double sx, double sy) {
        if (runtime.flat) {
            return TraceMap.flatUv(sx, sy, tracePanelX, tracePanelY);
        }
        Projection proj = traceProj;
        if (proj == null || traceU == null) return null;
        return TraceMap.worldUv(proj.cameraPos(), proj.rayDirection(sx, sy), traceO, traceU, traceV, traceN);
    }

    void traceMouseMoved(double sx, double sy) {
        PanelRuntime runtime = tracing;
        if (runtime == null) return;
        if (!runtime.presented) { // panel hid mid-trace (parked/offscreen) — drop the stroke
            endTrace(false);
            return;
        }
        InworldTraceable traceable = runtime.traceable();
        if (traceable == null) {
            endTrace(false);
            return;
        }
        FloatPos px = tracePanelPoint(runtime, sx, sy);
        if (px == null) return; // ray left the plane — keep the last cursor
        FloatPos off = contentOffset(runtime);
        Widget content = runtime.widget.content();
        FloatPos cl = TraceMap.clampContent(px, off.x, off.y, content.width(), content.height());
        float cx = (float) cl.x, cy = (float) cl.y;
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
        runtime.pinned = false;
        traceInspectHosted = false;
        traceScreen = null;
        traceProj = null;
        InworldTraceable traceable = runtime.traceable();
        if (traceable != null && mc.level != null && mc.player != null) {
            boolean tap = commit && traceMoved < 4f && tick - traceStartTick < 6;
            if (tap) {
                traceable.traceCancel();
                // a V-opened panel toggles shut on a tap; an always-on panel
                // has nothing to close, so the tap still fires its action
                if (runtime.spec.requiresEngage()) {
                    disengage(runtime);
                } else {
                    fireAction(runtime);
                }
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

    /** Raw poll of the interact binding — works while a capture screen owns input. */
    boolean interactHeld() {
        KeyMapping key = NimbusKeyMappings.interact;
        InputConstants.Key bound = key.key;
        long window = mc.getWindow().getWindow();
        return switch (bound.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> key.isDown();
        };
    }

    void onTraceScreenRemoved() {
        traceScreen = null;
        // ESC or a foreign screen took over mid-trace — drop the stroke
        if (tracing != null && !traceInspectHosted) endTrace(false);
    }

    /** GUI render while a trace screen is open — same chrome as the HUD pass. */
    void renderTrace(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderScreenSpace(graphics, mouseX, mouseY, partialTick);
    }

    // endregion

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
            return new double[] {pointed.inputSceneX() + pointedUv.x, pointed.inputSceneY() + pointedUv.y};
        }
        // parked off-screen — nothing under the bare crosshair counts as hovered
        return new double[] {-10_000, -10_000};
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
        // only interactive panels are worth cycling onto
        List<PanelRuntime> order = panels.values().stream()
                .filter(r -> r.presented && r.spec.interactive())
                .toList();
        if (order.isEmpty()) return;
        int idx = order.indexOf(focused);
        int next = idx < 0 ? (direction > 0 ? 0 : order.size() - 1) : (idx + direction + order.size()) % order.size();
        focus(order.get(next));
        manualFocusTick = tick;
    }

    // endregion

    // region rendering

    /** Face and Expand panels render in world space through the FBO quad path. */
    private static boolean worldSpace(Presentation p) {
        return p instanceof Presentation.Face || p instanceof Presentation.Expand;
    }

    /** Render-to-texture supersampling factor for world-space panels. */
    private static final int faceSs = 2;
    /** Dedicated buffer source for FBO passes — flushing the shared level source mid-pass would corrupt the world render. */
    private final MultiBufferSource.BufferSource panelBuffers =
            MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(1 << 18));

    /**
     * Weighted-blended OIT accumulation target — the world-space UI (panel
     * quads, scan frames, drag trail) draws into it per level stage and gets
     * composited back over the scene. See {@link OitTarget}.
     */
    private final OitTarget oit = new OitTarget();
    /** true while the OIT accumulation pass owns GL state — draw sites switch shaders and skip their own blend/depth setup. */
    private boolean oitActive;
    /** drawWithShader calls emitted into the current accumulation pass — zero means resolve can be skipped. */
    private int oitDraws;

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
        for (PanelRuntime runtime : panels.values()) {
            if (!worldSpace(runtime.spec.presentation())) continue;
            // inspect mode flattens expand panels to docks — nothing world-space to draw
            if (!runtime.presented || runtime.flat || !runtime.widget.visible() || runtime.faceU == null) continue;

            RenderTarget target = faceTarget(runtime);
            renderPanelToTarget(runtime, target, pt);
            drawFaceQuad(runtime, target);
        }
        RenderSystem.enableCull();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /** Lazily creates/resizes the panel's offscreen target at 2× its gui size. */
    private static RenderTarget faceTarget(PanelRuntime runtime) {
        RenderTarget target = runtime.faceTarget;
        int w = runtime.widget.width() * faceSs;
        int h = runtime.widget.height() * faceSs;
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

        // save the currently bound FBO + viewport — under Fabulous! graphics the
        // translucent stage renders into a non-main target, so blindly rebinding
        // the main target afterwards would break the level pass
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
                new Matrix4f().setOrtho(0, w, h, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
        try {
            // ortho is 0..w over a w*SS-px viewport — supersampling comes free,
            // no pose scale needed
            GuiGraphics graphics = new GuiGraphics(mc, new PoseStack(), panelBuffers);
            SceneCanvas canvas = SceneCanvas.create(graphics);
            FloatPos uv = runtime == pointed ? pointedUv : null;
            runtime.widget.setFrameState(runtime.focused(), uv != null);
            runtime.widget.render(canvas, uv != null ? (int) uv.x : -1, uv != null ? (int) uv.y : -1, pt);
            canvas.flushBatch();
            // private buffer source — never endBatch() the shared level source mid-pass
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

    /**
     * Blits the panel texture onto the face's world-space quad (texture v is
     * flipped). During the OIT pass the accumulation shader replaces
     * position_tex and the pass owns blend/depth state — each draw only
     * re-arms the per-attachment blend funcs (rendertype clear-states inside
     * {@link #renderPanelToTarget} clobber them between panels).
     */
    private void drawFaceQuad(PanelRuntime runtime, RenderTarget target) {
        BufferBuilder buffer =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        emitFaceQuad(buffer, runtime);
        if (oitActive) {
            oit.beginDraw();
            RenderSystem.setShader(NimbusShaders::oitAccumTex);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
        }
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        // the textured quad may wind clockwise from the viewing side; the
        // polygon-offset decal bias makes it win against its own block face.
        // Re-asserted per quad — rendertype clear-states inside the panel FBO
        // fill (text uses POLYGON_OFFSET_LAYERING) silently drop it.
        RenderSystem.disableCull();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1f, -4f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        if (oitActive) oitDraws++;
    }

    /**
     * Emits the four face-quad vertices (positions baked through worldToView)
     * into {@code buffer}. Shared by the accumulation draw and the post-resolve
     * depth stamp so both rasterize identical geometry.
     */
    private void emitFaceQuad(BufferBuilder buffer, PanelRuntime runtime) {
        Matrix4f mat = worldToView != null ? worldToView : new Matrix4f();
        Vec3 o = runtime.faceOrigin;
        Vec3 u = runtime.faceU;
        Vec3 v = runtime.faceV;
        double w = runtime.widget.width(), h = runtime.widget.height();
        Vec3 p10 = o.add(u.scale(w));
        Vec3 p01 = o.add(v.scale(h));
        Vec3 p11 = p10.add(v.scale(h));

        // open animation: scale the quad around its center
        float sc = runtime.openScale;
        if (sc < 0.999f) {
            Vec3 c = o.add(u.scale(w * 0.5)).add(v.scale(h * 0.5));
            o = c.add(o.subtract(c).scale(sc));
            p10 = c.add(p10.subtract(c).scale(sc));
            p01 = c.add(p01.subtract(c).scale(sc));
            p11 = c.add(p11.subtract(c).scale(sc));
        }

        buffer.addVertex(mat, (float) o.x, (float) o.y, (float) o.z).setUv(0, 1);
        buffer.addVertex(mat, (float) p01.x, (float) p01.y, (float) p01.z).setUv(0, 0);
        buffer.addVertex(mat, (float) p11.x, (float) p11.y, (float) p11.z).setUv(1, 0);
        buffer.addVertex(mat, (float) p10.x, (float) p10.y, (float) p10.z).setUv(1, 1);
    }

    /**
     * Re-writes the panel quads' depth into the scene depth buffer after the
     * OIT resolve. The accumulation pass ran with {@code depthMask(false)} —
     * without this stamp, later translucent draws (particles, weather) would
     * punch through the panels. The accum shader's alpha discard keeps
     * transparent texels from stamping depth.
     */
    private void stampPanelDepth() {
        if (inspecting) return; // no face quads were accumulated — nothing to stamp
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1f, -4f);
        RenderSystem.setShader(NimbusShaders::oitAccumTex);
        for (PanelRuntime runtime : panels.values()) {
            if (!worldSpace(runtime.spec.presentation())) continue;
            if (!runtime.presented || runtime.flat || !runtime.widget.visible() || runtime.faceU == null) continue;
            RenderTarget target = runtime.faceTarget;
            if (target == null) continue;
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            BufferBuilder buffer =
                    Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            emitFaceQuad(buffer, runtime);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.colorMask(true, true, true, true);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * Draws a POSITION_COLOR mesh through the OIT accumulation shader while the
     * pass is active, or through vanilla position_color on the direct path.
     */
    private void drawColorMesh(MeshData mesh) {
        if (oitActive) {
            oit.beginDraw();
            RenderSystem.setShader(NimbusShaders::oitAccumColor);
        } else {
            RenderSystem.enableBlend();
            // must not x-ray through the level — the canvas batch disables
            // depth testing and would leak it here
            RenderSystem.enableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
        }
        BufferUploader.drawWithShader(mesh);
        if (oitActive) oitDraws++;
    }

    // region scan frame

    /** world-space scan frame corners: which axis deltas each corner owns */
    private static final int[][] scanEdges = {
        {0, 1}, {1, 3}, {3, 2}, {2, 0}, // bottom loop
        {4, 5}, {5, 7}, {7, 6}, {6, 4}, // top loop
        {0, 4}, {1, 5}, {2, 6}, {3, 7} // pillars
    };

    private static final int[][] scanCorners = new int[8][3];

    static {
        for (int i = 0; i < 8; i++) {
            int x = i & 1, y = (i >> 1) & 1, z = (i >> 2) & 1;
            scanCorners[i][0] = (x ^ 1) | (y << 1) | (z << 2); // x-neighbor
            scanCorners[i][1] = x | ((y ^ 1) << 1) | (z << 2); // y-neighbor
            scanCorners[i][2] = x | (y << 1) | ((z ^ 1) << 2); // z-neighbor
        }
    }

    /**
     * Draws the hacker-style scan frame around the anchors of <em>selected</em>
     * panels only (focused or pointed). An idle anchor gets no box at all —
     * the leader line's end marker is the standing indication that something
     * tracks it; the voxel outline is reserved for "this is the UI you are
     * about to interact with".
     */
    private void renderScanFrames() {
        if (worldToView == null || mc.level == null) return;
        Set<BlockPos> framed = new HashSet<>();
        Set<Integer> framedEnts = new HashSet<>();
        Map<BlockPos, Float> frameHeat = new HashMap<>();
        Map<Integer, Float> frameEntHeat = new HashMap<>();
        for (PanelRuntime runtime : panels.values()) {
            // focus heat: ramps while this panel holds the player's attention,
            // decays after — the frame/chip fade instead of popping on/off
            boolean hot = runtime.focused() || runtime == pointed || runtime == softPointed;
            runtime.focusHeat = Mth.clamp(runtime.focusHeat + (hot ? 0.3f : -0.12f), 0f, 1f);
            if (!runtime.widget.visible()) continue;
            // dormant panels present nothing — but a targeted one's anchor is
            // exactly what the scan frame marks. inspectOnly panels with the
            // affordance flag get the same marker (that's the flag's purpose)
            if (!runtime.presented
                    && !isDormant(runtime)
                    && !(runtime.spec.presentation() instanceof Presentation.InspectOnly io && io.affordance()))
                continue;
            if (runtime.focusHeat <= 0.03f) continue;
            BlockPos pos = runtime.spec.anchor().blockPos();
            if (pos != null) {
                framed.add(pos);
                frameHeat.merge(pos, runtime.focusHeat, Math::max);
            } else if (runtime.spec.anchor() instanceof InworldAnchor.EntityTarget et) {
                framedEnts.add(et.entityId());
                frameEntHeat.merge(et.entityId(), runtime.focusHeat, Math::max);
            }
        }
        boolean hasExpand = false;
        for (PanelRuntime runtime : panels.values()) {
            if (runtime.effective instanceof Presentation.Expand
                    && runtime.presented
                    && !runtime.flat
                    && runtime.widget.visible()
                    && runtime.anchorWorld != null
                    && runtime.faceU != null) {
                hasExpand = true;
                break;
            }
        }
        if (!hasExpand && framed.isEmpty() && framedEnts.isEmpty()) return;

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        // everything rides one raw DEBUG_LINES POSITION_COLOR mesh — the old
        // RenderType.lines() pass bounced through rendertype output shards that
        // escape to other framebuffers under Fabulous! and would fight the OIT
        // accumulation target. Normals are simply dropped by the format.
        PoseStack pose = new PoseStack();
        pose.last().pose().set(worldToView);
        pose.last().normal().set(new Matrix3f(worldToView));
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = worldToView;

        // vanilla-style outlines — the block's real voxel shape and entity
        // hitboxes, same as the crosshair hit outline and the F3+B debug boxes
        for (BlockPos pos : framed) {
            float heat = frameHeat.getOrDefault(pos, 1f);
            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.empty());
            if (shape.isEmpty()) shape = Shapes.block();
            int c = HackerTheme.scanShapeHot;
            emitShape(
                    pose,
                    buffer,
                    shape,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    red(c),
                    green(c),
                    blue(c),
                    alpha(c) * heat);
        }
        for (int id : framedEnts) {
            float heat = frameEntHeat.getOrDefault(id, 1f);
            Entity entity = mc.level.getEntity(id);
            if (entity == null) continue;
            Vec3 p = entity.getPosition(framePartialTick);
            var dims = entity.getDimensions(entity.getPose());
            double hw = dims.width() * 0.5;
            AABB box = new AABB(p.x - hw, p.y, p.z - hw, p.x + hw, p.y + dims.height(), p.z + hw).inflate(0.03);
            int c = HackerTheme.scanShapeHot;
            LevelRenderer.renderLineBox(pose, buffer, box, red(c), green(c), blue(c), alpha(c) * heat);
        }

        // hacker accents — corner ticks, the top-loop sweep and expand connectors
        double t = (mc.level.getGameTime() + framePartialTick) * 0.9;
        for (BlockPos pos : framed) {
            emitScanFrame(buffer, mat, pos, t, true, frameHeat.getOrDefault(pos, 1f));
        }
        emitExpandConnectors(buffer, mat);
        var mesh = buffer.build();
        if (mesh != null) {
            drawColorMesh(mesh);
        }

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    // region world-drag rendering

    /**
     * In-world line pass for the drag session: hot scan frames on every trailed
     * container plus the live target, and the fading landing-flash outlines —
     * all in one DEBUG_LINES mesh routed through {@link #drawColorMesh} so it
     * lands in the OIT accumulation pass when active.
     */
    private void renderWorldDragFrames() {
        if (worldToView == null || mc.level == null) return;
        boolean hasDrag = dragSession != null && dragSession.carried() != null;
        if (!hasDrag && landFlash.isEmpty()) return;

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        // trail containers get the same hacker scan frame as selected anchors;
        // the live target (not yet swept long enough to trail) too
        double t = (mc.level.getGameTime() + framePartialTick) * 0.9;
        if (hasDrag) {
            for (BlockPos pos : dragTrail.targets()) {
                emitScanFrame(buffer, worldToView, pos, t, true);
            }
            if (dragTarget != null && !dragTrail.targets().contains(dragTarget)) {
                emitScanFrame(buffer, worldToView, dragTarget, t, true);
            }
        }
        // landing flashes fade a plain box outline — folded into the same mesh
        // (was RenderType.lines(), which can't run inside the OIT pass)
        if (!landFlash.isEmpty()) {
            PoseStack pose = new PoseStack();
            pose.last().pose().set(worldToView);
            pose.last().normal().set(new Matrix3f(worldToView));
            for (var e : landFlash.entrySet()) {
                float f = 1.0f - e.getValue() / (float) FlyingStack.flashTicks;
                AABB box = new AABB(e.getKey()).inflate(0.01);
                LevelRenderer.renderLineBox(pose, buffer, box, 0.36f, 0.95f, 1.0f, 0.85f * f);
            }
        }
        var mesh = buffer.build();
        if (mesh != null) {
            drawColorMesh(mesh);
        }

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * In-world item pass for the drag session: the carried stack billboarded
     * at the ray hit point and in-flight commit sprites. These stay on the
     * shared level buffer source (entity shading, lighting) and draw into the
     * scene target BEFORE the OIT pass begins — their rendertype output shards
     * would fight the accumulation framebuffer.
     */
    private void renderWorldDragSprites() {
        if (worldToView == null || mc.level == null) return;
        boolean hasDrag = dragSession != null && dragSession.carried() != null;
        if (!hasDrag && flying.isEmpty()) return;

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        // item sprites — carried stack and in-flight shares, billboarded to the camera
        PoseStack pose = new PoseStack();
        pose.last().pose().set(worldToView);
        pose.last().normal().set(new Matrix3f(worldToView));
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Quaternionf camRot = mc.gameRenderer.getMainCamera().rotation();
        if (hasDrag && dragHold != null) {
            renderDragItem(dragSession.carried(), dragHold, 0.35f, pose, buffers, camRot);
        }
        for (FlyingStack fs : flying) {
            renderDragItem(fs.stack, fs.pos(), fs.scale(), pose, buffers, camRot);
        }
        buffers.endBatch();

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private void renderDragItem(
            ItemStack stack,
            Vec3 pos,
            float scale,
            PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Quaternionf camRot) {
        pose.pushPose();
        pose.translate(pos.x, pos.y, pos.z);
        pose.mulPose(camRot);
        pose.scale(scale, scale, scale);
        int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(pos));
        mc.getItemRenderer()
                .renderStatic(
                        stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
    }

    /**
     * Screen-space drag preview: a {@code +n} chip at every trailed container
     * showing the share it will receive, plus the carried count under the
     * crosshair so the operation is legible without opening any GUI.
     */
    private void renderDragOverlay(GuiGraphics g) {
        if (dragSession == null || projection == null) return;
        List<BlockPos> targets = dragTrail.targets();
        if (targets.isEmpty() && dragTarget != null) targets = List.of(dragTarget);
        if (!targets.isEmpty()) {
            int[] shares = dragSession.wholeStack()
                    ? SplitPlan.evenly(dragSession.carried().getCount(), targets.size())
                    : SplitPlan.oneEach(dragSession.carried().getCount(), targets.size());
            for (int i = 0; i < targets.size(); i++) {
                if (shares[i] <= 0) continue;
                FloatPos s =
                        projection.worldToScreen(Vec3.atCenterOf(targets.get(i)).add(0, 0.75, 0));
                if (s == null) continue;
                String label = "+" + shares[i];
                int w = mc.font.width(label);
                g.fill((int) s.x() - w / 2 - 2, (int) s.y() - 5, (int) s.x() + w / 2 + 2, (int) s.y() + 4, 0x99081018);
                g.drawString(mc.font, label, (int) s.x() - w / 2, (int) s.y() - 4, 0xFF6CF2FF, false);
            }
        }
        // carried stack + count trail the active pointer — crosshair in world
        // mode, cursor when the drag is inspect-hosted
        int cx = dragInspectHosted ? (int) dragCursorX : mc.getWindow().getGuiScaledWidth() / 2;
        int cy = dragInspectHosted ? (int) dragCursorY : mc.getWindow().getGuiScaledHeight() / 2;
        g.renderItem(dragSession.carried(), cx + 5, cy - 8);
        g.renderItemDecorations(mc.font, dragSession.carried(), cx + 5, cy - 8);
        if (dragSession.wholeStack() && dragSession.carried().getCount() > 1) {
            g.drawString(mc.font, "×" + dragSession.carried().getCount(), cx + 5 + 17, cy - 3, 0xFF6CF2FF, false);
        }
    }

    // endregion

    private void emitExpandConnectors(BufferBuilder buffer, Matrix4f mat) {
        for (PanelRuntime runtime : panels.values()) {
            // effective, not spec: an engaged face/follow panel's hologram IS
            // an expand quad — gating on the declared presentation would skip
            // exactly the panels that need the connector
            if (!(runtime.effective instanceof Presentation.Expand)) continue;
            // inspect flattens expand panels to docks — no world quad, no connector
            if (!runtime.presented
                    || runtime.flat
                    || !runtime.widget.visible()
                    || runtime.anchorWorld == null
                    || runtime.faceU == null) continue;
            Vec3 a = runtime.anchorWorld;
            Vec3 pb = runtime.faceOrigin
                    .add(runtime.faceU.scale(runtime.widget.width() * 0.5))
                    .add(runtime.faceV.scale(runtime.widget.height()));
            int lc = runtime.focused() || runtime == pointed ? HackerTheme.lineFocused : HackerTheme.line;
            // a small axis cross at the anchor — the line visibly originates
            // AT the block instead of floating near it
            double n = 0.07;
            line(buffer, mat, new double[] {a.x - n, a.y, a.z}, new double[] {a.x + n, a.y, a.z}, lc);
            line(buffer, mat, new double[] {a.x, a.y - n, a.z}, new double[] {a.x, a.y + n, a.z}, lc);
            line(buffer, mat, new double[] {a.x, a.y, a.z - n}, new double[] {a.x, a.y, a.z + n}, lc);
            line(
                    buffer,
                    mat,
                    new double[] {a.x, a.y, a.z},
                    new double[] {pb.x, pb.y, pb.z},
                    lc);
        }
    }

    /** Replica of vanilla's private {@code LevelRenderer.renderShape} — true voxel edges, not AABB slices. */
    private static void emitShape(
            PoseStack pose,
            VertexConsumer out,
            VoxelShape shape,
            double x,
            double y,
            double z,
            float r,
            float g,
            float b,
            float a) {
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
                    .setColor(r, g, b, a)
                    .setNormal(p, nx, ny, nz);
            out.addVertex(p, (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .setColor(r, g, b, a)
                    .setNormal(p, nx, ny, nz);
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
        emitScanFrame(buffer, mat, pos, t, bright, 1f);
    }

    private static void emitScanFrame(
            BufferBuilder buffer, Matrix4f mat, BlockPos pos, double t, boolean bright, float alphaScale) {
        double e = 0.003;
        double x0 = pos.getX() - e, y0 = pos.getY() - e, z0 = pos.getZ() - e;
        double x1 = pos.getX() + 1 + e, y1 = pos.getY() + 1 + e, z1 = pos.getZ() + 1 + e;
        double[][] c = {
            {x0, y0, z0}, {x1, y0, z0}, {x0, y0, z1}, {x1, y0, z1},
            {x0, y1, z0}, {x1, y1, z0}, {x0, y1, z1}, {x1, y1, z1}
        };

        int tick = bright ? HackerTheme.scanTickHot : HackerTheme.scanTick;
        // corner ticks: short brighter stubs from each corner along its edges
        double tl = 0.14;
        for (int i = 0; i < 8; i++) {
            for (int nb : scanCorners[i]) {
                double[] a = c[i], b = c[nb];
                double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
                line(buffer, mat, a, new double[] {a[0] + dx * tl, a[1] + dy * tl, a[2] + dz * tl}, tick, alphaScale);
            }
        }
        // scan segment sweeping the top loop
        double s = ((t % 4) + 4) % 4;
        int seg = (int) s;
        double f = s - seg;
        double[] a = c[scanEdges[4 + seg][0]];
        double[] b = c[scanEdges[4 + seg][1]];
        double len = 0.22;
        double f1 = Math.max(0, f - len);
        double[] p0 = {a[0] + (b[0] - a[0]) * f1, a[1] + (b[1] - a[1]) * f1, a[2] + (b[2] - a[2]) * f1};
        double[] p1 = {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
        line(buffer, mat, p0, p1, HackerTheme.scanSweep, alphaScale);
    }

    private static void line(BufferBuilder buffer, Matrix4f mat, double[] a, double[] b, int color) {
        line(buffer, mat, a, b, color, 1f);
    }

    private static void line(BufferBuilder buffer, Matrix4f mat, double[] a, double[] b, int color, float alphaScale) {
        float alpha = ((color >> 24) & 0xFF) / 255f * alphaScale;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float bl = (color & 0xFF) / 255f;
        buffer.addVertex(mat, (float) a[0], (float) a[1], (float) a[2]).setColor(r, g, bl, alpha);
        buffer.addVertex(mat, (float) b[0], (float) b[1], (float) b[2]).setColor(r, g, bl, alpha);
    }

    // endregion

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
        m.m00((float) u.x);
        m.m10((float) u.y);
        m.m20((float) u.z);
        m.m01((float) v.x);
        m.m11((float) v.y);
        m.m21((float) v.z);
        m.m02((float) w.x);
        m.m12((float) w.y);
        m.m22((float) w.z);
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
        Tooltip tooltip = hit.hoverTooltip((int) (pointerX - hit.absolutePos().x()), (int)
                (pointerY - hit.absolutePos().y()));
        if (tooltip != null && tooltip.notEmpty()) {
            ScreenUtil.renderTooltip(graphics, tooltip, (int) pointerX, (int) pointerY);
        }
    }

    /** Draws a connector line from each flat panel's edge to its anchor's scan frame. */
    private void renderLeaderLines(GuiGraphics graphics) {
        for (PanelRuntime runtime : panels.values()) {
            if (!runtime.presented || !runtime.flat || !runtime.widget.visible()) continue;
            if (!runtime.spec.leaderLine()) continue;
            // a dead anchor has no world origin — the leader would point at a
            // fabricated screen hint, not the thing the panel tracks
            if (runtime.anchorWorld == null) continue;
            if (runtime.widget.screenX < -900_000) continue; // parked/hidden — no line
            FloatPos from = leaderOrigin(runtime);
            if (from == null) continue;

            int w = runtime.widget.width();
            int h = runtime.widget.height();
            float px = runtime.widget.screenX;
            float py = runtime.widget.screenY;

            // nearest point on the panel rect to the origin
            double ex = Math.max(px, Math.min(from.x, px + w));
            double ey = Math.max(py, Math.min(from.y, py + h));
            // project the origin onto the rect border
            double cx = px + w * 0.5;
            double cy = py + h * 0.5;
            if (ex > px && ex < px + w) {
                ey = from.y < cy ? py : py + h;
            } else if (ey > py && ey < py + h) {
                ex = from.x < cx ? px : px + w;
            }
            boolean inside = from.x >= px && from.x <= px + w && from.y >= py && from.y <= py + h;

            // adaptive ink: sample the world behind the line's midpoint — a
            // dark core + light halo over bright terrain, bright core + dark
            // halo in the dark. Smoothed per panel so crossing a brightness
            // edge doesn't flicker the line.
            runtime.lineLum += (sampleLineLuminance(ex, ey, from.x, from.y) - runtime.lineLum) * 0.25f;
            boolean brightBg = runtime.lineLum > 0.5f;
            int color = runtime.focused()
                    ? (brightBg ? HackerTheme.lineFocusedDark : HackerTheme.lineFocused)
                    : (brightBg ? HackerTheme.lineDark : HackerTheme.line);
            int edge = brightBg ? HackerTheme.lineEdgeLight : HackerTheme.lineEdge;
            if (!inside) {
                drawLine(graphics, (float) ex + 1, (float) ey, (float) from.x + 1, (float) from.y, edge);
                drawLine(graphics, (float) ex, (float) ey + 1, (float) from.x, (float) from.y + 1, edge);
                drawLine(graphics, (float) ex, (float) ey, (float) from.x, (float) from.y, color);
            }
            // WD2-style node at the anchor end: a hollow diamond ring + center
            // dot — crisp at gui scale where the old stacked fills read as a
            // blurry blob over the scan frame
            int fx = (int) from.x, fy = (int) from.y;
            for (int i = -3; i <= 3; i++) {
                int half = 3 - Math.abs(i);
                graphics.fill(fx - half, fy + i, fx - half + 1, fy + i + 1, edge);
                graphics.fill(fx + half, fy + i, fx + half + 1, fy + i + 1, edge);
            }
            for (int i = -1; i <= 1; i++) {
                int half = 1 - Math.abs(i);
                graphics.fill(fx - half, fy + i, fx - half + 1, fy + i + 1, color);
                graphics.fill(fx + half, fy + i, fx + half + 1, fy + i + 1, color);
            }
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
        var hit = level.clip(new ClipContext(
                eye, eye.add(dir.scale(48)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            int col = level.getBlockState(hit.getBlockPos()).getMapColor(level, hit.getBlockPos()).col;
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
        // panel rect center — pick the frame corner closest to it
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

    // endregion

    // region inspect presentation

    boolean inspectActive() {
        return inspecting;
    }

    /** The render entry the inspect screen delegates to. */
    void renderInspect(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // in inspect presentation the real cursor points; hover → focus (WD2 style)
        // — a live drag keeps the source panel focused instead of chasing the cursor
        if (!(dragInspectHosted && dragSession != null)) {
            pointed = panelOf(scene.hitTest(mouseX, mouseY));
            if (pointed != null) focus(pointed);
        }
        renderScreenSpace(graphics, mouseX, mouseY, partialTick);
        renderDragOverlay(graphics);
    }

    void onInspectScreenRemoved() {
        inspecting = false;
        inspectScreen = null;
        if (dragInspectHosted) cancelWorldDrag();
        // closed by ESC/another screen while the key is still held — stay out until released
        if (inspectHeld()) inspectDismissed = true;
    }

    /** Raw poll of the inspect keybind — works while the capture screen owns input. */
    public boolean inspectHeld() {
        KeyMapping key = NimbusKeyMappings.inspect;
        InputConstants.Key bound = key.key;
        long window = mc.getWindow().getWindow();
        return switch (bound.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> key.isDown();
        };
    }

    // inspect-mode input — forwarded by InworldInspectScreen
    void inspectMouseMoved(double x, double y) {
        if (tracing != null) {
            traceMouseMoved(x, y);
            return;
        }
        if (dragInspectHosted && dragSession != null) {
            dragCursorX = x;
            dragCursorY = y;
            return; // a live drag owns the cursor — the world is the target surface
        }
        scene.mouseMoved(x, y);
    }

    boolean inspectMouseClicked(double x, double y, int button) {
        if (tracing != null) {
            endTrace(true); // click mid-trace commits, same as releasing V
            return true;
        }
        if (dragInspectHosted && dragSession != null) {
            return true; // a live drag owns the cursor — extra presses do nothing
        }
        // world-as-UI: a press on a draggable widget starts a world-targeted
        // drag — the cursor ray picks containers through the frozen camera
        Widget hit = scene.hitTest(x, y);
        WorldDraggable src = WorldDraggable.find(hit);
        if (src != null) {
            PanelRuntime srcPanel = panelOf((Widget) src);
            if (srcPanel != null) {
                WorldDrag drag =
                        src.beginWorldDrag(new InworldPanelContext(mc.level, mc.player, srcPanel), x, y, button);
                if (drag != null) {
                    dragSession = drag;
                    dragPanel = srcPanel;
                    dragInspectHosted = true;
                    dragCursorX = x;
                    dragCursorY = y;
                    dragTrail.clear();
                    dragTarget = null;
                    return true;
                }
            }
        }
        boolean consumed = scene.mouseClicked(x, y, button);
        PanelRuntime panel = panelOf(hit);
        if (panel != null) focus(panel);
        return consumed;
    }

    boolean inspectMouseReleased(double x, double y, int button) {
        if (dragInspectHosted && dragSession != null) {
            commitWorldDrag(panelOf(scene.hitTest(x, y)) != null);
            dragInspectHosted = false;
            return true;
        }
        return scene.mouseReleased(x, y, button);
    }

    boolean inspectMouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragInspectHosted && dragSession != null) {
            dragCursorX = x;
            dragCursorY = y;
            return true;
        }
        return scene.mouseDragged(x, y, button, dx, dy);
    }

    boolean inspectMouseScrolled(double x, double y, double sx, double sy) {
        return scene.mouseScrolled(x, y, sx, sy);
    }

    boolean inspectKeyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (NimbusKeyMappings.interact.isActiveAndMatches(key)) {
            triggerInteract();
            return true;
        }
        if (NimbusKeyMappings.focusNext.isActiveAndMatches(key)) {
            focusStep(1);
            return true;
        }
        if (NimbusKeyMappings.focusPrevious.isActiveAndMatches(key)) {
            focusStep(-1);
            return true;
        }
        return scene.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean inspectKeyReleased(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (tracing != null && NimbusKeyMappings.interact.isActiveAndMatches(key)) {
            endTrace(true);
            return true;
        }
        return scene.keyReleased(keyCode, scanCode, modifiers);
    }

    boolean inspectCharTyped(char codePoint, int modifiers) {
        return scene.charTyped(codePoint, modifiers);
    }

    // endregion
}
