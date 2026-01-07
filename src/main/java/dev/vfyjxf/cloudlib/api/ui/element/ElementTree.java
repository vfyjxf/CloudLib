package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import org.jetbrains.annotations.Nullable;

/**
 * The root of an Element tree.
 * <p>
 * ElementTree manages:
 * <ul>
 *   <li>The root element</li>
 *   <li>The BuildOwner for scheduling builds</li>
 *   <li>Tree-wide operations</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * // Create the tree
 * ElementTree tree = new ElementTree();
 * 
 * // Attach a widget
 * RenderNode root = Render.Column(() -> {
 *     Text("Hello");
 *     Embed(myComponent);
 * });
 * tree.attachRoot(root);
 * 
 * // When state changes, flush builds
 * tree.flushBuild();
 * }</pre>
 */
public class ElementTree {
    
    /** The build owner for this tree */
    private final BuildOwner buildOwner;
    
    /** The root element */
    private @Nullable Element root;
    
    /** The current root widget */
    private @Nullable RenderNode rootWidget;
    
    public ElementTree() {
        this(new BuildOwner());
    }
    
    public ElementTree(BuildOwner buildOwner) {
        this.buildOwner = buildOwner;
    }
    
    /**
     * Attaches a root widget to this tree.
     * This creates the element tree and mounts it.
     * 
     * @param widget the root widget
     */
    public void attachRoot(RenderNode widget) {
        if (root != null) {
            // Detach old root
            root.unmount();
        }
        
        rootWidget = widget;
        root = Element.createElement(widget);
        root.mount(null, buildOwner);
    }
    
    /**
     * Updates the root widget.
     * Uses reconciliation to minimize changes.
     * 
     * @param newWidget the new root widget
     */
    public void updateRoot(RenderNode newWidget) {
        if (root == null) {
            attachRoot(newWidget);
            return;
        }
        
        if (Element.canUpdate(rootWidget, newWidget)) {
            root.update(newWidget);
            rootWidget = newWidget;
        } else {
            // Root type changed, need to recreate
            root.unmount();
            rootWidget = newWidget;
            root = Element.createElement(newWidget);
            root.mount(null, buildOwner);
        }
    }
    
    /**
     * Detaches and disposes the tree.
     */
    public void detach() {
        if (root != null) {
            root.unmount();
            root = null;
            rootWidget = null;
        }
    }
    
    /**
     * Flushes pending builds.
     * Call this at the start of each frame to process state changes.
     */
    public void flushBuild() {
        buildOwner.flushBuild();
    }
    
    /**
     * Gets the root element.
     */
    public @Nullable Element getRoot() {
        return root;
    }
    
    /**
     * Gets the root widget.
     */
    public @Nullable RenderNode getRootWidget() {
        return rootWidget;
    }
    
    /**
     * Gets the build owner.
     */
    public BuildOwner getBuildOwner() {
        return buildOwner;
    }
    
    /**
     * Finds an element by visiting the tree.
     * 
     * @param predicate the predicate to match
     * @return the first matching element, or null
     */
    public @Nullable Element findElement(java.util.function.Predicate<Element> predicate) {
        if (root == null) {
            return null;
        }
        
        return findElementRecursive(root, predicate);
    }
    
    private @Nullable Element findElementRecursive(Element element, java.util.function.Predicate<Element> predicate) {
        if (predicate.test(element)) {
            return element;
        }
        
        for (Element child : element.getChildren()) {
            Element found = findElementRecursive(child, predicate);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * Finds a ComponentElement by component type.
     */
    public @Nullable ComponentElement findComponentElement(Class<?> componentType) {
        return (ComponentElement) findElement(e -> 
            e instanceof ComponentElement ce && 
            componentType.isInstance(ce.getComponent())
        );
    }
    
    /**
     * Gets statistics about the tree.
     */
    public TreeStats getStats() {
        if (root == null) {
            return new TreeStats(0, 0, 0, 0);
        }
        
        int[] counts = {0, 0, 0, 0}; // total, components, leaves, groups
        
        // Count the root itself
        countElement(root, counts);
        
        // Count all descendants
        root.visitDescendants(e -> countElement(e, counts));
        
        return new TreeStats(counts[0], counts[1], counts[2], counts[3]);
    }
    
    private void countElement(Element e, int[] counts) {
        counts[0]++;
        if (e instanceof ComponentElement) counts[1]++;
        else if (e instanceof LeafElement) counts[2]++;
        else if (e instanceof GroupElement) counts[3]++;
    }
    
    /**
     * Tree statistics.
     */
    public record TreeStats(int totalElements, int componentElements, int leafElements, int groupElements) {
        @Override
        public String toString() {
            return String.format("TreeStats{total=%d, components=%d, leaves=%d, groups=%d}",
                totalElements, componentElements, leafElements, groupElements);
        }
    }
    
    /**
     * Dumps the tree structure for debugging.
     */
    public String dumpTree() {
        if (root == null) {
            return "(empty tree)";
        }
        
        StringBuilder sb = new StringBuilder();
        dumpElement(sb, root, 0);
        return sb.toString();
    }
    
    private void dumpElement(StringBuilder sb, Element element, int indent) {
        sb.append("  ".repeat(indent));
        
        // Element type
        String type = element.getClass().getSimpleName();
        sb.append(type);
        
        // Additional info
        if (element instanceof ComponentElement ce) {
            sb.append(" [").append(ce.getComponent().getClass().getSimpleName()).append("]");
            sb.append(" (signals=").append(ce.getSignalCount()).append(")");
        } else if (element instanceof LeafElement le) {
            sb.append(" [").append(le.getType()).append(": ").append(le.getContent()).append("]");
        } else if (element instanceof GroupElement ge) {
            sb.append(" [").append(ge.getLayout()).append("]");
        }
        
        if (element.isDirty()) {
            sb.append(" (dirty)");
        }
        
        sb.append("\n");
        
        // Children
        for (Element child : element.getChildren()) {
            dumpElement(sb, child, indent + 1);
        }
    }
}
