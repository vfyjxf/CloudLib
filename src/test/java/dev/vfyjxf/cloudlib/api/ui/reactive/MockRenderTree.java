package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A mock render tree implementation for testing the Compose-style DSL.
 * <p>
 * This class simulates how the RenderNode tree would be processed into an actual UI tree,
 * allowing us to verify the structure, styles, and content of the built UI.
 */
public class MockRenderTree {
    
    // ==================== Stateful Component Context Registry ====================
    
    /**
     * Registry that maintains component state across rebuilds.
     * This simulates how a real framework would preserve state for components.
     */
    public static class ComponentStateRegistry {
        private final Map<String, StatefulMockContext> contexts = new HashMap<>();
        private int anonymousCounter = 0;
        
        /**
         * Gets or creates a stateful context for a component.
         * @param componentKey unique key for the component (can be null for anonymous)
         */
        public StatefulMockContext getOrCreate(String componentKey) {
            String key = componentKey != null ? componentKey : "anon_" + (anonymousCounter++);
            return contexts.computeIfAbsent(key, k -> new StatefulMockContext());
        }
        
        /**
         * Gets or creates a child context for nested components.
         */
        public StatefulMockContext getChildContext(StatefulMockContext parent, String childKey) {
            String fullKey = parent.getContextId() + "/" + childKey;
            return contexts.computeIfAbsent(fullKey, k -> {
                StatefulMockContext child = new StatefulMockContext();
                child.setParent(parent);
                child.setContextId(fullKey);
                return child;
            });
        }
        
        public void clear() {
            contexts.clear();
            anonymousCounter = 0;
        }
        
        public int getContextCount() {
            return contexts.size();
        }
    }
    
    // Global registry for tests
    private static final ComponentStateRegistry GLOBAL_REGISTRY = new ComponentStateRegistry();
    
    public static ComponentStateRegistry getRegistry() {
        return GLOBAL_REGISTRY;
    }
    
    public static void resetRegistry() {
        GLOBAL_REGISTRY.clear();
    }
    
    // ==================== Stateful Mock ComponentContext ====================
    
    /**
     * A stateful mock ComponentContext that preserves state across rebuilds.
     * This is how a real framework would work - ctx.signal() returns the same
     * Signal instance on subsequent builds.
     */
    public static class StatefulMockContext implements ComponentContext {
        private String contextId = "root";
        private StatefulMockContext parent;
        private final Map<String, ComponentContext.Ref<?>> refs = new HashMap<>();
        
        // State slots - indexed by call order
        private final List<Signal<?>> signalSlots = new ArrayList<>();
        private final List<Computed<?>> computedSlots = new ArrayList<>();
        private final List<Object> memoSlots = new ArrayList<>();
        private final List<Object[]> memoDeps = new ArrayList<>();
        
        // Slot counters - reset on each build
        private int signalIndex = 0;
        private int computedIndex = 0;
        private int memoIndex = 0;
        
        // Child component counters
        private int childCounter = 0;
        private final Map<String, StatefulMockContext> children = new HashMap<>();
        
        public String getContextId() { return contextId; }
        public void setContextId(String id) { this.contextId = id; }
        public StatefulMockContext getParent() { return parent; }
        public void setParent(StatefulMockContext parent) { this.parent = parent; }
        
        /**
         * Call this before each rebuild to reset slot indices.
         */
        public void prepareForRebuild() {
            signalIndex = 0;
            computedIndex = 0;
            memoIndex = 0;
            childCounter = 0;
        }
        
        /**
         * Gets or creates a child context for an embedded component.
         */
        public StatefulMockContext getChildContext(String key) {
            String childKey = key != null ? key : "child_" + (childCounter++);
            return children.computeIfAbsent(childKey, k -> {
                StatefulMockContext child = new StatefulMockContext();
                child.setParent(this);
                child.setContextId(contextId + "/" + childKey);
                return child;
            });
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> Signal<T> signal(T initialValue) {
            if (signalIndex < signalSlots.size()) {
                // Return existing signal from previous build
                return (Signal<T>) signalSlots.get(signalIndex++);
            } else {
                // First build - create new signal
                Signal<T> signal = Signal.of(initialValue);
                signalSlots.add(signal);
                signalIndex++;
                return signal;
            }
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> Computed<T> computed(Supplier<T> computation) {
            if (computedIndex < computedSlots.size()) {
                return (Computed<T>) computedSlots.get(computedIndex++);
            } else {
                Computed<T> computed = Computed.of(computation);
                computedSlots.add(computed);
                computedIndex++;
                return computed;
            }
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> T memo(Supplier<T> factory, Object... dependencies) {
            if (memoIndex < memoSlots.size()) {
                Object[] oldDeps = memoDeps.get(memoIndex);
                if (!Arrays.equals(oldDeps, dependencies)) {
                    // Dependencies changed, recompute
                    T value = factory.get();
                    memoSlots.set(memoIndex, value);
                    memoDeps.set(memoIndex, dependencies.clone());
                }
                return (T) memoSlots.get(memoIndex++);
            } else {
                T value = factory.get();
                memoSlots.add(value);
                memoDeps.add(dependencies.clone());
                memoIndex++;
                return value;
            }
        }
        
        @Override
        public void effect(Runnable effect) {
            effect.run();
        }
        
        @Override
        public void effect(Supplier<Runnable> effect) {
            effect.get(); // Execute and get cleanup (ignored in tests)
        }
        
        @Override
        public void effect(Supplier<Runnable> effect, Object... dependencies) {
            effect.get();
        }
        
        @Override
        public void watch(ReactiveState<?> state, Runnable callback) {
            // No-op in tests
        }
        
        @Override
        public <T> void watch(ReactiveState<T> state, Consumer<T> callback) {
            // No-op in tests
        }
        
        @Override
        public void onMount(Runnable callback) {
            callback.run(); // Execute immediately in tests
        }
        
        @Override
        public void onUnmount(Runnable callback) {
            // No-op in tests
        }
        
        @Override
        public void onTick(Runnable callback) {
            // No-op in tests
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> Ref<T> ref(String name) {
            return (Ref<T>) refs.computeIfAbsent(name, k -> new MockRef<>());
        }
        
        @Override
        public <T> T provide(Providers.Key<T> key) {
            return null;
        }
        
        @Override
        public void invalidate() {
            // No-op in tests
        }
        
        // For debugging
        public int getSignalCount() { return signalSlots.size(); }
        public int getComputedCount() { return computedSlots.size(); }
        public int getChildCount() { return children.size(); }
        
        private static class MockRef<T> implements Ref<T> {
            private @Nullable T value;
            
            @Override
            public @Nullable T get() { return value; }
            
            @Override
            public void set(@Nullable T value) { this.value = value; }
        }
    }
    
    // ==================== Simple Mock ComponentContext (stateless) ====================
    
    /**
     * A simple stateless mock ComponentContext for basic testing.
     * Each call to signal() creates a new Signal - no state persistence.
     */
    public static class MockComponentContext implements ComponentContext {
        private final Map<String, ComponentContext.Ref<?>> refs = new HashMap<>();
        
        @Override
        public <T> Signal<T> signal(T initialValue) {
            return Signal.of(initialValue);
        }
        
        @Override
        public <T> Computed<T> computed(Supplier<T> computation) {
            return Computed.of(computation);
        }
        
        @Override
        public <T> T memo(Supplier<T> factory, Object... dependencies) {
            return factory.get();
        }
        
        @Override
        public void effect(Runnable effect) {
            // No-op in tests
        }
        
        @Override
        public void effect(Supplier<Runnable> effect) {
            // No-op in tests
        }
        
        @Override
        public void effect(Supplier<Runnable> effect, Object... dependencies) {
            // No-op in tests
        }
        
        @Override
        public void watch(ReactiveState<?> state, Runnable callback) {
            // No-op in tests
        }
        
        @Override
        public <T> void watch(ReactiveState<T> state, Consumer<T> callback) {
            // No-op in tests
        }
        
        @Override
        public void onMount(Runnable callback) {
            // No-op in tests
        }
        
        @Override
        public void onUnmount(Runnable callback) {
            // No-op in tests
        }
        
        @Override
        public void onTick(Runnable callback) {
            // No-op in tests
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> Ref<T> ref(String name) {
            return (Ref<T>) refs.computeIfAbsent(name, k -> new MockRef<>());
        }
        
        @Override
        public <T> T provide(Providers.Key<T> key) {
            return null;
        }
        
        @Override
        public void invalidate() {
            // No-op in tests
        }
        
        private static class MockRef<T> implements Ref<T> {
            private @Nullable T value;
            
            @Override
            public @Nullable T get() { return value; }
            
            @Override
            public void set(@Nullable T value) { this.value = value; }
        }
    }

    // ==================== Mock Node ====================
    
    /**
     * A mock UI node that simulates the actual rendered element.
     */
    public static class MockNode {
        private final String type;
        private final @Nullable Object content;
        private final @Nullable Style style;
        private final List<MockNode> children;
        private final Map<String, Object> attributes;
        private @Nullable MockNode parent;
        
        public MockNode(String type, @Nullable Object content, @Nullable Style style) {
            this.type = type;
            this.content = content;
            this.style = style;
            this.children = new ArrayList<>();
            this.attributes = new HashMap<>();
        }
        
        // Getters
        public String type() { return type; }
        public @Nullable Object content() { return content; }
        public @Nullable Style style() { return style; }
        public List<MockNode> children() { return Collections.unmodifiableList(children); }
        public @Nullable MockNode parent() { return parent; }
        public Map<String, Object> attributes() { return Collections.unmodifiableMap(attributes); }
        
        // Tree operations
        public void addChild(MockNode child) {
            children.add(child);
            child.parent = this;
        }
        
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        // Query methods
        public int childCount() {
            return children.size();
        }
        
        public int depth() {
            int d = 0;
            MockNode p = parent;
            while (p != null) {
                d++;
                p = p.parent;
            }
            return d;
        }
        
        public int totalNodeCount() {
            int count = 1;
            for (MockNode child : children) {
                count += child.totalNodeCount();
            }
            return count;
        }
        
        public @Nullable MockNode findFirst(Predicate<MockNode> predicate) {
            if (predicate.test(this)) return this;
            for (MockNode child : children) {
                MockNode found = child.findFirst(predicate);
                if (found != null) return found;
            }
            return null;
        }
        
        public List<MockNode> findAll(Predicate<MockNode> predicate) {
            List<MockNode> results = new ArrayList<>();
            collectAll(predicate, results);
            return results;
        }
        
        private void collectAll(Predicate<MockNode> predicate, List<MockNode> results) {
            if (predicate.test(this)) results.add(this);
            for (MockNode child : children) {
                child.collectAll(predicate, results);
            }
        }
        
        public @Nullable MockNode findByType(String type) {
            return findFirst(n -> n.type.equals(type));
        }
        
        public List<MockNode> findAllByType(String type) {
            return findAll(n -> n.type.equals(type));
        }
        
        public @Nullable MockNode findByContent(Object content) {
            return findFirst(n -> Objects.equals(n.content, content));
        }
        
        public List<MockNode> findAllTexts() {
            return findAll(n -> "text".equals(n.type));
        }
        
        public List<MockNode> findAllButtons() {
            return findAll(n -> "button".equals(n.type));
        }
        
        public List<String> getTextContents() {
            List<String> texts = new ArrayList<>();
            for (MockNode node : findAllTexts()) {
                if (node.content instanceof String s) {
                    texts.add(s);
                }
            }
            return texts;
        }
        
        // Style query helpers
        public int getBackgroundColor() {
            if (style == null) return 0;
            Style.Background bg = style.get(Style.Background.class);
            return bg != null ? bg.color() : 0;
        }
        
        public int getTextColor() {
            if (style == null) return 0;
            Style.Color c = style.get(Style.Color.class);
            return c != null ? c.value() : 0;
        }
        
        public int getPadding() {
            if (style == null) return 0;
            Style.Padding p = style.get(Style.Padding.class);
            return p != null ? p.left() : 0;
        }
        
        // Tree visualization
        public String toTreeString() {
            StringBuilder sb = new StringBuilder();
            toTreeString(sb, "", true);
            return sb.toString();
        }
        
        private void toTreeString(StringBuilder sb, String prefix, boolean isLast) {
            sb.append(prefix);
            sb.append(isLast ? "└── " : "├── ");
            sb.append(formatNode());
            sb.append("\n");
            
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            for (int i = 0; i < children.size(); i++) {
                children.get(i).toTreeString(sb, childPrefix, i == children.size() - 1);
            }
        }
        
        private String formatNode() {
            StringBuilder sb = new StringBuilder();
            sb.append(type);
            if (content != null) {
                String contentStr = content.toString();
                if (contentStr.length() > 30) {
                    contentStr = contentStr.substring(0, 27) + "...";
                }
                sb.append("(").append(contentStr).append(")");
            }
            if (style != null) {
                List<String> styleInfo = new ArrayList<>();
                Style.Background bg = style.get(Style.Background.class);
                Style.Color c = style.get(Style.Color.class);
                Style.Padding p = style.get(Style.Padding.class);
                if (bg != null && bg.color() != 0) styleInfo.add("bg=#" + Integer.toHexString(bg.color()));
                if (c != null && c.value() != 0) styleInfo.add("color=#" + Integer.toHexString(c.value()));
                if (p != null && p.left() != 0) styleInfo.add("pad=" + p.left());
                if (!styleInfo.isEmpty()) {
                    sb.append(" [").append(String.join(", ", styleInfo)).append("]");
                }
            }
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return formatNode();
        }
    }
    
    // ==================== Tree Builder ====================
    
    /**
     * Builds a MockNode tree from a RenderNode tree.
     */
    public static class TreeBuilder {
        private final List<BuildEvent> events = new ArrayList<>();
        private int nodeIdCounter = 0;
        
        /**
         * Builds a mock tree from a RenderNode.
         */
        public MockNode build(RenderNode renderNode) {
            events.clear();
            nodeIdCounter = 0;
            return buildNode(renderNode);
        }
        
        private MockNode buildNode(RenderNode node) {
            return switch (node) {
                case RenderNode.Empty() -> {
                    recordEvent("empty", null);
                    yield new MockNode("empty", null, null);
                }
                
                case RenderNode.Leaf(String type, Object content, Style style) -> {
                    recordEvent("leaf:" + type, content);
                    yield new MockNode(type, content, style);
                }
                
                case RenderNode.Group(LayoutType layout, List<RenderNode> children, Style style) -> {
                    String type = layout.name().toLowerCase();
                    recordEvent("group:" + type, null);
                    MockNode group = new MockNode(type, layout, style);
                    for (RenderNode child : children) {
                        MockNode childNode = buildNode(child);
                        group.addChild(childNode);
                    }
                    yield group;
                }
                
                case RenderNode.ComponentRef(Object key, Component component) -> {
                    recordEvent("component", key);
                    // Execute the component to get its render tree
                    RenderNode rendered = executeComponent(component);
                    MockNode result = buildNode(rendered);
                    result.setAttribute("componentKey", key);
                    yield result;
                }
                
                case RenderNode.Dynamic(var supplier) -> {
                    recordEvent("dynamic", null);
                    // Evaluate the dynamic content
                    RenderNode result = supplier.get();
                    yield buildNode(result);
                }
                
                case RenderNode.Conditional(var condition, RenderNode whenTrue, RenderNode whenFalse) -> {
                    boolean value = condition.get();
                    recordEvent("conditional", value);
                    if (value) {
                        yield buildNode(whenTrue);
                    } else if (whenFalse != null) {
                        yield buildNode(whenFalse);
                    } else {
                        yield new MockNode("empty", null, null);
                    }
                }
                
                case RenderNode.ForEach<?> forEach -> {
                    recordEvent("foreach", null);
                    MockNode container = new MockNode("foreach-container", null, null);
                    int index = 0;
                    for (Object item : forEach.items().get()) {
                        @SuppressWarnings("unchecked")
                        RenderNode.ItemRenderer<Object> renderer = (RenderNode.ItemRenderer<Object>) forEach.renderer();
                        RenderNode itemNode = renderer.render(item, index++);
                        container.addChild(buildNode(itemNode));
                    }
                    yield container;
                }
                
                case RenderNode.Slot(String name, RenderNode fallback) -> {
                    recordEvent("slot:" + name, null);
                    // For testing, just use the fallback
                    if (fallback != null) {
                        yield buildNode(fallback);
                    }
                    yield new MockNode("slot", name, null);
                }
                
                case RenderNode.ScrollArea scrollArea -> {
                    recordEvent("scrollarea", null);
                    MockNode container = new MockNode("scrollarea", null, null);
                    container.addChild(buildNode(scrollArea.content()));
                    yield container;
                }
                
                case RenderNode.PopupWithConfigurator popup -> {
                    recordEvent("popup", popup.title());
                    MockNode container = new MockNode("popup", popup.title(), null);
                    container.addChild(buildNode(popup.content()));
                    yield container;
                }
                
                case RenderNode.Popup popup -> {
                    recordEvent("popup", popup.title());
                    MockNode container = new MockNode("popup", popup.title(), null);
                    container.addChild(buildNode(popup.content()));
                    yield container;
                }
                
                case RenderNode.Floating floating -> {
                    recordEvent("floating", null);
                    MockNode container = new MockNode("floating", null, null);
                    container.addChild(buildNode(floating.content()));
                    yield container;
                }
            };
        }
        
        private RenderNode executeComponent(Component component) {
            // Create a mock context and render the component
            ComponentContext mockCtx = new MockComponentContext();
            return component.render(mockCtx);
        }
        
        private void recordEvent(String type, @Nullable Object data) {
            events.add(new BuildEvent(nodeIdCounter++, type, data));
        }
        
        public List<BuildEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }
        
        /**
         * Static convenience method to build from RenderNode.
         */
        public static MockNode buildFromRenderNode(RenderNode node) {
            return new TreeBuilder().build(node);
        }
        
        public record BuildEvent(int nodeId, String type, @Nullable Object data) {}
    }
    
    // ==================== Test Assertions ====================
    
    /**
     * Fluent assertions for MockNode.
     */
    public static class NodeAssertions {
        private final MockNode node;
        
        public NodeAssertions(MockNode node) {
            this.node = Objects.requireNonNull(node, "Node cannot be null");
        }
        
        public static NodeAssertions assertThat(MockNode node) {
            return new NodeAssertions(node);
        }
        
        public NodeAssertions hasType(String expectedType) {
            if (!node.type().equals(expectedType)) {
                throw new AssertionError("Expected type '" + expectedType + "' but was '" + node.type() + "'");
            }
            return this;
        }
        
        public NodeAssertions hasContent(Object expectedContent) {
            if (!Objects.equals(node.content(), expectedContent)) {
                throw new AssertionError("Expected content '" + expectedContent + "' but was '" + node.content() + "'");
            }
            return this;
        }
        
        public NodeAssertions hasChildCount(int expected) {
            if (node.childCount() != expected) {
                throw new AssertionError("Expected " + expected + " children but had " + node.childCount());
            }
            return this;
        }
        
        public NodeAssertions hasMinChildCount(int min) {
            if (node.childCount() < min) {
                throw new AssertionError("Expected at least " + min + " children but had " + node.childCount());
            }
            return this;
        }
        
        public NodeAssertions hasTotalNodeCount(int expected) {
            int actual = node.totalNodeCount();
            if (actual != expected) {
                throw new AssertionError("Expected " + expected + " total nodes but had " + actual);
            }
            return this;
        }
        
        public NodeAssertions containsText(String text) {
            List<String> texts = node.getTextContents();
            if (!texts.contains(text)) {
                throw new AssertionError("Expected to find text '" + text + "' but texts were: " + texts);
            }
            return this;
        }
        
        public NodeAssertions containsTexts(String... expectedTexts) {
            List<String> texts = node.getTextContents();
            for (String expected : expectedTexts) {
                if (!texts.contains(expected)) {
                    throw new AssertionError("Expected to find text '" + expected + "' but texts were: " + texts);
                }
            }
            return this;
        }
        
        public NodeAssertions hasButtonCount(int expected) {
            int actual = node.findAllButtons().size();
            if (actual != expected) {
                throw new AssertionError("Expected " + expected + " buttons but had " + actual);
            }
            return this;
        }
        
        public NodeAssertions hasTextCount(int expected) {
            int actual = node.findAllTexts().size();
            if (actual != expected) {
                throw new AssertionError("Expected " + expected + " texts but had " + actual);
            }
            return this;
        }
        
        public NodeAssertions hasNodeOfType(String type) {
            if (node.findByType(type) == null) {
                throw new AssertionError("Expected to find node of type '" + type + "' but none found");
            }
            return this;
        }
        
        public NodeAssertions hasNodesOfType(String type, int count) {
            int actual = node.findAllByType(type).size();
            if (actual != count) {
                throw new AssertionError("Expected " + count + " nodes of type '" + type + "' but had " + actual);
            }
            return this;
        }
        
        public NodeAssertions hasBackgroundColor(int expected) {
            int actual = node.getBackgroundColor();
            if (actual != expected) {
                throw new AssertionError("Expected background #" + Integer.toHexString(expected) 
                    + " but was #" + Integer.toHexString(actual));
            }
            return this;
        }
        
        public NodeAssertions hasStyle() {
            if (node.style() == null) {
                throw new AssertionError("Expected node to have style but it was null");
            }
            return this;
        }
        
        public NodeAssertions matches(Predicate<MockNode> predicate, String description) {
            if (!predicate.test(node)) {
                throw new AssertionError("Node did not match: " + description);
            }
            return this;
        }
        
        public MockNode getNode() {
            return node;
        }
        
        public NodeAssertions child(int index) {
            if (index < 0 || index >= node.childCount()) {
                throw new AssertionError("Child index " + index + " out of bounds (size: " + node.childCount() + ")");
            }
            return new NodeAssertions(node.children().get(index));
        }
        
        public NodeAssertions firstChild() {
            return child(0);
        }
        
        public NodeAssertions lastChild() {
            return child(node.childCount() - 1);
        }
        
        // ===== Structure Verification =====
        
        /**
         * Verifies the tree matches the expected structure.
         * Format: "type" or "type(content)" or "type[child1, child2, ...]"
         */
        public NodeAssertions hasStructure(StructureSpec spec) {
            verifyStructure(node, spec, "");
            return this;
        }
        
        private void verifyStructure(MockNode actual, StructureSpec expected, String path) {
            // Check type
            if (!expected.type.equals(actual.type())) {
                throw new AssertionError(
                    "At path '" + path + "': expected type '" + expected.type + "' but was '" + actual.type() + "'");
            }
            
            // Check content if specified
            if (expected.content != null) {
                if (!expected.content.equals(actual.content())) {
                    throw new AssertionError(
                        "At path '" + path + "': expected content '" + expected.content + "' but was '" + actual.content() + "'");
                }
            }
            
            // Check children count if children are specified
            if (expected.children != null) {
                if (expected.children.size() != actual.childCount()) {
                    throw new AssertionError(
                        "At path '" + path + "': expected " + expected.children.size() + 
                        " children but had " + actual.childCount() + "\n" +
                        "Expected: " + expected + "\n" +
                        "Actual tree:\n" + actual.toTreeString());
                }
                
                // Verify each child
                for (int i = 0; i < expected.children.size(); i++) {
                    verifyStructure(
                        actual.children().get(i),
                        expected.children.get(i),
                        path + "/" + expected.type + "[" + i + "]"
                    );
                }
            }
            
            // Check attributes if specified
            if (expected.attributes != null) {
                for (var entry : expected.attributes.entrySet()) {
                    Object actualValue = actual.attributes().get(entry.getKey());
                    if (!Objects.equals(entry.getValue(), actualValue)) {
                        throw new AssertionError(
                            "At path '" + path + "': expected attribute '" + entry.getKey() + 
                            "' = '" + entry.getValue() + "' but was '" + actualValue + "'");
                    }
                }
            }
        }
    }
    
    // ==================== Structure Specification ====================
    
    /**
     * Specifies expected tree structure for verification.
     */
    public static class StructureSpec {
        final String type;
        @Nullable Object content;
        @Nullable List<StructureSpec> children;
        @Nullable Map<String, Object> attributes;
        
        private StructureSpec(String type) {
            this.type = type;
        }
        
        public static StructureSpec node(String type) {
            return new StructureSpec(type);
        }
        
        public static StructureSpec text(String content) {
            return new StructureSpec("text").content(content);
        }
        
        public static StructureSpec button(String label) {
            return new StructureSpec("button").content(label);
        }
        
        public static StructureSpec column(StructureSpec... children) {
            return new StructureSpec("column").children(children);
        }
        
        public static StructureSpec row(StructureSpec... children) {
            return new StructureSpec("row").children(children);
        }
        
        public static StructureSpec stack(StructureSpec... children) {
            return new StructureSpec("stack").children(children);
        }
        
        public StructureSpec content(@Nullable Object content) {
            this.content = content;
            return this;
        }
        
        public StructureSpec children(StructureSpec... children) {
            this.children = Arrays.asList(children);
            return this;
        }
        
        public StructureSpec attr(String key, Object value) {
            if (this.attributes == null) {
                this.attributes = new HashMap<>();
            }
            this.attributes.put(key, value);
            return this;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(type);
            if (content != null) {
                sb.append("(").append(content).append(")");
            }
            if (children != null && !children.isEmpty()) {
                sb.append("[");
                for (int i = 0; i < children.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(children.get(i));
                }
                sb.append("]");
            }
            return sb.toString();
        }
    }
    
    // ==================== Tree Differ ====================
    
    /**
     * Computes differences between two mock trees.
     */
    public static class TreeDiffer {
        
        public static List<Diff> diff(MockNode oldTree, MockNode newTree) {
            List<Diff> diffs = new ArrayList<>();
            diffNodes(oldTree, newTree, "", diffs);
            return diffs;
        }
        
        private static void diffNodes(MockNode oldNode, MockNode newNode, String path, List<Diff> diffs) {
            // Check type change
            if (!oldNode.type().equals(newNode.type())) {
                diffs.add(new Diff(DiffType.TYPE_CHANGED, path, oldNode.type(), newNode.type()));
            }
            
            // Check content change
            if (!Objects.equals(oldNode.content(), newNode.content())) {
                diffs.add(new Diff(DiffType.CONTENT_CHANGED, path, oldNode.content(), newNode.content()));
            }
            
            // Check style change
            if (!Objects.equals(oldNode.style(), newNode.style())) {
                diffs.add(new Diff(DiffType.STYLE_CHANGED, path, oldNode.style(), newNode.style()));
            }
            
            // Check children
            int oldSize = oldNode.childCount();
            int newSize = newNode.childCount();
            
            // Compare existing children
            int minSize = Math.min(oldSize, newSize);
            for (int i = 0; i < minSize; i++) {
                diffNodes(oldNode.children().get(i), newNode.children().get(i), path + "/" + i, diffs);
            }
            
            // Removed children
            for (int i = minSize; i < oldSize; i++) {
                diffs.add(new Diff(DiffType.CHILD_REMOVED, path + "/" + i, oldNode.children().get(i), null));
            }
            
            // Added children
            for (int i = minSize; i < newSize; i++) {
                diffs.add(new Diff(DiffType.CHILD_ADDED, path + "/" + i, null, newNode.children().get(i)));
            }
        }
        
        public enum DiffType {
            TYPE_CHANGED,
            CONTENT_CHANGED,
            STYLE_CHANGED,
            CHILD_ADDED,
            CHILD_REMOVED
        }
        
        public record Diff(DiffType type, String path, @Nullable Object oldValue, @Nullable Object newValue) {
            @Override
            public String toString() {
                return switch (type) {
                    case TYPE_CHANGED -> "TYPE_CHANGED at " + path + ": " + oldValue + " -> " + newValue;
                    case CONTENT_CHANGED -> "CONTENT_CHANGED at " + path + ": " + oldValue + " -> " + newValue;
                    case STYLE_CHANGED -> "STYLE_CHANGED at " + path;
                    case CHILD_ADDED -> "CHILD_ADDED at " + path + ": " + newValue;
                    case CHILD_REMOVED -> "CHILD_REMOVED at " + path + ": " + oldValue;
                };
            }
        }
    }
    
    // ==================== Convenience Methods ====================
    
    /**
     * Builds a mock tree from a component (stateless - each build creates new state).
     */
    public static MockNode buildFrom(Component component) {
        // Create a mock context and render the component
        ComponentContext mockCtx = new MockComponentContext();
        RenderNode renderNode = component.render(mockCtx);
        return new TreeBuilder().build(renderNode);
    }
    
    /**
     * Builds a mock tree from a component with a stateful context.
     * The context preserves state across multiple builds, simulating real framework behavior.
     * 
     * @param component the component to build
     * @param ctx the stateful context (should be reused across rebuilds)
     * @return the built tree
     */
    public static MockNode buildStateful(Component component, StatefulMockContext ctx) {
        ctx.prepareForRebuild();
        RenderNode renderNode = component.render(ctx);
        return new TreeBuilder().build(renderNode);
    }
    
    /**
     * Creates a stateful component builder that maintains state across rebuilds.
     * Use this to test components with internal state that persists.
     */
    public static StatefulComponentBuilder statefulBuilder(Component component) {
        return new StatefulComponentBuilder(component);
    }
    
    /**
     * Helper class for building components with persistent state.
     */
    public static class StatefulComponentBuilder {
        private final Component component;
        private final StatefulMockContext rootContext;
        private MockNode lastTree;
        
        public StatefulComponentBuilder(Component component) {
            this.component = component;
            this.rootContext = new StatefulMockContext();
        }
        
        /**
         * Builds (or rebuilds) the component tree.
         * State from ctx.signal() calls is preserved across builds.
         */
        public MockNode build() {
            rootContext.prepareForRebuild();
            RenderNode renderNode = component.render(rootContext);
            lastTree = new TreeBuilder().build(renderNode);
            return lastTree;
        }
        
        /**
         * Gets the last built tree without rebuilding.
         */
        public MockNode getLastTree() {
            return lastTree;
        }
        
        /**
         * Gets the root context for inspection.
         */
        public StatefulMockContext getContext() {
            return rootContext;
        }
        
        /**
         * Gets the number of signals created by this component.
         */
        public int getSignalCount() {
            return rootContext.getSignalCount();
        }
    }
    
    /**
     * Builds a mock tree from a build block using Compose-style DSL.
     * The block should use Column/Row/etc at the top level.
     */
    public static MockNode buildFrom(Runnable buildBlock) {
        // The buildBlock should return a RenderNode via Column/Row/etc
        // Wrap in a Column to capture the result
        RenderNode renderNode = Render.Column(buildBlock);
        return new TreeBuilder().build(renderNode);
    }
    
    /**
     * Builds and asserts on a mock tree.
     */
    public static NodeAssertions assertTree(Runnable buildBlock) {
        return NodeAssertions.assertThat(buildFrom(buildBlock));
    }
    
    /**
     * Prints the tree structure to stdout.
     */
    public static void printTree(MockNode node) {
        System.out.println(node.toTreeString());
    }
}
