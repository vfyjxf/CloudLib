package dev.vfyjxf.cloudlib.api.data.serialize;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SerializerManagementTest {

    private static HolderLookup.Provider provider() {
        //an empty provider suffices for primitive codecs (INT/STRING), which do not consult registries
        return HolderLookup.Provider.create(Stream.empty());
    }

    @Test
    void roundTripsThroughCompoundTag() {
        var management = new SerializerManagement();
        Handle<Integer> count = Handle.of(0);
        Handle<String> label = Handle.of("");
        management.register(Serialize.create("count", count, Codec.INT));
        management.register(Serialize.create("label", label, Codec.STRING));

        count.set(42);
        label.set("hello");
        assertTrue(count.dirty());

        HolderLookup.Provider ra = provider();
        CompoundTag saved = new CompoundTag();
        management.saveAll(saved, ra);
        assertTrue(saved.contains("count"));
        assertTrue(saved.contains("label"));

        //load into fresh handles — must be silent (no dirty, no listeners)
        Handle<Integer> count2 = Handle.of(0);
        Handle<String> label2 = Handle.of("");
        var management2 = new SerializerManagement();
        management2.register(Serialize.create("count", count2, Codec.INT));
        management2.register(Serialize.create("label", label2, Codec.STRING));
        management2.loadAll(saved, ra);

        assertEquals(42, count2.get());
        assertEquals("hello", label2.get());
        assertFalse(count2.dirty()); //silent load
    }

    @Test
    void duplicateNameRejected() {
        var management = new SerializerManagement();
        management.register(Serialize.create("x", Handle.of(0), Codec.INT));
        assertThrows(IllegalArgumentException.class,
                () -> management.register(Serialize.create("x", Handle.of(0), Codec.INT)));
    }

    @Test
    void loadIsSilentForHandles() {
        var management = new SerializerManagement();
        Handle<Integer> h = Handle.of(0);
        int[] fires = {0};
        h.onChange(v -> fires[0]++);
        management.register(Serialize.create("n", h, Codec.INT));

        CompoundTag tag = new CompoundTag();
        tag.putInt("n", 7);
        management.loadAll(tag, provider());

        assertEquals(7, h.get());
        assertEquals(0, fires[0]); //load must not fire listeners
        assertFalse(h.dirty());
    }

    @Test
    void hasSerializersAndEmptyManagement() {
        var management = new SerializerManagement();
        assertFalse(management.hasSerializers());
        management.register(Serialize.create("a", Handle.of(0), Codec.INT));
        assertTrue(management.hasSerializers());

        //save/load of an empty tag is a no-op
        management.loadAll(new CompoundTag(), provider());
    }
}
