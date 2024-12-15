package dev.vfyjxf.cloudlib.api.event;

import com.google.common.reflect.AbstractInvocationHandler;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * <p>
 * Some basic promises:
 * <p>
 * 1. The naming define events follows the lowercase hump naming
 * <p>
 * 2. Pre-phase events do not need to add the Pre suffix, but Post-phase events need to add the Post suffix.
 * <p>
 * 3. Usually, an event is defined in an interface with its listeners.
 * <p>
 * 4.All event listener must be a functional interface.
 */
public final class EventFactory {


    private EventFactory() {
    }

    public static <T> EventDefinition<T> define(Class<T> type, Function<List<T>, T> invokerFactory) {
        return new EventDefinitionImpl<>(type, invokerFactory);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> EventDefinition<T> defineGeneric(Class<? super T> type, Function<List<? extends T>, ? extends T> invokerFactory) {
        return new EventDefinitionImpl<>(type, (Function) invokerFactory);
    }

    public static <T> Event<T> createEvent(Function<List<T>, T> invokerFactory) {
        return new EventImpl<>(invokerFactory);
    }

    private static class EventDefinitionImpl<T> implements EventDefinition<T> {

        private final Class<T> type;
        private final Function<List<T>, T> function;
        private final Event<T> global;

        private EventDefinitionImpl(Class<T> type, Function<List<T>, T> function) {
            this.type = type;
            this.function = function;
            this.global = new EventImpl<>(function);
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public Event<T> create() {
            return new EventImpl<>(function);
        }

        @Override
        public Event<T> global() {
            return global;
        }
    }

    private static class EventImpl<T> implements Event<T> {
        private final Function<List<T>, T> function;
        private T invoker = null;
        private final FastList<T> listeners;
        private final MutableMap<T, BooleanSupplier> listenerLifetimeManage;

        private EventImpl(Function<List<T>, T> function) {
            this.function = function;
            listeners = FastList.newList();
            listenerLifetimeManage = Maps.mutable.withInitialCapacity(1);
        }

        @Override
        public T invoker() {
            if (!listenerLifetimeManage.isEmpty()) {
                int size = listeners.size();
                listeners.removeIf(listener -> listenerLifetimeManage.get(listener).getAsBoolean());
                if (size != listeners.size()) {
                    invoker = null;
                }
            }
            if (invoker == null) {
                update();
            }
            return invoker;
        }

        @Override
        public T register(T listener) {
            Checks.checkNotNull(listener, "listener");
            listeners.add(listener);
            invoker = null;
            return listener;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T registerManaged(T listener, int lifetime) {
            Checks.checkNotNull(listener, "listener");
            Checks.checkArgument(lifetime > 0, "lifetime must be greater than 0");
            AtomicInteger counter = new AtomicInteger(lifetime);
            T proxied = (T) Proxy.newProxyInstance(
                    listener.getClass().getClassLoader(),
                    listener.getClass().getInterfaces(),
                    new AbstractInvocationHandler() {
                        @Override
                        protected @Nullable Object handleInvocation(Object proxy, Method method, @Nullable Object[] args) throws Throwable {
                            var ret = MethodHandles.lookup()
                                    .unreflect(method)
                                    .bindTo(listener)
                                    .invokeWithArguments(args);
                            counter.decrementAndGet();
                            return ret;
                        }
                    }
            );
            register(proxied);
            listenerLifetimeManage.put(proxied, () -> counter.get() <= 0);
            return proxied;
        }

        @Override
        public T registerManaged(T listener, BooleanSupplier condition) {
            Checks.checkNotNull(listener, "listener");
            Checks.checkNotNull(condition, "condition");
            register(listener);
            listenerLifetimeManage.put(listener, condition);
            return listener;
        }

        @Override
        public void unregister(T listener) {
            listeners.remove(listener);
            listeners.trimToSize();
            invoker = null;
        }

        @Override
        public boolean isRegistered(T listener) {
            return listeners.contains(listener);
        }

        @Override
        public void clearListeners() {
            listeners.clear();
            invoker = null;
        }

        public void update() {
            if (listeners.size() == 1) {
                invoker = listeners.getFirst();
            } else {
                invoker = function.apply(listeners);
            }
        }
    }
}
