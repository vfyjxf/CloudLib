package dev.vfyjxf.cloudlib.api.event;

import com.google.common.reflect.AbstractInvocationHandler;
import dev.vfyjxf.cloudlib.api.actor.MergeableActor;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
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
        return new EventDefinitionImpl<>(type, merger);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> EventDefinition<T> defineGeneric(Class<? super T> type, Function<List<? extends T>, ? extends T> merger) {
        return new EventDefinitionImpl<>(type, (Function) merger);
    }

    public static <T> Event<T> createEvent(Function<List<T>, T> combiner) {
        return new EventImpl<>(combiner);
    }

    private static class EventDefinitionImpl<T> implements EventDefinition<T> {

        private final Class<T> type;
        private final Function<List<T>, T> merger;
        private final Event<T> global;

        private EventDefinitionImpl(Class<T> type, Function<java.util.List<T>, T> merger) {
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

    private static class EventImpl<T> extends MergeableActor<T> implements Event<T> {
        private final MutableMap<T, BooleanSupplier> listenerLifetimeManage;

        private EventImpl(Function<List<T>, T> merger) {
            super(merger);
            listenerLifetimeManage = Maps.mutable.withInitialCapacity(1);
        }

        @Override
        public T actor() {
            checkLifetime();
            if (actor == null) {
                updateActor();
            }
            return actor;
        }

        @Override
        public T register(T listener, int priority) {
            Checks.checkRange(priority, 0, Integer.MAX_VALUE, "priority");
            Checks.checkNotNull(listener, "listener");
            Checks.checkArgument(!isRegistered(listener), "listener is already registered");

            actors.add(new ActorEntry<>(listener, priority));
            actors.sort(ActorEntry::compareTo);
            actor = null;
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
            actors.removeIf(l -> l.actor() == listener);
            actors.trimToSize();
            actor = null;
        }

        @Override
        public boolean isRegistered(T listener) {
            return actors.anySatisfy(l -> l.actor() == listener);
        }

        @Override
        public void clearListeners() {
            actors.clear();
            actor = null;
        }

        private void checkLifetime() {
            if (!listenerLifetimeManage.isEmpty()) {
                int size = actors.size();
                for (Iterator<ActorEntry<T>> iterator = actors.iterator(); iterator.hasNext(); ) {
                    T listener = iterator.next().actor();
                    var manage = listenerLifetimeManage.get(listener);
                    if (manage != null && manage.getAsBoolean()) {
                        iterator.remove();
                        listenerLifetimeManage.remove(listener);
                    }
                }
                if (size != actors.size()) {
                    actor = null;
                }
            }
        }

        @Override
        public void put(T actor, int priority) {
            this.register(actor, priority);
        }

        @Override
        public void put(T actor) {
            this.register(actor);
        }

        @Override
        public void remove(T actor) {
            this.unregister(actor);
        }
    }
}
