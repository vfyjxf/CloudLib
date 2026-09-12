package dev.vfyjxf.nimbusprojection.api;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.presentation.PresentationDriver;
import dev.vfyjxf.nimbusprojection.api.provider.PanelProvider;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderOptions;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceInfo;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelView;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * The client-side in-world UI runtime: declarative, anchor-bound CloudLib
 * panels rendered inside the 3D world (or flattened onto the screen while
 * inspecting).
 *
 * <h3>Provisioning</h3>
 * Panels come from three sources:
 * <ul>
 *   <li><b>providers</b> — re-evaluated on an interval; every panel the
 *       provider wants alive is re-offered each pass and reconciled by
 *       {@link PanelSpec#key()} (new keys create, missing keys close,
 *       surviving keys keep their widget state).</li>
 *   <li><b>imperative</b> — {@link #open(PanelSpec)} shows a caller-keyed
 *       panel until {@link #close(PanelKey)}.</li>
 *   <li><b>shared</b> — server-declared panels arrive over the network and
 *       are materialized through views registered with
 *       {@link #registerView} — every watching client sees the same UI.</li>
 * </ul>
 *
 * <h3>Interaction model</h3>
 * <ul>
 *   <li><b>soft focus</b> — the actionable panel nearest the look vector
 *       inside a cone is focused automatically; no pixel-perfect aim
 *       required. Per-panel override via {@code PanelSpec.focusPolicy}.</li>
 *   <li><b>engage toggle</b> — the interact key opens a focused dormant
 *       panel and closes an engaged one; holding it activates the pointer
 *       for clicks and drags.</li>
 *   <li><b>widget contracts</b> — content widgets implement
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable} or
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable} to opt
 *       into richer interactions.</li>
 *   <li><b>inspect</b> — while the inspect key is held every panel flattens
 *       to screen space under a free cursor, camera look captured.</li>
 * </ul>
 */
public interface NimbusClient {

    // region imperative panels

    /** Shows (or replaces) a panel. The spec key is the panel's identity. */
    InworldPanel open(PanelSpec spec);

    /** Closes the panel with the given key, if present. */
    void close(PanelKey key);

    // endregion

    // region providers

    /** Registers a provider evaluated every {@code intervalTicks} client ticks. */
    default void registerProvider(PanelProvider provider, int intervalTicks) {
        registerProvider(provider, intervalTicks, ProviderOptions.defaults());
    }

    default void registerProvider(PanelProvider provider) {
        registerProvider(provider, 10, ProviderOptions.defaults());
    }

    /**
     * Registers a provider with options — see {@link ProviderOptions} for
     * the shared-domain presence declaration.
     */
    void registerProvider(PanelProvider provider, int intervalTicks, ProviderOptions options);

    void unregisterProvider(PanelProvider provider);

    // endregion

    // region presentation drivers

    /**
     * Registers the solver for a {@code Presentation.type()} id — the open
     * extension point for custom presentation modes. Re-registering a type
     * replaces its driver.
     */
    void registerPresentation(PresentationDriver<?> driver);

    // endregion

    // region shared-panel views

    /**
     * Registers the client-side materializer for a server-declared shared
     * panel view. When the server shares a
     * {@link dev.vfyjxf.nimbusprojection.api.sync.SharedPanelSpec} naming
     * {@code view}, every watching client decodes the payload and asks
     * {@code factory} for the {@link PanelSpec} to present (keyed by the
     * shared key via {@code SharedViewContext.key()}).
     * <p>
     * {@code type} + {@code codec} are also registered as a channel type —
     * the spec's initial payload and later {@code SharedPanel.update}
     * payloads share that wire identity, so registering a view is all the
     * wiring a payload type needs.
     */
    <P extends CustomPacketPayload> void registerView(
            ResourceLocation view,
            CustomPacketPayload.Type<P> type,
            StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            SharedPanelView<P> factory);

    // endregion

    // region state

    Collection<? extends InworldPanel> panels();

    @Nullable
    InworldPanel panel(PanelKey key);

    /** The panel currently holding the in-world focus, if any. */
    @Nullable
    InworldPanel focused();

    /** Moves the panel focus to the next visible panel (keynav). */
    void focusNext();

    void focusPrevious();

    /** @return true while inspect presentation is active (key held). */
    boolean inspecting();

    /**
     * Screen-space rects (gui px) currently occupied by visible panels —
     * exposed so HUD mods / JEI-style exclusion zones can avoid overlapping
     * in-world UI.
     */
    List<Rect2i> exclusionAreas();

    /** The shared scene hosting every panel. */
    Scene scene();

    // endregion

    // region presence

    /**
     * Interaction presence of <em>remote</em> players — who is looking at,
     * engaged with, dragging on or tracing on which shared-visible panel.
     * Lets a client render ghost affordances of other players' operations.
     */
    Collection<PresenceInfo> presence();

    /** Presence entries for one panel key. */
    Collection<PresenceInfo> presence(PanelKey key);

    // endregion
}
