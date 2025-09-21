package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.*;
import org.eclipse.collections.api.annotation.Beta;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.MutableBagIterable;
import org.eclipse.collections.api.bag.sorted.ImmutableSortedBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.ParallelListIterable;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.ordered.OrderedIterable;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;


public final class MutableListState<T> implements CompoundState, RandomAccess, MutableList<T> {
    private final MutableList<T> internal = MutableLists.empty();
    private boolean changed = false;

    public MutableListState() {
        // Default constructor
    }

    public MutableListState(T[] array) {
        Collections.addAll(this.internal, array);
    }

    @Override
    public boolean changed() {
        if (changed) {
            changed = false;
            return true;
        } else return false;
    }

    //region delegate

    @Override
    public MutableList<T> with(T element) {return internal.with(element);}

    @Override
    public MutableList<T> without(T element) {return internal.without(element);}

    @Override
    public MutableList<T> withAll(Iterable<? extends T> elements) {return internal.withAll(elements);}

    @Override
    public MutableList<T> withoutAll(Iterable<? extends T> elements) {return internal.withoutAll(elements);}

    @Override
    public MutableList<T> newEmpty() {return internal.newEmpty();}

    @Override
    public MutableList<T> clone() {return internal.clone();}

    @Override
    public MutableList<T> tap(Procedure<? super T> procedure) {return internal.tap(procedure);}

    @Override
    public MutableList<T> select(Predicate<? super T> predicate) {return internal.select(predicate);}

    @Override
    public <P> MutableList<T> selectWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.selectWith(predicate, parameter);}

    @Override
    public MutableList<T> reject(Predicate<? super T> predicate) {return internal.reject(predicate);}

    @Override
    public <P> MutableList<T> rejectWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.rejectWith(predicate, parameter);}

    @Override
    public PartitionMutableList<T> partition(Predicate<? super T> predicate) {return internal.partition(predicate);}

    @Override
    public <P> PartitionMutableList<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.partitionWith(predicate, parameter);}

    @Override
    public <S> MutableList<S> selectInstancesOf(Class<S> clazz) {return internal.selectInstancesOf(clazz);}

    @Override
    public <V> MutableList<V> collect(Function<? super T, ? extends V> function) {return internal.collect(function);}

    @Override
    public <V> MutableList<V> collectWithIndex(ObjectIntToObjectFunction<? super T, ? extends V> function) {return internal.collectWithIndex(function);}

    @Override
    public MutableList<T> selectWithIndex(ObjectIntPredicate<? super T> predicate) {return internal.selectWithIndex(predicate);}

    @Override
    public MutableList<T> rejectWithIndex(ObjectIntPredicate<? super T> predicate) {return internal.rejectWithIndex(predicate);}

    @Override
    public MutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction) {return internal.collectBoolean(booleanFunction);}

    @Override
    public MutableByteList collectByte(ByteFunction<? super T> byteFunction) {return internal.collectByte(byteFunction);}

    @Override
    public MutableCharList collectChar(CharFunction<? super T> charFunction) {return internal.collectChar(charFunction);}

    @Override
    public MutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction) {return internal.collectDouble(doubleFunction);}

    @Override
    public MutableFloatList collectFloat(FloatFunction<? super T> floatFunction) {return internal.collectFloat(floatFunction);}

    @Override
    public MutableIntList collectInt(IntFunction<? super T> intFunction) {return internal.collectInt(intFunction);}

    @Override
    public MutableLongList collectLong(LongFunction<? super T> longFunction) {return internal.collectLong(longFunction);}

    @Override
    public MutableShortList collectShort(ShortFunction<? super T> shortFunction) {return internal.collectShort(shortFunction);}

    @Override
    public <P, V> MutableList<V> collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter) {return internal.collectWith(function, parameter);}

    @Override
    public <V> MutableList<V> collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function) {return internal.collectIf(predicate, function);}

    @Override
    public <V> MutableList<V> flatCollect(Function<? super T, ? extends Iterable<V>> function) {return internal.flatCollect(function);}

    @Override
    public <P, V> MutableList<V> flatCollectWith(Function2<? super T, ? super P, ? extends Iterable<V>> function, P parameter) {return internal.flatCollectWith(function, parameter);}

    @Override
    public MutableList<T> distinct() {return internal.distinct();}

    @Override
    public MutableList<T> distinct(HashingStrategy<? super T> hashingStrategy) {return internal.distinct(hashingStrategy);}

    @Override
    public <V> MutableList<T> distinctBy(Function<? super T, ? extends V> function) {return internal.distinctBy(function);}

    @Override
    public MutableList<T> sortThis(Comparator<? super T> comparator) {return internal.sortThis(comparator);}

    @Override
    public MutableList<T> sortThis() {return internal.sortThis();}

    @Override
    public <V extends Comparable<? super V>> MutableList<T> sortThisBy(Function<? super T, ? extends V> function) {return internal.sortThisBy(function);}

    @Override
    public MutableList<T> sortThisByInt(IntFunction<? super T> function) {return internal.sortThisByInt(function);}

    @Override
    public MutableList<T> sortThisByBoolean(BooleanFunction<? super T> function) {return internal.sortThisByBoolean(function);}

    @Override
    public MutableList<T> sortThisByChar(CharFunction<? super T> function) {return internal.sortThisByChar(function);}

    @Override
    public MutableList<T> sortThisByByte(ByteFunction<? super T> function) {return internal.sortThisByByte(function);}

    @Override
    public MutableList<T> sortThisByShort(ShortFunction<? super T> function) {return internal.sortThisByShort(function);}

    @Override
    public MutableList<T> sortThisByFloat(FloatFunction<? super T> function) {return internal.sortThisByFloat(function);}

    @Override
    public MutableList<T> sortThisByLong(LongFunction<? super T> function) {return internal.sortThisByLong(function);}

    @Override
    public MutableList<T> sortThisByDouble(DoubleFunction<? super T> function) {return internal.sortThisByDouble(function);}

    @Override
    public MutableList<T> subList(int fromIndex, int toIndex) {return internal.subList(fromIndex, toIndex);}

    @Override
    public MutableList<T> asUnmodifiable() {return internal.asUnmodifiable();}

    @Override
    public MutableList<T> asSynchronized() {return internal.asSynchronized();}

    @Override
    public ImmutableList<T> toImmutable() {return internal.toImmutable();}

    @Override
    public <V> MutableListMultimap<V, T> groupBy(Function<? super T, ? extends V> function) {return internal.groupBy(function);}

    @Override
    public <V> MutableListMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function) {return internal.groupByEach(function);}

    @Override
    public <S> MutableList<Pair<T, S>> zip(Iterable<S> that) {return internal.zip(that);}

    @Override
    public MutableList<Pair<T, Integer>> zipWithIndex() {return internal.zipWithIndex();}

    @Override
    public MutableList<T> take(int count) {return internal.take(count);}

    @Override
    public MutableList<T> takeWhile(Predicate<? super T> predicate) {return internal.takeWhile(predicate);}

    @Override
    public MutableList<T> drop(int count) {return internal.drop(count);}

    @Override
    public MutableList<T> dropWhile(Predicate<? super T> predicate) {return internal.dropWhile(predicate);}

    @Override
    public PartitionMutableList<T> partitionWhile(Predicate<? super T> predicate) {return internal.partitionWhile(predicate);}

    @Override
    public MutableList<T> toReversed() {return internal.toReversed();}

    @Override
    public MutableList<T> reverseThis() {return internal.reverseThis();}

    @Override
    public MutableList<T> shuffleThis() {return internal.shuffleThis();}

    @Override
    public MutableList<T> shuffleThis(Random random) {return internal.shuffleThis(random);}

    @Override
    public ImmutableList<T> toImmutableList() {return internal.toImmutableList();}

    @Deprecated
    @Override
    public <P> Twin<MutableList<T>> selectAndRejectWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.selectAndRejectWith(predicate, parameter);}

    @Override
    public boolean removeIf(Predicate<? super T> predicate) {return internal.removeIf(predicate);}

    @Override
    public <P> boolean removeIfWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.removeIfWith(predicate, parameter);}

    @Override
    public <IV, P> IV injectIntoWith(IV injectValue, Function3<? super IV, ? super T, ? super P, ? extends IV> function, P parameter) {return internal.injectIntoWith(injectValue, function, parameter);}

    @Override
    public <V> MutableObjectLongMap<V> sumByInt(Function<? super T, ? extends V> groupBy, IntFunction<? super T> function) {return internal.sumByInt(groupBy, function);}

    @Override
    public <V> MutableObjectDoubleMap<V> sumByFloat(Function<? super T, ? extends V> groupBy, FloatFunction<? super T> function) {return internal.sumByFloat(groupBy, function);}

    @Override
    public <V> MutableObjectLongMap<V> sumByLong(Function<? super T, ? extends V> groupBy, LongFunction<? super T> function) {return internal.sumByLong(groupBy, function);}

    @Override
    public <V> MutableObjectDoubleMap<V> sumByDouble(Function<? super T, ? extends V> groupBy, DoubleFunction<? super T> function) {return internal.sumByDouble(groupBy, function);}

    @Override
    public <V> MutableBag<V> countBy(Function<? super T, ? extends V> function) {return internal.countBy(function);}

    @Override
    public <V, P> MutableBag<V> countByWith(Function2<? super T, ? super P, ? extends V> function, P parameter) {return internal.countByWith(function, parameter);}

    @Override
    public <V> MutableBag<V> countByEach(Function<? super T, ? extends Iterable<V>> function) {return internal.countByEach(function);}

    @Override
    public <V> MutableMap<V, T> groupByUniqueKey(Function<? super T, ? extends V> function) {return internal.groupByUniqueKey(function);}

    @Override
    public boolean addAllIterable(Iterable<? extends T> iterable) {return internal.addAllIterable(iterable);}

    @Override
    public boolean removeAllIterable(Iterable<?> iterable) {return internal.removeAllIterable(iterable);}

    @Override
    public boolean retainAllIterable(Iterable<?> iterable) {return internal.retainAllIterable(iterable);}

    @Override
    public <K, V> MutableMap<K, V> aggregateInPlaceBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Procedure2<? super V, ? super T> mutatingAggregator) {return internal.aggregateInPlaceBy(groupBy, zeroValueFactory, mutatingAggregator);}

    @Override
    public <K, V> MutableMap<K, V> aggregateBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Function2<? super V, ? super T, ? extends V> nonMutatingAggregator) {return internal.aggregateBy(groupBy, zeroValueFactory, nonMutatingAggregator);}

    @Override
    public int size() {return internal.size();}

    @Override
    public boolean isEmpty() {return internal.isEmpty();}

    @Override
    public boolean contains(Object o) {return internal.contains(o);}

    @Override
    public @NotNull Iterator<T> iterator() {return internal.iterator();}

    @Override
    public @NotNull Object[] toArray() {return internal.toArray();}

    @Override
    public @NotNull <T1> T1[] toArray(@NotNull T1[] a) {return internal.toArray(a);}

    @Override
    public <T1> T1[] toArray(java.util.function.IntFunction<T1[]> generator) {return internal.toArray(generator);}

    @Override
    public boolean add(T t) {return internal.add(t);}

    @Override
    public boolean remove(Object o) {return internal.remove(o);}

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {return internal.containsAll(c);}

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {return internal.addAll(c);}

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {return internal.removeAll(c);}

    @Override
    public boolean removeIf(java.util.function.Predicate<? super T> filter) {return internal.removeIf(filter);}

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {return internal.retainAll(c);}

    @Override
    public void clear() {internal.clear();}

    @Override
    public boolean equals(Object o) {return internal.equals(o);}

    @Override
    public int hashCode() {return internal.hashCode();}

    @Override
    public Spliterator<T> spliterator() {return internal.spliterator();}

    @Override
    public Stream<T> stream() {return internal.stream();}

    @Override
    public Stream<T> parallelStream() {return internal.parallelStream();}

    @Override
    public void forEach(Consumer<? super T> action) {internal.forEach(action);}

    @Override
    public void forEach(Procedure<? super T> procedure) {internal.forEach(procedure);}

    @Override
    public boolean notEmpty() {return internal.notEmpty();}

    @Override
    public T getAny() {return internal.getAny();}

    @Deprecated
    @Override
    public T getFirst() {return internal.getFirst();}

    @Deprecated
    @Override
    public T getLast() {return internal.getLast();}

    @Override
    public T getOnly() {return internal.getOnly();}

    @Override
    public <V> boolean containsBy(Function<? super T, ? extends V> function, V value) {return internal.containsBy(function, value);}

    @Override
    public boolean containsAny(Collection<?> source) {return internal.containsAny(source);}

    @Override
    public boolean containsNone(Collection<?> source) {return internal.containsNone(source);}

    @Override
    public boolean containsAnyIterable(Iterable<?> source) {return internal.containsAnyIterable(source);}

    @Override
    public boolean containsNoneIterable(Iterable<?> source) {return internal.containsNoneIterable(source);}

    @Override
    public boolean containsAllIterable(Iterable<?> source) {return internal.containsAllIterable(source);}

    @Override
    public boolean containsAllArguments(Object... elements) {return internal.containsAllArguments(elements);}

    @Override
    public void each(Procedure<? super T> procedure) {internal.each(procedure);}

    @Override
    public <R extends Collection<T>> R select(Predicate<? super T> predicate, R target) {return internal.select(predicate, target);}

    @Override
    public <P, R extends Collection<T>> R selectWith(Predicate2<? super T, ? super P> predicate, P parameter, R targetCollection) {return internal.selectWith(predicate, parameter, targetCollection);}

    @Override
    public <R extends Collection<T>> R reject(Predicate<? super T> predicate, R target) {return internal.reject(predicate, target);}

    @Override
    public <P, R extends Collection<T>> R rejectWith(Predicate2<? super T, ? super P> predicate, P parameter, R targetCollection) {return internal.rejectWith(predicate, parameter, targetCollection);}

    @Override
    public <V, R extends Collection<V>> R collect(Function<? super T, ? extends V> function, R target) {return internal.collect(function, target);}

    @Override
    public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super T> booleanFunction, R target) {return internal.collectBoolean(booleanFunction, target);}

    @Override
    public <R extends MutableByteCollection> R collectByte(ByteFunction<? super T> byteFunction, R target) {return internal.collectByte(byteFunction, target);}

    @Override
    public <R extends MutableCharCollection> R collectChar(CharFunction<? super T> charFunction, R target) {return internal.collectChar(charFunction, target);}

    @Override
    public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super T> doubleFunction, R target) {return internal.collectDouble(doubleFunction, target);}

    @Override
    public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super T> floatFunction, R target) {return internal.collectFloat(floatFunction, target);}

    @Override
    public <R extends MutableIntCollection> R collectInt(IntFunction<? super T> intFunction, R target) {return internal.collectInt(intFunction, target);}

    @Override
    public <R extends MutableLongCollection> R collectLong(LongFunction<? super T> longFunction, R target) {return internal.collectLong(longFunction, target);}

    @Override
    public <R extends MutableShortCollection> R collectShort(ShortFunction<? super T> shortFunction, R target) {return internal.collectShort(shortFunction, target);}

    @Override
    public <P, V, R extends Collection<V>> R collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter, R targetCollection) {return internal.collectWith(function, parameter, targetCollection);}

    @Override
    public <V, R extends Collection<V>> R collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function, R target) {return internal.collectIf(predicate, function, target);}

    @Override
    public <R extends MutableByteCollection> R flatCollectByte(Function<? super T, ? extends ByteIterable> function, R target) {return internal.flatCollectByte(function, target);}

    @Override
    public <R extends MutableCharCollection> R flatCollectChar(Function<? super T, ? extends CharIterable> function, R target) {return internal.flatCollectChar(function, target);}

    @Override
    public <R extends MutableIntCollection> R flatCollectInt(Function<? super T, ? extends IntIterable> function, R target) {return internal.flatCollectInt(function, target);}

    @Override
    public <R extends MutableShortCollection> R flatCollectShort(Function<? super T, ? extends ShortIterable> function, R target) {return internal.flatCollectShort(function, target);}

    @Override
    public <R extends MutableDoubleCollection> R flatCollectDouble(Function<? super T, ? extends DoubleIterable> function, R target) {return internal.flatCollectDouble(function, target);}

    @Override
    public <R extends MutableFloatCollection> R flatCollectFloat(Function<? super T, ? extends FloatIterable> function, R target) {return internal.flatCollectFloat(function, target);}

    @Override
    public <R extends MutableLongCollection> R flatCollectLong(Function<? super T, ? extends LongIterable> function, R target) {return internal.flatCollectLong(function, target);}

    @Override
    public <R extends MutableBooleanCollection> R flatCollectBoolean(Function<? super T, ? extends BooleanIterable> function, R target) {return internal.flatCollectBoolean(function, target);}

    @Override
    public <V, R extends Collection<V>> R flatCollect(Function<? super T, ? extends Iterable<V>> function, R target) {return internal.flatCollect(function, target);}

    @Override
    public <P, V, R extends Collection<V>> R flatCollectWith(Function2<? super T, ? super P, ? extends Iterable<V>> function, P parameter, R target) {return internal.flatCollectWith(function, parameter, target);}

    @Override
    public T detect(Predicate<? super T> predicate) {return internal.detect(predicate);}

    @Override
    public <P> T detectWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.detectWith(predicate, parameter);}

    @Override
    public Optional<T> detectOptional(Predicate<? super T> predicate) {return internal.detectOptional(predicate);}

    @Override
    public <P> Optional<T> detectWithOptional(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.detectWithOptional(predicate, parameter);}

    @Override
    public T detectIfNone(Predicate<? super T> predicate, Function0<? extends T> function) {return internal.detectIfNone(predicate, function);}

    @Override
    public <P> T detectWithIfNone(Predicate2<? super T, ? super P> predicate, P parameter, Function0<? extends T> function) {return internal.detectWithIfNone(predicate, parameter, function);}

    @Override
    public int count(Predicate<? super T> predicate) {return internal.count(predicate);}

    @Override
    public <P> int countWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.countWith(predicate, parameter);}

    @Override
    public boolean anySatisfy(Predicate<? super T> predicate) {return internal.anySatisfy(predicate);}

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.anySatisfyWith(predicate, parameter);}

    @Override
    public boolean allSatisfy(Predicate<? super T> predicate) {return internal.allSatisfy(predicate);}

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.allSatisfyWith(predicate, parameter);}

    @Override
    public boolean noneSatisfy(Predicate<? super T> predicate) {return internal.noneSatisfy(predicate);}

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super T, ? super P> predicate, P parameter) {return internal.noneSatisfyWith(predicate, parameter);}

    @Override
    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super T, ? extends IV> function) {return internal.injectInto(injectedValue, function);}

    @Deprecated
    @Override
    public int injectInto(int injectedValue, IntObjectToIntFunction<? super T> function) {return internal.injectInto(injectedValue, function);}

    @Override
    public int injectIntoInt(int injectedValue, IntObjectToIntFunction<? super T> function) {return internal.injectIntoInt(injectedValue, function);}

    @Deprecated
    @Override
    public long injectInto(long injectedValue, LongObjectToLongFunction<? super T> function) {return internal.injectInto(injectedValue, function);}

    @Override
    public long injectIntoLong(long injectedValue, LongObjectToLongFunction<? super T> function) {return internal.injectIntoLong(injectedValue, function);}

    @Deprecated
    @Override
    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super T> function) {return internal.injectInto(injectedValue, function);}

    @Override
    public float injectIntoFloat(float injectedValue, FloatObjectToFloatFunction<? super T> function) {return internal.injectIntoFloat(injectedValue, function);}

    @Deprecated
    @Override
    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super T> function) {return internal.injectInto(injectedValue, function);}

    @Override
    public double injectIntoDouble(double injectedValue, DoubleObjectToDoubleFunction<? super T> function) {return internal.injectIntoDouble(injectedValue, function);}

    @Override
    public <R extends Collection<T>> R into(R target) {return internal.into(target);}

    @Override
    public MutableList<T> toList() {return internal.toList();}

    @Override
    public MutableList<T> toSortedList() {return internal.toSortedList();}

    @Override
    public MutableList<T> toSortedList(Comparator<? super T> comparator) {return internal.toSortedList(comparator);}

    @Override
    public <V extends Comparable<? super V>> MutableList<T> toSortedListBy(Function<? super T, ? extends V> function) {return internal.toSortedListBy(function);}

    @Override
    public MutableSet<T> toSet() {return internal.toSet();}

    @Override
    public MutableSortedSet<T> toSortedSet() {return internal.toSortedSet();}

    @Override
    public MutableSortedSet<T> toSortedSet(Comparator<? super T> comparator) {return internal.toSortedSet(comparator);}

    @Override
    public <V extends Comparable<? super V>> MutableSortedSet<T> toSortedSetBy(Function<? super T, ? extends V> function) {return internal.toSortedSetBy(function);}

    @Override
    public MutableBag<T> toBag() {return internal.toBag();}

    @Override
    public MutableSortedBag<T> toSortedBag() {return internal.toSortedBag();}

    @Override
    public MutableSortedBag<T> toSortedBag(Comparator<? super T> comparator) {return internal.toSortedBag(comparator);}

    @Override
    public <V extends Comparable<? super V>> MutableSortedBag<T> toSortedBagBy(Function<? super T, ? extends V> function) {return internal.toSortedBagBy(function);}

    @Override
    public <NK, NV> MutableMap<NK, NV> toMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toMap(keyFunction, valueFunction);}

    @Override
    public <NK, NV, R extends Map<NK, NV>> R toMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction, R target) {return internal.toMap(keyFunction, valueFunction, target);}

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toSortedMap(keyFunction, valueFunction);}

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Comparator<? super NK> comparator, Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toSortedMap(comparator, keyFunction, valueFunction);}

    @Override
    public <KK extends Comparable<? super KK>, NK, NV> MutableSortedMap<NK, NV> toSortedMapBy(Function<? super NK, KK> sortBy, Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toSortedMapBy(sortBy, keyFunction, valueFunction);}

    @Override
    public <NK, NV> MutableBiMap<NK, NV> toBiMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toBiMap(keyFunction, valueFunction);}

    @Override
    public ImmutableSet<T> toImmutableSet() {return internal.toImmutableSet();}

    @Override
    public ImmutableBag<T> toImmutableBag() {return internal.toImmutableBag();}

    @Override
    public ImmutableList<T> toImmutableSortedList() {return internal.toImmutableSortedList();}

    @Override
    public ImmutableList<T> toImmutableSortedList(Comparator<? super T> comparator) {return internal.toImmutableSortedList(comparator);}

    @Override
    public <V extends Comparable<? super V>> ImmutableList<T> toImmutableSortedListBy(Function<? super T, ? extends V> function) {return internal.toImmutableSortedListBy(function);}

    @Override
    public ImmutableSortedSet<T> toImmutableSortedSet() {return internal.toImmutableSortedSet();}

    @Override
    public ImmutableSortedSet<T> toImmutableSortedSet(Comparator<? super T> comparator) {return internal.toImmutableSortedSet(comparator);}

    @Override
    public <V extends Comparable<? super V>> ImmutableSortedSet<T> toImmutableSortedSetBy(Function<? super T, ? extends V> function) {return internal.toImmutableSortedSetBy(function);}

    @Override
    public ImmutableSortedBag<T> toImmutableSortedBag() {return internal.toImmutableSortedBag();}

    @Override
    public ImmutableSortedBag<T> toImmutableSortedBag(Comparator<? super T> comparator) {return internal.toImmutableSortedBag(comparator);}

    @Override
    public <V extends Comparable<? super V>> ImmutableSortedBag<T> toImmutableSortedBagBy(Function<? super T, ? extends V> function) {return internal.toImmutableSortedBagBy(function);}

    @Override
    public <NK, NV> ImmutableMap<NK, NV> toImmutableMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toImmutableMap(keyFunction, valueFunction);}

    @Override
    public <NK, NV> ImmutableBiMap<NK, NV> toImmutableBiMap(Function<? super T, ? extends NK> keyFunction, Function<? super T, ? extends NV> valueFunction) {return internal.toImmutableBiMap(keyFunction, valueFunction);}

    @Override
    public LazyIterable<T> asLazy() {return internal.asLazy();}

    @Override
    public T min(Comparator<? super T> comparator) {return internal.min(comparator);}

    @Override
    public T max(Comparator<? super T> comparator) {return internal.max(comparator);}

    @Override
    public Optional<T> minOptional(Comparator<? super T> comparator) {return internal.minOptional(comparator);}

    @Override
    public Optional<T> maxOptional(Comparator<? super T> comparator) {return internal.maxOptional(comparator);}

    @Override
    public T min() {return internal.min();}

    @Override
    public T max() {return internal.max();}

    @Override
    public Optional<T> minOptional() {return internal.minOptional();}

    @Override
    public Optional<T> maxOptional() {return internal.maxOptional();}

    @Override
    public <V extends Comparable<? super V>> T minBy(Function<? super T, ? extends V> function) {return internal.minBy(function);}

    @Override
    public <V extends Comparable<? super V>> T maxBy(Function<? super T, ? extends V> function) {return internal.maxBy(function);}

    @Override
    public <V extends Comparable<? super V>> Optional<T> minByOptional(Function<? super T, ? extends V> function) {return internal.minByOptional(function);}

    @Override
    public <V extends Comparable<? super V>> Optional<T> maxByOptional(Function<? super T, ? extends V> function) {return internal.maxByOptional(function);}

    @Override
    public long sumOfInt(IntFunction<? super T> function) {return internal.sumOfInt(function);}

    @Override
    public double sumOfFloat(FloatFunction<? super T> function) {return internal.sumOfFloat(function);}

    @Override
    public long sumOfLong(LongFunction<? super T> function) {return internal.sumOfLong(function);}

    @Override
    public double sumOfDouble(DoubleFunction<? super T> function) {return internal.sumOfDouble(function);}

    @Override
    public IntSummaryStatistics summarizeInt(IntFunction<? super T> function) {return internal.summarizeInt(function);}

    @Override
    public DoubleSummaryStatistics summarizeFloat(FloatFunction<? super T> function) {return internal.summarizeFloat(function);}

    @Override
    public LongSummaryStatistics summarizeLong(LongFunction<? super T> function) {return internal.summarizeLong(function);}

    @Override
    public DoubleSummaryStatistics summarizeDouble(DoubleFunction<? super T> function) {return internal.summarizeDouble(function);}

    @Override
    public <R, A> R reduceInPlace(Collector<? super T, A, R> collector) {return internal.reduceInPlace(collector);}

    @Override
    public <R> R reduceInPlace(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator) {return internal.reduceInPlace(supplier, accumulator);}

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {return internal.reduce(accumulator);}

    @Override
    public String makeString() {return internal.makeString();}

    @Override
    public String makeString(String separator) {return internal.makeString(separator);}

    @Override
    public String makeString(String start, String separator, String end) {return internal.makeString(start, separator, end);}

    @Override
    public String makeString(Function<? super T, Object> function, String start, String separator, String end) {return internal.makeString(function, start, separator, end);}

    @Override
    public void appendString(Appendable appendable) {internal.appendString(appendable);}

    @Override
    public void appendString(Appendable appendable, String separator) {internal.appendString(appendable, separator);}

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end) {internal.appendString(appendable, start, separator, end);}

    @Override
    public <V, R extends MutableBagIterable<V>> R countBy(Function<? super T, ? extends V> function, R target) {return internal.countBy(function, target);}

    @Override
    public <V, P, R extends MutableBagIterable<V>> R countByWith(Function2<? super T, ? super P, ? extends V> function, P parameter, R target) {return internal.countByWith(function, parameter, target);}

    @Override
    public <V, R extends MutableBagIterable<V>> R countByEach(Function<? super T, ? extends Iterable<V>> function, R target) {return internal.countByEach(function, target);}

    @Override
    public <V, R extends MutableMultimap<V, T>> R groupBy(Function<? super T, ? extends V> function, R target) {return internal.groupBy(function, target);}

    @Override
    public <V, R extends MutableMultimap<V, T>> R groupByEach(Function<? super T, ? extends Iterable<V>> function, R target) {return internal.groupByEach(function, target);}

    @Override
    public <V, R extends MutableMapIterable<V, T>> R groupByUniqueKey(Function<? super T, ? extends V> function, R target) {return internal.groupByUniqueKey(function, target);}

    @Override
    public String toString() {return internal.toString();}

    @Deprecated
    @Override
    public <S, R extends Collection<Pair<T, S>>> R zip(Iterable<S> that, R target) {return internal.zip(that, target);}

    @Deprecated
    @Override
    public <R extends Collection<Pair<T, Integer>>> R zipWithIndex(R target) {return internal.zipWithIndex(target);}

    @Override
    public RichIterable<RichIterable<T>> chunk(int size) {return internal.chunk(size);}

    @Override
    public <K, V, R extends MutableMapIterable<K, V>> R aggregateBy(Function<? super T, ? extends K> groupBy, Function0<? extends V> zeroValueFactory, Function2<? super V, ? super T, ? extends V> nonMutatingAggregator, R target) {return internal.aggregateBy(groupBy, zeroValueFactory, nonMutatingAggregator, target);}

    @Override
    public <K, V, R extends MutableMultimap<K, V>> R groupByAndCollect(Function<? super T, ? extends K> groupByFunction, Function<? super T, ? extends V> collectFunction, R target) {return internal.groupByAndCollect(groupByFunction, collectFunction, target);}

    @Deprecated
    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super T> objectIntProcedure) {internal.forEachWithIndex(objectIntProcedure);}

    @Override
    public <P> void forEachWith(Procedure2<? super T, ? super P> procedure, P parameter) {internal.forEachWith(procedure, parameter);}

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {return internal.addAll(index, c);}

    @Override
    public void replaceAll(UnaryOperator<T> operator) {internal.replaceAll(operator);}

    @Override
    public void sort(Comparator<? super T> c) {internal.sort(c);}

    @Override
    public T get(int index) {return internal.get(index);}

    @Override
    public T set(int index, T element) {return internal.set(index, element);}

    @Override
    public void add(int index, T element) {internal.add(index, element);}

    @Override
    public T remove(int index) {return internal.remove(index);}

    @Override
    public int indexOf(Object o) {return internal.indexOf(o);}

    @Override
    public int lastIndexOf(Object o) {return internal.lastIndexOf(o);}

    @Override
    public @NotNull ListIterator<T> listIterator() {return internal.listIterator();}

    @Override
    public @NotNull ListIterator<T> listIterator(int index) {return internal.listIterator(index);}

    @Override
    public void addFirst(T t) {internal.addFirst(t);}

    @Override
    public void addLast(T t) {internal.addLast(t);}

    @Override
    public T removeFirst() {return internal.removeFirst();}

    @Override
    public T removeLast() {return internal.removeLast();}

    @Override
    public List<T> reversed() {return internal.reversed();}

    public static <E> List<E> of() {return List.of();}

    public static <E> List<E> of(E e1) {return List.of(e1);}

    public static <E> List<E> of(E e1, E e2) {return List.of(e1, e2);}

    public static <E> List<E> of(E e1, E e2, E e3) {return List.of(e1, e2, e3);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4) {return List.of(e1, e2, e3, e4);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {return List.of(e1, e2, e3, e4, e5);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {return List.of(e1, e2, e3, e4, e5, e6);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {return List.of(e1, e2, e3, e4, e5, e6, e7);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {return List.of(e1, e2, e3, e4, e5, e6, e7, e8);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {return List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9);}

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {return List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);}

    @SafeVarargs
    public static <E> List<E> of(E... elements) {return List.of(elements);}

    public static <E> List<E> copyOf(Collection<? extends E> coll) {return List.copyOf(coll);}

    @Beta
    @Override
    public ParallelListIterable<T> asParallel(ExecutorService executorService, int batchSize) {return internal.asParallel(executorService, batchSize);}

    @Override
    public int binarySearch(T key, Comparator<? super T> comparator) {return internal.binarySearch(key, comparator);}

    @Override
    public int binarySearch(T key) {return internal.binarySearch(key);}

    @Override
    public <T2> void forEachInBoth(ListIterable<T2> other, Procedure2<? super T, ? super T2> procedure) {internal.forEachInBoth(other, procedure);}

    @Override
    public void reverseForEach(Procedure<? super T> procedure) {internal.reverseForEach(procedure);}

    @Override
    public void reverseForEachWithIndex(ObjectIntProcedure<? super T> procedure) {internal.reverseForEachWithIndex(procedure);}

    @Override
    public LazyIterable<T> asReversed() {return internal.asReversed();}

    @Override
    public int detectLastIndex(Predicate<? super T> predicate) {return internal.detectLastIndex(predicate);}

    @Override
    public Optional<T> getFirstOptional() {return internal.getFirstOptional();}

    @Override
    public Optional<T> getLastOptional() {return internal.getLastOptional();}

    @Override
    public <S> boolean corresponds(OrderedIterable<S> other, Predicate2<? super T, ? super S> predicate) {return internal.corresponds(other, predicate);}

    @Override
    public void forEach(int startIndex, int endIndex, Procedure<? super T> procedure) {internal.forEach(startIndex, endIndex, procedure);}

    @Override
    public void forEachWithIndex(int fromIndex, int toIndex, ObjectIntProcedure<? super T> objectIntProcedure) {internal.forEachWithIndex(fromIndex, toIndex, objectIntProcedure);}

    @Override
    public MutableStack<T> toStack() {return internal.toStack();}

    @Override
    public <V, R extends Collection<V>> R collectWithIndex(ObjectIntToObjectFunction<? super T, ? extends V> function, R target) {return internal.collectWithIndex(function, target);}

    @Override
    public <R extends Collection<T>> R selectWithIndex(ObjectIntPredicate<? super T> predicate, R target) {return internal.selectWithIndex(predicate, target);}

    @Override
    public <R extends Collection<T>> R rejectWithIndex(ObjectIntPredicate<? super T> predicate, R target) {return internal.rejectWithIndex(predicate, target);}

    @Override
    public int detectIndex(Predicate<? super T> predicate) {return internal.detectIndex(predicate);}

    //endregion
}
