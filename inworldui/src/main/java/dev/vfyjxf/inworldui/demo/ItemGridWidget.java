package dev.vfyjxf.inworldui.demo;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemGridWidget.class);
    private static final int CELL = 18;
    private static final int COLS = 9;
    private static final int SLOT_BG = 0x330F1C24;
    /** main-inventory rows first, hotbar row last — same order as vanilla */
    private static final int ROWS = 4;

    /** The live client player — never cache the entity instance. */
    private static Inventory inventory() {
        Player player = Minecraft.getInstance().player;
        return player != null ? player.getInventory() : null;
    }

    public ItemGridWidget() {
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) ->
                        new FloatSize(COLS * CELL, ROWS * CELL + 2)));
    }

    /** Vanilla slot index under scene coords, or -1 off-grid. */
    private int slotAt(double sceneX, double sceneY) {
        var local = sceneToLocal(sceneX, sceneY);
        int cx = (int) local.x() / CELL;
        int cy = (int) local.y() / CELL;
        if (cx < 0 || cx >= COLS || cy < 0 || cy >= ROWS) return -1;
        return cy == ROWS - 1 ? cx : 9 + cy * COLS + cx;
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
        //renderInternal receives widget-local mouse coords already
        int localX = mouseX, localY = mouseY;
        boolean any = false;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = col * CELL;
                int y = row * CELL;
                int slot = row == ROWS - 1 ? col : 9 + row * COLS + col;
                ItemStack stack = inv.getItem(slot);
                boolean hover = localX >= x && localX < x + CELL && localY >= y && localY < y + CELL;
                canvas.fill(x, y, CELL - 1, CELL - 1,
                        hover ? 0x5536C4D8 : SLOT_BG);
                if (!stack.isEmpty()) {
                    any = true;
                    canvas.renderItem(stack, x, y);
                    canvas.renderItemDecorations(stack, x, y);
                }
            }
        }
        //all-empty grid would look like a render failure — say so explicitly
        if (!any) {
            canvas.text("empty", COLS * CELL / 2 - canvas.font().width("empty") / 2,
                    ROWS * CELL / 2 - 4, 0x5536C4D8);
        }
        if (!loggedOnce) {
            loggedOnce = true;
            int filled = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!inv.getItem(i).isEmpty()) filled++;
            }
            LOGGER.info("inv grid first render: {} non-empty of {} slots", filled, inv.getContainerSize());
        }
    }

    private boolean loggedOnce;
}
