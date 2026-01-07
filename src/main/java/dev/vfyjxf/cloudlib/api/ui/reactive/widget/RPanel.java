package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A panel widget with background, border, and optional title.
 * <p>
 * RPanel acts as a container and properly broadcasts lifecycle events
 * to its content widget.
 */
public class RPanel extends RWidget {

    private RWidget content;
    private String title;

    private int backgroundColor = 0xC0101010;
    private int borderColor = 0xFF333333;
    private int titleColor = 0xFFFFFFFF;
    private int padding = 8;

    public RPanel() {}

    public RPanel(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setContent(RWidget content) {
        if (this.content != null) {
            this.content.setParent(null);
            this.content.onDetach();
        }
        this.content = content;
        if (content != null) {
            content.setParent(this);
            content.onAttach();
        }
    }

    public RWidget getContent() {
        return content;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
    }

    public void setTitleColor(int color) {
        this.titleColor = color;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Draw background
        graphics.fill(x, y, x + width, y + height, backgroundColor);

        // Draw border
        graphics.fill(x, y, x + width, y + 1, borderColor); // top
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor); // bottom
        graphics.fill(x, y, x + 1, y + height, borderColor); // left
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor); // right

        int contentY = y + padding;

        // Draw title if present
        if (title != null && !title.isEmpty()) {
            graphics.drawString(font, title, x + padding, contentY, titleColor);
            contentY += font.lineHeight + 4;

            // Draw title separator
            graphics.fill(x + padding, contentY - 2, x + width - padding, contentY - 1, borderColor);
        }

        // Render content
        if (content != null) {
            content.setPos(x + padding, contentY);
            content.setSize(width - padding * 2, height - (contentY - y) - padding);

            if (content instanceof RGroup group) {
                group.layout(font);
            }

            content.render(graphics, font, mouseX, mouseY, delta);
        }
    }

    @Override
    public int[] measure(Font font) {
        if (content != null) {
            int[] contentSize = content.measure(font);
            int titleHeight = (title != null && !title.isEmpty()) ? font.lineHeight + 4 : 0;

            return new int[]{
                    Math.max(width, contentSize[0] + padding * 2),
                    Math.max(height, contentSize[1] + padding * 2 + titleHeight)
            };
        }
        return new int[]{width, height};
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !contains(mouseX, mouseY)) return false;

        if (content != null && content.contains(mouseX, mouseY)) {
            return content.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        
        if (content != null) {
            content.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible || !contains(mouseX, mouseY)) return false;

        if (content != null && content.contains(mouseX, mouseY)) {
            return content.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void update() {
        super.update();
        if (content != null) {
            content.update();
        }
    }

    @Override
    public void onAttach() {
        super.onAttach();
        if (content != null) {
            content.onAttach();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (content != null) {
            content.onDetach();
        }
    }
    
    @Override
    protected void broadcastToChildren(RUIContext ctx, BroadcastHandler handler) {
        if (content != null) {
            handler.handle(content, ctx);
        }
    }
}
