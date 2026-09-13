package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.cloudlib.data.lang.CloudLang;
import dev.vfyjxf.cloudlib.ui.sync.ContainerContents;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
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

    private static final int cell = 18;
    private static final int cols = 9;
    private static final int maxRows = 6;
    /** Fallback slot tints — themes override via the {@code slot} / {@code slot-hot} props. */
    private static final int slotBg = 0x40221B10;
    private static final int slotBgHot = 0x80D1904B;

    private final Supplier<BlockPos> pos;
    /**
     * External stacks source — when set, the widget reads this instead of
     * querying {@link ContainerContents} itself (the caller owns the sync
     * pipeline, e.g. a section cache). Null return = still syncing.
     */
    private final @Nullable Supplier<List<ItemStack>> stacksSource;
    /** Entity drag source — when set and {@code pos} is null, drags carry {@code sourceEntity}. */
    private final @Nullable Supplier<UUID> entitySource;

    private BlockPos lastPos;
    private int lastSlotCount;

    public ContainerGridWidget(Supplier<BlockPos> pos) {
        this(pos, null, null, null);
    }

    public ContainerGridWidget(Supplier<BlockPos> pos, @Nullable SlotAction action) {
        this(pos, null, null, action);
    }

    public ContainerGridWidget(
            Supplier<BlockPos> pos, @Nullable Supplier<List<ItemStack>> stacks, @Nullable SlotAction action) {
        this(pos, stacks, null, action);
    }

    public ContainerGridWidget(
            Supplier<BlockPos> pos,
            @Nullable Supplier<List<ItemStack>> stacks,
            @Nullable Supplier<UUID> entity,
            @Nullable SlotAction action) {
        this.pos = pos;
        this.stacksSource = stacks;
        this.entitySource = entity;
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(cols * cell, rows() * cell + 2)));
        if (action != null) {
            onMouseClicked((input, context) -> {
                int slot = slotAt(input.mouseX(), input.mouseY());
                if (slot < 0) return EventDispatch.pass;
                action.onSlot(slot, input.key().getValue(), input);
                return EventDispatch.consumed;
            });
        }
    }

    /** Latest stacks for the anchor — external source or the legacy {@link ContainerContents} poll. */
    private @Nullable List<ItemStack> stacks() {
        if (stacksSource != null) return stacksSource.get();
        BlockPos p = pos.get();
        return p != null ? ContainerContents.watch(p) : null;
    }

    private int slotCount() {
        if (stacksSource != null) {
            List<ItemStack> stacks = stacksSource.get();
            return stacks != null ? stacks.size() : cols * 3;
        }
        BlockPos p = pos.get();
        return p != null ? ContainerContents.slotsOf(p, cols * 3) : cols * 3;
    }

    private int rows() {
        return Math.max(1, Math.min(maxRows, (slotCount() + cols - 1) / cols));
    }

    /** Handler slot index under scene coords, or -1 off-grid/out of range. */
    private int slotAt(double sceneX, double sceneY) {
        FloatPos local = sceneToLocal(sceneX, sceneY);
        int cx = (int) Math.floor(local.x() / cell);
        int cy = (int) Math.floor(local.y() / cell);
        if (cx < 0 || cx >= cols || cy < 0 || cy >= rows()) return -1;
        int slot = cy * cols + cx;
        return slot < slotCount() ? slot : -1;
    }

    @Override
    public @Nullable WorldDrag beginWorldDrag(InworldPanelContext ctx, double sceneX, double sceneY, int button) {
        int slot = slotAt(sceneX, sceneY);
        if (slot < 0) return null;
        BlockPos p = pos.get();
        UUID entity = entitySource != null ? entitySource.get() : null;
        if (p == null && entity == null) return null;
        List<ItemStack> stacks = stacks();
        if (stacks == null || slot >= stacks.size()) return null;
        ItemStack stack = stacks.get(slot);
        if (stack.isEmpty()) return null;
        ItemStack carried = button == 0 ? stack.copy() : stack.copyWithCount(1);
        return p != null
                ? new WorldDrag(carried, slot, button, p)
                : WorldDrag.fromEntity(carried, slot, button, entity);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        // the panel outlives its anchor — re-measure when the container moves
        // or the first snapshot lands with a different slot count
        BlockPos p = pos.get();
        int sc = slotCount();
        if (!java.util.Objects.equals(p, lastPos) || sc != lastSlotCount) {
            lastPos = p;
            lastSlotCount = sc;
            if (lifecycle().mounted()) scene().layoutTree().markDirty(nodeId());
        }
        List<ItemStack> stacks = stacks();
        if (stacks == null) {
            // external source pending = syncing; a widget with no source at
            // all (no pos, no supplier) is the actual "no container" case
            String label = p == null && stacksSource == null
                    ? CloudLang.Ui.noContainer.string()
                    : CloudLang.Ui.syncing.string();
            Integer dim = style().get(Styles.textDim);
            canvas.text(label, 4, 4, dim != null ? dim : 0x80C07030);
            return;
        }
        int rows = rows();
        boolean any = false;
        int slots = Math.min(stacks.size(), cols * maxRows);
        for (int i = 0; i < slots; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = col * cell;
            int y = row * cell;
            ItemStack stack = stacks.get(i);
            boolean hover = mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell;
            Integer themed = style().get(hover ? Styles.slotHot : Styles.slot);
            canvas.fill(x, y, cell - 1, cell - 1, themed != null ? themed : (hover ? slotBgHot : slotBg));
            if (!stack.isEmpty()) {
                any = true;
                canvas.renderItemIcon(stack, x, y);
                canvas.renderItemDecorations(stack, x, y);
            }
        }
        if (!any) {
            String label = CloudLang.Ui.empty.string();
            Integer dim = style().get(Styles.textDim);
            canvas.text(label, cols * cell / 2 - canvas.font().width(label) / 2, rows * cell / 2 - 4, dim != null ? dim : 0x80C07030);
        }
    }
}
