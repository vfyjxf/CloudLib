package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * One container slot: the vanilla-looking bevelled cell
 * ({@code generic_54.png}-matched) with the item icon and its count / durability
 * decorations on top; an empty cell draws the slot bed only.
 * <p>
 * The cell is a part in the structural sense — its owner positions it, taffy
 * never does — but it is addressed by the {@code .slot} class rather than by
 * {@code ::part(name)}: a repeated cell has a role, not a name. It carries the
 * class the base sheet paints ({@code --slot} / {@code --slot-dark}), so a
 * theme restyles the bed: a themed background wins, the vanilla bevel is the
 * code fallback. It stays hit-testable so {@code .slot:hover} works.
 * <p>
 * Two switches let an owner shed that framing and pick the icon path:
 * <ul>
 *   <li>{@link #slotBacked(boolean)} — off drops the {@code .slot} class, so no
 *       bed, no hover wash, only the icon on the panel behind it. The cell keeps
 *       its 18×18 stride and its decorations, so a bare icon line still lines up
 *       with a slot-backed one;</li>
 *   <li>{@link #iconMode(IconRenderMode)} — {@code flat} blits the atlas sprite,
 *       {@code model} forwards the vanilla item renderer so blocks and shaped
 *       items keep their 3D model.</li>
 * </ul>
 * Shared by {@link SlotGridWidget} (row-major cells of a grid) and
 * {@link IconRowWidget} (one row of icons) — both place cells by hand, so both
 * get the same bed, hover wash, icon path and decorations from here.
 */
class SlotCellWidget extends WidgetPart {

    /** The class every slot-backed cell carries — the {@code .slot} selector of the base sheet. */
    static final String slotClass = "slot";

    /** {@code hover-overlay} — the wash the oreui sheets lay over a hovered slot. */
    static final String propHoverOverlay = "hover-overlay";

    /** The vanilla slot cell — 18×18 container cells, pixel-matched. */
    static final int cell = 18;

    /** The icon's inset inside the cell — the 16×16 sprite centred in the 18×18 stride. */
    static final int iconInset = 1;

    private ItemStack stack = ItemStack.EMPTY;
    private boolean slotBacked = true;
    private IconRenderMode iconMode = IconRenderMode.flat;

    SlotCellWidget(Widget owner, Supplier<Rect> bounds, boolean interactive) {
        super(owner, null, bounds, null);
        addStyleClass(slotClass);
        setInteractive(interactive);
    }

    ItemStack stack() {
        return stack;
    }

    void setStack(ItemStack stack) {
        this.stack = stack;
    }

    // region configuration

    /** Whether the cell paints the slot bed under its icon. */
    boolean slotBacked() {
        return slotBacked;
    }

    /**
     * Turns the {@code .slot} bed and its hover wash on or off. Off is a bare icon
     * cell: the class leaves the selector surface alongside the bed.
     */
    SlotCellWidget slotBacked(boolean backed) {
        if (this.slotBacked != backed) {
            this.slotBacked = backed;
            if (backed) {
                addStyleClass(slotClass);
            } else {
                removeStyleClass(slotClass);
            }
        }
        return this;
    }

    /** How the icon draws — the flat atlas sprite or the vanilla 3D model. */
    IconRenderMode iconMode() {
        return iconMode;
    }

    SlotCellWidget iconMode(IconRenderMode mode) {
        this.iconMode = mode;
        return this;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        if (slotBacked) {
            VisualTexture themed = style().visualContext().background();
            if (themed != null && !themed.isEmpty()) {
                canvas.texture(themed, 0, 0, width(), height());
            } else {
                bed(canvas);
            }
            VisualTexture overlay = hoverOverlay();
            if (overlay != null && !overlay.isEmpty()) {
                canvas.texture(overlay, 0, 0, width(), height());
            }
        }
        if (stack.isEmpty()) {
            return;
        }
        if (iconMode == IconRenderMode.model) {
            canvas.renderItem(stack, iconInset, iconInset);
        } else {
            canvas.renderItemIcon(stack, iconInset, iconInset);
        }
        canvas.renderItemDecorations(stack, iconInset, iconInset);
    }

    /**
     * The wash a theme lays over the slot bed while the pointer is on it —
     * {@code hover-overlay}, the oreui sheets' slot hover. Null when the pointer
     * is elsewhere or the sheet declares none.
     */
    @Nullable
    VisualTexture hoverOverlay() {
        if (!hovered()) {
            return null;
        }
        return style().visualContext().getProperty(propHoverOverlay, VisualTexture.class);
    }

    /**
     * The classic container slot: 8B8B8B face, 373737 top/left inset,
     * FFFFFF bottom/right bevel — five fills, pixel-matched to vanilla.
     */
    private static void bed(SceneCanvas canvas) {
        canvas.fill(0, 0, 18, 18, 0xFF8B8B8B);
        canvas.fill(0, 0, 17, 1, 0xFF373737);
        canvas.fill(0, 0, 1, 17, 0xFF373737);
        canvas.fill(17, 1, 1, 17, 0xFFFFFFFF);
        canvas.fill(1, 17, 17, 1, 0xFFFFFFFF);
    }

    // endregion
}
