package dev.vfyjxf.inworldui.demo;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * A flat grid bound to a container block's {@link IItemHandler} — the "world
 * is UI" counterpart of {@link ItemGridWidget}: it renders whatever the block
 * at the supplier's position currently holds (chest, barrel, hopper, any
 * item-handler capability) and supports dragging stacks <em>out</em> of it —
 * the resulting {@link WorldDrag} carries {@code sourceContainer = pos}, so
 * the server extracts from the container rather than the player inventory.
 * <p>
 * Layout mirrors vanilla: nine columns, as many rows as the handler has
 * slots (capped at {@link #MAX_ROWS} for absurd handlers). Both the position
 * and the handler resolve lazily — the host panel outlives any single anchor
 * (the provider re-anchors as the crosshair moves between containers) and a
 * captured capability would go stale the moment the block changes.
 */
public final class ContainerGridWidget extends Widget implements WorldDraggable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerGridWidget.class);
    private static final int CELL = 18;
    private static final int COLS = 9;
    private static final int MAX_ROWS = 6;
    private static final int SLOT_BG = 0x33121F2B;
    private static final int SLOT_BG_HOT = 0x5536C4D8;

    private final Supplier<BlockPos> pos;
    private BlockPos lastPos;
    private boolean loggedOnce;

    public ContainerGridWidget(Supplier<BlockPos> pos) {
        this.pos = pos;
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) ->
                        new FloatSize(COLS * CELL, rows() * CELL + 2)));
    }

    /** The live item handler at the current position, or null when gone. */
    private @Nullable IItemHandler handler() {
        BlockPos p = pos.get();
        Level level = Minecraft.getInstance().level;
        return p != null && level != null
                ? level.getCapability(Capabilities.ItemHandler.BLOCK, p, null)
                : null;
    }

    private int rows() {
        IItemHandler handler = handler();
        int slots = handler != null ? handler.getSlots() : COLS;
        return Math.max(1, Math.min(MAX_ROWS, (slots + COLS - 1) / COLS));
    }

    /** Handler slot index under scene coords, or -1 off-grid/out of range. */
    private int slotAt(double sceneX, double sceneY) {
        FloatPos local = sceneToLocal(sceneX, sceneY);
        int cx = (int) Math.floor(local.x() / CELL);
        int cy = (int) Math.floor(local.y() / CELL);
        if (cx < 0 || cx >= COLS || cy < 0 || cy >= rows()) return -1;
        int slot = cy * COLS + cx;
        IItemHandler handler = handler();
        return handler != null && slot < handler.getSlots() ? slot : -1;
    }

    @Override
    public @Nullable WorldDrag beginWorldDrag(InworldPanelContext ctx, double sceneX, double sceneY, int button) {
        int slot = slotAt(sceneX, sceneY);
        if (slot < 0) return null;
        IItemHandler handler = handler();
        BlockPos p = pos.get();
        if (handler == null || p == null) return null;
        ItemStack stack = handler.getStackInSlot(slot);
        if (stack.isEmpty()) return null;
        ItemStack carried = button == 0 ? stack.copy() : stack.copyWithCount(1);
        return new WorldDrag(carried, slot, button, p);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        //the panel outlives its anchor — re-measure when the container moves
        BlockPos p = pos.get();
        if (!java.util.Objects.equals(p, lastPos)) {
            lastPos = p;
            if (lifecycle().mounted()) scene().layoutTree().markDirty(nodeId());
        }
        IItemHandler handler = handler();
        if (handler == null) {
            if (!loggedOnce) {
                loggedOnce = true;
                LOGGER.info("container grid first render: pos={} handler=null", pos.get());
            }
            canvas.text("no container", 4, 4, 0x5536C4D8);
            return;
        }
        int slots = Math.min(handler.getSlots(), COLS * MAX_ROWS);
        int rows = rows();
        boolean any = false;
        if (!loggedOnce) {
            loggedOnce = true;
            int nonEmpty = 0;
            for (int i = 0; i < handler.getSlots(); i++)
                if (!handler.getStackInSlot(i).isEmpty()) nonEmpty++;
            LOGGER.info("container grid first render: pos={} slots={} nonEmpty={} bounds={}x{}",
                    pos.get(), handler.getSlots(), nonEmpty, width(), height());
        }
        for (int i = 0; i < slots; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = col * CELL;
            int y = row * CELL;
            ItemStack stack = handler.getStackInSlot(i);
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            canvas.fill(x, y, CELL - 1, CELL - 1, hover ? SLOT_BG_HOT : SLOT_BG);
            if (!stack.isEmpty()) {
                any = true;
                canvas.renderItem(stack, x, y);
                canvas.renderItemDecorations(stack, x, y);
            }
        }
        if (!any) {
            canvas.text("empty", COLS * CELL / 2 - canvas.font().width("empty") / 2,
                    rows * CELL / 2 - 4, 0x5536C4D8);
        }
    }
}
