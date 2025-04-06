package dev.vfyjxf.cloudlib.api.event;

import com.google.common.reflect.AbstractInvocationHandler;
import dev.vfyjxf.cloudlib.utils.Checks;
import dev.vfyjxf.cloudlib.utils.ClassUtils;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * The EventFactory is a factory class for defining {@link EventDefinition}
 *
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

    public static <T> EventDefinition<T> define(Class<T> type, Function<List<T>, T> merger) {
        Checks.checkArgument(ClassUtils.isFunctionalInterface(type), "type must be a functional interface");
        return new EventDefinitionImpl<>(type, merger);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> EventDefinition<T> defineGeneric(Class<? super T> type, Function<List<? extends T>, ? extends T> merger) {
        Checks.checkArgument(ClassUtils.isFunctionalInterface(type), "type must be a functional interface");
        return new EventDefinitionImpl<>(type, (Function) merger);
    }

    public static <T> Event<T> createEvent(Function<List<T>, T> combiner) {
        return new EventImpl<>(combiner);
    }

    private static class EventDefinitionImpl<T> implements EventDefinition<T> {

        private final Class<T> type;
        private final Function<List<T>, T> merger;
        private final Event<T> global;

        private EventDefinitionImpl(Class<T> type, Function<List<T>, T> merger) {
            this.type = type;
            this.merger = merger;
            this.global = new EventImpl<>(merger);
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public Event<T> create() {
            return new EventImpl<>(merger);
        }

        @Override
        public Event<T> global() {
            return global;
        }

        @Override
        public String toString() {
            return "EventDefinitionImpl{" +
                    "type=" + type.getSimpleName() +
                    '}';
        }
    }

    private static class EventImpl<T> implements Event<T> {
        private final MutableMap<T, BooleanSupplier> listenerLifetimeManage;
        private final Function<List<T>, T> merger;
        private final FastList<ListenerEntry<T>> listeners = FastList.newList();
        private T invoker;

        private EventImpl(Function<List<T>, T> merger) {
            this.merger = merger;
            listenerLifetimeManage = Maps.mutable.withInitialCapacity(1);
        }

        @Override
        public T invoker() {
            checkLifetime();
            if (invoker == null) {
                update();
            }
            return invoker;
        }

        @Override
        public T register(T listener, int priority) {
            Checks.checkRange(priority, 0, Integer.MAX_VALUE, "priority");
            Checks.checkNotNull(listener, "listener");
            Checks.checkArgument(!isRegistered(listener), "listener is already registered");

            listeners.add(new ListenerEntry<>(listener, priority));
            listeners.sort(ListenerEntry::compareTo);
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
            return registerManaged(proxied, () -> counter.get() <= 0);
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
        public T registerManaged(T listener, Object reference) {
            Checks.checkNotNull(listener, "listener");
            Checks.checkNotNull(reference, "reference");
            WeakReference<Object> ref = new WeakReference<>(reference);
            return registerManaged(listener, () -> ref.get() == null);
        }

        @Override
        public void unregister(T listener) {
            listeners.removeIf(l -> l.listener == listener);
            listenerLifetimeManage.remove(listener);
            listeners.trimToSize();
            invoker = null;
        }

        @Override
        public boolean isRegistered(T listener) {
            return listeners.anySatisfy(l -> l.listener == listener);
        }

        @Override
        public void clearListeners() {
            listeners.clear();
            invoker = null;
        }

        private void update() {
            if (listeners.size() == 1) {
                invoker = listeners.getFirst().listener;
            } else {
                invoker = merger.apply(listeners.collect(ListenerEntry::listener));
            }
        }

        private void checkLifetime() {
            if (!listenerLifetimeManage.isEmpty()) {
                int size = listeners.size();
                for (Iterator<ListenerEntry<T>> iterator = listeners.iterator(); iterator.hasNext(); ) {
                    T listener = iterator.next().listener;
                    var manage = listenerLifetimeManage.get(listener);
                    if (manage != null && manage.getAsBoolean()) {
                        iterator.remove();
                        listenerLifetimeManage.remove(listener);
                    }
                }
                if (size != listeners.size()) {
                    invoker = null;
                }
            }
        }

        private record ListenerEntry<T>(T listener, int priority) implements Comparable<ListenerEntry<T>> {
            @Override
            public int compareTo(ListenerEntry<T> o) {
                return Integer.compare(o.priority, priority);
            }
        }

    }
}
