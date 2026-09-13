package dev.vfyjxf.nimbusprojection.feature.inventory;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDragAcceptor;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.internal.NimbusPalette;
import dev.vfyjxf.nimbusprojection.network.TransferPayload;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The satellite inventory panel: the player's own inventory mirrored into
 * the in-world layer — main grid (vanilla slots 9-35), the armor column
 * (36-39) and offhand (40).
 * <p>
 * Both drag directions live here: dragging OUT starts a world drag whose
 * {@code sourceContainer = null} means "my inventory"; dropping a foreign
 * drag ON a slot commits a {@link TransferPayload} to that exact slot
 * (dest = null = player inventory). A click on a slot quick-inserts into
 * the linked container when one is engaged. The widget never mutates the
 * inventory itself — every move is a server-validated op.
 */
public final class InventoryPanelWidget extends Widget implements WorldDraggable, WorldDragAcceptor {

    /** Which slice of the inventory this panel shows. */
    public enum Section {
        /** 27 main slots (9-35) + armor (36-39) + offhand (40). */
        main,
        /** The 9 hotbar slots (0-8) alone — the drop-anything strip. */
        hotbar
    }

    private static final int cell = 18;
    private static final int cols = 9;
    private static final int rows = 3;
    private static final int slotBg = 0x33221B10;
    private static final int slotBgHot = 0x66F2C04D;

    private final Player player;
    private final Section section;
    /** The container this panel was summoned for — quick-insert target; null when standalone. */
    private final Supplier<@Nullable BlockPos> linked;

    public InventoryPanelWidget(Player player, Section section, Supplier<@Nullable BlockPos> linked) {
        this.player = player;
        this.section = section;
        this.linked = linked;
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(
                        nodeId(),
                        (style, space) -> section == Section.hotbar
                                ? new FloatSize(cols * cell, cell)
                                : new FloatSize(cols * cell, (rows + 1) * cell + 6)));
        // click a slot = quick-insert into the linked container (LMB stack,
        // RMB single); no link = no-op
        onMouseClicked((input, context) -> {
            int slot = slotAt(input.mouseX(), input.mouseY());
            BlockPos target = linked.get();
            if (slot < 0 || target == null) return EventDispatch.pass;
            int count = input.isRightClick() ? 1 : -1;
            sendTransfer(null, slot, SectionTarget.of(target), -1, count);
            return EventDispatch.consumed;
        });
    }

    /** Vanilla inventory index under scene coords, or -1. */
    private int slotAt(double sceneX, double sceneY) {
        FloatPos local = sceneToLocal(sceneX, sceneY);
        int cx = (int) Math.floor(local.x() / cell);
        int cy = (int) Math.floor(local.y() / cell);
        if (cx < 0 || cx >= cols) return -1;
        if (section == Section.hotbar) {
            return cy == 0 ? cx : -1; // hotbar: 0-8
        }
        if (cy < 0 || cy > rows) return -1;
        if (cy < rows) return 9 + cy * cols + cx; // main grid: 9-35
        // bottom row: armor 36-39 then offhand 40 at the far right
        return switch (cx) {
            case 0, 1, 2, 3 -> 36 + cx; // armor: feet..head
            case 8 -> 40; // offhand
            default -> -1;
        };
    }

    private void sendTransfer(
            @Nullable SectionTarget src, int srcSlot, @Nullable SectionTarget dst, int dstSlot, int count) {
        Inventory inv = player.getInventory();
        if (srcSlot < 0 || srcSlot >= inv.getContainerSize()) return;
        PacketDistributor.sendToServer(new TransferPayload(src, srcSlot, dst, dstSlot, count));
    }

    @Override
    public @Nullable WorldDrag beginWorldDrag(InworldPanelContext ctx, double sceneX, double sceneY, int button) {
        int slot = slotAt(sceneX, sceneY);
        if (slot < 0) return null;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) return null;
        // source = null → the server resolves the player's own inventory
        ItemStack carried = button == 0 ? stack.copy() : stack.copyWithCount(1);
        return new WorldDrag(carried, slot, button, null);
    }

    @Override
    public boolean acceptWorldDrag(WorldDrag drag, InworldPanelContext dropCtx, double sceneX, double sceneY) {
        int slot = slotAt(sceneX, sceneY);
        // dropping on the frame (not a slot) still counts: auto-insert main inventory
        sendTransfer(
                SectionTarget.of(drag.sourceContainer(), drag.sourceEntity()),
                drag.sourceSlot(),
                null,
                slot,
                drag.carried().getCount());
        return true;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        Inventory inv = player.getInventory();
        if (section == Section.hotbar) {
            for (int i = 0; i < cols; i++) {
                drawSlot(canvas, inv.getItem(i), i * cell, 0, mouseX, mouseY);
            }
            return;
        }
        // main grid
        for (int i = 0; i < rows * cols; i++) {
            int x = (i % cols) * cell;
            int y = (i / cols) * cell;
            drawSlot(canvas, inv.getItem(9 + i), x, y, mouseX, mouseY);
        }
        // armor + offhand row
        int ey = rows * cell + 6;
        for (int i = 0; i < 4; i++) {
            drawSlot(canvas, inv.getItem(36 + i), i * cell, ey, mouseX, mouseY);
        }
        drawSlot(canvas, inv.getItem(40), 8 * cell, ey, mouseX, mouseY);
        canvas.text("inv", 4 * cell + 8, ey + 5, NimbusPalette.dim(this));
    }

    private void drawSlot(SceneCanvas canvas, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell;
        canvas.fill(x, y, cell - 1, cell - 1, hover ? slotBgHot : slotBg);
        if (!stack.isEmpty()) {
            canvas.renderItemIcon(stack, x, y);
            canvas.renderItemDecorations(stack, x, y);
        }
    }
}
