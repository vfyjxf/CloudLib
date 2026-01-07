package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Element for Conditional nodes (If/Else).
 * <p>
 * Manages conditional rendering by evaluating the condition
 * and rendering either the then or else branch.
 */
public class ConditionalElement extends Element {
    
    /** Currently active child (then or else branch) */
    private @Nullable Element activeChild;
    
    /** Whether the condition was true last build */
    private boolean lastCondition;
    
    public ConditionalElement(RenderNode.Conditional conditional) {
        super(conditional);
    }
    
    @Override
    protected void rebuild() {
        if (!(widget instanceof RenderNode.Conditional conditional)) {
            return;
        }
        
        // Evaluate condition
        boolean condition = conditional.condition().get();
        
        // Get the appropriate branch
        RenderNode branch = condition ? conditional.whenTrue() : conditional.whenFalse();
        
        // If condition changed, we may need to switch branches
        if (condition != lastCondition && activeChild != null) {
            activeChild.unmount();
            activeChild = null;
        }
        
        // Update or create child
        activeChild = updateChild(activeChild, branch);
        
        // Update children list
        children.clear();
        if (activeChild != null) {
            children.add(activeChild);
        }
        
        lastCondition = condition;
    }
    
    @Override
    protected void onUnmount() {
        if (activeChild != null) {
            activeChild.unmount();
            activeChild = null;
        }
        super.onUnmount();
    }
    
    /**
     * Gets the current condition value.
     */
    public boolean getCondition() {
        return lastCondition;
    }
}
