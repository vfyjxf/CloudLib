package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.taffy.geometry.FloatSize;

/**
 * The FE section — a single stored/capacity bar. Display-only; energy
 * isn't player-transferable.
 */
public final class EnergySectionWidget extends Widget {

    public static final SectionWidgetFactory<EnergySectionData> factory = EnergySectionWidget::new;

    private static final int height = 11;
    private static final int barBg = 0x33121F2B;
    private static final int barBorder = 0x5536C4D8;
    private static final int fill = 0xCC36C4D8;

    private final SectionView<EnergySectionData> view;

    private EnergySectionWidget(SectionView<EnergySectionData> view) {
        this.view = view;
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(9 * 18, height)));
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        EnergySectionData live = view.live();
        if (live == null) {
            canvas.text("···", 4, 3, HackerTheme.textDim);
            return;
        }
        int width = 9 * 18;
        canvas.strokeRect(0, 0, width, height, barBorder);
        if (live.capacity() > 0 && live.stored() > 0) {
            int w = Math.max(1, Math.round((width - 2) * (live.stored() / (float) live.capacity())));
            canvas.fill(1, 1, w, height - 2, fill);
        }
        String label = live.stored() + " / " + live.capacity() + " FE";
        canvas.text(label, 4, 2, 0xFFFFFFFF);
    }
}
