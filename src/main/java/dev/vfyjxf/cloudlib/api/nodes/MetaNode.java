package dev.vfyjxf.cloudlib.api.nodes;

import dev.vfyjxf.cloudlib.api.ui.gather.Gatherer;
import dev.vfyjxf.cloudlib.api.ui.gather.Gatherer.Integrator;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MetaNode<T> {

    private MutableList<T> children;
    private final MutableList<Gatherer<MetaNode<T>, ?, ? extends T>> gatherers = Lists.mutable.empty();
    private final MutableList<Configurator<?>> configurators = Lists.mutable.empty();

    private record Configurator<T>(Class<T> type, Consumer<T> configurator) {

    }

    public <R extends T> MetaNode<T> addConfigurator(
            Class<R> type,
            Consumer<R> configurator
    ) {
        Checks.checkNotNull(type, "type");
        Checks.checkNotNull(configurator, "configurator");
        this.configurators.add(new Configurator<>(type, configurator));
        return this;
    }

    public MutableList<T> currentChildren() {
        return children.asUnmodifiable();
    }

    @SuppressWarnings("unchecked")
    public void start() {
        MutableList<T> nodes = Lists.mutable.empty();
        UpperNodeAppender<T> appender = (node -> {
            var ret = nodes.add(node);
            Configurator<?> detect = configurators.detect(c -> c.type == node.getClass());
            if (detect != null) {
                Consumer<T> configurator = (Consumer<T>) detect.configurator;
                configurator.accept(node);
            }
            return ret;
        });
        for (Gatherer<MetaNode<T>, ?, ? extends T> gatherer : gatherers) {
            evaluate(gatherer, this, appender);
        }
        this.children = nodes;
    }

    private static <T, A> void evaluate(Gatherer<MetaNode<T>, A, ? extends T> gatherer, MetaNode<T> from, UpperNodeAppender<? super T> appender) {
        var initializer = gatherer.initializer();
        A state = initializer == Gatherer.defaultInitializer() ? null : initializer.get();
        var integrator = gatherer.integrator();
        boolean condition;
        do {condition = integrator.integrate(state, from, appender);}
        while (condition);

    }

    public <A, R extends T> MetaNode<T> gather(
            Gatherer<MetaNode<T>, A, R> gatherer
    ) {
        this.gatherers.add(gatherer);
        return this;
    }

    @SafeVarargs
    public final <R extends T> MetaNode<T> gatherAll(
            Gatherer<MetaNode<T>, ?, R>... gatherers
    ) {
        this.gatherers.addAll(Arrays.asList(gatherers));
        return this;
    }

    /**
     * Aka {@code for (int i = fromInclusive; i < toExclusive; i++) { ... }}
     */
    public MetaNode<T> range(
            int fromInclusive,
            int toExclusive,
            BiFunction<Integer, MetaNode<T>, ? extends T> factory
    ) {
        return range(fromInclusive, toExclusive, 1, factory);
    }

    /**
     * Aka
     * {@code for (int i = fromInclusive; i < toExclusive; i+=step) { ... }}
     */
    public MetaNode<T> range(
            int fromInclusive,
            int toExclusive,
            int step,
            BiFunction<Integer, MetaNode<T>, ? extends T> factory
    ) {
        Checks.checkArgument(fromInclusive < toExclusive, "fromInclusive must be less than toExclusive");

        class State {
            int current = fromInclusive;

            boolean integrate(MetaNode<T> from, UpperNodeAppender<? super T> appender) {
                T node = factory.apply(current, from);
                appender.append(node);
                current += step;
                return current < toExclusive;
            }
        }
        var gatherer = Gatherer.ofSequential(
                State::new,
                Integrator.<MetaNode<T>, State, T>ofGreedy(State::integrate)
        );
        return gather(gatherer);
    }

    /**
     * Aka {@code for(A value : iterable) { ... }}
     */
    public <A> MetaNode<T> each(
            Iterable<A> iterable,
            BiFunction<A, MetaNode<T>, ? extends T> factory
    ) {
        class State {
            final Iterator<A> iterator = iterable.iterator();
            final boolean isEmpty = !iterator.hasNext();
            A current = iterator.hasNext() ? iterator.next() : null;

            boolean integrate(MetaNode<T> from, UpperNodeAppender<? super T> appender) {
                if (isEmpty) return false;
                T node = factory.apply(current, from);
                appender.append(node);
                if (iterator.hasNext()) current = iterator.next();
                return iterator.hasNext();
            }
        }

        var gather = Gatherer.ofSequential(
                State::new,
                Integrator.<MetaNode<T>, State, T>ofGreedy(State::integrate)
        );
        return gather(
                gather
        );
    }

    /**
     * Aka {@code do { ... } while(walkAndDetect.test(state)); }
     */
    public <A> MetaNode<T> until(
            Supplier<A> initializer,
            Predicate<A> walkAndDetect,
            BiFunction<A, MetaNode<T>, ? extends T> factory
    ) {
        class State {
            final A state = initializer.get();

            boolean integrate(MetaNode<T> from, UpperNodeAppender<? super T> appender) {
                T node = factory.apply(state, from);
                appender.append(node);
                return walkAndDetect.test(state);
            }
        }
        return gather(Gatherer.ofSequential(
                State::new,
                Integrator.<MetaNode<T>, State, T>ofGreedy(State::integrate)
        ));
    }

    public MetaNode<T> single(
            Function<MetaNode<T>, ? extends T> factory
    ) {
        return gather(FactoryGatherers.create(factory));
    }


}
