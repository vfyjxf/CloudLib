package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.AbstractElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement.ElementVisitor;
import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Flutter-style component system.
 */
@DisplayName("Component System Tests")
class ComponentTest {

    // ==================== StatelessComponent Tests ====================

    @Nested
    @DisplayName("StatelessComponent")
    class StatelessComponentTests {

        @Test
        @DisplayName("should build when mounted")
        void buildWhenMounted() {
            AtomicInteger buildCount = new AtomicInteger(0);
            
            StatelessComponent component = new StatelessComponent() {
                @Override
                protected Blueprint build(BuildContext context) {
                    buildCount.incrementAndGet();
                    return new EmptyBlueprint();
                }
            };
            
            UIElement<?> element = component.createElement();
            element.mount(null, null);
            
            assertEquals(1, buildCount.get());
        }

        @Test
        @DisplayName("should receive context in build")
        void receiveContextInBuild() {
            List<BuildContext> contexts = new ArrayList<>();
            
            StatelessComponent component = new StatelessComponent() {
                @Override
                protected Blueprint build(BuildContext context) {
                    contexts.add(context);
                    return new EmptyBlueprint();
                }
            };
            
            UIElement<?> element = component.createElement();
            element.mount(null, null);
            
            assertEquals(1, contexts.size());
            assertNotNull(contexts.get(0));
        }

        @Test
        @DisplayName("should use constructor for props")
        void useConstructorForProps() {
            List<String> capturedMessages = new ArrayList<>();
            
            StatelessComponent component = new MessageComponent("Hello", capturedMessages);
            
            component.createElement().mount(null, null);
            
            assertEquals(List.of("Hello"), capturedMessages);
        }

        @Test
        @DisplayName("default canUpdate should compare class type")
        void canUpdateByClassType() {
            StatelessComponent comp1 = new StatelessComponent() {
                @Override
                protected Blueprint build(BuildContext context) {
                    return new EmptyBlueprint();
                }
            };
            
            StatelessComponent comp2 = new StatelessComponent() {
                @Override
                protected Blueprint build(BuildContext context) {
                    return new EmptyBlueprint();
                }
            };
            
            // Anonymous classes have different class types
            assertFalse(comp1.canUpdate(comp2));
            
            // Same instance should match
            assertTrue(comp1.canUpdate(comp1));
        }
    }

    // ==================== StatefulComponent Tests ====================

    @Nested
    @DisplayName("StatefulComponent")
    class StatefulComponentTests {

        @Test
        @DisplayName("should create state on mount")
        void createStateOnMount() {
            AtomicInteger createCount = new AtomicInteger(0);
            
            StatefulComponent<SimpleState> component = new SimpleStatefulComponent(createCount, null);
            UIElement<?> element = component.createElement();
            element.mount(null, null);
            
            assertEquals(1, createCount.get());
        }

        @Test
        @DisplayName("should call initState once")
        void callInitStateOnce() {
            AtomicInteger initCount = new AtomicInteger(0);
            
            StatefulComponent<InitTrackingState> component = new InitTrackingComponent(initCount);
            StatefulComponentElement<?, ?> element = 
                (StatefulComponentElement<?, ?>) component.createElement();
            element.mount(null, null);
            
            assertEquals(1, initCount.get());
            
            // Rebuild should not call initState again
            element.performRebuild();
            assertEquals(1, initCount.get());
        }

        @Test
        @DisplayName("should access component props in state")
        void accessPropsInState() {
            List<String> capturedNames = new ArrayList<>();
            
            StatefulComponent<GreetingState> component = new GreetingComponent("Bob", capturedNames);
            component.createElement().mount(null, null);
            
            assertEquals(List.of("Bob"), capturedNames);
        }

        @Test
        @DisplayName("should call dispose on unmount")
        void callDisposeOnUnmount() {
            AtomicInteger disposeCount = new AtomicInteger(0);
            
            StatefulComponent<DisposeTrackingState> component = new DisposeTrackingComponent(disposeCount);
            UIElement<?> element = component.createElement();
            element.mount(null, null);
            
            assertEquals(0, disposeCount.get());
            
            element.unmount();
            assertEquals(1, disposeCount.get());
        }

        @Test
        @DisplayName("should track reactive state changes")
        void trackReactiveStateChanges() {
            AtomicInteger buildCount = new AtomicInteger(0);
            
            StatefulComponent<CounterState> component = new CounterComponent(buildCount);
            StatefulComponentElement<?, ?> element = 
                (StatefulComponentElement<?, ?>) component.createElement();
            element.mount(null, null);
            
            assertEquals(1, buildCount.get());
            
            // Change state
            CounterState state = (CounterState) element.getState();
            state.increment();
            
            // Rebuild (in real app, this would be automatic)
            element.performRebuild();
            assertEquals(2, buildCount.get());
        }

        @Test
        @DisplayName("should call didUpdateComponent when props change")
        void callDidUpdateComponentOnPropsChange() {
            List<String> updates = new ArrayList<>();
            
            ConfigComponent component1 = new ConfigComponent("A", updates);
            @SuppressWarnings("unchecked")
            StatefulComponentElement<ConfigComponent, ConfigState> element = 
                (StatefulComponentElement<ConfigComponent, ConfigState>) component1.createElement();
            element.mount(null, null);
            
            assertTrue(updates.isEmpty(), "No updates on initial mount");
            
            // Update with new component
            ConfigComponent component2 = new ConfigComponent("B", updates);
            element.update(component2);
            
            assertEquals(List.of("A -> B"), updates);
        }

        @Test
        @DisplayName("createSignal should work in state")
        void createSignalInState() {
            StatefulComponent<SignalUsingState> component = new SignalUsingComponent();
            StatefulComponentElement<?, ?> element = 
                (StatefulComponentElement<?, ?>) component.createElement();
            element.mount(null, null);
            
            SignalUsingState state = (SignalUsingState) element.getState();
            assertNotNull(state.count);
            assertEquals(42, state.count.get());
        }

        @Test
        @DisplayName("setState should trigger rebuild request")
        void setStateTriggersRebuild() {
            AtomicInteger buildCount = new AtomicInteger(0);
            
            StatefulComponent<SetStateTestState> component = new SetStateTestComponent(buildCount);
            StatefulComponentElement<?, ?> element = 
                (StatefulComponentElement<?, ?>) component.createElement();
            element.mount(null, null);
            
            assertEquals(1, buildCount.get());
            
            // Use setState
            SetStateTestState state = (SetStateTestState) element.getState();
            state.incrementAndRebuild();
            
            // Force rebuild (in real app BuildOwner would do this)
            element.performRebuild();
            assertEquals(2, buildCount.get());
        }
    }

    // ==================== Mixed Usage Tests ====================

    @Nested
    @DisplayName("Mixed Functional and Class-based")
    class MixedUsageTests {

        @Test
        @DisplayName("class components should work as child of functional")
        void classAsChildOfFunctional() {
            // This just verifies the type system works for mixing
            StatelessComponent child = new StatelessComponent() {
                @Override
                protected Blueprint build(BuildContext context) {
                    return new EmptyBlueprint();
                }
            };
            
            // ChildComponent can be used as a Blueprint
            Blueprint blueprint = child;
            assertNotNull(blueprint.createElement());
        }
    }

    // ==================== Helper Component Classes ====================
    
    // Stateless component with props
    private static class MessageComponent extends StatelessComponent {
        private final String message;
        private final List<String> capturedMessages;
        
        MessageComponent(String message, List<String> capturedMessages) {
            this.message = message;
            this.capturedMessages = capturedMessages;
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            capturedMessages.add(message);
            return new EmptyBlueprint();
        }
    }
    
    // Simple stateful component for create count test
    private static class SimpleStatefulComponent extends StatefulComponent<SimpleState> {
        private final AtomicInteger createCount;
        private final AtomicInteger buildCount;
        
        SimpleStatefulComponent(AtomicInteger createCount, AtomicInteger buildCount) {
            this.createCount = createCount;
            this.buildCount = buildCount;
        }
        
        @Override
        protected SimpleState createState() {
            createCount.incrementAndGet();
            return new SimpleState(buildCount);
        }
    }
    
    private static class SimpleState extends ComponentState<SimpleStatefulComponent> {
        private final AtomicInteger buildCount;
        
        SimpleState(AtomicInteger buildCount) {
            this.buildCount = buildCount;
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            if (buildCount != null) buildCount.incrementAndGet();
            return new EmptyBlueprint();
        }
    }
    
    // Init tracking component
    private static class InitTrackingComponent extends StatefulComponent<InitTrackingState> {
        private final AtomicInteger initCount;
        
        InitTrackingComponent(AtomicInteger initCount) {
            this.initCount = initCount;
        }
        
        @Override
        protected InitTrackingState createState() {
            return new InitTrackingState(initCount);
        }
    }
    
    private static class InitTrackingState extends ComponentState<InitTrackingComponent> {
        private final AtomicInteger initCount;
        
        InitTrackingState(AtomicInteger initCount) {
            this.initCount = initCount;
        }
        
        @Override
        protected void initState() {
            initCount.incrementAndGet();
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            return new EmptyBlueprint();
        }
    }
    
    // Greeting component for props access test
    private static class GreetingComponent extends StatefulComponent<GreetingState> {
        private final String name;
        private final List<String> capturedNames;
        
        GreetingComponent(String name, List<String> capturedNames) {
            this.name = name;
            this.capturedNames = capturedNames;
        }
        
        String getName() { return name; }
        
        @Override
        protected GreetingState createState() {
            return new GreetingState(capturedNames);
        }
    }
    
    private static class GreetingState extends ComponentState<GreetingComponent> {
        private final List<String> capturedNames;
        
        GreetingState(List<String> capturedNames) {
            this.capturedNames = capturedNames;
        }
        
        @Override
        protected void initState() {
            capturedNames.add(getComponent().getName());
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            return new EmptyBlueprint();
        }
    }
    
    // Dispose tracking component
    private static class DisposeTrackingComponent extends StatefulComponent<DisposeTrackingState> {
        private final AtomicInteger disposeCount;
        
        DisposeTrackingComponent(AtomicInteger disposeCount) {
            this.disposeCount = disposeCount;
        }
        
        @Override
        protected DisposeTrackingState createState() {
            return new DisposeTrackingState(disposeCount);
        }
    }
    
    private static class DisposeTrackingState extends ComponentState<DisposeTrackingComponent> {
        private final AtomicInteger disposeCount;
        
        DisposeTrackingState(AtomicInteger disposeCount) {
            this.disposeCount = disposeCount;
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            return new EmptyBlueprint();
        }
        
        @Override
        protected void dispose() {
            disposeCount.incrementAndGet();
        }
    }
    
    // Counter component for reactive tracking test
    private static class CounterComponent extends StatefulComponent<CounterState> {
        private final AtomicInteger buildCount;
        
        CounterComponent(AtomicInteger buildCount) {
            this.buildCount = buildCount;
        }
        
        @Override
        protected CounterState createState() {
            return new CounterState(buildCount);
        }
    }
    
    private static class CounterState extends ComponentState<CounterComponent> {
        private final AtomicInteger buildCount;
        private final Signal<Integer> count = Signal.of(0);
        
        CounterState(AtomicInteger buildCount) {
            this.buildCount = buildCount;
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            buildCount.incrementAndGet();
            int c = count.get();
            return new TextBlueprint("Count: " + c);
        }
        
        void increment() {
            count.update(n -> n + 1);
        }
    }
    
    // Config component for didUpdateComponent test
    private static class ConfigComponent extends StatefulComponent<ConfigState> {
        private final String value;
        private final List<String> updates;
        
        ConfigComponent(String value, List<String> updates) {
            this.value = value;
            this.updates = updates;
        }
        
        String getValue() { return value; }
        
        @Override
        protected ConfigState createState() {
            return new ConfigState(updates);
        }
    }
    
    private static class ConfigState extends ComponentState<ConfigComponent> {
        private final List<String> updates;
        
        ConfigState(List<String> updates) {
            this.updates = updates;
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            return new EmptyBlueprint();
        }
        
        @Override
        protected void didUpdateComponent(ConfigComponent oldComponent) {
            updates.add(oldComponent.getValue() + " -> " + getComponent().getValue());
        }
    }
    
    // Signal using component
    private static class SignalUsingComponent extends StatefulComponent<SignalUsingState> {
        @Override
        protected SignalUsingState createState() {
            return new SignalUsingState();
        }
    }
    
    private static class SignalUsingState extends ComponentState<SignalUsingComponent> {
        Signal<Integer> count;
        
        @Override
        protected void initState() {
            count = createSignal(42);
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            return new TextBlueprint("Value: " + count.get());
        }
    }
    
    // SetState test component
    private static class SetStateTestComponent extends StatefulComponent<SetStateTestState> {
        private final AtomicInteger buildCount;
        
        SetStateTestComponent(AtomicInteger buildCount) {
            this.buildCount = buildCount;
        }
        
        @Override
        protected SetStateTestState createState() {
            return new SetStateTestState(buildCount);
        }
    }
    
    private static class SetStateTestState extends ComponentState<SetStateTestComponent> {
        private final AtomicInteger buildCount;
        private int count = 0;
        
        SetStateTestState(AtomicInteger buildCount) {
            this.buildCount = buildCount;
        }
        
        @Override
        protected Blueprint build(BuildContext context) {
            buildCount.incrementAndGet();
            return new TextBlueprint("Count: " + count);
        }
        
        void incrementAndRebuild() {
            setState(() -> count++);
        }
    }

    // ==================== Helper Blueprint/Element Classes ====================

    private static class EmptyBlueprint implements Blueprint {
        @Override
        public UIElement<?> createElement() {
            return new EmptyElement(this);
        }

        @Override
        public boolean canUpdate(Blueprint other) {
            return other instanceof EmptyBlueprint;
        }
    }

    private static class TextBlueprint implements Blueprint {
        private final String text;

        TextBlueprint(String text) {
            this.text = text;
        }

        String getText() { return text; }

        @Override
        public UIElement<?> createElement() {
            return new TextElement(this);
        }

        @Override
        public boolean canUpdate(Blueprint other) {
            return other instanceof TextBlueprint;
        }
    }

    private static class EmptyElement extends AbstractElement<EmptyBlueprint> {
        public EmptyElement(EmptyBlueprint blueprint) {
            super(blueprint);
        }

        @Override
        protected void build() {}

        @Override
        protected void unmountChildren() {}

        @Override
        public void visitChildren(ElementVisitor visitor) {}
    }

    private static class TextElement extends AbstractElement<TextBlueprint> {
        public TextElement(TextBlueprint blueprint) {
            super(blueprint);
        }

        @Override
        protected void build() {}

        @Override
        protected void unmountChildren() {}

        @Override
        public void visitChildren(ElementVisitor visitor) {}
    }
}
