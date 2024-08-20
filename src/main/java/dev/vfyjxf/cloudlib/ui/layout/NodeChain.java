package dev.vfyjxf.cloudlib.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.traits.CombinedTrait;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.api.ui.traits.ITraitElement;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class NodeChain {

    private final LayoutHandler layoutHandler;

    private ITrait trait;
    private MutableList<ITraitElement> elements = Lists.mutable.empty();

    public NodeChain(LayoutHandler layoutHandler) {
        this.layoutHandler = layoutHandler;
    }

    public void setTrait(ITrait trait) {
        this.trait = trait;
    }

    private void updateNodes(ITrait trait) {
        var before = elements;
        int capacity = Math.max(elements.size(), 16);
        MutableList<ITrait> traits = Lists.mutable.withInitialCapacity(capacity);
        MutableList<ITraitElement> elements = Lists.mutable.withInitialCapacity(capacity);
        traits.add(trait);
        while (traits.notEmpty()) {
            ITrait currentTrait = traits.remove(traits.size() - 1);
            if (currentTrait instanceof CombinedTrait combinedTrait) {
                traits.add(combinedTrait.getInner());
                traits.add(combinedTrait.getOuter());
                continue;
            }
            if (currentTrait instanceof ITraitElement element) {
                elements.add(element);
                continue;
            }
            currentTrait.all(elements::add);
        }
        if (before.size() == elements.size()) {

        } else if (!layoutHandler.attached() && before.isEmpty()) {

        }
    }

    private static MutableList<ITraitElement> getElements() {

    }

    private static Action getAction(ITraitElement before, ITraitElement after) {
        if (before == after) {
            return Action.REUSE;
        }
        if (before.getClass() == after.getClass()) {
            return Action.UPDATE;
        }
        return Action.REPLACE;
    }

    private enum Action {
        REUSE,
        REPLACE,
        UPDATE
    }

}
