package dev.vfyjxf.cloudlib.api.nodes.concept;

import java.util.Collection;

public interface GroupScope<T> {

    void instance(InstanceBlueprint<T> blueprint);

    void group(GroupBlueprint<?, T> blueprint);

    void instance(T instance);

    void instance(Collection<T> instances);

    void group(T groupInstance);

    void group(Collection<T> groupInstances);

}


