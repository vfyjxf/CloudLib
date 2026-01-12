package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.UIContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused unit tests for {@link ScopedReceiver}.
 * <p>
 * Keep this file small and high-signal: it only verifies scope collection and nesting rules.
 */
class ScopedReceiverTest {

    private static final class SimpleBlueprint implements Blueprint<Widget> {
        private final String id;

        private SimpleBlueprint(String id) {
            this.id = id;
        }

        @Override
        public Widget createWidget(UIContext context) {
            return new Widget();
        }

        @Override
        public void updateWidget(Widget widget, UIContext context) {
            // no-op
        }

        @Override
        public String toString() {
            return "SimpleBlueprint{" + id + "}";
        }
    }

    @Test
    void collects_added_blueprints_in_order() {
        List<Blueprint<?>> blueprints = ScopedReceiver.buildChildren(() -> {
            ScopedReceiver.add(new SimpleBlueprint("a"));
            ScopedReceiver.add(new SimpleBlueprint("b"));
            ScopedReceiver.add(new SimpleBlueprint("c"));
        });

        assertEquals(3, blueprints.size());
        assertEquals("SimpleBlueprint{a}", blueprints.get(0).toString());
        assertEquals("SimpleBlueprint{b}", blueprints.get(1).toString());
        assertEquals("SimpleBlueprint{c}", blueprints.get(2).toString());
    }

    @Test
    void adding_outside_scope_is_a_noop() {
        // No active scope: should not throw, should not attach anywhere.
        ScopedReceiver.add(new SimpleBlueprint("outside"));

        List<Blueprint<?>> blueprints = ScopedReceiver.buildChildren(() -> {
            ScopedReceiver.add(new SimpleBlueprint("inside"));
        });

        assertEquals(1, blueprints.size());
        assertEquals("SimpleBlueprint{inside}", blueprints.get(0).toString());
    }

    @Test
    void nested_scopes_do_not_leak_into_each_other() {
        List<Blueprint<?>> outer = ScopedReceiver.buildChildren(() -> {
            ScopedReceiver.add(new SimpleBlueprint("outer-1"));

            List<Blueprint<?>> inner = ScopedReceiver.buildChildren(() -> {
                ScopedReceiver.add(new SimpleBlueprint("inner-1"));
                ScopedReceiver.add(new SimpleBlueprint("inner-2"));
            });

            assertEquals(2, inner.size());
            assertEquals("SimpleBlueprint{inner-1}", inner.get(0).toString());
            assertEquals("SimpleBlueprint{inner-2}", inner.get(1).toString());

            ScopedReceiver.add(new SimpleBlueprint("outer-2"));
        });

        assertEquals(2, outer.size());
        assertEquals("SimpleBlueprint{outer-1}", outer.get(0).toString());
        assertEquals("SimpleBlueprint{outer-2}", outer.get(1).toString());
    }
}

