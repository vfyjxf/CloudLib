package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldSink;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.cloudlib.ui.widget.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Demo provider for {@link WaypointBlock}: a long-range floating marker with a
 * live distance readout — the content widget ticks client-side and recomputes
 * the distance every frame. No block entity involved; the marker is pure UI.
 */
public final class WaypointPanelProvider implements InworldProvider {

    private static final int RANGE = 12;
    private static final double MAX_DISTANCE = 96;

    @Override
    public void provide(InworldContext context, InworldSink sink) {
        var level = context.level();
        BlockPos center = context.player().blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-RANGE, -6, -RANGE),
                center.offset(RANGE, 6, RANGE))) {
            if (!(level.getBlockState(pos).getBlock() instanceof WaypointBlock)) continue;
            BlockPos p = pos.immutable();

            sink.offer(InworldPanelSpec
                    .of("wp/" + p,
                            InworldAnchor.of(p, new Vec3(0.5, 1.25, 0.5)),
                            InworldPlacement.dock(),
                            WaypointPanelProvider::content)
                    .title(Component.literal("WP//" + p.getX() + "," + p.getZ()))
                    .interactive(false)
                    .maxDistance(MAX_DISTANCE));
        }
    }

    private static Widget content(InworldPanelContext ctx) {
        var distance = TextWidget.of("◈ --").setColor(InworldTheme.ACCENT);
        distance.setTickable(true);
        distance.onTick(() -> {
            var player = Minecraft.getInstance().player;
            BlockPos pos = ctx.panel().blockPos();
            if (player == null || pos == null) return;
            int d = (int) Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(pos)));
            distance.setText("◈ " + d + "m");
        });
        return distance;
    }
}
