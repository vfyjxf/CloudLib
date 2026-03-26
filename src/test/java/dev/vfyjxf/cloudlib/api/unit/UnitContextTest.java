package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnitContextTest {

    private static final ContextKey<String> MOD_ID = ContextKey.create(
            Namespace.ofMc("mod_id"),
            String.class
    );
    private static final ContextKey<Integer> TIER = ContextKey.create(
            Namespace.ofMc("tier"),
            Integer.class
    );

    @Test
    void freezeCreatesDetachedSnapshot() {
        MutableConvertContext mutable = MutableConvertContext.create();
        mutable.set(MOD_ID, "cloudlib");

        ConvertContext snapshot = mutable.freeze();
        mutable.set(MOD_ID, "other");
        mutable.set(TIER, 3);

        assertEquals("cloudlib", snapshot.require(MOD_ID));
        assertTrue(snapshot.find(TIER).isEmpty());
    }

    @Test
    void typedReadFollowsContextKeyType() {
        MutableConvertContext mutable = MutableConvertContext.create();
        mutable.set(TIER, 8);

        ConvertContext snapshot = mutable.freeze();
        int tier = snapshot.require(TIER);

        assertEquals(8, tier);
    }

    @Test
    void frozenSnapshotMapIsImmutable() {
        MutableConvertContext mutable = MutableConvertContext.create();
        mutable.set(MOD_ID, "cloudlib");

        ConvertContext snapshot = mutable.freeze();

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.entries().put(MOD_ID, "other")
        );
    }
}
