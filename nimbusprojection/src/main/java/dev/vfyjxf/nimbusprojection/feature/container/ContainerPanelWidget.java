package dev.vfyjxf.nimbusprojection.feature.container;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDragAcceptor;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.nimbusprojection.internal.NimbusPalette;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.api.panel.PanelKeySink;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.feature.container.section.ItemSectionData;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import dev.vfyjxf.nimbusprojection.internal.section.SectionContents;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import dev.vfyjxf.nimbusprojection.internal.section.SectionWidgets;
import dev.vfyjxf.nimbusprojection.network.ContainerOpsPayload;
import dev.vfyjxf.nimbusprojection.network.TransferPayload;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adaptive container content: a compact summary strip while the Face
 * panel is dormant (top items + fill ratio, Jade-style), and — once
 * engaged into the hologram — a vertical stack of every section the
 * block exposes (items, tanks, energy) built through the section SPI.
 * <p>
 * The widget keeps the position subscribed via {@link SectionContents#watch}
 * for its whole lifetime, so snapshots flow regardless of which layer is
 * currently visible.
 */
public final class ContainerPanelWidget extends WidgetGroup<Widget> implements WorldDragAcceptor, PanelKeySink {

    private static final int cell = 12;

    private final InworldPanelContext ctx;
    private final Supplier<BlockPos> pos;
    private final ColumnWidget sections;

    private final Widget summary;

    // built lazily in the ctor — the measure lambda captures `pos`, which a
    // field initializer can't touch before the ctor assigns it
    private Widget buildSummary() {
        return new Widget() {
            {
                onMount((scene, context, handle) -> scene.layoutTree()
                    .setMeasureFunc(
                            nodeId(),
                            (style, space) -> {
                                // measure to content, not capacity — an empty
                                // chest's strip is "0/27", not 9 ghost slots
                                var font = net.minecraft.client.Minecraft.getInstance().font;
                                int w = 4;
                                BlockPos p = pos.get();
                                List<ItemStack> stacks = p == null ? null : itemStacks(p);
                                if (stacks == null || stacks.isEmpty()) {
                                    w += font.width(stacks == null ? "···" : "0/0");
                                } else {
                                    int shown = 0;
                                    int used = 0;
                                    for (ItemStack s : stacks) {
                                        if (s.isEmpty()) continue;
                                        used++;
                                        if (shown < NimbusConfig.containerTopItems()) shown++;
                                    }
                                    w += shown * cell;
                                    w += 4 + font.width(used + "/" + stacks.size());
                                }
                                return new FloatSize(w, cell + 6);
                            }));
        }

        private int lastSignature = -1;

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            Integer themedDim = style().get(Styles.textDim);
            int dim = themedDim != null ? themedDim : NimbusPalette.textDim;
            BlockPos p = pos.get();
            int x = 2;
            if (p == null) {
                canvas.text("···", x, 3, dim);
                return;
            }
            List<ItemStack> stacks = itemStacks(p);
            if (stacks == null) {
                canvas.text("···", x, 3, dim);
                return;
            }
            // measure is data-driven — relayout when the fill signature changes
            int signature = stacks.size() * 31 + stacks.stream().mapToInt(s -> s.isEmpty() ? 0 : 1).sum();
            if (signature != lastSignature && lifecycle().mounted()) {
                lastSignature = signature;
                scene().layoutTree().markDirty(nodeId());
            }
            int shown = 0;
            int used = 0;
            int limit = NimbusConfig.containerTopItems();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) continue;
                used++;
                if (shown >= limit) continue;
                canvas.renderItemIcon(stack, x, 0);
                x += cell;
                shown++;
            }
            String fill = stacks.isEmpty() ? "empty" : used + "/" + stacks.size();
            canvas.text(fill, x + 4, 4, dim);
        }
        };
    }

    public ContainerPanelWidget(InworldPanelContext ctx, Supplier<BlockPos> pos) {
        this.ctx = ctx;
        this.pos = pos;
        this.summary = buildSummary();
        this.sections = ColumnWidget.create(2);
        BlockPos p = pos.get();
        if (p != null) {
            SectionTarget target = SectionTarget.of(p);
            for (SectionInstance<?> instance : SectionProviders.collectAll(ctx.level(), p)) {
                Widget widget = SectionWidgets.create(ctx, target, instance);
                if (widget != null) sections.addWidget(widget);
            }
        }
        addWidget(summary);
        addWidget(sections);
    }

    /** Item slots from the section mirror — null while the first snapshot is in flight. */
    private @Nullable List<ItemStack> itemStacks(BlockPos p) {
        return SectionContents.latest(SectionTarget.of(p), SectionProviders.idOf(SectionTypes.item, 0))
                        instanceof ItemSectionData data
                ? data.stacks()
                : null;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        BlockPos p = pos.get();
        if (p != null) SectionContents.watch(SectionTarget.of(p));
        boolean engaged = ctx.panel().engaged();
        // display:none, not just invisible — hidden children must leave the
        // layout entirely or the idle card measures itself as the full grid
        summary.setVisible(!engaged);
        summary.set(Styles.display, engaged ? TaffyDisplay.NONE : TaffyDisplay.FLEX);
        sections.setVisible(engaged);
        sections.set(Styles.display, engaged ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
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
                        SectionTarget.of(drag.sourceContainer(), drag.sourceEntity()),
                        drag.sourceSlot(),
                        SectionTarget.of(p),
                        -1,
                        drag.carried().getCount()));
        return true;
    }

    /**
     * Quick-store while the panel is engaged: {@code X} pushes the held
     * stack into the block, {@code Shift+X} dumps the whole main inventory.
     * ({@code Q} stays vanilla drop — overriding it would fire both paths.)
     * No-op when the target has no item section — signs and hives ignore it.
     */
    @Override
    public EventDispatch keyPressed(InputContext input) {
        BlockPos p = pos.get();
        if (!input.isKey(GLFW.GLFW_KEY_X) || p == null || ctx.channel() == null) return EventDispatch.pass;
        if (itemStacks(p) == null) return EventDispatch.pass;
        int op = input.isShiftDown() ? ContainerOpsPayload.insertAll : ContainerOpsPayload.insert;
        ctx.channel()
                .sendToServer(new ContainerOpsPayload(
                        SectionProviders.idOf(SectionTypes.item, 0), op, SectionTarget.of(p), -1, -1));
        return EventDispatch.consumed;
    }
}
