package dev.vfyjxf.cloudlib.api.util;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.set.MutableSetFactory;
import org.eclipse.collections.api.set.MutableSet;

import java.util.stream.Stream;

public class MutableSets {

    private static final MutableSetFactory setFactory = Sets.mutable;

    public static <T> MutableSet<T> empty() {
        return setFactory.empty();
    }

    public static <T> MutableSet<T> fromStream(Stream<? extends T> stream) {
        return setFactory.fromStream(stream);
    }

    public static <T> MutableSet<T> ofAll(Iterable<? extends T> items) {
        return setFactory.ofAll(items);
    }

    public static <T> MutableSet<T> with() {
        return setFactory.with();
    }

    @SafeVarargs
    public static <T> MutableSet<T> with(T... items) {
        return setFactory.with(items);
    }

    public static <T> MutableSet<T> withAll(Iterable<? extends T> items) {
        return setFactory.withAll(items);
    }

    public static <T> MutableSet<T> withInitialCapacity(int capacity) {
        return setFactory.withInitialCapacity(capacity);
    }

    public static <T> MutableSet<T> of() {
        return setFactory.of();
    }

    public static <T> MutableSet<T> ofInitialCapacity(int capacity) {
        return setFactory.ofInitialCapacity(capacity);
    }

    @SafeVarargs
    public static <T> MutableSet<T> of(T... items) {
        return setFactory.of(items);
    }
}
