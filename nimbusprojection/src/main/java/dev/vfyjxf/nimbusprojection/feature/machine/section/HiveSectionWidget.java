package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.nimbusprojection.internal.NimbusPalette;
import dev.vfyjxf.taffy.geometry.FloatSize;

/** The hive face — bee occupancy and honey level as two labeled bars. */
public final class HiveSectionWidget extends Widget {

    public static final SectionWidgetFactory<HiveSectionData> factory =
            view -> NimbusConfig.machineHive() ? new HiveSectionWidget(view) : null;

    private static final int width = 9 * 18;
    private static final int barHeight = 8;
    private static final int height = barHeight * 2 + 4;
    private static final int barBg = 0x33221B10;
    private static final int barBorder = 0x66F2C04D;
    private static final int beeFill = 0xCC4CAF50;
    private static final int honeyFill = 0xCCFFC107;
    private static final int honeyMax = 5;

    private final SectionView<HiveSectionData> view;

    private HiveSectionWidget(SectionView<HiveSectionData> view) {
        this.view = view;
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(width, height)));
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        HiveSectionData live = view.live();
        if (live == null) {
            canvas.text("···", 4, 4, NimbusPalette.dim(this));
            return;
        }
        bar(canvas, 0, live.occupied(), live.max(), beeFill, live.occupied() + "/" + live.max() + " bees");
        bar(
                canvas,
                barHeight + 4,
                live.honeyLevel(),
                honeyMax,
                honeyFill,
                "honey " + live.honeyLevel() + "/" + honeyMax);
    }

    private void bar(SceneCanvas canvas, int y, int value, int max, int color, String label) {
        canvas.strokeRect(0, y, width, barHeight, barBorder);
        if (max > 0 && value > 0) {
            int w = Math.max(1, Math.round((width - 2) * (value / (float) max)));
            canvas.fill(1, y + 1, w, barHeight - 2, color);
        }
        canvas.text(label, 4, y, 0xFFFFFFFF);
    }
}
