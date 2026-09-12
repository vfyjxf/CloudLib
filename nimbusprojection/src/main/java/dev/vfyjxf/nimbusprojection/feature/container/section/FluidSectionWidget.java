package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.List;

/**
 * The tank section — one horizontal bar per tank: fluid-tinted fill,
 * hover name and {@code amount/capacity mB}. Display-only for now;
 * fluid moves need a different op than slot transfer.
 */
public final class FluidSectionWidget extends Widget {

    public static final SectionWidgetFactory<FluidSectionData> factory = FluidSectionWidget::new;

    private static final int rowHeight = 13;
    private static final int barBg = 0x33121F2B;
    private static final int barBorder = 0x5536C4D8;

    private final SectionView<FluidSectionData> view;

    private FluidSectionWidget(SectionView<FluidSectionData> view) {
        this.view = view;
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(9 * 18, rows() * rowHeight + 2)));
    }

    private int rows() {
        FluidSectionData data = view.data();
        return Math.max(1, data.tanks().size());
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        FluidSectionData live = view.live();
        if (live == null) {
            canvas.text("···", 4, 4, HackerTheme.textDim);
            return;
        }
        List<FluidSectionData.Tank> tanks = live.tanks();
        int width = 9 * 18;
        for (int i = 0; i < tanks.size(); i++) {
            FluidSectionData.Tank tank = tanks.get(i);
            int y = i * rowHeight;
            canvas.strokeRect(0, y, width, rowHeight - 2, barBorder);
            if (!tank.fluid().isEmpty() && tank.capacity() > 0) {
                int fill = Math.max(1, Math.round((width - 2) * (tank.fluid().getAmount() / (float) tank.capacity())));
                int tint =
                        IClientFluidTypeExtensions.of(tank.fluid().getFluid()).getTintColor(tank.fluid()) | 0xC0000000;
                canvas.fill(1, y + 1, fill, rowHeight - 4, tint);
                String label = tank.fluid().getHoverName().getString() + "  "
                        + tank.fluid().getAmount() + "/" + tank.capacity() + " mB";
                canvas.text(label, 4, y + 2, 0xFFFFFFFF);
            } else {
                canvas.text("empty  0/" + tank.capacity() + " mB", 4, y + 2, HackerTheme.textDim);
            }
        }
    }
}
