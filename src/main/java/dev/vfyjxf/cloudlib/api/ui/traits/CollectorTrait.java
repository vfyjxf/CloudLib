package dev.vfyjxf.cloudlib.api.ui.traits;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;
import java.util.function.Predicate;

@ApiStatus.Internal
public class CollectorTrait implements ITrait {

    private final MutableList<ITrait> traits = Lists.mutable.empty();

    @Override
    public <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator) {
        return traits.injectInto(initial, (acc, trait) -> trait.foldIn(acc, operator));
    }

    @Override
    public <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator) {
        return traits.injectInto(initial, (acc, trait) -> trait.foldOut(acc, operator));
    }

    @Override
    public boolean any(Predicate<ITraitElement> predicate) {
        return traits.anySatisfy(trait -> trait.any(predicate));
    }

    @Override
    public boolean all(Predicate<ITraitElement> predicate) {
        return traits.allSatisfy(trait -> trait.all(predicate));
    }

    @Override
    public ITrait then(ITrait trait) {
        traits.add(trait);
        return this;
    }

    public ITrait toImmutable() {
        if (traits.isEmpty()) return ITrait.EMPTY;
        if (traits.size() == 1) {
            return traits.getFirst();
        } else {
            ITrait trait = ITrait.EMPTY;
            for (ITrait next : traits) {
                trait = trait.then(next);
            }
            return trait;
        }
    }

}
