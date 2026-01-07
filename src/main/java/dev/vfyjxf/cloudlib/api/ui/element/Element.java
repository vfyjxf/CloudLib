package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.LayoutType;
import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import dev.vfyjxf.cloudlib.api.ui.reactive.Signal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Element is the persistent instance of a RenderNode in the UI tree.
 * <p>
 * This is the core of the fine-grained recomposition system. While RenderNode (Widget)
 * describes what to render, Element is the actual instance that:
 * <ul>
 *   <li>Persists across rebuilds</li>
 *   <li>Holds state via {@link ElementState}</li>
 *   <li>Manages lifecycle (mount, update, unmount)</li>
 *   <li>Tracks dependencies for precise rebuilding</li>
 * </ul>
 * <p>
 * Architecture (similar to Flutter):
 * <pre>
 * RenderNode (Widget)  - Immutable description, created each build
 *       │
 *       ▼
 * Element             - Persistent instance, manages lifecycle
 *       │
 *       ▼  
 * RenderObject        - Actual rendering (layout, paint)
 * </pre>
 * 
 * @see ElementState for state management
 * @see BuildOwner for scheduling rebuilds
 */
public abstract class Element {
    
    /** The current RenderNode (Widget) configuration */
    protected RenderNode widget;
    
    /** The optional key for identity preservation */
    protected @Nullable Object key;
    
    /** Parent element in the tree */
    protected @Nullable Element parent;
    
    /** Child elements */
    protected final List<Element> children = new ArrayList<>();
    
    /** Element lifecycle state */
    protected ElementLifecycle lifecycle = ElementLifecycle.CREATED;
    
    /** Depth in the tree (for ordering rebuilds) */
    protected int depth = 0;
    
    /** Whether this element needs to rebuild */
    protected boolean dirty = false;
    
    /** The build owner that manages this element */
    protected @Nullable BuildOwner owner;
    
    /**
     * Creates an element for the given widget.
     */
    protected Element(RenderNode widget) {
        this.widget = widget;
        this.key = extractKey(widget);
    }
    
    // ===== Factory Methods =====
    
    /**
     * Creates an appropriate Element for the given RenderNode.
     */
    public static Element createElement(RenderNode node) {
        return switch (node) {
            case RenderNode.Empty e -> new LeafElement(e);
            case RenderNode.Leaf l -> new LeafElement(l);
            case RenderNode.Group g -> new GroupElement(g);
            case RenderNode.ComponentRef c -> new ComponentElement(c);
            case RenderNode.Conditional c -> new ConditionalElement(c);
            case RenderNode.ForEach f -> new ForEachElement(f);
            case RenderNode.Dynamic d -> new DynamicElement(d);
            case RenderNode.Slot s -> new SlotElement(s);
            case RenderNode.ScrollArea sa -> new GroupElement(RenderNode.group(LayoutType.COLUMN, List.of(sa.content())));
            case RenderNode.Popup p -> new LeafElement(p);
            case RenderNode.PopupWithConfigurator p -> new LeafElement(p);
            case RenderNode.Floating f -> new GroupElement(RenderNode.group(LayoutType.COLUMN, List.of(f.content())));
        };
    }
    
    // ===== Core Lifecycle Methods =====
    
    /**
     * Mounts this element to the tree under the given parent.
     * Called when the element is first inserted into the tree.
     */
    public void mount(@Nullable Element parent, BuildOwner owner) {
        this.parent = parent;
        this.owner = owner;
        this.depth = parent != null ? parent.depth + 1 : 0;
        this.lifecycle = ElementLifecycle.MOUNTED;
        
        onMount();
        
        // Initial build
        performRebuild();
    }
    
    /**
     * Updates this element with a new widget configuration.
     * Called when the parent rebuilds and produces a new widget that can update this element.
     * 
     * @param newWidget the new widget configuration
     */
    public void update(RenderNode newWidget) {
        assert canUpdate(this.widget, newWidget) : "Cannot update element with incompatible widget";
        
        RenderNode oldWidget = this.widget;
        this.widget = newWidget;
        this.key = extractKey(newWidget);
        
        onUpdate(oldWidget, newWidget);
    }
    
    /**
     * Unmounts this element from the tree.
     * Called when the element is removed.
     */
    public void unmount() {
        lifecycle = ElementLifecycle.UNMOUNTED;
        
        // Unmount all children first
        for (Element child : children) {
            child.unmount();
        }
        children.clear();
        
        onUnmount();
        
        parent = null;
        owner = null;
    }
    
    // ===== Build Methods =====
    
    /**
     * Marks this element as needing rebuild.
     * The actual rebuild is scheduled via BuildOwner for batching.
     */
    public void markNeedsBuild() {
        if (lifecycle != ElementLifecycle.MOUNTED) {
            return;
        }
        
        if (dirty) {
            return; // Already marked
        }
        
        dirty = true;
        
        if (owner != null) {
            owner.scheduleBuild(this);
        }
    }
    
    /**
     * Performs the actual rebuild.
     * Subclasses override this to implement their specific rebuild logic.
     */
    public void performRebuild() {
        dirty = false;
        rebuild();
    }
    
    /**
     * Rebuilds this element's children.
     * Implemented by subclasses.
     */
    protected abstract void rebuild();
    
    // ===== Child Management =====
    
    /**
     * Updates the children list to match the new list of widgets.
     * This is the core reconciliation algorithm.
     * 
     * @param newWidgets the new list of child widgets
     */
    protected void updateChildren(List<RenderNode> newWidgets) {
        List<Element> newChildren = new ArrayList<>();
        
        int oldIndex = 0;
        int newIndex = 0;
        
        // Build map of keyed old children for fast lookup
        var keyedOldChildren = new java.util.HashMap<Object, Element>();
        for (Element child : children) {
            if (child.key != null) {
                keyedOldChildren.put(child.key, child);
            }
        }
        
        while (newIndex < newWidgets.size()) {
            RenderNode newWidget = newWidgets.get(newIndex);
            Object newKey = extractKey(newWidget);
            
            Element oldChild = null;
            
            // Try to find matching old child
            if (newKey != null) {
                // Keyed: look up by key
                oldChild = keyedOldChildren.remove(newKey);
            } else if (oldIndex < children.size()) {
                // Non-keyed: try sequential match
                Element candidate = children.get(oldIndex);
                if (candidate.key == null && canUpdate(candidate.widget, newWidget)) {
                    oldChild = candidate;
                    oldIndex++;
                }
            }
            
            if (oldChild != null && canUpdate(oldChild.widget, newWidget)) {
                // Reuse existing element
                oldChild.update(newWidget);
                newChildren.add(oldChild);
            } else {
                // Create new element
                Element newChild = createElement(newWidget);
                newChild.mount(this, owner);
                newChildren.add(newChild);
            }
            
            newIndex++;
        }
        
        // Unmount remaining old children
        for (Element oldChild : children) {
            if (!newChildren.contains(oldChild)) {
                oldChild.unmount();
            }
        }
        
        // Also unmount keyed children that weren't reused
        for (Element orphan : keyedOldChildren.values()) {
            if (!newChildren.contains(orphan)) {
                orphan.unmount();
            }
        }
        
        children.clear();
        children.addAll(newChildren);
    }
    
    /**
     * Updates a single child slot.
     */
    protected Element updateChild(@Nullable Element child, @Nullable RenderNode newWidget) {
        if (newWidget == null) {
            if (child != null) {
                child.unmount();
            }
            return null;
        }
        
        if (child != null && canUpdate(child.widget, newWidget)) {
            child.update(newWidget);
            return child;
        }
        
        if (child != null) {
            child.unmount();
        }
        
        Element newChild = createElement(newWidget);
        newChild.mount(this, owner);
        return newChild;
    }
    
    // ===== Lifecycle Hooks =====
    
    /**
     * Called when the element is mounted.
     * Override to perform initialization.
     */
    protected void onMount() { }
    
    /**
     * Called when the element is updated with a new widget.
     * Override to respond to configuration changes.
     */
    protected void onUpdate(RenderNode oldWidget, RenderNode newWidget) { }
    
    /**
     * Called when the element is unmounted.
     * Override to perform cleanup.
     */
    protected void onUnmount() { }
    
    // ===== Utility Methods =====
    
    /**
     * Determines if an element can be updated with a new widget.
     * Elements can be reused if they have the same runtime type and key.
     */
    public static boolean canUpdate(RenderNode oldWidget, RenderNode newWidget) {
        return oldWidget.getClass() == newWidget.getClass()
            && Objects.equals(extractKey(oldWidget), extractKey(newWidget));
    }
    
    /**
     * Extracts the key from a widget.
     */
    protected static @Nullable Object extractKey(RenderNode widget) {
        return switch (widget) {
            case RenderNode.ComponentRef c -> c.key();
            default -> null;
        };
    }
    
    // ===== Getters =====
    
    public RenderNode getWidget() {
        return widget;
    }
    
    public @Nullable Object getKey() {
        return key;
    }
    
    public @Nullable Element getParent() {
        return parent;
    }
    
    public List<Element> getChildren() {
        return List.copyOf(children);
    }
    
    public ElementLifecycle getLifecycle() {
        return lifecycle;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Visits this element and all descendants.
     */
    public void visitChildren(java.util.function.Consumer<Element> visitor) {
        for (Element child : children) {
            visitor.accept(child);
        }
    }
    
    /**
     * Visits this element and all descendants recursively.
     */
    public void visitDescendants(java.util.function.Consumer<Element> visitor) {
        for (Element child : children) {
            visitor.accept(child);
            child.visitDescendants(visitor);
        }
    }
}
