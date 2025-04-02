package dev.vfyjxf.cloudlib.api.network.sync;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ExposeLayerValue<E> extends ExposeValue<Object> {

    static <S_UP, S_EXPORT, S_HOLDER> ExposeValue<S_EXPORT> create(
            Function<S_HOLDER, S_UP> upstreamGetter,
            FlowEncoder<S_UP> encoder,
            FlowEncoder<S_EXPORT> decoder

    ) {
        throw new NotImplementedException();
    }


    void consume(Consumer<E> consumer);

    record CompositeTransformer<S, T, R, C>(
            Supplier<S> initializer,
            ContainerPusher<C, R> pusher,
            CompositeIntegrator<S, T, R, C> integrator,
            Finisher<S, C, ContainerPusher<C, R>> finisher
    ) {
        static <S, T, R, C> CompositeTransformer<S, T, R, C> of(
                Supplier<S> initializer,
                ContainerPusher<C, R> pusher,
                CompositeIntegrator<S, T, R, C> integrator,
                Finisher<S, C, ContainerPusher<C, R>> finisher
        ) {
            return new CompositeTransformer<>(initializer, pusher, integrator, finisher);
        }

        interface Finisher<S, C, R> {
            void finish(S state, C collection, R data);
        }
    }

    interface CompositeIntegrator<S, T, R, C> {

        boolean integrate(S state, T element, C container, ContainerPusher<C, R> pusher);

        interface Greedy<S, T, R, C> extends CompositeIntegrator<S, T, R, C> {}
    }


    interface ContainerPusher<C, T> {
        static <T> ContainerPusher<List<T>, T> list(Supplier<List<T>> supplier) {
            return create(supplier, List::add);
        }

        static <T> ContainerPusher<List<T>, T> list() {
            return create(Lists.mutable::empty, List::add);
        }

        static <T> ContainerPusher<Collection<T>, T> collection(Supplier<Collection<T>> containerInitializer) {
            return create(containerInitializer, Collection::add);
        }

        static <T> ContainerPusher<Set<T>, T> set(Supplier<Set<T>> containerInitializer) {
            return create(containerInitializer, Set::add);
        }

        static <T> ContainerPusher<Set<T>, T> set() {
            return create(Sets.mutable::empty, Set::add);
        }

        static <T> ContainerPusher<T[], T> array(Supplier<T[]> supplier) {
            class ArrayPusher implements ContainerPusher<T[], T> {
                int index = 0;

                @Override
                public T[] createContainer() {
                    return supplier.get();
                }

                @Override
                public void push(T[] array, T value) {
                    array[index++] = value;
                }

                @Override
                public boolean rejecting(T[] array) {
                    return index >= array.length;
                }
            }
            return new ArrayPusher();
        }

        static <C, T> ContainerPusher<C, T> create(Supplier<C> containerInitializer, BiConsumer<C, T> pusher) {
            return new ContainerPusher<>() {
                @Override
                public C createContainer() {
                    return containerInitializer.get();
                }

                @Override
                public void push(C container, T value) {
                    pusher.accept(container, value);
                }
            };
        }

        C createContainer();

        void push(C container, T value);

        default boolean rejecting(C container) {
            return false;
        }
    }

    interface Transformer<T, I, R> {

        static <T, I> ToStreamTransformer<T, I> toStream(Function<I, Stream<T>> function) {
            return function::apply;
        }

        R transform(I input);

    }

    interface DiffTransformer<T, I, R> {

        static <T, I> DiffTransformer<T, I, Stream<T>> toStream(Function<I, Stream<T>> function) {
            return function::apply;
        }

        R transform(I input);
    }

    interface ToStreamTransformer<T, I> extends Transformer<T, I, Stream<T>> {}

    interface ToCollectionTransformer<T, I> extends Transformer<T, I, Collection<T>> {}

    interface ToListTransformer<T, I> extends Transformer<T, I, List<T>> {}


}
