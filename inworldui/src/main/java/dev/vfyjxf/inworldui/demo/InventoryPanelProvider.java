package dev.vfyjxf.inworldui.demo;

import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldSink;
import dev.vfyjxf.inworldui.internal.InworldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The "world is UI" demo: a player-inventory panel that materializes whenever
 * the crosshair rests on an item container (chest, barrel, hopper, the demo
 * blocks themselves — anything exposing an item-handler capability).
 * <p>
 * Press a slot in the panel and keep the button held: the stack leaves the UI
 * and the world becomes the drop surface. Sweep across containers to gather a
 * trail — release commits the split (LMB = even spread, RMB = one each);
 * release over air throws; release back onto the panel cancels.
 * <p>
 * While a drag session is live the panel keeps being offered regardless of
 * where the player looks, so the gesture never dies mid-flight.
 */
public final class InventoryPanelProvider implements InworldProvider {

    @Override
    public void provide(InworldContext context, InworldSink sink) {
        var player = context.player();
        InworldManager manager = InworldManager.instance();
        boolean dragging = manager != null && manager.dragActive();
        if (!dragging && !lookingAtContainer(context)) return;

        sink.offer(InworldPanelSpec
                .of("player/inv",
                        InworldAnchor.of(() -> player.position().add(0, 1.2, 0)),
                        InworldPlacement.floating(
                                FloatingPlacement.rightStart,
                                FloatingMiddlewares.offset(10),
                                FloatingMiddlewares.flip(),
                                FloatingMiddlewares.shift(4),
                                FloatingMiddlewares.hide()),
                        ctx -> new ItemGridWidget(player))
                .title(Component.literal("INV//LOCAL"))
                .hints("R:inspect+drag", "RMB:one"));
    }

    private boolean lookingAtContainer(InworldContext context) {
        HitResult hit = context.player().pick(6.0, 0, false);
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return false;
        BlockPos pos = bhr.getBlockPos();
        return context.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
    }
}
