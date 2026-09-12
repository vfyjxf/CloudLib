package dev.vfyjxf.nimbusprojection.feature.container;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDragAcceptor;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.cloudlib.ui.sync.ContainerContents;
import dev.vfyjxf.cloudlib.ui.widget.ContainerGridWidget;
import dev.vfyjxf.nimbusprojection.network.ContainerOpsPayload;
import dev.vfyjxf.nimbusprojection.network.TransferPayload;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adaptive container content: a compact summary strip while the Face panel
 * is dormant (fill ratio + top items, Jade-style), the full interactive
 * {@link ContainerGridWidget} once engaged into the hologram.
 */
public final class ContainerPanelWidget extends WidgetGroup<Widget> implements WorldDragAcceptor {

    private static final int cell = 12;
    private static final int topItems = 4;

    private final InworldPanelContext ctx;
    private final Supplier<BlockPos> pos;
    private final ContainerGridWidget grid;

    private final Widget summary = new Widget() {
        {
            onMount((scene, context, handle) -> scene.layoutTree()
                    .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(topItems * cell + 48, cell + 6)));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            List<ItemStack> stacks = ContainerContents.watch(pos.get());
            int x = 2;
            if (stacks == null) {
                canvas.text("···", x, 3, HackerTheme.textDim);
                return;
            }
            int shown = 0;
            for (ItemStack stack : stacks) {
                if (stack.isEmpty() || shown >= topItems) continue;
                canvas.renderItem(stack, x, 0);
                x += cell;
                shown++;
            }
            int used = 0;
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) used++;
            }
            String fill = stacks.isEmpty() ? "empty" : used + "/" + stacks.size();
            canvas.text(fill, x + 4, 4, HackerTheme.textDim);
        }
    };

    public ContainerPanelWidget(InworldPanelContext ctx, Supplier<BlockPos> pos) {
        this.ctx = ctx;
        this.pos = pos;
        this.grid = new ContainerGridWidget(pos, (slot, button, input) -> {
            BlockPos p = pos.get();
            if (p == null) return;
            boolean shift = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            int op = input.isLeftClick() && shift ? ContainerOpsPayload.extractAll : ContainerOpsPayload.extract;
            int count = input.isRightClick() ? 1 : -1;
            if (ctx.channel() != null) {
                ctx.channel().sendToServer(new ContainerOpsPayload(op, p, slot, count));
            }
        });
        addWidget(summary);
        addWidget(grid);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        boolean engaged = ctx.panel().engaged();
        summary.setVisible(!engaged);
        grid.setVisible(engaged);
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
    }

    /**
     * A stack dropped anywhere on this panel is deposited into the
     * container — no slot precision needed (destSlot -1 = first fitting).
     */
    @Override
    public boolean acceptWorldDrag(WorldDrag drag, InworldPanelContext dropCtx, double sceneX, double sceneY) {
        BlockPos p = pos.get();
        if (p == null || ctx.channel() == null) return false;
        ctx.channel()
                .sendToServer(new TransferPayload(
                        drag.sourceContainer(),
                        drag.sourceSlot(),
                        p,
                        -1,
                        drag.carried().getCount()));
        return true;
    }
}
