package dev.vfyjxf.inworldui.demo;

import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldSink;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import dev.vfyjxf.inworldui.internal.InworldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

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
                .hints("R:inspect+drag", "RMB:one"));
    }

    private BlockPos lookingAtContainer(InworldContext context) {
        HitResult hit = context.player().pick(6.0, 0, false);
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return null;
        BlockPos pos = bhr.getBlockPos();
        return context.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null ? pos : null;
    }
}
