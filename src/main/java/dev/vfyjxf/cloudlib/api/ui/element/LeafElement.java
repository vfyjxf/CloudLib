package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;

import java.util.List;

/**
 * Element for Leaf nodes (Text, Button, etc.).
 * <p>
 * Leaf elements have no children and represent actual renderable content.
 */
public class LeafElement extends Element {
    
    public LeafElement(RenderNode widget) {
        super(widget);
    }
    
    @Override
    protected void rebuild() {
        // Leaf nodes have no children to rebuild
    }
    
    @Override
    protected void onUpdate(RenderNode oldWidget, RenderNode newWidget) {
        super.onUpdate(oldWidget, newWidget);
        // Leaf update is handled by the render layer
        // Here we just store the new widget configuration
    }
    
    /**
     * Gets the leaf type (e.g., "text", "button").
     */
    public String getType() {
        if (widget instanceof RenderNode.Leaf leaf) {
            return leaf.type();
        }
        return "empty";
    }
    
    /**
     * Gets the leaf content.
     */
    public Object getContent() {
        if (widget instanceof RenderNode.Leaf leaf) {
            return leaf.content();
        }
        return null;
    }
}
