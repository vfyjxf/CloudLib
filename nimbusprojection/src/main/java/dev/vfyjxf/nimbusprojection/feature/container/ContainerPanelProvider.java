package dev.vfyjxf.nimbusprojection.feature.container;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.cloudlib.util.ContainerScan;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.provider.PanelProvider;
import dev.vfyjxf.nimbusprojection.api.provider.PanelSink;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderContext;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;

/**
 * The container feature's provider: every {@code IItemHandler} block inside
 * the soft-focus cone gets an always-on Face strip keyed
 * {@code container/<x>_<y>_<z>} — a compact summary (top items + fill ratio)
 * that expands into the world hologram on the interact key.
 * <p>
 * Keys are position-stable and {@code sharedDomain} — two players looking at
 * the same chest share the panel identity, so presence (watching / engaged /
 * dragging) relays between them.
 */
public final class ContainerPanelProvider implements PanelProvider {

    /** ~31° cone covers the whole 30° soft-focus cone — a panel exists
     *  wherever the interact key could reach it. */
    private static final double coneCosEnter = Math.cos(Math.toRadians(31));

    @Override
    public void provide(ProviderContext context, PanelSink sink) {
        if (!NimbusConfig.containersEnabled()) return;
        Vec3 eye = context.player().getEyePosition();
        List<BlockPos> found = ContainerScan.all(
                context.level(),
                context.player(),
                eye,
                context.player().getLookAngle().normalize(),
                NimbusConfig.containerReach(),
                coneCosEnter,
                pos -> SectionProviders.hasAny(context.level(), pos));
        for (BlockPos pos : found) {
            // double chest: both halves report the same combined handler —
            // only the LEFT half gets a panel, or one chest shows two strips
            var state = context.level().getBlockState(pos);
            if (state.getBlock() instanceof AbstractChestBlock<?> && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT)
                continue;
            boolean items = context.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
            PanelSpec spec = PanelSpec.of(
                            keyOf(pos, items),
                            InworldAnchor.of(pos, new Vec3(0.5, 0.55, 0.5)),
                            Presentation.face(faceToward(eye, pos)),
                            ctx -> new ContainerPanelWidget(ctx, () -> pos))
                    .title(state.getBlock().getName())
                    // base info (summary strip) shows on sight as a screen
                    // card; V pins the panel to the world as the section
                    // hologram — the hint exists because there's an
                    // interactive form, not because the card needs a key
                    .onDemand(false)
                    .dockOnIdle(true);
            sink.offer(items ? spec.hints("V:expand", "X:store") : spec.hints("V:expand"));
        }
    }

    /**
     * Position-stable shared-domain key — same chest, same identity across
     * clients. Item-handling blocks key under {@code container/} (the
     * inventory satellite claims those); section-only blocks like signs or
     * hives key under {@code block/} — they share presence the same way but
     * never summon an inventory companion.
     */
    public static PanelKey keyOf(BlockPos pos, boolean items) {
        return PanelKey.of(
                "nimbusprojection",
                (items ? "container/" : "block/") + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
    }

    /** The block face most directly facing the viewer — re-picked each offer. */
    private static Direction faceToward(Vec3 eye, BlockPos pos) {
        Vec3 toEye = eye.subtract(Vec3.atCenterOf(pos));
        double ax = Math.abs(toEye.x), ay = Math.abs(toEye.y), az = Math.abs(toEye.z);
        if (ax >= ay && ax >= az) return toEye.x > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return toEye.y > 0 ? Direction.UP : Direction.DOWN;
        return toEye.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
