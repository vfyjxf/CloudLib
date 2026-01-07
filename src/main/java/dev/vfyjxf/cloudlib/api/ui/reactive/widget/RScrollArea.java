package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A scrollable container widget.
 */
public class RScrollArea extends RWidget {

    private RWidget content;
    private RenderNode.ScrollState scrollState;

    // Scroll bar appearance - increased width for easier clicking
    private int scrollBarWidth = 10;
    private int scrollBarColor = 0xFFAAAAAA;
    private int scrollBarHoverColor = 0xFFCCCCCC;
    private int trackColor = 0x40000000;

    // Interaction state (transient - ok to reset each frame)
    private boolean hovered = false;
    private boolean scrollBarHovered = false;
    // Note: dragging state is stored in scrollState to persist across widget rebuilds

    public RScrollArea(int width, int height) {
        this.width = width;
        this.height = height;
        this.scrollState = new RenderNode.ScrollState();
    }

    public RScrollArea(int width, int height, RenderNode.ScrollState scrollState) {
        this.width = width;
        this.height = height;
        this.scrollState = scrollState != null ? scrollState : new RenderNode.ScrollState();
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

    public RenderNode.ScrollState getScrollState() {
        return scrollState;
    }

    public void setScrollBarWidth(int width) {
        this.scrollBarWidth = width;
    }

    /**
     * Layout the content within the scroll area.
     * This ensures content is properly positioned before event handling.
     */
    public void layout(Font font) {
        if (content == null) return;
        
        int scrollY = scrollState.getScrollY();
        
        // Layout content at scroll offset position
        content.setPos(x, y - scrollY);
        content.setSize(width - scrollBarWidth - 2, 10000);
        
        if (content instanceof RGroup group) {
            group.layout(font);
        }
        
        // Measure and update scroll state
        int[] contentSize = content.measure(font);
        int contentHeight = Math.max(contentSize[1], 1);
        scrollState.setContentSize(width - scrollBarWidth - 2, contentHeight);
        scrollState.clamp(0, height);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        hovered = contains(mouseX, mouseY);

        // Draw background
        int bgColor = getStyleBackground(0x40000000);
        graphics.fill(x, y, x + width, y + height, bgColor);

        if (content == null) return;

        // Layout content at the actual render position (with scroll offset)
        // to get correct measurements
        int scrollY = scrollState.getScrollY();
        
        // First pass: layout to measure size (at base position)
        content.setPos(x, y);
        content.setSize(width - scrollBarWidth - 2, 10000);
        
        if (content instanceof RGroup group) {
            group.layout(font);
        }

        // Measure content after layout
        int[] contentSize = content.measure(font);
        int contentHeight = Math.max(contentSize[1], 1);

        // Determine if scrollbar is needed
        boolean needsScrollBar = contentHeight > height;
        int contentAreaWidth = needsScrollBar ? width - scrollBarWidth - 2 : width;

        // Update scroll state
        scrollState.setContentSize(contentAreaWidth, contentHeight);
        scrollState.clamp(0, height);
        scrollY = scrollState.getScrollY(); // Update after clamp

        // Second pass: re-layout at scroll offset position for rendering
        content.setPos(x, y - scrollY);
        content.setSize(contentAreaWidth, 10000);
        
        if (content instanceof RGroup group) {
            group.layout(font);
        }

        // Enable scissoring for content area
        graphics.enableScissor(x, y, x + contentAreaWidth, y + height);

        // Render content - DON'T adjust mouseY here, the content is already positioned correctly
        // for rendering. Mouse coordinates stay in screen space.
        content.render(graphics, font, mouseX, mouseY, delta);

        graphics.disableScissor();

        // Render scroll bar only if needed
        if (needsScrollBar) {
            renderScrollBar(graphics, contentHeight, mouseX, mouseY);
        }
    }

    private void renderScrollBar(GuiGraphics graphics, int contentHeight, int mouseX, int mouseY) {
        // Only render scrollbar if scrolling is needed
        if (contentHeight <= height) return;

        int scrollBarX = x + width - scrollBarWidth;

        // Draw track
        graphics.fill(scrollBarX, y, x + width, y + height, trackColor);

        // Calculate thumb size and position
        float visibleRatio = (float) height / contentHeight;
        int thumbHeight = Math.max(20, (int) (height * visibleRatio));

        int maxScroll = contentHeight - height;
        float scrollRatio = maxScroll > 0 ? (float) scrollState.getScrollY() / maxScroll : 0;
        int thumbY = y + (int) ((height - thumbHeight) * scrollRatio);

        // Check if mouse is over scroll bar
        scrollBarHovered = mouseX >= scrollBarX && mouseX < x + width &&
                mouseY >= thumbY && mouseY < thumbY + thumbHeight;

        int barColor = scrollBarHovered || scrollState.isDragging() ? scrollBarHoverColor : scrollBarColor;

        // Draw thumb
        graphics.fill(scrollBarX + 1, thumbY, x + width - 1, thumbY + thumbHeight, barColor);
    }

    @Override
    public int[] measure(Font font) {
        return new int[]{width, height};
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        
        // Check if mouse is in scroll area bounds (not relying on hovered from render)
        if (!contains(mouseX, mouseY)) return false;

        if (button == 0) {
            // Check if clicking on scroll bar area (only if draggable is enabled)
            int contentHeight = scrollState.getContentHeight();
            boolean needsScrollBar = contentHeight > height;
            int scrollBarX = x + width - scrollBarWidth;
            
            if (scrollState.isDraggable() && needsScrollBar && mouseX >= scrollBarX) {
                scrollState.setDragging(true);
                scrollState.setDragStartY(mouseY);
                scrollState.setDragStartScroll(scrollState.getScrollY());
                return true;
            }

            // Check if click is in content area (not on scrollbar)
            int contentAreaWidth = needsScrollBar ? width - scrollBarWidth - 2 : width;
            if (content != null && mouseX >= x && mouseX < x + contentAreaWidth &&
                mouseY >= y && mouseY < y + height) {
                // Content is rendered at y - scrollY, but we need to check in screen coords
                // The content's position is already offset, so just dispatch directly
                if (content.isVisible()) {
                    if (content.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }
        // Consume click if it's in the scroll area to prevent pass-through
        return true;
    }

    /**
     * Finds the deepest target widget for a mouse event at the given position.
     * Returns null if no suitable target found (caller should handle).
     */
    public RWidget findTargetAt(int mouseX, int mouseY) {
        if (content == null) return null;
        
        if (content.isVisible() && content.contains(mouseX, mouseY)) {
            if (content instanceof RGroup group) {
                RWidget target = group.findTargetAt(mouseX, mouseY);
                if (target != null) return target;
            }
            // Return content, not this - to avoid recursion
            return content;
        }
        
        return null;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        // First, check if we are dragging our own scrollbar
        if (scrollState.isDragging()) {
            // Use stored content height from scrollState
            int contentHeight = scrollState.getContentHeight();

            if (contentHeight > height) {
                float scrollableHeight = contentHeight - height;
                float trackHeight = height - 20; // Approximate thumb height

                float deltaScroll = (mouseY - scrollState.getDragStartY()) / trackHeight * scrollableHeight;
                int newScrollY = (int) (scrollState.getDragStartScroll() + deltaScroll);
                scrollState.setScrollY(newScrollY);
                scrollState.clamp(0, height);
            }
            return true;
        }
        
        // Not dragging our scrollbar - propagate to content
        // This allows nested ScrollAreas to receive drag events
        if (content != null) {
            if (content instanceof RScrollArea nestedScroll) {
                if (nestedScroll.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            } else if (content instanceof RGroup group) {
                if (propagateDragToContent(group, mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Recursively propagate drag events to nested draggable widgets.
     */
    private boolean propagateDragToContent(RGroup group, int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        for (RWidget child : group.getChildren()) {
            if (!child.isVisible()) continue;
            
            if (child instanceof RScrollArea scrollArea) {
                if (scrollArea.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RSlider slider) {
                if (slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RPopup popup) {
                if (popup.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RGroup childGroup) {
                if (propagateDragToContent(childGroup, mouseX, mouseY, button, deltaX, deltaY)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        scrollState.setDragging(false);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible) return false;
        
        // Check if mouse is in scroll area bounds
        if (!contains(mouseX, mouseY)) return false;

        // First, let content handle the scroll (e.g., nested scroll areas)
        if (content != null && content.isVisible()) {
            if (content.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true; // Content (or nested scroll area) handled it
            }
        }

        // Check if we have content to scroll
        int contentHeight = scrollState.getContentHeight();
        
        // If contentHeight is still 0, try to estimate based on content's current height
        if (contentHeight <= 0 && content != null) {
            contentHeight = content.getHeight();
        }
        
        if (contentHeight <= height) {
            return false; // No scrolling needed
        }

        // Scroll content
        int oldScrollY = scrollState.getScrollY();
        scrollState.scrollBy(0, (int) (-scrollY * 20));

        // Clamp scroll
        int maxScroll = Math.max(0, contentHeight - height);
        int scrollYPos = scrollState.getScrollY();
        scrollState.setScrollY(Math.max(0, Math.min(scrollYPos, maxScroll)));
        
        // Only consume the event if scroll actually changed
        return scrollState.getScrollY() != oldScrollY;
    }

    @Override
    protected void broadcastToChildren(RUIContext ctx, BroadcastHandler handler) {
        if (content != null) {
            handler.handle(content, ctx);
        }
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void onAttach() {
        super.onAttach();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
