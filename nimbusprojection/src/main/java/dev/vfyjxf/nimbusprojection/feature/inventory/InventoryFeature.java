package dev.vfyjxf.nimbusprojection.feature.inventory;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.nimbusprojection.api.Nimbus;
import dev.vfyjxf.nimbusprojection.api.NimbusClient;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the player's inventory panels in the in-world layer — two forms,
 * summoned and dismissed together:
 * <ul>
 *   <li><b>satellite</b> ({@code inventory/self}) — the full mirror: main
 *       grid + armor + offhand, floating near the linked container (or the
 *       player when summoned manually)</li>
 *   <li><b>hotbar strip</b> ({@code inventory/hotbar}) — the nine hotbar
 *       slots projected as a thin screen-anchored strip under the viewer —
 *       the quick drop target while dragging</li>
 * </ul>
 * Two summon modes: the inventory keybind toggles a manual pair that stays
 * until dismissed; engaging a container panel auto-summons the pair linked
 * to that container and dismissing it retracts them again. A manually
 * summoned pair ignores the auto-link lifecycle.
 */
public final class InventoryFeature {

    private static final PanelKey satellite = PanelKey.of("nimbusprojection", "inventory/self");
    private static final PanelKey hotbar = PanelKey.of("nimbusprojection", "inventory/hotbar");

    /** The container the satellite is currently linked to — read by the
     *  widget's quick-insert; swaps silently when another chest engages. */
    private static @Nullable BlockPos linked;
    /** True when the pair is up because of a container engage, not the keybind. */
    private static boolean autoSummoned;
    /** Manually dismissed during a live engage — the auto-link must not
     *  resurrect the pair until that engagement ends. */
    private static boolean dismissed;

    private InventoryFeature() {}

    /** Keybind toggle — a manual summon ignores the auto-link lifecycle. */
    public static void toggle() {
        NimbusClient client = Nimbus.client();
        if (client == null) return;
        if (client.panel(satellite) != null) {
            client.close(satellite);
            client.close(hotbar);
            autoSummoned = false;
            linked = null;
            dismissed = true; // a live container link won't reopen it
        } else {
            open(client, null);
            autoSummoned = false;
        }
    }

    /**
     * Per-tick link maintenance, driven from the manager's client tick:
     * an engaged {@code container/} panel claims the satellite; losing the
     * last engaged container dismisses an auto-summoned pair only.
     */
    public static void tick(InworldManager manager) {
        NimbusClient client = Nimbus.client();
        if (client == null) return;

        BlockPos engaged = engagedContainer(manager);
        boolean open = client.panel(satellite) != null;

        if (engaged != null) {
            linked = engaged;
            if (!open && !dismissed) {
                open(client, engaged);
                autoSummoned = true;
            }
        } else {
            dismissed = false; // no engagement — the veto lapses
            if (open && autoSummoned) {
                client.close(satellite);
                client.close(hotbar);
                autoSummoned = false;
                linked = null;
            }
        }
    }

    private static void open(NimbusClient client, @Nullable BlockPos link) {
        linked = link;
        client.open(PanelSpec.of(
                        satellite,
                        satelliteAnchor(link),
                        Presentation.floating(),
                        ctx -> new InventoryPanelWidget(ctx.player(), InventoryPanelWidget.Section.main, () -> linked))
                .title(Component.translatable("container.inventory"))
                .hints("LMB/RMB:insert"));
        // the hotbar strip: player-anchored follow — a stable projection just
        // under the viewer regardless of which container is engaged
        client.open(PanelSpec.of(
                        hotbar,
                        playerAnchor(0.9),
                        Presentation.follow(0, 0),
                        ctx -> new InventoryPanelWidget(
                                ctx.player(), InventoryPanelWidget.Section.hotbar, () -> linked))
                .title(Component.literal("hotbar"))
                .interactive(true));
    }

    private static InworldAnchor satelliteAnchor(@Nullable BlockPos link) {
        return link != null ? InworldAnchor.of(link, new Vec3(0.5, 1.3, 0.5)) : playerAnchor(0.5);
    }

    /** A tracked anchor hovering below the player's eye — unshareable by
     *  design (a personal panel is nobody else's business). */
    private static InworldAnchor playerAnchor(double drop) {
        return InworldAnchor.of(() -> {
            var mc = Minecraft.getInstance();
            return mc.player != null ? mc.player.getEyePosition().subtract(0, drop, 0) : Vec3.ZERO;
        });
    }

    /** The currently engaged container panel's anchor block, or null. */
    private static @Nullable BlockPos engagedContainer(InworldManager manager) {
        for (var panel : manager.panels()) {
            if (!panel.key().path().startsWith("container/") || !panel.engaged()) continue;
            BlockPos pos = panel.blockPos();
            if (pos != null) return pos;
        }
        return null;
    }
}
