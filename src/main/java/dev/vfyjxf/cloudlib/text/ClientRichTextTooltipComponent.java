package dev.vfyjxf.cloudlib.text;

import dev.vfyjxf.cloudlib.api.text.RichTexts;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.LayoutConstraints;
import dev.vfyjxf.cloudlib.api.text.render.RenderOptions;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.tooltip.RichTextTooltipComponent;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * Client renderer for {@link RichTextTooltipComponent}: lays the document out at
 * its natural (max-content) width and renders it through the rich text pipeline.
 */
public final class ClientRichTextTooltipComponent implements ClientTooltipComponent {

    private final RichTextTooltipComponent component;
    private LaidOutText laidOut;

    public ClientRichTextTooltipComponent(RichTextTooltipComponent component) {
        this.component = component;
    }

    private LaidOutText laidOut() {
        if (laidOut == null) {
            laidOut = RichTexts.layout(component.text().root(), LayoutConstraints.unconstrained());
        }
        return laidOut;
    }

    @Override
    public int getWidth(Font font) {
        return (int) Math.ceil(laidOut().width());
    }

    @Override
    public int getHeight() {
        return (int) Math.ceil(laidOut().height());
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        SceneCanvas canvas = SceneCanvas.create(graphics);
        RenderOptions options = RenderOptions.DEFAULT
                .withMouse((float) ScreenUtil.getMouseX() - x, (float) ScreenUtil.getMouseY() - y);
        RichTexts.renderer().render(canvas, laidOut(), x, y, options);
    }
}
