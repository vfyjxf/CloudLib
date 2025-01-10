package dev.vfyjxf.cloudlib.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class EventFactoryTest {

    EventDefinition<OnTest> onTest = EventFactory.define(OnTest.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.test();
        }
    });

    private static class EventHandlerImpl implements EventHandler<OnTest> {
        private final EventChannel<OnTest> channel = EventChannel.create(this);

        @Override
        public EventChannel<OnTest> events() {
            return channel;
        }
    }

    @FunctionalInterface
    interface OnTest {
        void test();
    }

    private final EventHandlerImpl eventHandler = new EventHandlerImpl();
    private int customCondition = 0;

    @BeforeEach
    void setUp() {
        eventHandler.events().get(onTest).registerManaged(() -> {
            System.out.println("testTransient");
        }, 1);
        eventHandler.events().get(onTest).registerManaged(() -> {
            System.out.println("testIndependent");
            customCondition++;
        }, () -> customCondition == 2);
        eventHandler.events().get(onTest).register(() -> {
            System.out.println("test");
        },100);
    }

    @Test
    public void test(MinecraftServer server) {
        eventHandler.events().get(onTest).actor().test();
        eventHandler.events().get(onTest).actor().test();
        eventHandler.events().get(onTest).actor().test();
    }
}