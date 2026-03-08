package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.InputContext;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Records events fired on a widget for later assertion.
 * Eliminates the AtomicBoolean/AtomicInteger/List boilerplate
 * that is otherwise needed in every event-verification test.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * var rec = EventRecorder.on(button);
 * scene.tap("button");
 * rec.assertFired("click");
 * rec.assertNotFired("drag");
 * rec.assertFiredTimes("click", 1);
 * }</pre>
 *
 * <h3>Supports</h3>
 * <ul>
 *   <li>mouseClicked, mouseReleased, mouseClick (logical click)</li>
 *   <li>mouseDragged, mouseScrolled</li>
 *   <li>keyPressed, keyReleased, charTyped</li>
 * </ul>
 */
public final class EventRecorder {

    /**
     * A single recorded event occurrence.
     */
    public record Entry(String name, String phase, Map<String, Object> data) {
        @Override
        public String toString() {
            var sb = new StringBuilder(name);
            if (phase != null) sb.append("(").append(phase).append(")");
            if (!data.isEmpty()) sb.append(" ").append(data);
            return sb.toString();
        }
    }

    private final Widget widget;
    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private EventDispatch defaultReturn = EventDispatch.pass;

    private EventRecorder(Widget widget) {
        this.widget = widget;
    }

    /**
     * Creates a recorder and attaches all input event listeners to the widget.
     * Call this BEFORE {@code scene.setup()} so listeners are available when events fire.
     */
    public static EventRecorder on(Widget widget) {
        var rec = new EventRecorder(widget);
        rec.attach();
        return rec;
    }

    /**
     * Creates a recorder that consumes all events it receives.
     */
    public static EventRecorder consuming(Widget widget) {
        var rec = new EventRecorder(widget);
        rec.defaultReturn = EventDispatch.consumed;
        rec.attach();
        return rec;
    }

    private void attach() {
        widget.onMouseClicked((input, context) -> {
            record("mouseClicked", context.phase().name(), inputData(input));
            return defaultReturn;
        });
        widget.onMouseReleased((input, context) -> {
            record("mouseReleased", context.phase().name(), inputData(input));
            return defaultReturn;
        });
        widget.onMouseClick((input, clickCount, context) -> {
            var data = inputData(input);
            data.put("clickCount", clickCount);
            record("click", context.phase().name(), data);
            return defaultReturn;
        });
        widget.onMouseDragged((input, deltaX, deltaY, context) -> {
            var data = inputData(input);
            data.put("deltaX", deltaX);
            data.put("deltaY", deltaY);
            record("drag", context.phase().name(), data);
            return defaultReturn;
        });
        widget.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("scrollX", scrollX);
            data.put("scrollY", scrollY);
            record("scroll", context.phase().name(), data);
            return defaultReturn;
        });
        widget.onKeyPressed((input, context) -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("key", input.key().getValue());
            record("keyPressed", context.phase().name(), data);
            return defaultReturn;
        });
        widget.onKeyReleased((input, context) -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("key", input.key().getValue());
            record("keyReleased", context.phase().name(), data);
            return defaultReturn;
        });
        widget.onCharTyped((codePoint, modifiers, context) -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("char", (char) codePoint);
            record("charTyped", context.phase().name(), data);
            return defaultReturn;
        });
    }

    private void record(String name, String phase, Map<String, Object> data) {
        entries.add(new Entry(name, phase, data));
    }

    private static LinkedHashMap<String, Object> inputData(InputContext input) {
        var data = new LinkedHashMap<String, Object>();
        data.put("button", input.key().getValue());
        data.put("mouseX", input.mouseX());
        data.put("mouseY", input.mouseY());
        return data;
    }

    // ==================== Queries ====================

    /** All recorded entries in order. */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Entries matching a specific event name (e.g., "click", "mouseClicked"). */
    public List<Entry> entriesOf(String name) {
        return entries.stream().filter(e -> e.name().equals(name)).toList();
    }

    /** Total number of times any event was recorded. */
    public int totalCount() {
        return entries.size();
    }

    /** Number of times this event name was recorded. */
    public int count(String name) {
        return (int) entries.stream().filter(e -> e.name().equals(name)).count();
    }

    /** Whether this event name was recorded at least once. */
    public boolean wasFired(String name) {
        return entries.stream().anyMatch(e -> e.name().equals(name));
    }

    /** The sequence of event names, in order. */
    public List<String> eventNames() {
        return entries.stream().map(Entry::name).toList();
    }

    /** Returns the last recorded entry, or null if empty. */
    public Entry lastEntry() {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    /** Returns the first recorded entry, or null if empty. */
    public Entry firstEntry() {
        return entries.isEmpty() ? null : entries.get(0);
    }

    /** Returns the last entry matching the given event name, or null. */
    public Entry lastEntryOf(String name) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).name().equals(name)) return entries.get(i);
        }
        return null;
    }

    /** Clears all recorded entries. Useful between test phases. */
    public EventRecorder clear() {
        entries.clear();
        return this;
    }

    // ==================== Assertions ====================

    /** Assert that an event was fired at least once. */
    public EventRecorder assertFired(String name) {
        assertTrue(wasFired(name),
                "Expected event '" + name + "' to have been fired on " + widgetDesc()
                        + ". Recorded events: " + eventNames());
        return this;
    }

    /** Assert that an event was NOT fired. */
    public EventRecorder assertNotFired(String name) {
        assertFalse(wasFired(name),
                "Expected event '" + name + "' to NOT have been fired on " + widgetDesc()
                        + ". But it was fired " + count(name) + " time(s)");
        return this;
    }

    /** Assert that an event was fired exactly N times. */
    public EventRecorder assertFiredTimes(String name, int expectedCount) {
        assertEquals(expectedCount, count(name),
                "Expected event '" + name + "' to fire " + expectedCount + " time(s) on " + widgetDesc()
                        + ". Recorded events: " + eventNames());
        return this;
    }

    /** Assert that an event was fired at least N times. */
    public EventRecorder assertFiredAtLeast(String name, int minCount) {
        assertTrue(count(name) >= minCount,
                "Expected event '" + name + "' to fire at least " + minCount + " time(s) on " + widgetDesc()
                        + ", but fired " + count(name) + " time(s)");
        return this;
    }

    /** Assert the exact sequence of event names fired. */
    public EventRecorder assertOrder(String... expectedNames) {
        assertEquals(List.of(expectedNames), eventNames(),
                "Event order mismatch on " + widgetDesc());
        return this;
    }

    /** Assert that no events were fired at all. */
    public EventRecorder assertNothingFired() {
        assertTrue(entries.isEmpty(),
                "Expected no events on " + widgetDesc()
                        + ", but recorded: " + eventNames());
        return this;
    }

    /** Assert that exactly these event names were fired (in any order), and nothing else. */
    public EventRecorder assertOnlyFired(String... expectedNames) {
        var expected = new HashSet<>(List.of(expectedNames));
        var actual = new HashSet<>(eventNames());
        assertEquals(expected, actual,
                "Event set mismatch on " + widgetDesc()
                        + ". Expected: " + expected + ", Actual: " + actual);
        return this;
    }

    /** Assert that this event was fired with specific data values. */
    public EventRecorder assertFiredWith(String name, Predicate<Entry> matcher) {
        var matching = entries.stream()
                .filter(e -> e.name().equals(name))
                .filter(matcher)
                .toList();
        assertFalse(matching.isEmpty(),
                "Expected event '" + name + "' matching predicate on " + widgetDesc()
                        + ". Entries of '" + name + "': " + entriesOf(name));
        return this;
    }

    /** Returns a readable summary of all recorded events. */
    public String summary() {
        if (entries.isEmpty()) return widgetDesc() + ": (no events)";
        var sb = new StringBuilder(widgetDesc()).append(":\n");
        for (int i = 0; i < entries.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(entries.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String widgetDesc() {
        var key = widget.key();
        return widget.getClass().getSimpleName() + (key != null ? "[" + key + "]" : "");
    }
}
