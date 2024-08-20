package dev.vfyjxf.cloudlib.api.ui.traits;

public abstract class TraitNodeElement<N extends ITrait.Node> implements ITraitElement {

    public abstract N create();

    public abstract void update(N node);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
