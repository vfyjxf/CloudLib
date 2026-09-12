package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.cloudlib.data.lang.CloudLang;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A flat inventory grid bound to the player's own {@link Inventory} — the
 * "world is UI" drag source. Pressing a slot starts a {@link WorldDrag}
 * instead of a click: while the button is held the carried stack floats at
 * the world ray hit point and every container the view crosses joins the
 * commit trail.
 * <p>
 * Button semantics follow vanilla pickup: left carries the whole stack,
 * right carries a single item. Slot layout mirrors the vanilla inventory —
 * three rows of the main inventory (slots 9–35) over the hotbar (0–8).
 * <p>
 * The player is resolved lazily ({@link Minecraft#player}) rather than
 * captured — the panel can outlive the entity instance across respawns and
 * dimension changes, and a stale {@link Player} reference would render a
 * frozen (usually empty) inventory.
 */
public final class ItemGridWidget extends Widget implements WorldDraggable {

    private static final int cell = 18;
    private static final int cols = 9;
    private static final int slotBg = 0x330F1C24;
    /** main-inventory rows first, hotbar row last — same order as vanilla */
    private static final int rows = 4;

    /** The live client player — never cache the entity instance. */
    private static Inventory inventory() {
        Player player = Minecraft.getInstance().player;
        return player != null ? player.getInventory() : null;
    }

    public ItemGridWidget() {
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(cols * cell, rows * cell + 2)));
    }

    /** Vanilla slot index under scene coords, or -1 off-grid. */
    private int slotAt(double sceneX, double sceneY) {
        var local = sceneToLocal(sceneX, sceneY);
        int cx = (int) Math.floor(local.x() / cell);
        int cy = (int) Math.floor(local.y() / cell);
        if (cx < 0 || cx >= cols || cy < 0 || cy >= rows) return -1;
        return cy == rows - 1 ? cx : 9 + cy * cols + cx;
    }

    @Override
    public @Nullable WorldDrag beginWorldDrag(InworldPanelContext ctx, double sceneX, double sceneY, int button) {
        int slot = slotAt(sceneX, sceneY);
        if (slot < 0) return null;
        Inventory inv = inventory();
        if (inv == null) return null;
        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty()) return null;
        ItemStack carried = button == 0 ? stack.copy() : stack.copyWithCount(1);
        return new WorldDrag(carried, slot, button);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        Inventory inv = inventory();
        if (inv == null) return;
        // renderInternal receives widget-local mouse coords already
        int localX = mouseX, localY = mouseY;
        boolean any = false;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * cell;
                int y = row * cell;
                int slot = row == rows - 1 ? col : 9 + row * cols + col;
                ItemStack stack = inv.getItem(slot);
                boolean hover = localX >= x && localX < x + cell && localY >= y && localY < y + cell;
                canvas.fill(x, y, cell - 1, cell - 1, hover ? 0x66F2C04D : slotBg);
                if (!stack.isEmpty()) {
                    any = true;
                    canvas.renderItemIcon(stack, x, y);
                    canvas.renderItemDecorations(stack, x, y);
                }
            }
        }
        // all-empty grid would look like a render failure — say so explicitly
        if (!any) {
            String label = CloudLang.Ui.empty.string();
            canvas.text(label, cols * cell / 2 - canvas.font().width(label) / 2, rows * cell / 2 - 4, 0x66F2C04D);
        }
    }
}
