package dev.vfyjxf.cloudlib.api.nodes;

import dev.vfyjxf.cloudlib.api.ui.gather.Gatherer;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static dev.vfyjxf.cloudlib.api.nodes.FactoryGatherers.create;
import static dev.vfyjxf.cloudlib.api.nodes.LoopGatherers.each;
import static dev.vfyjxf.cloudlib.api.nodes.LoopGatherers.range;
import static dev.vfyjxf.cloudlib.api.nodes.LoopGatherers.until;
import static dev.vfyjxf.cloudlib.api.ui.gather.Gatherer.Integrator.ofGreedy;

class GathererTest {

    private final MetaNode<Node> metaNode = new MetaNode<>();

    @Test
    void buildSimpleTree() {
        MutableList<String> strings = Lists.mutable.of(
                "string1",
                "string2",
                "string3",
                "string4",
                "string5",
                "string6",
                "string7"
        );
        metaNode.addConfigurator(ParentNode.class, ParentNode::start);
        //@formatter:off
        //region varargs
        metaNode.gatherAll(
            create((a) -> new RefNode<>("Head")),
            range(
                0, 10,
                ((var i, var from) -> new RefNode<>(i))
            ),
            each(
                strings,
                 ((string, from) -> new StringNode(string))
            ),
            until(
                () -> new AtomicInteger(0),
                (state) -> state.getAndIncrement() < 10,
                (state, from) -> new RefNode<>(state.get())
            ),
            create((from) -> new ParentNode<>()
                .gatherAll(
                    create((a) -> new IntNode(1)),
                    create((a) -> new StringNode("string")),
                    each(
                        strings,
                        (string, u) -> new StringNode(string)
                    ),
                    range(
                        0, 10,
                        (i, u) -> new IntNode(i)
                    )
                )
            ),

            create((unused)->new RefNode<>("End"))
        );

        metaNode.gatherAll(
                range(
                        0, 10,
                        ((var i, var from) -> new RefNode<>(i))
                ),
                configure(
                        ParentNode::new,
                        ParentNode::gatherAll,
                        create((b) -> new IntNode(1)),
                        create((b) -> new StringNode("string")),
                        each(
                                strings,
                                (string, u) -> new StringNode(string)
                        ),
                        range(
                                0, 10,
                                (i, u) -> new IntNode(i)
                        )
                )
        );

        //@formatter:on
        //endregion
        //region chained
        //@formatter:off
        metaNode.range(
                0, 10,
                ((var i, var from) -> new RefNode<>(i))
            )
            .each(
                strings,
                ((string, from) -> new RefNode<>(string))
            )
            .until(
                () -> new AtomicInteger(0),
                (state) -> state.getAndIncrement() < 10,
                (state, from) -> new RefNode<>(state.get())
            ).single(
                (u1) -> new ParentNode<>()
                     .single((u2) -> new RefNode<>("Head"))
                     .each(strings, (string, u) -> new StringNode(string))
                     .range(0, 10, (i, u) -> new IntNode(i))
                     .single(
                         (u2) -> new ParentNode<>()
                               .each(
                                   strings,
                                 (string, u) -> new StringNode(string)
                               )
                               .range(
                                  0, 10,
                                 (i, u) -> new IntNode(i)
                               )
                     )
                     .single((u2) -> new RefNode<>("End"))
            );
        //@formatter:on
        //endregion
        metaNode.start();
        MutableList<Node> nodes = metaNode.currentChildren();
        System.out.println(nodes);
    }

    @SafeVarargs
    private static <T, R> Gatherer<T, ?, R> configure(
            Supplier<R> factory,
            BiConsumer<R, Gatherer<T, ?, R>[]> applier,
            Gatherer<T, ?, R>... gatherers
    ) {
        return Gatherer.of(ofGreedy(
                (state, from, appender) -> {
                    R r = factory.get();
                    applier.accept(r, gatherers);
                    appender.append(r);
                    return false;
                }
        ));
    }

    private static class Node {

    }

    private static class RefNode<T> extends Node {
        private final T value;

        private RefNode(T value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "RefNode{" +
                           "value=" + value +
                           '}';
        }
    }

    private static class IntNode extends Node {
        private final int value;

        private IntNode(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "IntNode{" +
                           "value=" + value +
                           '}';
        }
    }

    private static class StringNode extends Node {
        private final String value;

        private StringNode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "StringNode{" +
                           "value='" + value + '\'' +
                           '}';
        }
    }

    private static class ParentNode<T extends Node> extends Node {
        private MutableList<T> children;
        private final MutableList<Gatherer<ParentNode<T>, ?, ? extends T>> gatherers = Lists.mutable.empty();
        private final MutableList<Configurator<?>> configurators = Lists.mutable.empty();

        public ParentNode() {
            this.configurators.add(new Configurator<>(ParentNode.class, ParentNode::start));
        }


        private record Configurator<T>(Class<T> type, Consumer<T> configurator) {

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
            for (Gatherer<ParentNode<T>, ?, ? extends T> gatherer : gatherers) {
                evaluate(gatherer, this, appender);
            }
            this.children = nodes;
        }

        private static <T extends Node, A> void evaluate(Gatherer<ParentNode<T>, A, ? extends T> gatherer, ParentNode<T> from, UpperNodeAppender<? super T> appender) {
            var initializer = gatherer.initializer();
            A state = initializer == Gatherer.defaultInitializer() ? null : initializer.get();
            var integrator = gatherer.integrator();
            boolean condition;
            do {condition = integrator.integrate(state, from, appender);}
            while (condition);

        }

        public <A, R extends T> ParentNode<T> gather(
                Gatherer<ParentNode<T>, A, R> gatherer
        ) {
            this.gatherers.add(gatherer);
            return this;
        }

        @SafeVarargs
        public final <R extends T> ParentNode<T> gatherAll(
                Gatherer<ParentNode<T>, ?, R>... gatherers
        ) {
            this.gatherers.addAll(Arrays.asList(gatherers));
            return this;
        }

        /**
         * Aka {@code for (int i = fromInclusive; i < toExclusive; i++) { ... }}
         */
        public ParentNode<T> range(
                int fromInclusive,
                int toExclusive,
                BiFunction<Integer, ParentNode<T>, ? extends T> factory
        ) {
            return range(fromInclusive, toExclusive, 1, factory);
        }

        /**
         * Aka
         * {@code for (int i = fromInclusive; i < toExclusive; i+=step) { ... }}
         */
        public ParentNode<T> range(
                int fromInclusive,
                int toExclusive,
                int step,
                BiFunction<Integer, ParentNode<T>, ? extends T> factory
        ) {
            Checks.checkArgument(fromInclusive < toExclusive, "fromInclusive must be less than toExclusive");

            class State {
                int current = fromInclusive;

                boolean integrate(ParentNode<T> from, UpperNodeAppender<? super T> appender) {
                    T node = factory.apply(current, from);
                    appender.append(node);
                    current += step;
                    return current < toExclusive;
                }
            }
            var gatherer = Gatherer.ofSequential(
                    State::new,
                    Gatherer.Integrator.<ParentNode<T>, State, T>ofGreedy(State::integrate)
            );
            return gather(gatherer);
        }

        /**
         * Aka {@code for(A value : iterable) { ... }}
         */
        public <A> ParentNode<T> each(
                Iterable<A> iterable,
                BiFunction<A, ParentNode<T>, ? extends T> factory
        ) {
            class State {
                final Iterator<A> iterator = iterable.iterator();
                final boolean isEmpty = !iterator.hasNext();
                A current = iterator.hasNext() ? iterator.next() : null;

                boolean integrate(ParentNode<T> from, UpperNodeAppender<? super T> appender) {
                    if (isEmpty) return false;
                    T node = factory.apply(current, from);
                    appender.append(node);
                    if (iterator.hasNext()) current = iterator.next();
                    return iterator.hasNext();
                }
            }

            var gather = Gatherer.ofSequential(
                    State::new,
                    Gatherer.Integrator.<ParentNode<T>, State, T>ofGreedy(State::integrate)
            );
            return gather(
                    gather
            );
        }

        /**
         * Aka {@code do { ... } while(walkAndDetect.test(state)); }
         */
        public <A> ParentNode<T> until(
                Supplier<A> initializer,
                Predicate<A> walkAndDetect,
                BiFunction<A, ParentNode<T>, ? extends T> factory
        ) {
            class State {
                final A state = initializer.get();

                boolean integrate(ParentNode<T> from, UpperNodeAppender<? super T> appender) {
                    T node = factory.apply(state, from);
                    appender.append(node);
                    return walkAndDetect.test(state);
                }
            }
            return gather(Gatherer.ofSequential(
                    State::new,
                    Gatherer.Integrator.<ParentNode<T>, State, T>ofGreedy(State::integrate)
            ));
        }

        public ParentNode<T> single(
                Function<ParentNode<T>, ? extends T> factory
        ) {
            return gather(create(factory));
        }

        @Override
        public String toString() {
            return "ParentNode{" +
                           "children=" + children +
                           '}';
        }
    }
}
