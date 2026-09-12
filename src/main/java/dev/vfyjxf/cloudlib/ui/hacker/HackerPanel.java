package dev.vfyjxf.cloudlib.ui.hacker;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.flexColumn;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.padding;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.rowGap;

/**
 * A plain screen-space container carrying the in-world hacker chrome: dark
 * translucent fill, hairline border and an optional accent title strip.
 * Usable in normal screens too — it only draws visuals.
 */
public class HackerPanel extends WidgetGroup<Widget> {

    private @Nullable Component title;

    public HackerPanel() {
        this(null);
    }

    public HackerPanel(@Nullable Component title) {
        this.title = title;
        useStyle(UIStyle.of(
                flexColumn(),
                rowGap(3),
                padding(
                        title != null ? HackerTheme.titleHeight + 2 : HackerTheme.padding,
                        6,
                        HackerTheme.padding + 1,
                        6)));
    }

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    public HackerPanel setTitle(@Nullable Component title) {
        this.title = title;
        return this;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();
        canvas.fill(0, 0, w, h, HackerTheme.bg);
        canvas.strokeRect(0, 0, w, h, HackerTheme.border);
        if (title != null) {
            canvas.fill(3, 5, 3, 3, HackerTheme.accent);
            canvas.text(title, 9, 3, HackerTheme.text);
            canvas.fill(0, HackerTheme.titleHeight + 1, w, 1, HackerTheme.titleRule);
        }
    }
}
