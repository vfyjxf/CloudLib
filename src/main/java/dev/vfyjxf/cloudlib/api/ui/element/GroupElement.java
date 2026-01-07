package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.LayoutType;
import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;

import java.util.List;

/**
 * Element for Group nodes (Column, Row, Stack, etc.).
 * <p>
 * Group elements have multiple children and handle layout.
 */
public class GroupElement extends Element {
    
    public GroupElement(RenderNode.Group group) {
        super(group);
    }
    
    @Override
    protected void rebuild() {
        if (widget instanceof RenderNode.Group group) {
            // Update children with the new widget list
            updateChildren(group.children());
        }
    }
    
    @Override
    protected void onUpdate(RenderNode oldWidget, RenderNode newWidget) {
        super.onUpdate(oldWidget, newWidget);
        
        // Check if children changed
        if (oldWidget instanceof RenderNode.Group oldGroup && 
            newWidget instanceof RenderNode.Group newGroup) {
            
            // If children are different, rebuild
            if (!oldGroup.children().equals(newGroup.children())) {
                markNeedsBuild();
            }
        }
    }
    
    /**
     * Gets the layout type of this group.
     */
    public LayoutType getLayout() {
        if (widget instanceof RenderNode.Group group) {
            return group.layout();
        }
        return LayoutType.COLUMN;
    }
}
