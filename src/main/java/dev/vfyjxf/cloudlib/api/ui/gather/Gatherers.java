package dev.vfyjxf.cloudlib.api.ui.gather;

import dev.vfyjxf.cloudlib.api.nodes.UpperNodeAppender;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Gatherers {

    @SuppressWarnings("rawtypes")
    enum Value implements Supplier, BiConsumer {
        DEFAULT;

        // BiConsumer
        @Override
        public void accept(Object state, Object downstream) {
        }

        // Supplier
        @Override
        public Object get() {
            return null;
        }

        @SuppressWarnings("unchecked")
        <A> Supplier<A> initializer() {
            return (Supplier<A>) this;
        }

        @SuppressWarnings("unchecked")
        <S, R> BiConsumer<S, UpperNodeAppender<? super R>> finisher() {
            return (BiConsumer<S, UpperNodeAppender<? super R>>) this;
        }
    }

    record GathererImpl<T, A, R>(
            @Override Supplier<A> initializer,
            @Override Gatherer.Integrator<T, A, R> integrator,
            @Override BiConsumer<A, UpperNodeAppender<? super R>> finisher
    ) implements Gatherer<T, A, R> {}

    static class Composite<T, A, R, AA, RR> implements Gatherer<T, Object, RR> {
        private final Gatherer<T, A, ? extends R> left;
        private final Gatherer<? super R, AA, ? extends RR> right;
        private final GathererImpl<T, Object, RR> impl;

        @SuppressWarnings("unchecked")
        Composite(Gatherer<T, A, ? extends R> left, Gatherer<? super R, AA, ? extends RR> right) {
            this.left = left;
            this.right = right;
            this.impl = (GathererImpl<T, Object, RR>) composite(left, right);
        }

        @Override
        public Supplier<Object> initializer() {
            return null;
        }

        @Override
        public Integrator<T, Object, RR> integrator() {
            return null;
        }

        @Override
        public BiConsumer<Object, UpperNodeAppender<? super RR>> finisher() {
            return null;
        }

        private static <F, S, R, SS, RR> GathererImpl<F, ?, RR> composite(
                Gatherer<F, S, ? extends R> left,
                Gatherer<? super R, SS, ? extends RR> right
        ) {
            throw new UnsupportedOperationException("Not Implemented");
        }
    }

}
