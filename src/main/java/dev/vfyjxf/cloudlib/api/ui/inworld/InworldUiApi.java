package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * The in-world UI layer: declarative, anchor-bound CloudLib panels rendered
 * inside the 3D world (or flattened onto the screen in inspect mode).
 *
 * <h3>Presentation</h3>
 * Every panel is described by an {@link InworldPanelSpec} — an
 * {@link InworldAnchor}, an {@link InworldPlacement} (face / floating /
 * follow) and a content factory producing ordinary CloudLib widgets.
 * <ul>
 *   <li><b>world presentation</b> — default. Panels render per their
 *       placement and are pointed at with the crosshair.</li>
 *   <li><b>inspect presentation</b> — while the inspect key is held
 *       (default {@code R}), camera-look is captured by a transparent screen,
 *       all panels flatten to screen space and a free cursor interacts with
 *       them directly. Leader lines connect each panel back to its anchor.</li>
 * </ul>
 *
 * <h3>Interaction</h3>
 * <ul>
 *   <li><b>point</b> — crosshair pointing at a panel (face: real 3D ray
 *       against the panel plane; floating/follow: the panel under the
 *       crosshair, or the panel whose anchor block is looked at) plus mouse
 *       clicks, which are swallowed when a widget consumes them.</li>
 *   <li><b>inspect</b> — free mouse cursor while the inspect key is held.</li>
 *   <li><b>keynav</b> — the cycle-focus key walks the panel focus ring;
 *       keyboard input is routed to the focused panel.</li>
 * </ul>
 *
 * <h3>Sync</h3>
 * Panels read live block-entity state through {@code SyncedBlockEntity}
 * handles (server → client, batched per tick) and send user actions back
 * through {@code ReversedOnly} exposes (client → server). Both directions
 * ride on the {@link dev.vfyjxf.cloudlib.api.network.expose} machinery.
 *
 * <h3>Compatibility</h3>
 * Panels pause when a real {@code Screen} is open, when the game is paused,
 * or when the anchor leaves range / the camera; inspect mode is a transparent
 * non-pausing screen marked by {@code InworldOverlayScreen} so mods can
 * recognise it.
 */
public interface InworldUiApi {

    //region imperative panels

    /** Shows (or replaces) a panel. The spec key is the panel's identity. */
    InworldPanel show(InworldPanelSpec spec);

    /** Closes the panel with the given key, if present. */
    void close(Object key);

    //endregion

    //region providers

    /** Registers a provider evaluated every {@code intervalTicks} client ticks. */
    void registerProvider(InworldProvider provider, int intervalTicks);

    default void registerProvider(InworldProvider provider) {
        registerProvider(provider, 10);
    }

    void unregisterProvider(InworldProvider provider);

    //endregion

    //region state

    Collection<? extends InworldPanel> panels();

    @Nullable InworldPanel panel(Object key);

    /** The panel currently holding the in-world focus, if any. */
    @Nullable InworldPanel focused();

    void focus(@Nullable InworldPanel panel);

    /** Moves the panel focus to the next visible panel (keynav). */
    void focusNext();

    void focusPrevious();

    /** @return true while inspect presentation is active (key held). */
    boolean inspecting();

    /** @return true while the inspect key is physically held. */
    boolean inspectHeld();

    /**
     * Screen-space rects (gui px) currently occupied by visible panels —
     * exposed so HUD mods / JEI-style exclusion zones can avoid overlapping
     * in-world UI.
     */
    List<net.minecraft.client.renderer.Rect2i> exclusionAreas();

    /** The shared in-world scene hosting every panel. */
    Scene scene();

    //endregion
}
