package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * A container widget that arranges children in a vertical or horizontal layout.
 */
public class RGroup extends RWidget {

    public enum Direction {
        VERTICAL,
        HORIZONTAL,
        STACK  // Children are stacked on top of each other (overlay)
    }

    private final Direction direction;
    private final List<RWidget> children = new ArrayList<>();
    private int gap = 4;
    private int padding = 0;
    private boolean drawBackground = false;

    public RGroup(Direction direction) {
        this.direction = direction;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public void setDrawBackground(boolean draw) {
        this.drawBackground = draw;
    }

    public void addChild(RWidget child) {
        child.setParent(this);
        children.add(child);
    }

    public void addChildren(List<RWidget> widgets) {
        for (RWidget widget : widgets) {
            widget.setParent(this);
        }
        children.addAll(widgets);
    }

    public void clearChildren() {
        for (RWidget child : children) {
            child.setParent(null);
            child.onDetach();
        }
        children.clear();
    }

    public List<RWidget> getChildren() {
        return children;
    }
    /**
     * Performs layout of children.
     * Call this after adding children and before rendering.
     */
    public void layout(Font font) {
        int offsetX = x + padding;
        int offsetY = y + padding;

        for (RWidget child : children) {
            if (!child.isVisible()) continue;

            int[] size = child.measure(font);

            child.setPos(offsetX, offsetY);
            child.setSize(size[0], size[1]);

            // Layout nested containers
            if (child instanceof RGroup group) {
                group.layout(font);
            } else if (child instanceof RScrollArea scrollArea) {
                scrollArea.layout(font);
            }
            // Note: RPopup and RPanel don't need layout here - they manage their own layout in render()

            // STACK: all children at same position (overlaid)
            // VERTICAL: advance Y
            // HORIZONTAL: advance X
            if (direction == Direction.VERTICAL) {
                offsetY += size[1] + gap;
            } else if (direction == Direction.HORIZONTAL) {
                offsetX += size[0] + gap;
            }
            // STACK: don't advance - children overlap
        }

        // Update our own size based on children
        updateSize(font);
    }

    private void updateSize(Font font) {
        int maxWidth = 0;
        int maxHeight = 0;
        int totalWidth = 0;
        int totalHeight = 0;

        for (RWidget child : children) {
            if (!child.isVisible()) continue;

            int[] size = child.measure(font);
            maxWidth = Math.max(maxWidth, size[0]);
            maxHeight = Math.max(maxHeight, size[1]);
            totalWidth += size[0];
            totalHeight += size[1];
        }

        int numVisible = (int) children.stream().filter(RWidget::isVisible).count();
        int gapTotal = Math.max(0, (numVisible - 1) * gap);

        if (direction == Direction.VERTICAL) {
            this.width = maxWidth + padding * 2;
            this.height = totalHeight + gapTotal + padding * 2;
        } else if (direction == Direction.HORIZONTAL) {
            this.width = totalWidth + gapTotal + padding * 2;
            this.height = maxHeight + padding * 2;
        } else {
            // STACK: size is max of all children
            this.width = maxWidth + padding * 2;
            this.height = maxHeight + padding * 2;
        }
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Draw background if enabled
        if (drawBackground) {
            int bgColor = getStyleBackground(0x80000000);
            graphics.fill(x, y, x + width, y + height, bgColor);
        }

        // Render children
        for (RWidget child : children) {
            if (child.isVisible()) {
                child.render(graphics, font, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public int[] measure(Font font) {
        updateSize(font);
        return new int[]{width, height};
    }

    // ===== Event Handling =====

    /**
     * Finds the target widget for a mouse event at the given position.
     * Returns the deepest widget that contains the point.
     */
    public RWidget findTargetAt(int mouseX, int mouseY) {
        if (!visible) return null;

        // Check children in reverse order (top-most first)
        for (int i = children.size() - 1; i >= 0; i--) {
            RWidget child = children.get(i);
            if (child.isVisible() && child.contains(mouseX, mouseY)) {
                if (child instanceof RGroup group) {
                    RWidget target = group.findTargetAt(mouseX, mouseY);
                    if (target != null) return target;
                }
                return child;
            }
        }

        return this.contains(mouseX, mouseY) ? this : null;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;

        // Find the target widget and dispatch to it
        RWidget target = findTargetAt(mouseX, mouseY);
        if (target != null && target != this) {
            return target.mouseClicked(mouseX, mouseY, button);
        }

        // Handle on self using parent implementation
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible) return false;

        // Find the target widget and dispatch to it
        RWidget target = findTargetAt(mouseX, mouseY);
        if (target != null && target != this) {
            return target.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // Handle on self using parent implementation
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void broadcastToChildren(RUIContext ctx, BroadcastHandler handler) {
        for (RWidget child : children) {
            handler.handle(child, ctx);
        }
    }

    @Override
    public void onAttach() {
        super.onAttach();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void update() {
        super.update();
    }
}
