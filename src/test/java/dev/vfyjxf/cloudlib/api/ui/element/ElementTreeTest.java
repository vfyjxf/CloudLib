package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.*;
import org.junit.jupiter.api.*;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Element system.
 */
public class ElementTreeTest {
    
    @Test
    @DisplayName("ElementTree creates and mounts leaf elements")
    void testLeafElement() {
        RenderNode widget = text("Hello");
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(widget);
        
        assertNotNull(tree.getRoot());
        assertTrue(tree.getRoot() instanceof LeafElement);
        
        ElementTree.TreeStats stats = tree.getStats();
        assertEquals(1, stats.totalElements());
        assertEquals(1, stats.leafElements());
    }
    
    @Test
    @DisplayName("ElementTree creates group elements")
    void testGroupElement() {
        RenderNode widget = column(
            text("Line 1"),
            text("Line 2"),
            text("Line 3")
        );
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(widget);
        
        assertTrue(tree.getRoot() instanceof GroupElement);
        
        ElementTree.TreeStats stats = tree.getStats();
        assertEquals(4, stats.totalElements()); // 1 group + 3 leaves
        assertEquals(1, stats.groupElements());
        assertEquals(3, stats.leafElements());
    }
    
    @Test
    @DisplayName("ElementTree creates component elements")
    void testComponentElement() {
        Component counter = Component.stateful(ctx -> {
            var count = ctx.signal(0);
            return text("Count: " + count.get());
        });
        
        RenderNode widget = RenderNode.component(counter);
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(widget);
        
        assertTrue(tree.getRoot() instanceof ComponentElement);
        
        ElementTree.TreeStats stats = tree.getStats();
        assertEquals(2, stats.totalElements()); // 1 component + 1 leaf
        assertEquals(1, stats.componentElements());
    }
    
    @Test
    @DisplayName("ComponentElement tracks dependencies")
    void testDependencyTracking() {
        Signal<Integer> externalCount = Signal.of(0);
        
        Component counter = Component.stateful(ctx -> {
            // Read external signal
            int count = externalCount.get();
            return text("External: " + count);
        });
        
        RenderNode widget = RenderNode.component(counter);
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(widget);
        
        ComponentElement ce = (ComponentElement) tree.getRoot();
        
        // Component should track the external signal
        assertTrue(ce.getTrackedDependencies().contains(externalCount));
    }
    
    @Test
    @DisplayName("ComponentElement manages state across rebuilds")
    void testStateManagement() {
        Component counter = Component.stateful(ctx -> {
            var count = ctx.signal(0);
            return button("Click", () -> count.update(n -> n + 1));
        });
        
        RenderNode widget = RenderNode.component(counter);
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(widget);
        
        ComponentElement ce = (ComponentElement) tree.getRoot();
        assertEquals(1, ce.getSignalCount());
    }
    
    @Test
    @DisplayName("Nested component hierarchy")
    void testNestedComponents() {
        Component inner = Component.stateless(ctx -> text("Inner"));
        Component outer = Component.stateful(ctx -> 
            column(
                text("Header"),
                RenderNode.component(inner),
                text("Footer")
            )
        );
        
        RenderNode widget = RenderNode.component(outer);
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(widget);
        
        ElementTree.TreeStats stats = tree.getStats();
        assertEquals(2, stats.componentElements()); // outer + inner
        assertEquals(3, stats.leafElements()); // Header + Inner text + Footer
    }
    
    @Test
    @DisplayName("BuildOwner schedules and processes builds")
    void testBuildOwner() {
        Component counter = Component.stateful(ctx -> {
            var count = ctx.signal(0);
            return text("Count: " + count.get());
        });
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(RenderNode.component(counter));
        
        BuildOwner owner = tree.getBuildOwner();
        assertNotNull(owner);
        
        // Mark root dirty
        tree.getRoot().markNeedsBuild();
        
        // Process builds
        owner.buildScope();
    }
}
