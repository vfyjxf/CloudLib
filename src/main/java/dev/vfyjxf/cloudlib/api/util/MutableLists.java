package dev.vfyjxf.cloudlib.api.util;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.list.MutableListFactory;
import org.eclipse.collections.api.list.MutableList;

import java.util.stream.Stream;

public final class MutableLists {

    private static final MutableListFactory listFactory = Lists.mutable;

    public static <T> MutableList<T> empty() {return listFactory.empty();}

    public static <T> MutableList<T> of() {return listFactory.of();}

    @SafeVarargs
    public static <T> MutableList<T> wrapCopy(T... array) {return listFactory.wrapCopy(array);}

    public static <T> MutableList<T> withInitialCapacity(int capacity) {return listFactory.withInitialCapacity(capacity);}

    public static <T> MutableList<T> withNValues(int size, Function0<? extends T> factory) {return listFactory.withNValues(size, factory);}

    public static <T> MutableList<T> withAll(Iterable<? extends T> iterable) {return listFactory.withAll(iterable);}

    public static <T> MutableList<T> ofInitialCapacity(int capacity) {return listFactory.ofInitialCapacity(capacity);}

    public static <T> MutableList<T> with() {return listFactory.with();}

    @SafeVarargs
    public static <T> MutableList<T> with(T... items) {return listFactory.with(items);}

    public static <T> MutableList<T> fromStream(Stream<? extends T> stream) {return listFactory.fromStream(stream);}

    @SafeVarargs
    public static <T> MutableList<T> of(T... items) {return listFactory.of(items);}

    public static <T> MutableList<T> ofAll(Iterable<? extends T> iterable) {return listFactory.ofAll(iterable);}

    private MutableLists() {}
}
