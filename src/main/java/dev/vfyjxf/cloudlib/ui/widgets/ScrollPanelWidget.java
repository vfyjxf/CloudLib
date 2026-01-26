package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * A scrollable container widget.
 * <p>
 * Features:
 * <ul>
 *   <li>Vertical and horizontal scrolling</li>
 *   <li>Scroll bar with customizable appearance</li>
 *   <li>Mouse wheel support</li>
 *   <li>Content clipping</li>
 * </ul>
 */
public class ScrollPanelWidget extends CompositeWidget<Widget> {

    public enum ScrollDirection {
        VERTICAL,
        HORIZONTAL,
        BOTH
    }

    private ScrollDirection direction = ScrollDirection.VERTICAL;
    private int scrollX = 0;
    private int scrollY = 0;
    private int contentWidth = 0;
    private int contentHeight = 0;
    private int scrollSpeed = 10;
    private boolean showScrollBar = true;
    private int scrollBarWidth = 6;

    private VisualTexture scrollBarBackground = new ColorTexture(0x80000000);
    private VisualTexture scrollBarThumb = new ColorTexture(0xFFAAAAAA);
    private @Nullable VisualTexture backgroundTexture = null;

    public static ScrollPanelWidget create() {
        return new ScrollPanelWidget();
    }

    public static ScrollPanelWidget vertical() {
        return new ScrollPanelWidget().setDirection(ScrollDirection.VERTICAL);
    }

    public static ScrollPanelWidget horizontal() {
        return new ScrollPanelWidget().setDirection(ScrollDirection.HORIZONTAL);
    }

    private ScrollPanelWidget() {
        onMouseDragged((input, deltaX, deltaY, context) -> {
            // Handle scroll bar dragging
            return EventDispatch.pass;
        });
    }

    public ScrollDirection direction() {
        return direction;
    }

    public ScrollPanelWidget setDirection(ScrollDirection direction) {
        this.direction = direction;
        return this;
    }

    public int scrollX() {
        return scrollX;
    }

    public ScrollPanelWidget setScrollX(int scrollX) {
        this.scrollX = clampScrollX(scrollX);
        return this;
    }

    public int scrollY() {
        return scrollY;
    }

    public ScrollPanelWidget setScrollY(int scrollY) {
        this.scrollY = clampScrollY(scrollY);
        return this;
    }

    public ScrollPanelWidget scrollBy(int deltaX, int deltaY) {
        if (direction == ScrollDirection.VERTICAL || direction == ScrollDirection.BOTH) {
            this.scrollY = clampScrollY(this.scrollY + deltaY);
        }
        if (direction == ScrollDirection.HORIZONTAL || direction == ScrollDirection.BOTH) {
            this.scrollX = clampScrollX(this.scrollX + deltaX);
        }
        return this;
    }

    public int contentWidth() {
        return contentWidth;
    }

    public ScrollPanelWidget setContentWidth(int contentWidth) {
        this.contentWidth = contentWidth;
        return this;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public ScrollPanelWidget setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        return this;
    }

    public ScrollPanelWidget setContentSize(int width, int height) {
        this.contentWidth = width;
        this.contentHeight = height;
        return this;
    }

    public int scrollSpeed() {
        return scrollSpeed;
    }

    public ScrollPanelWidget setScrollSpeed(int scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
        return this;
    }

    public boolean showScrollBar() {
        return showScrollBar;
    }

    public ScrollPanelWidget setShowScrollBar(boolean showScrollBar) {
        this.showScrollBar = showScrollBar;
        return this;
    }

    public int scrollBarWidth() {
        return scrollBarWidth;
    }

    public ScrollPanelWidget setScrollBarWidth(int scrollBarWidth) {
        this.scrollBarWidth = scrollBarWidth;
        return this;
    }

    public ScrollPanelWidget setScrollBarTextures(VisualTexture background, VisualTexture thumb) {
        this.scrollBarBackground = background;
        this.scrollBarThumb = thumb;
        return this;
    }

    public ScrollPanelWidget setBackgroundTexture(@Nullable VisualTexture texture) {
        this.backgroundTexture = texture;
        return this;
    }

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    private int clampScrollX(int x) {
        int maxScroll = Math.max(0, contentWidth - width());
        return Math.clamp(x, 0, maxScroll);
    }

    private int clampScrollY(int y) {
        int maxScroll = Math.max(0, contentHeight - height());
        return Math.clamp(y, 0, maxScroll);
    }

    private boolean canScrollVertically() {
        return (direction == ScrollDirection.VERTICAL || direction == ScrollDirection.BOTH)
               && contentHeight > height();
    }

    private boolean canScrollHorizontally() {
        return (direction == ScrollDirection.HORIZONTAL || direction == ScrollDirection.BOTH)
               && contentWidth > width();
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Render background
        if (backgroundTexture != null) {
            backgroundTexture.render(graphics, 0, 0, width(), height());
        }

        // Enable scissor for content clipping
        int absX = absolutePos().x();
        int absY = absolutePos().y();
        graphics.enableScissor(absX, absY, absX + width(), absY + height());

        // Render children with scroll offset
        graphics.pose().pushPose();
        graphics.pose().translate(-scrollX, -scrollY, 0);

        for (Widget child : children()) {
            graphics.pose().pushPose();
            {
                graphics.pose().translate(child.pos().x(), child.pos().y(), 0);
                int relativeX = mouseX + scrollX - child.pos().x();
                int relativeY = mouseY + scrollY - child.pos().y();
                child.renderWidget(graphics, relativeX, relativeY, partialTicks);
            }
            graphics.pose().popPose();
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        // Render scroll bars
        if (showScrollBar) {
            renderScrollBars(graphics);
        }
    }

    private void renderScrollBars(GuiGraphics graphics) {
        // Vertical scroll bar
        if (canScrollVertically()) {
            int barX = width() - scrollBarWidth;
            int barHeight = height();
            int thumbHeight = Math.max(20, (int) ((float) height() / contentHeight * barHeight));
            int thumbY = (int) ((float) scrollY / (contentHeight - height()) * (barHeight - thumbHeight));

            scrollBarBackground.render(graphics, barX, 0, scrollBarWidth, barHeight);
            scrollBarThumb.render(graphics, barX, thumbY, scrollBarWidth, thumbHeight);
        }

        // Horizontal scroll bar
        if (canScrollHorizontally()) {
            int barY = height() - scrollBarWidth;
            int barWidth = width() - (canScrollVertically() ? scrollBarWidth : 0);
            int thumbWidth = Math.max(20, (int) ((float) width() / contentWidth * barWidth));
            int thumbX = (int) ((float) scrollX / (contentWidth - width()) * (barWidth - thumbWidth));

            scrollBarBackground.render(graphics, 0, barY, barWidth, scrollBarWidth);
            scrollBarThumb.render(graphics, thumbX, barY, thumbWidth, scrollBarWidth);
        }
    }
}
