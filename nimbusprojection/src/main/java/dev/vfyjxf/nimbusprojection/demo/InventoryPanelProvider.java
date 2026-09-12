package dev.vfyjxf.nimbusprojection.demo;

import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldSink;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.ContainerGridWidget;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import dev.vfyjxf.cloudlib.ui.widget.ItemGridWidget;
import dev.vfyjxf.cloudlib.util.ContainerScan;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

/**
 * The "world is UI" demo: a combined inventory panel that materializes next
 * to whatever item container the crosshair rests on — the container's own
 * contents on top, the player's inventory below, like a vanilla chest screen
 * floated into the world.
 * <p>
 * The panel anchors to the <em>container</em>, not the player — anchoring at
 * the player position would project right at the camera and the floating
 * placement could never resolve. The anchor is sticky: it only follows the
 * crosshair between drags, so the panel doesn't re-anchor mid-gesture and the
 * leader line stays readable while sweeping between containers.
 * <p>
 * Both grids are drag sources: press a slot and keep the button held — the
 * stack leaves the UI and the world becomes the drop surface. Sweep across
 * containers to gather a trail; release commits the split (LMB = even spread,
 * RMB = one each); release over air throws; release back onto the panel
 * cancels. Dragging out of the container section extracts from the container
 * server-side; the player section reads the player's own inventory.
 */
public final class InventoryPanelProvider implements InworldProvider {

    private static final String KEY = "player/inv";
    private static final double REACH = 6.0;
    /** ~26° soft-aim cone: the crosshair only has to rest near the container —
     *  spans most of the manager's 30° soft-focus cone so the dormant panel
     *  exists wherever V could reach it. */
    private static final double CONE_COS_ENTER = Math.cos(Math.toRadians(26));
    /** ~34° hold cone: the current anchor survives a bit past the enter cone
     *  and past the soft-focus cone, so targeting doesn't flap at the edge. */
    private static final double CONE_COS_HOLD = Math.cos(Math.toRadians(34));

    /** The container the panel is anchored to — follows the crosshair while no drag is live. */
    private BlockPos anchorPos;

    @Override
    public void provide(InworldContext context, InworldSink sink) {
        InworldManager manager = InworldManager.instance();
        boolean dragging = manager != null && manager.dragActive();
        boolean engaged = manager != null && manager.engaged(KEY);

        BlockPos target = lookingAtContainer(context);
        if (!dragging && !engaged) {
            anchorPos = target; //follow the crosshair while idle; stick while dragging or engaged
        }
        if (anchorPos == null) return;

        Component title = anchorPos != null
                ? context.level().getBlockState(anchorPos).getBlock().getName()
                : Component.literal("CONTAINER");

        sink.offer(InworldPanelSpec
                .of(KEY,
                        InworldAnchor.of(anchorPos, new Vec3(0.5, 0.55, 0.5)),
                        InworldPlacement.floating(
                                FloatingPlacement.rightStart,
                                FloatingMiddlewares.offset(10),
                                FloatingMiddlewares.flip(),
                                FloatingMiddlewares.shift(4),
                                FloatingMiddlewares.hide()),
                        ctx -> {
                            var col = ColumnWidget.create(3);
                            col.addWidget(new ContainerGridWidget(() -> anchorPos));
                            col.addWidget(DividerWidget.horizontal());
                            col.addWidget(new ItemGridWidget());
                            return col;
                        })
                .title(title)
                .hints("V:open/close", "R:inspect+drag", "RMB:one"));
    }

    /**
     * Soft-aim container targeting: an exact block hit wins outright;
     * otherwise the nearest item-handling block inside a ~15° cone around
     * the look vector counts, so a chest merely <em>near</em> the crosshair
     * still engages — Watch-Dogs-style point-at-thing tolerance instead of
     * demanding pixel-perfect aim.
     */
    private @Nullable BlockPos lookingAtContainer(InworldContext context) {
        var player = context.player();
        var level = context.level();
        HitResult hit = player.pick(REACH, 0, false);
        if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) return pos;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        //cone-edge flicker guard: the current anchor holds inside a slightly
        //wider cone, so hovering at the boundary doesn't alternate target/null
        if (anchorPos != null
                && ContainerScan.holds(level, player, eye, look, anchorPos, REACH, CONE_COS_HOLD)) {
            return anchorPos;
        }
        return ContainerScan.nearest(level, player, eye, look, REACH, CONE_COS_ENTER);
    }
}
