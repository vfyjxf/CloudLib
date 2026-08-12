package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HandleTest {

    @Test
    void setMarksDirtyAndFiresListeners() {
        Handle<Integer> h = Handle.of(0);
        List<Integer> seen = new ArrayList<>();
        h.onChange(v -> seen.add(v));

        assertFalse(h.dirty());
        h.set(1);
        assertEquals(1, h.get());
        assertTrue(h.dirty());
        assertEquals(List.of(1), seen);

        h.clearDirty();
        assertFalse(h.dirty());
    }

    @Test
    void setUnchangedIsNoOp() {
        Handle<String> h = Handle.of("a", CheckStrategy.equals());
        AtomicInteger fires = new AtomicInteger();
        h.onChange(v -> fires.incrementAndGet());

        h.set("a"); //unchanged
        assertEquals(0, fires.get());
        assertFalse(h.dirty());

        h.set("b");
        assertEquals(1, fires.get());
        assertTrue(h.dirty());
    }

    @Test
    void applyFiresWithoutDirty() {
        Handle<Integer> h = Handle.of(0);
        List<Integer> seen = new ArrayList<>();
        h.onChange(v -> seen.add(v));

        h.apply(5);
        assertEquals(5, h.get());
        assertFalse(h.dirty());        //no dirty
        assertEquals(List.of(5), seen); //listeners fired
    }

    @Test
    void applyUnchangedIsNoOp() {
        Handle<Integer> h = Handle.of(5);
        AtomicInteger fires = new AtomicInteger();
        h.onChange(v -> fires.incrementAndGet());
        h.apply(5);
        assertEquals(0, fires.get());
    }

    @Test
    void loadIsSilent() {
        Handle<Integer> h = Handle.of(0);
        AtomicInteger fires = new AtomicInteger();
        h.onChange(v -> fires.incrementAndGet());

        h.load(9);
        assertEquals(9, h.get());
        assertFalse(h.dirty());
        assertEquals(0, fires.get());
    }

    @Test
    void loadDoesNotClearExistingDirty() {
        Handle<Integer> h = Handle.of(0);
        h.set(1);
        assertTrue(h.dirty());

        h.load(2); //a load mid-tick must not suppress an already-queued change
        assertTrue(h.dirty());
        assertEquals(2, h.get());
    }

    @Test
    void pairListenerReceivesPrevAndCurrent() {
        Handle<Integer> h = Handle.of(10);
        List<int[]> pairs = new ArrayList<>();
        h.onChange((prev, cur) -> pairs.add(new int[]{prev, cur}));

        h.set(20);
        assertEquals(1, pairs.size());
        assertEquals(10, pairs.get(0)[0]);
        assertEquals(20, pairs.get(0)[1]);
    }

    @Test
    void subscriptionUnregisters() {
        Handle<Integer> h = Handle.of(0);
        AtomicInteger count = new AtomicInteger();
        Subscription sub = h.onChange(v -> count.incrementAndGet());

        h.set(1);
        assertEquals(1, count.get());

        sub.unsubscribe();
        h.set(2);
        assertEquals(1, count.get()); //not fired after unsubscribe
    }

    @Test
    void changedEqualsDirty() {
        Handle<Integer> h = Handle.of(0);
        assertFalse(h.changed());
        h.set(1);
        assertTrue(h.changed());
        h.clearDirty();
        assertFalse(h.changed());
    }

    @Test
    void readOnlyIsAReadOnlyHandle() {
        Handle<Integer> h = Handle.of(0);
        ReadOnlyHandle<Integer> ro = h.readOnly();
        AtomicInteger seen = new AtomicInteger();
        ro.onChange(v -> seen.set(v));
        h.set(7); //fires on the read-only view too (shared events)
        assertEquals(7, ro.get());
        assertEquals(7, seen.get());
    }

    @Test
    void diffHandleUnionsValueChange() {
        var value = new TestDiffObservable();
        DiffHandle<TestDiffObservable, String> h = DiffHandle.of(value);

        assertFalse(h.dirty());
        assertFalse(h.changed());

        value.markChanged("delta");
        //in-place mutation surfaces via value.changed() even though handle.set was never called
        assertTrue(h.changed());
        assertEquals("delta", h.difference());

        //difference() resets the value's internal state
        assertFalse(value.changed());
    }

    @Test
    void diffHandleDirtyAlsoSurfaces() {
        var value = new TestDiffObservable();
        DiffHandle<TestDiffObservable, String> h = DiffHandle.of(value);

        h.set(value); //set to the same reference: strategy is observable() -> value.changed() is false -> no-op
        //force a real change by setting a new value
        var value2 = new TestDiffObservable();
        h.set(value2);
        assertTrue(h.dirty());
        assertTrue(h.changed());
        h.clearDirty();
        //now value2 has no change; union is false
        assertFalse(h.changed());
    }

    @Test
    void diffHandleToleratesNullValue() {
        DiffHandle<TestDiffObservable, String> h = DiffHandle.of(new TestDiffObservable());
        h.load(null); //store null
        assertNull(h.get());
        assertFalse(h.changed()); //must not NPE on get().changed()

        h.set(new TestDiffObservable()); //set a non-null value (previous null -> strategy says changed)
        assertTrue(h.dirty());
        h.clearDirty();

        h.load(null); //back to null
        assertFalse(h.changed()); //dirty false, value null -> false, no NPE
    }

    @Test
    void reentrantSetDoesNotCorruptIteration() {
        Handle<Integer> h = Handle.of(0);
        List<Integer> seen = new ArrayList<>();
        int[] depth = {0};
        h.onChange(v -> {
            seen.add(v);
            if (depth[0]++ == 0) {
                h.set(2); //reentrant set during fire()
            }
        });
        h.set(1);
        assertEquals(2, h.get());
        assertTrue(seen.contains(1));
        assertTrue(seen.contains(2));
    }

    @Test
    void unsubscribeDuringCallbackIsSafe() {
        Handle<Integer> h = Handle.of(0);
        List<Integer> seen = new ArrayList<>();
        Subscription[] sub = new Subscription[1];
        sub[0] = h.onChange(v -> {
            seen.add(v);
            sub[0].unsubscribe(); //self-unsubscribe during fire()
        });
        h.set(1);
        assertEquals(List.of(1), seen);
        h.set(2); //no longer subscribed
        assertEquals(List.of(1), seen);
    }

    static class TestDiffObservable implements DiffObservable<String> {
        private boolean changed;
        private String diff = "";

        void markChanged(String d) {
            changed = true;
            diff = d;
        }

        @Override
        public boolean changed() {
            return changed;
        }

        @Override
        public String difference() {
            changed = false;
            String d = diff;
            diff = "";
            return d;
        }
    }
}
