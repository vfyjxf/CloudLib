package dev.vfyjxf.cloudlib.api.lifecycle;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class LifecycleStates {

    private final String namespace;
    private final Set<String> ids = new LinkedHashSet<>();

    private LifecycleStates(String namespace) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("Lifecycle state namespace must not be blank");
        }
    }

    public static LifecycleStates create(String namespace) {
        return new LifecycleStates(namespace);
    }

    public LifecycleState<Void> context(String path) {
        return context(path, Void.class);
    }

    public <T> LifecycleState<T> context(String path, Class<T> type) {
        return create(path, LifecycleState.Kind.context, type);
    }

    public LifecycleState<Void> event(String path) {
        return event(path, Void.class);
    }

    public <T> LifecycleState<T> event(String path, Class<T> type) {
        return create(path, LifecycleState.Kind.event, type);
    }

    private <T> LifecycleState<T> create(String path, LifecycleState.Kind kind, Class<T> type) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("Lifecycle state path must not be blank");
        }
        String id = namespace + "." + path;
        if (!ids.add(id)) {
            throw new IllegalArgumentException("Duplicate lifecycle state: " + id);
        }
        return new LifecycleState<>(id, kind, type);
    }
}
