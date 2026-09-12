package dev.vfyjxf.nimbusprojection.feature.container;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.cloudlib.util.ContainerScan;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.provider.PanelProvider;
import dev.vfyjxf.nimbusprojection.api.provider.PanelSink;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;

/**
 * The container feature's provider: every {@code IItemHandler} block inside
 * the soft-focus cone gets an always-on Face strip keyed
 * {@code container/<x>,<y>,<z>} — a compact summary (top items + fill ratio)
 * that expands into the world hologram on the interact key.
 * <p>
 * Keys are position-stable and {@code sharedDomain} — two players looking at
 * the same chest share the panel identity, so presence (watching / engaged /
 * dragging) relays between them.
 */
public final class ContainerPanelProvider implements PanelProvider {

    private static final double REACH = 6.0;
    /** ~31° cone covers the whole 30° soft-focus cone — a panel exists
     *  wherever the interact key could reach it. */
    private static final double CONE_COS_ENTER = Math.cos(Math.toRadians(31));

    @Override
    public void provide(ProviderContext context, PanelSink sink) {
        Vec3 eye = context.player().getEyePosition();
        List<BlockPos> found = ContainerScan.all(
                context.level(), context.player(),
                eye, context.player().getLookAngle().normalize(),
                REACH, CONE_COS_ENTER);
        for (BlockPos pos : found) {
            if (context.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null) continue;
            sink.offer(PanelSpec
                    .of(keyOf(pos),
                            InworldAnchor.of(pos, new Vec3(0.5, 0.55, 0.5)),
                            Presentation.face(faceToward(eye, pos)),
                            ctx -> new ContainerPanelWidget(ctx, () -> pos))
                    .title(context.level().getBlockState(pos).getBlock().getName())
                    .hints("V:expand"));
        }
    }

    /** Position-stable shared-domain key — same chest, same identity across clients. */
    public static PanelKey keyOf(BlockPos pos) {
        return PanelKey.of("nimbusprojection",
                "container/" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
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
