package dev.vfyjxf.cloudlib.api.nodes.concept;

public interface GroupBlueprint<R, E> {

    R construct(GroupScope<E> scope);

}
