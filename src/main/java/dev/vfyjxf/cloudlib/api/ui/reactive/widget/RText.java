package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.Style;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.function.Supplier;

/**
 * A reactive text widget that can display dynamic content.
 * <p>
 * Text widgets use the broadcast model for updates - when the parent
 * broadcasts an update, the text will refresh its content.
 */
public class RText extends RWidget {

    private final Supplier<String> textSupplier;
    private String cachedText;
    private boolean shadow = true;

    public RText(String text) {
        this(() -> text);
    }

    public RText(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;
        this.cachedText = textSupplier.get();
    }

    @Override
    public void update() {
        super.update(); // Fire update event and broadcast
        this.cachedText = textSupplier.get();
    }

    public String getText() {
        return cachedText;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible || cachedText == null || cachedText.isEmpty()) return;

        int color = getStyleColor(0xFFFFFFFF);

        // Apply bold styling if present
        String displayText = cachedText;
        if (style != null) {
            var bold = style.get(Style.Bold.class);
            var strikethrough = style.get(Style.Strikethrough.class);

            StringBuilder sb = new StringBuilder();
            if (bold != null && bold.enabled()) sb.append("§l");
            if (strikethrough != null && strikethrough.enabled()) sb.append("§m");
            if (sb.length() > 0) {
                displayText = sb + cachedText + "§r";
            }
        }

        if (shadow) {
            graphics.drawString(font, displayText, x, y, color);
        } else {
            graphics.drawString(font, displayText, x, y, color, false);
        }
    }

    @Override
    public int[] measure(Font font) {
        String text = textSupplier.get();
        return new int[]{font.width(text), font.lineHeight};
    }
}
