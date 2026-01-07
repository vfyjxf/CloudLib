package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.EventDef;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A draggable popup widget with title bar and close button.
 * <p>
 * Features:
 * <ul>
 *   <li>Draggable by title bar</li>
 *   <li>Close button with onClose event</li>
 *   <li>Internal visibility management via show()/hide()/toggle()</li>
 *   <li>Optional scrollable content</li>
 *   <li>Type-safe events for show/hide/close</li>
 * </ul>
 * <p>
 * Usage with internal state:
 * <pre>{@code
 * RPopup popup = new RPopup("My Popup", 200, 150, state);
 * 
 * // Register events
 * popup.onShow().register((ctx) -> System.out.println("Popup shown"));
 * popup.onHide().register((ctx) -> System.out.println("Popup hidden"));
 * popup.onClose().register((ctx) -> System.out.println("Close clicked"));
 * 
 * // Control visibility
 * popup.show();
 * popup.hide();
 * popup.toggle();
 * 
 * // Check visibility
 * if (popup.isShowing()) { ... }
 * }</pre>
 */
public class RPopup extends RWidget {

    // ===== Custom Popup Events =====
    
    @FunctionalInterface
    public interface OnShow {
        void onShow(RUIContext ctx);
    }
    
    @FunctionalInterface
    public interface OnHide {
        void onHide(RUIContext ctx);
    }
    
    @FunctionalInterface
    public interface OnCloseRequest {
        void onCloseRequest(RUIContext ctx);
    }
    
    private static final EventDef<OnShow> ON_SHOW = EventDef.direct(
        OnShow.class,
        listeners -> (ctx) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onShow(ctx);
            }
        }
    );
    
    private static final EventDef<OnHide> ON_HIDE = EventDef.direct(
        OnHide.class,
        listeners -> (ctx) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onHide(ctx);
            }
        }
    );
    
    private static final EventDef<OnCloseRequest> ON_CLOSE_REQUEST = EventDef.direct(
        OnCloseRequest.class,
        listeners -> (ctx) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onCloseRequest(ctx);
            }
        }
    );
    
    private final EventDef.REvent<OnShow> onShowEvent = ON_SHOW.create();
    private final EventDef.REvent<OnHide> onHideEvent = ON_HIDE.create();
    private final EventDef.REvent<OnCloseRequest> onCloseRequestEvent = ON_CLOSE_REQUEST.create();

    private String title;
    private RWidget content;
    private Runnable onClose; // Legacy callback support

    // Appearance
    private int titleBarHeight = 20;
    private int titleBarColor = 0xFF4466AA;
    private int backgroundColor = 0xE0222233;
    private int borderColor = 0xFF5577BB;
    private int closeButtonColor = 0xFFCC4444;
    private int closeButtonHoverColor = 0xFFFF6666;
    private int titleColor = 0xFFFFFFFF;

    // Drag state - stored in PopupState for persistence
    private final PopupState state;

    // Transient state
    private boolean titleBarHovered = false;
    private boolean closeButtonHovered = false;

    public RPopup(String title, int width, int height, PopupState state) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.state = state;
        
        // Initialize position if not set
        if (!state.positionInitialized) {
            state.popupX = 50;
            state.popupY = 50;
            state.positionInitialized = true;
        }
    }

    public void setContent(RWidget content) {
        if (this.content != null) {
            this.content.setParent(null);
        }
        this.content = content;
        if (content != null) {
            content.setParent(this);
        }
    }
    
    public RWidget getContent() {
        return content;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
    
    // ===== Event Accessors =====
    
    /**
     * Event fired when popup becomes visible (is attached).
     */
    public EventDef.REvent<OnShow> onShow() {
        return onShowEvent;
    }
    
    /**
     * Event fired when popup becomes hidden (is detached).
     */
    public EventDef.REvent<OnHide> onHide() {
        return onHideEvent;
    }
    
    /**
     * Event fired when user clicks close button.
     * Parent should handle this to remove the popup from the component tree.
     */
    public EventDef.REvent<OnCloseRequest> onCloseRequest() {
        return onCloseRequestEvent;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleBarColor(int color) {
        this.titleBarColor = color;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Get actual position from state
        int px = state.popupX;
        int py = state.popupY;

        // Update hover states
        titleBarHovered = mouseX >= px && mouseX < px + width - 20 &&
                mouseY >= py && mouseY < py + titleBarHeight;
        closeButtonHovered = mouseX >= px + width - 20 && mouseX < px + width &&
                mouseY >= py && mouseY < py + titleBarHeight;

        // Draw shadow
        graphics.fill(px + 3, py + 3, px + width + 3, py + height + 3, 0x40000000);

        // Draw background
        graphics.fill(px, py, px + width, py + height, backgroundColor);

        // Draw border
        drawBorder(graphics, px, py, width, height, borderColor);

        // Draw title bar
        int tbColor = state.dragging ? brighten(titleBarColor) : titleBarColor;
        graphics.fill(px, py, px + width, py + titleBarHeight, tbColor);

        // Draw title text
        graphics.drawString(font, title, px + 6, py + 6, titleColor);

        // Draw close button
        int cbColor = closeButtonHovered ? closeButtonHoverColor : closeButtonColor;
        graphics.fill(px + width - 18, py + 2, px + width - 2, py + titleBarHeight - 2, cbColor);
        graphics.drawCenteredString(font, "×", px + width - 10, py + 5, 0xFFFFFFFF);

        // Draw content area
        if (content != null) {
            int contentX = px + 4;
            int contentY = py + titleBarHeight + 4;
            int contentWidth = width - 8;
            int contentHeight = height - titleBarHeight - 8;

            // Position and render content
            content.setPos(contentX, contentY);
            content.setSize(contentWidth, contentHeight);

            if (content instanceof RGroup group) {
                group.layout(font);
            }

            // Scissor for content
            graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
            content.render(graphics, font, mouseX, mouseY, delta);
            graphics.disableScissor();
        }
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color); // Top
        graphics.fill(x, y + h - 1, x + w, y + h, color); // Bottom
        graphics.fill(x, y, x + 1, y + h, color); // Left
        graphics.fill(x + w - 1, y, x + w, y + h, color); // Right
    }

    private int brighten(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 30);
        int b = Math.min(255, (color & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public int[] measure(Font font) {
        return new int[]{width, height};
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;

        int px = state.popupX;
        int py = state.popupY;

        // Calculate hover states directly (don't rely on render having been called)
        boolean onTitleBar = mouseX >= px && mouseX < px + width - 20 &&
                mouseY >= py && mouseY < py + titleBarHeight;
        boolean onCloseButton = mouseX >= px + width - 20 && mouseX < px + width &&
                mouseY >= py && mouseY < py + titleBarHeight;

        // Check if clicking on close button
        if (onCloseButton && button == 0) {
            // Fire new event system
            RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
            onCloseRequestEvent.invoker().onCloseRequest(ctx);
            
            // Legacy callback support
            if (onClose != null) {
                onClose.run();
            }
            return true;
        }

        // Check if clicking on title bar (start drag)
        if (onTitleBar && button == 0) {
            state.dragging = true;
            state.dragOffsetX = mouseX - px;
            state.dragOffsetY = mouseY - py;
            return true;
        }

        // Check if clicking on content - dispatch to content directly
        if (content != null) {
            int contentX = px + 4;
            int contentY = py + titleBarHeight + 4;
            int contentWidth = width - 8;
            int contentHeight = height - titleBarHeight - 8;

            if (mouseX >= contentX && mouseX < contentX + contentWidth &&
                mouseY >= contentY && mouseY < contentY + contentHeight) {
                // Dispatch directly to content, avoid findTargetAt to prevent recursion
                if (content.isVisible() && content.contains(mouseX, mouseY)) {
                    if (content.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }

        // Clicking anywhere on popup consumes the event
        if (mouseX >= px && mouseX < px + width && mouseY >= py && mouseY < py + height) {
            return true;
        }

        return false;
    }

    /**
     * Finds the deepest target widget for a mouse event at the given position.
     * Returns null if no suitable target found (to avoid recursion when caller is this).
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
        // First check if we're dragging the popup title bar
        if (state.dragging) {
            state.popupX = mouseX - state.dragOffsetX;
            state.popupY = mouseY - state.dragOffsetY;
            return true;
        }
        
        // Not dragging popup - propagate to content (e.g., nested ScrollArea)
        if (content != null) {
            if (content instanceof RScrollArea scrollArea) {
                if (scrollArea.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            } else if (content instanceof RSlider slider) {
                if (slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
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
            } else if (child instanceof RGroup childGroup) {
                if (propagateDragToContent(childGroup, mouseX, mouseY, button, deltaX, deltaY)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        state.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible) return false;

        int px = state.popupX;
        int py = state.popupY;

        // Check if scrolling on content
        if (content != null) {
            int contentX = px + 4;
            int contentY = py + titleBarHeight + 4;
            int contentWidth = width - 8;
            int contentHeight = height - titleBarHeight - 8;

            if (mouseX >= contentX && mouseX < contentX + contentWidth &&
                mouseY >= contentY && mouseY < contentY + contentHeight) {
                return content.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
        }

        return false;
    }

    @Override
    public boolean contains(int mouseX, int mouseY) {
        // Use popup state position instead of the base x/y fields
        int px = state.popupX;
        int py = state.popupY;
        return mouseX >= px && mouseX < px + width && mouseY >= py && mouseY < py + height;
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

    /**
     * State for popup that persists across widget rebuilds.
     */
    public static class PopupState {
        public int popupX = 50;
        public int popupY = 50;
        public boolean positionInitialized = false;
        public boolean dragging = false;
        public int dragOffsetX = 0;
        public int dragOffsetY = 0;
        public boolean visible = true;

        public PopupState setPosition(int x, int y) {
            this.popupX = x;
            this.popupY = y;
            this.positionInitialized = true;
            return this;
        }

        public PopupState setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }
    }
}
