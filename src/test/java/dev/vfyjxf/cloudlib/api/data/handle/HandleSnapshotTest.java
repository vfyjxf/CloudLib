package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandleSnapshotTest {

    @Test
    void currentStateReflectsDirtyAndNeverIllegal() {
        Handle<Integer> h = Handle.of(0);
        Snapshot<Integer> snap = Snapshot.HandleSnapshot.of(h);

        assertEquals(Snapshot.State.unchanged, snap.currentState(h.get()));
        h.set(1);
        assertEquals(Snapshot.State.changed, snap.currentState(h.get()));
        assertNotEquals(Snapshot.State.illegal, snap.currentState(h.get()));
    }

    @Test
    void updateStateReturnsChangedAndClearsDirty() {
        Handle<Integer> h = Handle.of(0);
        Snapshot.HandleSnapshot<Integer> snap = Snapshot.HandleSnapshot.of(h);

        h.set(1);
        assertTrue(snap.updateState(h.get()));
        assertFalse(h.dirty());
        //second call: nothing pending
        assertFalse(snap.updateState(h.get()));
    }

    @Test
    void forceUpdateClearsDirty() {
        Handle<Integer> h = Handle.of(0);
        Snapshot.HandleSnapshot<Integer> snap = Snapshot.HandleSnapshot.of(h);
        h.set(1);
        snap.forceUpdateState(h.get());
        assertFalse(h.dirty());
    }

    @Test
    void mutableIsTrue() {
        assertTrue(Snapshot.HandleSnapshot.of(Handle.of(0)).mutable());
    }

    @Test
    void readValueIsHandleGet() {
        Handle<Integer> h = Handle.of(42);
        assertEquals(42, Snapshot.HandleSnapshot.of(h).readValue());
        assertEquals(42, Snapshot.HandleSnapshot.of(h).value());
    }
}
