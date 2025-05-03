package dev.vfyjxf.cloudlib.api.nodes;

import dev.vfyjxf.cloudlib.api.ui.gather.Gatherer;

import java.util.function.Function;
import java.util.function.Supplier;

import static dev.vfyjxf.cloudlib.api.ui.gather.Gatherer.Integrator.ofGreedy;

public class FactoryGatherers {


    public static <T, R> Gatherer<T, ?, R> create(
            Supplier<? extends R> factory
    ) {
        return Gatherer.of(ofGreedy(
                (unused, from, appender) -> {
                    appender.append(factory.get());
                    return false;
                }));
    }

    public static <T, R> Gatherer<T, ?, R> create(
            Function<T, ? extends R> factory
    ) {
        return Gatherer.of(ofGreedy(
                (unused, from, appender) -> {
                    appender.append(factory.apply(from));
                    return false;
                }));
    }

}
