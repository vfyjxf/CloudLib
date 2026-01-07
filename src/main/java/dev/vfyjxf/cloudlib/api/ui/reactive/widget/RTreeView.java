package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.EventDef;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A collapsible tree view widget.
 * <p>
 * Supports the new event system for type-safe callbacks:
 * <pre>{@code
 * treeView.onNodeSelect().register((ctx, node) -> {
 *     System.out.println("Selected: " + node.label);
 * });
 * 
 * treeView.onNodeExpand().register((ctx, node, expanded) -> {
 *     System.out.println("Node " + node.label + " expanded: " + expanded);
 * });
 * }</pre>
 */
public class RTreeView extends RWidget {

    // ===== Custom TreeView Events =====
    
    @FunctionalInterface
    public interface OnNodeSelect {
        void onNodeSelect(RUIContext ctx, TreeNode node);
    }
    
    @FunctionalInterface
    public interface OnNodeExpand {
        void onNodeExpand(RUIContext ctx, TreeNode node, boolean expanded);
    }
    
    private static final EventDef<OnNodeSelect> ON_NODE_SELECT = EventDef.direct(
        OnNodeSelect.class,
        listeners -> (ctx, node) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onNodeSelect(ctx, node);
            }
        }
    );
    
    private static final EventDef<OnNodeExpand> ON_NODE_EXPAND = EventDef.direct(
        OnNodeExpand.class,
        listeners -> (ctx, node, expanded) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onNodeExpand(ctx, node, expanded);
            }
        }
    );
    
    private final EventDef.REvent<OnNodeSelect> onNodeSelectEvent = ON_NODE_SELECT.create();
    private final EventDef.REvent<OnNodeExpand> onNodeExpandEvent = ON_NODE_EXPAND.create();

    private final List<TreeNode> rootNodes = new ArrayList<>();
    private int indentSize = 16;
    private int itemHeight = 16;
    private Consumer<TreeNode> onNodeClick; // Legacy callback support

    // Colors
    private int backgroundColor = 0x40000000;
    private int textColor = 0xFFFFFFFF;
    private int hoverColor = 0x40FFFFFF;
    private int selectedColor = 0xFF4488FF;
    private int expandIconColor = 0xFFAAAAAA;

    // State
    private TreeNode hoveredNode = null;
    private TreeNode selectedNode = null;

    public RTreeView(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void addRootNode(TreeNode node) {
        rootNodes.add(node);
    }

    public void clearNodes() {
        rootNodes.clear();
    }

    public void setOnNodeClick(Consumer<TreeNode> callback) {
        this.onNodeClick = callback;
    }
    
    /**
     * Get the node select event for this tree view.
     * This is a direct event that fires when a node is selected.
     */
    public EventDef.REvent<OnNodeSelect> onNodeSelect() {
        return onNodeSelectEvent;
    }
    
    /**
     * Get the node expand event for this tree view.
     * This is a direct event that fires when a node is expanded/collapsed.
     */
    public EventDef.REvent<OnNodeExpand> onNodeExpand() {
        return onNodeExpandEvent;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setColors(int text, int hover, int selected) {
        this.textColor = text;
        this.hoverColor = hover;
        this.selectedColor = selected;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Draw background
        graphics.fill(x, y, x + width, y + height, backgroundColor);

        // Reset hovered node
        hoveredNode = null;

        // Render tree
        int currentY = y + 2;
        for (TreeNode node : rootNodes) {
            currentY = renderNode(graphics, font, node, 0, currentY, mouseX, mouseY);
        }
    }

    private int renderNode(GuiGraphics graphics, Font font, TreeNode node, int depth, int currentY, int mouseX, int mouseY) {
        if (currentY > y + height) return currentY;

        int nodeX = x + 2 + depth * indentSize;
        int nodeY = currentY;

        // Check hover
        boolean isHovered = mouseX >= x && mouseX < x + width &&
                mouseY >= nodeY && mouseY < nodeY + itemHeight;
        if (isHovered) {
            hoveredNode = node;
        }

        // Draw hover/selection background
        if (node == selectedNode) {
            graphics.fill(x + 1, nodeY, x + width - 1, nodeY + itemHeight, selectedColor);
        } else if (isHovered) {
            graphics.fill(x + 1, nodeY, x + width - 1, nodeY + itemHeight, hoverColor);
        }

        // Draw expand/collapse icon if has children
        if (!node.children.isEmpty()) {
            String icon = node.expanded ? "▼" : "▶";
            graphics.drawString(font, icon, nodeX, nodeY + 4, expandIconColor);
        }

        // Draw icon if present
        int textX = nodeX + 12;
        if (node.icon != null && !node.icon.isEmpty()) {
            graphics.drawString(font, node.icon, textX, nodeY + 4, node.iconColor);
            textX += font.width(node.icon) + 2;
        }

        // Draw label
        graphics.drawString(font, node.label, textX, nodeY + 4, textColor);

        currentY += itemHeight;

        // Render children if expanded
        if (node.expanded) {
            for (TreeNode child : node.children) {
                currentY = renderNode(graphics, font, child, depth + 1, currentY, mouseX, mouseY);
            }
        }

        return currentY;
    }

    @Override
    public int[] measure(Font font) {
        // Calculate total height based on visible nodes
        int totalHeight = calculateVisibleHeight(rootNodes);
        return new int[]{width, Math.max(height, totalHeight + 4)};
    }

    private int calculateVisibleHeight(List<TreeNode> nodes) {
        int height = 0;
        for (TreeNode node : nodes) {
            height += itemHeight;
            if (node.expanded) {
                height += calculateVisibleHeight(node.children);
            }
        }
        return height;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !contains(mouseX, mouseY)) return false;

        if (button == 0 && hoveredNode != null) {
            RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
            
            // Check if clicking on expand icon area
            int nodeDepth = getNodeDepth(hoveredNode);
            int expandIconX = x + 2 + nodeDepth * indentSize;
            
            if (mouseX >= expandIconX && mouseX < expandIconX + 12 && !hoveredNode.children.isEmpty()) {
                // Toggle expand
                hoveredNode.expanded = !hoveredNode.expanded;
                
                // Fire expand event
                onNodeExpandEvent.invoker().onNodeExpand(ctx, hoveredNode, hoveredNode.expanded);
            } else {
                // Select node
                selectedNode = hoveredNode;
                
                // Fire select event
                onNodeSelectEvent.invoker().onNodeSelect(ctx, hoveredNode);
                
                // Legacy callback support
                if (onNodeClick != null) {
                    onNodeClick.accept(hoveredNode);
                }
            }
            
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        return false;
    }

    private int getNodeDepth(TreeNode target) {
        for (TreeNode root : rootNodes) {
            int depth = findNodeDepth(root, target, 0);
            if (depth >= 0) return depth;
        }
        return 0;
    }

    private int findNodeDepth(TreeNode current, TreeNode target, int depth) {
        if (current == target) return depth;
        for (TreeNode child : current.children) {
            int found = findNodeDepth(child, target, depth + 1);
            if (found >= 0) return found;
        }
        return -1;
    }

    /**
     * Tree node data class.
     */
    public static class TreeNode {
        public String label;
        public String icon;
        public int iconColor = 0xFFFFFFFF;
        public Object data;
        public boolean expanded = false;
        public final List<TreeNode> children = new ArrayList<>();

        public TreeNode(String label) {
            this.label = label;
        }

        public TreeNode(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }

        public TreeNode addChild(TreeNode child) {
            children.add(child);
            return this;
        }

        public TreeNode addChild(String label) {
            children.add(new TreeNode(label));
            return this;
        }

        public TreeNode setIcon(String icon, int color) {
            this.icon = icon;
            this.iconColor = color;
            return this;
        }

        public TreeNode setExpanded(boolean expanded) {
            this.expanded = expanded;
            return this;
        }

        public TreeNode setData(Object data) {
            this.data = data;
            return this;
        }
    }
}
