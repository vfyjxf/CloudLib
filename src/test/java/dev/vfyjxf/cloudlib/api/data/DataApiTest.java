package dev.vfyjxf.cloudlib.api.data;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Data API (DataKey, DataContainer, DataAttachable).
 */
@DisplayName("Data API Tests")
class DataApiTest {

    //region Test Keys
    static final DataKey<String> NAME = DataKey.create(Namespace.ofMc("name"));
    static final DataKey<Integer> COUNT = DataKey.create(Namespace.ofMc("count"), 0);
    static final DataKey<List<String>> ITEMS = DataKey.create(Namespace.ofMc("items"), ArrayList::new);
    static final DataKey<String> COMPUTED = DataKey.createComputed(Namespace.ofMc("computed"),
        holder -> holder != null ? "holder-present" : "no-holder");
    //endregion

    @Nested
    @DisplayName("DataKey Tests")
    class DataKeyTests {

        @Test
        @DisplayName("Simple key has no default value")
        void simpleKeyNoDefault() {
            assertFalse(NAME.hasDefaultValue());
            assertNull(NAME.defaultValue(null));
        }

        @Test
        @DisplayName("Key with constant default value")
        void keyWithConstantDefault() {
            assertTrue(COUNT.hasDefaultValue());
            assertEquals(0, COUNT.defaultValue(null));
        }

        @Test
        @DisplayName("Key with supplier default creates new instances")
        void keyWithSupplierDefault() {
            assertTrue(ITEMS.hasDefaultValue());
            List<String> list1 = ITEMS.defaultValue(null);
            List<String> list2 = ITEMS.defaultValue(null);
            assertNotSame(list1, list2, "Supplier should create new instances");
        }

        @Test
        @DisplayName("Key with context-aware default")
        void keyWithContextAwareDefault() {
            assertEquals("no-holder", COMPUTED.defaultValue(null));

            DataAttachable holder = new SimpleHolder();
            assertEquals("holder-present", COMPUTED.defaultValue(holder));
        }

        @Test
        @DisplayName("Keys with same id are equal")
        void keysWithSameIdAreEqual() {
            DataKey<String> key1 = DataKey.create(Namespace.ofMc("test"));
            DataKey<String> key2 = DataKey.create(Namespace.ofMc("test"));
            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }
    }

    @Nested
    @DisplayName("DataContainer Tests")
    class DataContainerTests {

        private DataContainer container;

        @BeforeEach
        void setUp() {
            container = new DataContainer();
        }

        @Test
        @DisplayName("set and get basic operations")
        void setAndGet() {
            container.set(NAME, "test-name");
            assertEquals("test-name", container.get(NAME));
        }

        @Test
        @DisplayName("get returns default value when not set")
        void getReturnsDefault() {
            assertEquals(0, container.get(COUNT));
        }

        @Test
        @DisplayName("get returns null for key without default")
        void getReturnsNullForNoDefault() {
            assertNull(container.get(NAME));
        }

        @Test
        @DisplayName("find returns Optional without using defaults")
        void findReturnsOptionalWithoutDefaults() {
            assertTrue(container.find(COUNT).isEmpty(), "find should not use defaults");

            container.set(COUNT, 42);
            assertEquals(42, container.find(COUNT).orElse(-1));
        }

        @Test
        @DisplayName("require throws when not present")
        void requireThrowsWhenNotPresent() {
            assertThrows(IllegalStateException.class, () -> container.require(NAME));
        }

        @Test
        @DisplayName("require returns value when present")
        void requireReturnsWhenPresent() {
            container.set(NAME, "present");
            assertEquals("present", container.require(NAME));
        }

        @Test
        @DisplayName("remove returns removed value")
        void removeReturnsValue() {
            container.set(NAME, "to-remove");
            assertEquals("to-remove", container.remove(NAME));
            assertNull(container.get(NAME));
        }

        @Test
        @DisplayName("computeIfAbsent only computes when absent")
        void computeIfAbsentOnlyComputesWhenAbsent() {
            AtomicInteger counter = new AtomicInteger(0);

            String result1 = container.computeIfAbsent(NAME, () -> {
                counter.incrementAndGet();
                return "computed";
            });
            assertEquals("computed", result1);
            assertEquals(1, counter.get());

            String result2 = container.computeIfAbsent(NAME, () -> {
                counter.incrementAndGet();
                return "should-not-run";
            });
            assertEquals("computed", result2);
            assertEquals(1, counter.get(), "Supplier should not run when value present");
        }

        @Test
        @DisplayName("update creates and transforms value")
        void updateCreatesAndTransforms() {
            int result = container.update(COUNT, 10, v -> v + 5);
            assertEquals(15, result);

            result = container.update(COUNT, 0, v -> v * 2);
            assertEquals(30, result);
        }

        @Test
        @DisplayName("has checks for explicit presence")
        void hasChecksExplicitPresence() {
            assertFalse(container.has(COUNT), "has should return false even with default");

            container.set(COUNT, 0);
            assertTrue(container.has(COUNT));
        }

        @Test
        @DisplayName("clear removes all data")
        void clearRemovesAll() {
            container.set(NAME, "test");
            container.set(COUNT, 42);

            container.clear();

            assertTrue(container.isEmpty());
            assertEquals(0, container.size());
        }
    }

    @Nested
    @DisplayName("DataHolder Integration Tests")
    class DataHolderTests {

        private SimpleHolder holder;

        @BeforeEach
        void setUp() {
            holder = new SimpleHolder();
        }

        @Test
        @DisplayName("Convenience methods delegate to container")
        void convenienceMethodsDelegate() {
            holder.set(NAME, "holder-test");
            assertEquals("holder-test", holder.get(NAME));
            assertTrue(holder.has(NAME));

            holder.remove(NAME);
            assertFalse(holder.has(NAME));
        }

        @Test
        @DisplayName("Context-aware defaults receive holder")
        void contextAwareDefaultsReceiveHolder() {
            DataKey<String> contextKey = DataKey.createComputed(Namespace.ofMc("context"),
                h -> h instanceof SimpleHolder ? "simple" : "other");

            holder.data().setOwner(holder);
            assertEquals("simple", holder.get(contextKey));
        }

        @Test
        @DisplayName("computeIfAbsent works through holder")
        void computeIfAbsentThroughHolder() {
            List<String> list = holder.computeIfAbsent(ITEMS, ArrayList::new);
            list.add("item1");

            List<String> sameList = holder.get(ITEMS);
            assertTrue(sameList.contains("item1"));
        }
    }

    // Simple DataHolder implementation for testing
    static class SimpleHolder implements DataAttachable {
        private final DataContainer container = new DataContainer(this);

        @Override
        public DataContainer data() {
            return container;
        }
    }
}
