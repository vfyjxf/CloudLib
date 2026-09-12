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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The "world is UI" demo: a player-inventory panel that materializes next to
 * whatever item container the crosshair rests on (chest, barrel, hopper, the
 * demo blocks — anything exposing an item-handler capability).
 * <p>
 * The panel anchors to the <em>container</em>, not the player — anchoring at
 * the player position would project right at the camera and the floating
 * placement could never resolve. The anchor is sticky: it only follows the
 * crosshair between drags, so the panel doesn't re-anchor mid-gesture and the
 * leader line stays readable while sweeping between containers.
 * <p>
 * Interaction: hold R for the cursor (inspect), press a slot and keep the
 * button held — the stack leaves the UI and the world becomes the drop
 * surface. Sweep across containers to gather a trail; release commits the
 * split (LMB = even spread, RMB = one each); release over air throws; release
 * back onto the panel cancels.
 */
public final class InventoryPanelProvider implements InworldProvider {

    /** The container the panel is anchored to — follows the crosshair while no drag is live. */
    private BlockPos anchorPos;

    @Override
    public void provide(InworldContext context, InworldSink sink) {
        InworldManager manager = InworldManager.instance();
        boolean dragging = manager != null && manager.dragActive();

        BlockPos target = lookingAtContainer(context);
        if (!dragging) {
            anchorPos = target; //follow the crosshair when idle; stick while dragging
        }
        if (anchorPos == null) return;

        sink.offer(InworldPanelSpec
                .of("player/inv",
                        InworldAnchor.of(anchorPos, new Vec3(0.5, 0.55, 0.5)),
                        InworldPlacement.floating(
                                FloatingPlacement.rightStart,
                                FloatingMiddlewares.offset(10),
                                FloatingMiddlewares.flip(),
                                FloatingMiddlewares.shift(4),
                                FloatingMiddlewares.hide()),
                        ctx -> new ItemGridWidget())
                .title(Component.literal("INV//LOCAL"))
                .hints("R:inspect+drag", "RMB:one"));
    }

    private BlockPos lookingAtContainer(InworldContext context) {
        HitResult hit = context.player().pick(6.0, 0, false);
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return null;
        BlockPos pos = bhr.getBlockPos();
        return context.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null ? pos : null;
    }
}
