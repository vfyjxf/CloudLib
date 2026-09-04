package dev.vfyjxf.cloudlib.api.lifecycle;

@FunctionalInterface
public interface LifecycleSource {

    void bind(LifecycleReceiver receiver);
}
