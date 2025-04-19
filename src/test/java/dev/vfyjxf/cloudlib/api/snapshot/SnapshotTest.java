package dev.vfyjxf.cloudlib.api.snapshot;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ExtendWith(EphemeralTestServerProvider.class)
class SnapshotTest {

    private static class TestHolder {
        //primitive value
        private int number = 0;
        //immutable reference
        private final List<String> strings = new ArrayList<>();
        private final List<String> copyOfStrings = new ArrayList<>();
        //mutable reference
        private NamedObject name;
        //readonly value
        private final NamedObject readonlyName = new NamedObject("readonly");
        //illegal value
        private final Reference<List<String>> illegal = new Reference<>(new ArrayList<>());
    }

    private record Expose<T>(Snapshot<T> snapshot, Supplier<T> supplier) {

        public Snapshot.State state() {
            return snapshot.currentState(supplier.get());
        }

        public boolean changed() {
            return Snapshot.changed(snapshot, supplier.get());
        }
    }

    private record NamedObject(String name) {
    }

    private static class Reference<T> {
        public Reference(T value) {
            this.value = value;
        }

        private T value;
    }

    @BeforeEach
    void setup() {

    }

    @Test
    void serverTick(MinecraftServer server) {
        TestHolder testHolder = new TestHolder();
        Expose<Integer> copyOf = new Expose<>(
                Snapshot.copyOf(Integer::intValue, CheckStrategy.primitive()),
                () -> testHolder.number
        );
        Expose<@NotNull List<String>> copyOfList = new Expose<>(
                Snapshot.copyOf(ArrayList::new, CheckStrategy.equals()),
                () -> testHolder.copyOfStrings
        );
        Expose<@NotNull NamedObject> mutableRef = new Expose<>(
                Snapshot.mutableRefOf(CheckStrategy.equals()),
                () -> testHolder.name
        );
        Expose<@NotNull List<String>> immutableRef = new Expose<>(
                Snapshot.immutableRefOf(testHolder.strings, new Predicate<>() {
                    private List<String> copy = new ArrayList<>(testHolder.strings);

                    @Override
                    public boolean test(List<String> strings) {
                        if (copy.size() != strings.size()) {
                            //fast fail
                            copy = new ArrayList<>(strings);
                            return false;
                        }
                        for (int i = 0; i < strings.size(); i++) {
                            if (!copy.get(i).equals(strings.get(i))) {
                                copy = new ArrayList<>(strings);
                                return false;
                            }
                        }
                        return true;
                    }
                }),
                () -> testHolder.strings
        );
        copyOfList.changed();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        AtomicBoolean copyOfFlag = new AtomicBoolean(false);
        AtomicBoolean mutableRefFlag = new AtomicBoolean(false);
        AtomicBoolean copyOfListFlag = new AtomicBoolean(false);
        AtomicBoolean immutableRefFlag = new AtomicBoolean(false);
        NeoForge.EVENT_BUS.addListener(
                ServerTickEvent.Pre.class,
                event -> {
                    int i = random.nextInt(0, 1000);
                    //stimulate the value change
                    var oldInt = testHolder.number;
                    testHolder.number = random.nextInt(0, 100);
                    copyOfFlag.set(oldInt != testHolder.number);
                    var oldName = testHolder.name;
                    switch (i % 5) {
                        case 0 -> testHolder.name = new NamedObject("A");
                        case 1 -> testHolder.name = new NamedObject("B");
                        case 2 -> testHolder.name = new NamedObject("C");
                        case 3 -> testHolder.name = new NamedObject("D");
                        case 4 -> testHolder.name = new NamedObject("E");
                    }
                    mutableRefFlag.set(!testHolder.name.equals(oldName));

                    if (i % 5 == 0) {
                        testHolder.strings.add("A");
                        immutableRefFlag.set(true);
                    } else {
                        immutableRefFlag.set(false);
                    }
                    if (i % 2 == 0) {
                        testHolder.copyOfStrings.add("B");
                        copyOfListFlag.set(true);
                    } else {
                        copyOfListFlag.set(false);
                    }
                }
        );
        NeoForge.EVENT_BUS.addListener(
                ServerTickEvent.Post.class,
                event -> {
                    Assertions.assertEquals(copyOf.changed(), copyOfFlag.get());
                    Assertions.assertEquals(copyOfList.changed(), copyOfListFlag.get());
                    Assertions.assertEquals(mutableRef.changed(), mutableRefFlag.get());
                    Assertions.assertEquals(immutableRef.changed(), immutableRefFlag.get());
                }
        );

    }
}