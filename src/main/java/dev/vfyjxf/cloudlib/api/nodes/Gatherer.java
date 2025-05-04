package dev.vfyjxf.cloudlib.api.nodes;


import dev.vfyjxf.cloudlib.utils.Checks;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @param <T> the type of upstream node
 * @param <A> the type of state
 * @param <R> the type of result
 */
public interface Gatherer<T, A, R> {


    static <S> Supplier<S> defaultInitializer() {
        return Gatherers.Value.DEFAULT.initializer();
    }

    static <S, R> BiConsumer<S, Appender<? super R>> defaultFinisher() {
        return Gatherers.Value.DEFAULT.finisher();
    }

    default Supplier<A> initializer() {
        return defaultInitializer();
    }

    Integrator<T, A, R> integrator();

    default BiConsumer<A, Appender<? super R>> finisher() {
        return defaultFinisher();
    }


    static <T, A, R> Gatherer<T, A, R> ofSequential(
            Supplier<A> initializer,
            Integrator<T, A, R> integrator
    ) {
        return new Gatherers.GathererImpl<>(
                Checks.checkNotNull(initializer, "initializer"),
                Checks.checkNotNull(integrator, "integrator"),
                defaultFinisher()
        );
    }

    static <T, R> Gatherer<T, Void, R> of(Integrator<T, Void, R> integrator) {
        return of(
                defaultInitializer(),
                integrator,
                defaultFinisher()
        );
    }

    static <T, A, R> Gatherer<T, A, R> of(
            Supplier<A> initializer,
            Integrator<T, A, R> integrator,
            BiConsumer<A, Appender<? super R>> finisher
    ) {
        return new Gatherers.GathererImpl<>(
                Checks.checkNotNull(initializer, "initializer"),
                Checks.checkNotNull(integrator, "integrator"),
                Checks.checkNotNull(finisher, "finisher")
        );
    }


    interface Integrator<T, A, R> {

        boolean integrate(A state, T from, Appender<? super R> appender);


        static <T, A, R> Greedy<T, A, R> ofGreedy(Greedy<T, A, R> greedy) {
            return greedy;
        }

        interface Greedy<T, A, R> extends Integrator<T, A, R> {}
    }
}
