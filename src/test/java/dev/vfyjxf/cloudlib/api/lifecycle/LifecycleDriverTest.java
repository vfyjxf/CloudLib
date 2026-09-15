package dev.vfyjxf.cloudlib.api.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleDriverTest {

    private static final LifecycleStates states = LifecycleStates.create("test");
    private static final LifecycleState<String> context = states.context("context", String.class);
    private static final LifecycleState<Void> tags = states.event("tags");
    private static final LifecycleState<Void> recipes = states.event("recipes");

    @Test
    void loadsAfterAllRequiredContextsAndEventsAreSatisfied() {
        List<String> calls = new ArrayList<>();
        LifecycleDriver driver = driver(calls);
        LifecycleReceiver receiver = driver.receiver();

        receiver.event(tags, "tags");
        receiver.context(context, "one", "context");

        assertFalse(driver.isLoaded());
        assertEquals(List.of("test.recipes"), driver.missing().stream().map(LifecycleState::id).toList());

        receiver.event(recipes, "recipes");

        assertTrue(driver.isLoaded());
        assertEquals(List.of("load"), calls);
    }

    @Test
    void completedEventCycleReloadsLoadedDriver() {
        List<String> calls = new ArrayList<>();
        LifecycleReceiver receiver = driver(calls).receiver();

        receiver.context(context, "one", "context");
        receiver.event(tags, "tags");
        receiver.event(recipes, "recipes");
        receiver.event(tags, "tags");

        assertEquals(List.of("load"), calls);

        receiver.event(recipes, "recipes");

        assertEquals(List.of("load", "unload", "load"), calls);
    }

    @Test
    void clearingContextUnloadsAndBlocksFutureEvents() {
        List<String> calls = new ArrayList<>();
        LifecycleDriver driver = driver(calls);
        LifecycleReceiver receiver = driver.receiver();

        receiver.context(context, "one", "context");
        receiver.event(tags, "tags");
        receiver.event(recipes, "recipes");
        receiver.clear(context, "contextLost");
        receiver.event(tags, "tags");
        receiver.event(recipes, "recipes");

        assertFalse(driver.isLoaded());
        assertEquals(List.of("load", "unload"), calls);
    }

    @Test
    void changingContextValueUnloadsAndClearsPendingEvents() {
        List<String> calls = new ArrayList<>();
        LifecycleDriver driver = driver(calls);
        LifecycleReceiver receiver = driver.receiver();

        receiver.context(context, "one", "context");
        receiver.event(tags, "tags");
        receiver.event(recipes, "recipes");
        receiver.event(tags, "tags");
        receiver.context(context, "two", "contextChanged");
        receiver.event(recipes, "recipes");

        assertFalse(driver.isLoaded());
        assertEquals(List.of("test.tags"), driver.missing().stream().map(LifecycleState::id).toList());
        assertEquals(List.of("load", "unload"), calls);
    }

    @Test
    void sourceCanBindToTheReceiver() {
        List<String> calls = new ArrayList<>();

        LifecycleDriver.builder("test-source")
                .require(context)
                .require(tags)
                .source(receiver -> {
                    receiver.context(context, "one", "context");
                    receiver.event(tags, "tags");
                })
                .onLoad(() -> calls.add("load"))
                .build();

        assertEquals(List.of("load"), calls);
    }

    private static LifecycleDriver driver(List<String> calls) {
        return LifecycleDriver.builder("test")
                .require(context)
                .require(tags)
                .require(recipes)
                .onLoad(() -> calls.add("load"))
                .onUnload(() -> calls.add("unload"))
                .build();
    }
}
