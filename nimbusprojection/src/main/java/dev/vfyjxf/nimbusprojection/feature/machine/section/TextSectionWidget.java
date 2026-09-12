package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.network.chat.Component;

/**
 * The sign face — the written lines as plain rows, front first, back under
 * a separator when the sign has a written back side.
 */
public final class TextSectionWidget extends Widget {

    public static final SectionWidgetFactory<TextSectionData> factory =
            view -> NimbusConfig.machineText() ? new TextSectionWidget(view) : null;

    private static final int width = 9 * 18;
    private static final int rowHeight = 9;

    private final SectionView<TextSectionData> view;

    private TextSectionWidget(SectionView<TextSectionData> view) {
        this.view = view;
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(width, rows() * rowHeight + 2)));
    }

    private int rows() {
        TextSectionData data = view.data();
        int front = countNonBlank(data.front());
        int back = countNonBlank(data.back());
        return Math.max(1, front + (back > 0 ? back + 1 : 0));
    }

    private static int countNonBlank(java.util.List<Component> lines) {
        int n = 0;
        for (Component line : lines) {
            if (!line.getString().isBlank()) n++;
        }
        return n;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        TextSectionData data = view.data();
        int y = 2;
        boolean wrote = false;
        for (Component line : data.front()) {
            if (line.getString().isBlank()) continue;
            canvas.text(line, 2, y, HackerTheme.text);
            y += rowHeight;
            wrote = true;
        }
        int back = countNonBlank(data.back());
        if (back > 0) {
            canvas.text(wrote ? "— back —" : "back:", 2, y, HackerTheme.textDim);
            y += rowHeight;
            for (Component line : data.back()) {
                if (line.getString().isBlank()) continue;
                canvas.text(line, 2, y, HackerTheme.text);
                y += rowHeight;
            }
        }
        if (y == 2) canvas.text("(blank)", 2, y, HackerTheme.textDim);
    }
}
