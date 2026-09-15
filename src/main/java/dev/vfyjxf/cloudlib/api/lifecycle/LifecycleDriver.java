package dev.vfyjxf.cloudlib.api.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LifecycleDriver {

    private final String name;
    private final Logger logger;
    private final LinkedHashSet<LifecycleState<?>> required;
    private final List<LifecycleSource> sources;
    private final Runnable loadAction;
    private final Runnable unloadAction;
    private final Runnable reloadAction;
    private final LifecycleReceiver receiver = new Receiver();

    private final Map<LifecycleState<?>, Object> contexts = new LinkedHashMap<>();
    private final Set<LifecycleState<?>> events = new LinkedHashSet<>();
    private boolean loaded;

    private LifecycleDriver(Builder builder) {
        this.name = builder.name;
        this.logger = builder.logger;
        this.required = new LinkedHashSet<>(builder.required);
        this.sources = List.copyOf(builder.sources);
        this.loadAction = builder.loadAction;
        this.unloadAction = builder.unloadAction;
        this.reloadAction = builder.reloadAction != null ? builder.reloadAction : this::unloadThenLoad;
        sources.forEach(source -> source.bind(receiver));
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public LifecycleReceiver receiver() {
        return receiver;
    }

    public String name() {
        return name;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<LifecycleState<?>> missing() {
        ArrayList<LifecycleState<?>> missing = new ArrayList<>();
        for (LifecycleState<?> state : required) {
            if (state.isContext() && !contexts.containsKey(state)) {
                missing.add(state);
            } else if (state.isEvent() && !events.contains(state)) {
                missing.add(state);
            }
        }
        return List.copyOf(missing);
    }

    private <T> void context(LifecycleState<T> state, T value, String reason) {
        require(state);
        if (!state.isContext()) {
            throw new IllegalArgumentException("Lifecycle state is not a context: " + state);
        }
        Object normalized = state.normalizeValue(value);
        Object previous = contexts.put(state, normalized);
        if (previous != null && !Objects.equals(previous, normalized)) {
            resetEvents(reason);
            if (loaded) {
                unload(reason);
            }
        }
        tryRun(reason, false);
    }

    private <T> void event(LifecycleState<T> state, T value, String reason) {
        require(state);
        if (!state.isEvent()) {
            throw new IllegalArgumentException("Lifecycle state is not an event: " + state);
        }
        state.normalizeValue(value);
        events.add(state);
        tryRun(reason, true);
    }

    private void clear(LifecycleState<?> state, String reason) {
        require(state);
        if (state.isContext()) {
            Object previous = contexts.remove(state);
            if (previous != null) {
                resetEvents(reason);
                if (loaded) {
                    unload(reason);
                }
            }
        } else {
            events.remove(state);
        }
    }

    private void resetEvents(String reason) {
        if (!events.isEmpty()) {
            logger.debug("{} reset events reason={}", name, reason);
        }
        events.clear();
    }

    private boolean reload(String reason) {
        if (!loaded) {
            return false;
        }
        reloadLoaded(reason);
        return true;
    }

    private boolean loadNow(String reason) {
        if (loaded) {
            return false;
        }
        load(reason);
        return true;
    }

    private void tryRun(String reason, boolean eventDriven) {
        if (!ready()) {
            return;
        }
        if (!loaded) {
            load(reason);
        } else if (eventDriven) {
            reloadLoaded(reason);
        }
    }

    private boolean ready() {
        return missing().isEmpty();
    }

    private void load(String reason) {
        logger.info("{} load reason={}", name, reason);
        loadAction.run();
        loaded = true;
        events.clear();
    }

    private void unload(String reason) {
        logger.info("{} unload reason={}", name, reason);
        unloadAction.run();
        loaded = false;
        events.clear();
    }

    private void reloadLoaded(String reason) {
        logger.info("{} reload reason={}", name, reason);
        reloadAction.run();
        events.clear();
    }

    private void unloadThenLoad() {
        unloadAction.run();
        loadAction.run();
    }

    private void require(LifecycleState<?> state) {
        Objects.requireNonNull(state, "state");
        if (!required.contains(state)) {
            throw new IllegalArgumentException("Lifecycle state is not required by " + name + ": " + state);
        }
    }

    public static final class Builder {
        private final String name;
        private final LinkedHashSet<LifecycleState<?>> required = new LinkedHashSet<>();
        private final List<LifecycleSource> sources = new ArrayList<>();
        private Logger logger = LoggerFactory.getLogger(LifecycleDriver.class);
        private Runnable loadAction = () -> {};
        private Runnable unloadAction = () -> {};
        private Runnable reloadAction;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Driver name must not be blank");
            }
        }

        public Builder logger(Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public Builder require(LifecycleState<?> state) {
            Objects.requireNonNull(state, "state");
            if (!required.add(state)) {
                throw new IllegalArgumentException("Duplicate lifecycle state requirement: " + state);
            }
            return this;
        }

        public Builder source(LifecycleSource source) {
            sources.add(Objects.requireNonNull(source, "source"));
            return this;
        }

        public Builder onLoad(Runnable loadAction) {
            this.loadAction = Objects.requireNonNull(loadAction, "loadAction");
            return this;
        }

        public Builder onUnload(Runnable unloadAction) {
            this.unloadAction = Objects.requireNonNull(unloadAction, "unloadAction");
            return this;
        }

        public Builder onReload(Runnable reloadAction) {
            this.reloadAction = Objects.requireNonNull(reloadAction, "reloadAction");
            return this;
        }

        public LifecycleDriver build() {
            return new LifecycleDriver(this);
        }
    }

    private final class Receiver implements LifecycleReceiver {

        @Override
        public <T> void context(LifecycleState<T> state, T value, String reason) {
            LifecycleDriver.this.context(state, value, reason);
        }

        @Override
        public <T> void event(LifecycleState<T> state, T value, String reason) {
            LifecycleDriver.this.event(state, value, reason);
        }

        @Override
        public void clear(LifecycleState<?> state, String reason) {
            LifecycleDriver.this.clear(state, reason);
        }

        @Override
        public void resetEvents(String reason) {
            LifecycleDriver.this.resetEvents(reason);
        }

        @Override
        public boolean reload(String reason) {
            return LifecycleDriver.this.reload(reason);
        }

        @Override
        public boolean loadNow(String reason) {
            return LifecycleDriver.this.loadNow(reason);
        }

        @Override
        public boolean isLoaded() {
            return LifecycleDriver.this.isLoaded();
        }

        @Override
        public List<LifecycleState<?>> missing() {
            return LifecycleDriver.this.missing();
        }
    }
}
