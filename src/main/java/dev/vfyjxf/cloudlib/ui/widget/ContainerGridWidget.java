package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.data.lang.CloudLang;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.cloudlib.ui.sync.ContainerContents;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A flat grid bound to a container block's contents — the "world is UI"
 * counterpart of {@link ItemGridWidget}.
 * <p>
 * Vanilla inventories are server-only: the client-side block entity exposes
 * the capability shape but its slots are always empty, so this widget reads
 * {@link ContainerContents} — a client-side mirror filled by
 * {@code ContainerQueryPayload}/{@code ContainerContentsPayload}. While a
 * snapshot is missing it shows a "syncing" hint; an all-empty snapshot shows
 * "empty". Drags out of the grid carry {@code sourceContainer = pos}; the
 * server re-reads the real handler at commit time, so a slightly stale
 * snapshot can only ever cost a ghost preview, never items.
 */
public final class ContainerGridWidget extends Widget implements WorldDraggable {

    /**
     * Quick-action hook for a slot click — wired by the owning feature
     * (e.g. the container panel sends a server-validated op). The widget
     * stays dumb: it reports slot + button, the server decides what happens.
     */
    @FunctionalInterface
    public interface SlotAction {
        void onSlot(int slot, int button, InputContext input);
    }

    private static final int CELL = 18;
    private static final int COLS = 9;
    private static final int MAX_ROWS = 6;
    private static final int SLOT_BG = 0x33121F2B;
    private static final int SLOT_BG_HOT = 0x5536C4D8;

    private final Supplier<BlockPos> pos;
    private BlockPos lastPos;
    private int lastSlotCount;

    public ContainerGridWidget(Supplier<BlockPos> pos) {
        this(pos, null);
    }

    public ContainerGridWidget(Supplier<BlockPos> pos, @Nullable SlotAction action) {
        this.pos = pos;
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) ->
                        new FloatSize(COLS * CELL, rows() * CELL + 2)));
        if (action != null) {
            onMouseClicked((input, context) -> {
                int slot = slotAt(input.mouseX(), input.mouseY());
                if (slot < 0) return EventDispatch.pass;
                action.onSlot(slot, input.key().getValue(), input);
                return EventDispatch.consumed;
            });
        }
    }

    /** Latest server snapshot for the anchor (also keeps the pos subscribed). */
    private @Nullable List<ItemStack> stacks() {
        BlockPos p = pos.get();
        return p != null ? ContainerContents.watch(p) : null;
    }

    private int slotCount() {
        BlockPos p = pos.get();
        return p != null ? ContainerContents.slotsOf(p, COLS * 3) : COLS * 3;
    }

    private int rows() {
        return Math.max(1, Math.min(MAX_ROWS, (slotCount() + COLS - 1) / COLS));
    }

    /** Handler slot index under scene coords, or -1 off-grid/out of range. */
    private int slotAt(double sceneX, double sceneY) {
        FloatPos local = sceneToLocal(sceneX, sceneY);
        int cx = (int) Math.floor(local.x() / CELL);
        int cy = (int) Math.floor(local.y() / CELL);
        if (cx < 0 || cx >= COLS || cy < 0 || cy >= rows()) return -1;
        int slot = cy * COLS + cx;
        return slot < slotCount() ? slot : -1;
    }

    @Override
    public @Nullable WorldDrag beginWorldDrag(InworldPanelContext ctx, double sceneX, double sceneY, int button) {
        int slot = slotAt(sceneX, sceneY);
        if (slot < 0) return null;
        BlockPos p = pos.get();
        if (p == null) return null;
        ItemStack stack = ContainerContents.stackAt(p, slot);
        if (stack.isEmpty()) return null;
        ItemStack carried = button == 0 ? stack.copy() : stack.copyWithCount(1);
        return new WorldDrag(carried, slot, button, p);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        //the panel outlives its anchor — re-measure when the container moves
        //or the first snapshot lands with a different slot count
        BlockPos p = pos.get();
        int sc = slotCount();
        if (!java.util.Objects.equals(p, lastPos) || sc != lastSlotCount) {
            lastPos = p;
            lastSlotCount = sc;
            if (lifecycle().mounted()) scene().layoutTree().markDirty(nodeId());
        }
        List<ItemStack> stacks = stacks();
        if (p == null) {
            canvas.text(CloudLang.Ui.noContainer.string(), 4, 4, 0x5536C4D8);
            return;
        }
        if (stacks == null) {
            canvas.text(CloudLang.Ui.syncing.string(), 4, 4, 0x5536C4D8);
            return;
        }
        int rows = rows();
        boolean any = false;
        int slots = Math.min(stacks.size(), COLS * MAX_ROWS);
        for (int i = 0; i < slots; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = col * CELL;
            int y = row * CELL;
            ItemStack stack = stacks.get(i);
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            canvas.fill(x, y, CELL - 1, CELL - 1, hover ? SLOT_BG_HOT : SLOT_BG);
            if (!stack.isEmpty()) {
                any = true;
                canvas.renderItem(stack, x, y);
                canvas.renderItemDecorations(stack, x, y);
            }
        }
        if (!any) {
            String label = CloudLang.Ui.empty.string();
            canvas.text(label, COLS * CELL / 2 - canvas.font().width(label) / 2,
                    rows * CELL / 2 - 4, 0x5536C4D8);
        }
    }
}
