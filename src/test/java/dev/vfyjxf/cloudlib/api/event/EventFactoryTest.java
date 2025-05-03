package dev.vfyjxf.cloudlib.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class EventFactoryTest {

    EventDefinition<OnTest> onTest = EventFactory.define(OnTest.class, listeners -> (int a, Object b, long c, double d) -> {
        for (var listener : listeners) {
            listener.test(0, new Object(), 100L, 300D);
        }
    });

    EventDefinition<GenericTest<Integer>> genericIntTest = EventFactory.defineGeneric(
            GenericTest.class,
            listeners -> (Integer obj, long a, double b) -> {
                for (var listener : listeners) {
                    listener.test(obj, a, b);
                }
            }
    );

    EventDefinition<GenericTest<String>> genericStringTest = EventFactory.defineGeneric(
            GenericTest.class,
            listeners -> (String obj, long a, double b) -> {
                for (var listener : listeners) {
                    listener.test(obj, a, b);
                }
            }
    );

    EventDefinition<ReifiedTestEvent> reifiedTestEvent = EventFactory.define(
            ReifiedTestEvent.class,
            listeners -> (String obj, long a, double b) -> {
                for (var listener : listeners) {
                    listener.test(obj, a, b);
                }
            }
    );

    private static class EventHandlerImpl implements EventHandler<TestEvent> {
        private final EventChannel<TestEvent> channel = EventChannel.create(this);

        @Override
        public EventChannel<TestEvent> events() {
            return channel;
        }
    }

    interface TestEvent {

    }

    @FunctionalInterface
    interface OnTest extends TestEvent {
        void test(int a, Object b, long c, double d);
    }

    interface GenericTest<T> extends TestEvent {
        void test(T obj, long a, double b);
    }

    interface ReifiedTestEvent extends GenericTest<String> {
        @Override
        void test(String obj, long a, double b);
    }

    private final EventHandlerImpl eventHandler = new EventHandlerImpl();
    private int customCondition = 0;

    @BeforeEach
    void setUp() {
        eventHandler.events().get(onTest).registerManaged((int a, Object b, long c, double d) -> {
            System.out.println("testTransient");
        }, 1);
        eventHandler.events().get(onTest).registerManaged((int a, Object b, long c, double d) -> {
            System.out.println("testIndependent");
            customCondition++;
        }, () -> customCondition == 2);
        eventHandler.events().get(onTest).register((int a, Object b, long c, double d) -> {
            System.out.println("test");
        }, 100);
        eventHandler.events().get(genericIntTest).registerManaged(
                (Integer obj, long a, double b) -> {
                    System.out.println("testGenericInt" + obj);
                }, 2);
        eventHandler.events().get(genericStringTest).registerManaged(
                (String obj, long a, double b) -> {
                    System.out.println("testGenericString " + obj);
                }, 3);
        eventHandler.events().get(reifiedTestEvent).registerManaged(
                (String obj, long a, double b) -> {
                    System.out.println("testReified " + obj);
                }, 4);
    }

    @Test
    public void test(MinecraftServer server) {
        eventHandler.events().get(onTest).invoker().test(30, new Object(), 100L, 300D);
        eventHandler.events().get(onTest).invoker().test(40, new Object(), 100L, 300D);
        eventHandler.events().get(onTest).invoker().test(50, new Object(), 100L, 300D);
        eventHandler.events().get(genericIntTest).invoker().test(100, 100L, 300D);
        eventHandler.events().get(genericIntTest).invoker().test(200, 100L, 300D);
        eventHandler.events().get(genericIntTest).invoker().test(300, 100L, 300D);
        eventHandler.events().get(genericStringTest).invoker().test("test1", 100L, 300D);
        eventHandler.events().get(genericStringTest).invoker().test("test2", 100L, 300D);
        eventHandler.events().get(genericStringTest).invoker().test("test3", 100L, 300D);
        eventHandler.events().get(genericStringTest).invoker().test("test4", 100L, 300D);
        eventHandler.events().get(reifiedTestEvent).invoker().test("test1", 100L, 300D);
        eventHandler.events().get(reifiedTestEvent).invoker().test("test2", 100L, 300D);
        eventHandler.events().get(reifiedTestEvent).invoker().test("test3", 100L, 300D);
        eventHandler.events().get(reifiedTestEvent).invoker().test("test4", 100L, 300D);
        eventHandler.events().get(reifiedTestEvent).invoker().test("test5", 100L, 300D);
    }
}