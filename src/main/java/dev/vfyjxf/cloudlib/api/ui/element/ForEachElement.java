package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Element for ForEach nodes (list rendering).
 * <p>
 * Efficiently handles list rendering by:
 * <ul>
 *   <li>Tracking items by key for stable identity</li>
 *   <li>Reusing elements when items move</li>
 *   <li>Minimizing creation/destruction</li>
 * </ul>
 */
public class ForEachElement extends Element {
    
    /** Map of key to element for efficient lookup */
    private final Map<Object, Element> keyedElements = new LinkedHashMap<>();
    
    public ForEachElement(RenderNode.ForEach forEach) {
        super(forEach);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void rebuild() {
        if (!(widget instanceof RenderNode.ForEach<?> forEach)) {
            return;
        }
        
        Iterable<?> items = (Iterable<?>) forEach.items().get();
        RenderNode.KeyExtractor<Object> keyExtractor = (RenderNode.KeyExtractor<Object>) forEach.keyExtract();
        RenderNode.ItemRenderer<Object> itemRenderer = (RenderNode.ItemRenderer<Object>) forEach.renderer();
        
        // Build new keyed element map
        Map<Object, Element> newKeyedElements = new LinkedHashMap<>();
        List<Element> newChildren = new ArrayList<>();
        
        int index = 0;
        for (Object item : items) {
            // Extract key (or use index if no key extractor)
            Object key = keyExtractor != null ? keyExtractor.getKey(item) : index;
            
            // Build widget for this item
            RenderNode itemWidget = itemRenderer.render(item, index);
            
            // Try to reuse existing element
            Element existingElement = keyedElements.remove(key);
            
            Element child;
            if (existingElement != null && canUpdate(existingElement.widget, itemWidget)) {
                // Reuse existing element
                existingElement.update(itemWidget);
                child = existingElement;
            } else {
                // Create new element
                if (existingElement != null) {
                    existingElement.unmount();
                }
                child = createElement(itemWidget);
                child.mount(this, owner);
            }
            
            newKeyedElements.put(key, child);
            newChildren.add(child);
            index++;
        }
        
        // Unmount removed elements
        for (Element orphan : keyedElements.values()) {
            orphan.unmount();
        }
        
        // Update state
        keyedElements.clear();
        keyedElements.putAll(newKeyedElements);
        
        children.clear();
        children.addAll(newChildren);
    }
    
    @Override
    protected void onUnmount() {
        for (Element child : keyedElements.values()) {
            child.unmount();
        }
        keyedElements.clear();
        super.onUnmount();
    }
    
    /**
     * Gets the number of items in the list.
     */
    public int getItemCount() {
        return children.size();
    }
    
    /**
     * Gets the element for a specific key.
     */
    public @Nullable Element getElementByKey(Object key) {
        return keyedElements.get(key);
    }
}
