package dev.vfyjxf.cloudlib.api.nodes;

import dev.vfyjxf.cloudlib.api.ui.gather.Gatherer;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class LoopGatherers {

    public static <T, R> Gatherer<T, ?, R> range(
            int fromInclusive,
            int toExclusive,
            TriConsumer<Integer, T, UpperNodeAppender<? super R>> consumer
    ) {
        return range(fromInclusive, toExclusive, 1, consumer);
    }

    public static <T, R> Gatherer<T, ?, R> range(
            int fromInclusive,
            int toExclusive,
            int step,
            TriConsumer<Integer, T, UpperNodeAppender<? super R>> consumer
    ) {
        Checks.checkArgument(fromInclusive < toExclusive, "fromInclusive must be less than toExclusive");

        class State {
            int current = fromInclusive;

            boolean integrate(T from, UpperNodeAppender<? super R> appender) {
                consumer.accept(current, from, appender);
                current += step;
                return current < toExclusive;
            }
        }

        return Gatherer.ofSequential(
                State::new,
                Gatherer.Integrator.<T, State, R>ofGreedy(State::integrate)
        );
    }

    public static <T, R> Gatherer<T, ?, R> range(
            int fromInclusive,
            int toExclusive,
            BiFunction<Integer, T, ? extends R> factory
    ) {
        return range(fromInclusive, toExclusive, 1, factory);
    }

    public static <T, R> Gatherer<T, ?, R> range(
            int fromInclusive,
            int toExclusive,
            int step,
            BiFunction<Integer, T, ? extends R> factory
    ) {
        Checks.checkArgument(fromInclusive < toExclusive, "fromInclusive must be less than toExclusive");

        class State {
            int current = fromInclusive;

            boolean integrate(T from, UpperNodeAppender<? super R> appender) {
                R node = factory.apply(current, from);
                appender.append(node);
                current += step;
                return current < toExclusive;
            }
        }

        return Gatherer.ofSequential(
                State::new,
                Gatherer.Integrator.<T, State, R>ofGreedy(State::integrate)
        );
    }


    /**
     * Aka {@code for(A value : iterable) { ... }}
     */
    public static <A, T, R> Gatherer<T, ?, R> each(
            Iterable<A> iterable,
            TriConsumer<A, T, UpperNodeAppender<? super R>> consumer
    ) {
        class State {
            final Iterator<A> iterator = iterable.iterator();
            final boolean isEmpty = !iterator.hasNext();
            A current = iterator.hasNext() ? iterator.next() : null;

            boolean integrate(T from, UpperNodeAppender<? super R> appender) {
                if (isEmpty) return false;
                consumer.accept(current, from, appender);
                if (iterator.hasNext()) current = iterator.next();
                return iterator.hasNext();
            }
        }

        return Gatherer.ofSequential(
                State::new,
                Gatherer.Integrator.<T, State, R>ofGreedy(State::integrate)
        );

    }

    /**
     * Aka {@code for(A value : iterable) { ... }}
     */
    public static <A, T, R> Gatherer<T, ?, R> each(
            Iterable<A> iterable,
            BiFunction<A, T, ? extends R> factory
    ) {
        class State {
            final Iterator<A> iterator = iterable.iterator();
            final boolean isEmpty = !iterator.hasNext();
            A current = iterator.hasNext() ? iterator.next() : null;

            boolean integrate(T from, UpperNodeAppender<? super R> appender) {
                if (isEmpty) return false;
                R node = factory.apply(current, from);
                appender.append(node);
                if (iterator.hasNext()) current = iterator.next();
                return iterator.hasNext();
            }
        }

        return Gatherer.ofSequential(
                State::new,
                Gatherer.Integrator.<T, State, R>ofGreedy(State::integrate)
        );

    }

    /**
     * Aka {@code do { ... } while(walkAndDetect.test(state)); }
     */
    public static <A, T, R> Gatherer<T, ?, R> until(
            Supplier<A> initializer,
            Predicate<A> walkAndDetect,
            TriConsumer<A, T, UpperNodeAppender<? super R>> consumer
    ) {
        class State {
            final A state = initializer.get();

            boolean integrate(T from, UpperNodeAppender<? super R> appender) {
                consumer.accept(state, from, appender);
                return walkAndDetect.test(state);
            }
        }
        return Gatherer.ofSequential(
                State::new,
                Gatherer.Integrator.<T, State, R>ofGreedy(State::integrate)
        );
    }

    /**
     * Aka {@code do { ... } while(walkAndDetect.test(state)); }
     */
    public static <A, T, R> Gatherer<T, ?, R> until(
            Supplier<A> initializer,
            Predicate<A> walkAndDetect,
            BiFunction<A, T, ? extends R> factory
    ) {
        class State {
            final A state = initializer.get();

            boolean integrate(T from, UpperNodeAppender<? super R> appender) {
                R node = factory.apply(state, from);
                appender.append(node);
                return walkAndDetect.test(state);
            }
        }
        return Gatherer.ofSequential(
                State::new,
                Gatherer.Integrator.<T, State, R>ofGreedy(State::integrate)
        );
    }
}
