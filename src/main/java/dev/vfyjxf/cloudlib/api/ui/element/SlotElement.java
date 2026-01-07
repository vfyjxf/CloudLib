package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import org.jetbrains.annotations.Nullable;

/**
 * Element for Slot nodes (content injection points).
 */
public class SlotElement extends Element {
    
    /** Injected child element */
    private @Nullable Element child;
    
    public SlotElement(RenderNode.Slot slot) {
        super(slot);
    }
    
    @Override
    protected void rebuild() {
        if (!(widget instanceof RenderNode.Slot slot)) {
            return;
        }
        
        RenderNode content = slot.fallback();
        child = updateChild(child, content);
        
        children.clear();
        if (child != null) {
            children.add(child);
        }
    }
    
    @Override
    protected void onUnmount() {
        if (child != null) {
            child.unmount();
            child = null;
        }
        super.onUnmount();
    }
    
    /**
     * Gets the slot name.
     */
    public String getSlotName() {
        if (widget instanceof RenderNode.Slot slot) {
            return slot.name();
        }
        return "default";
    }
}
