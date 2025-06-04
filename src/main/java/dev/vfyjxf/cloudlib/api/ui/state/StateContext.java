package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.utils.ClassUtils;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public final class StateContext {
    private static final Logger log = LoggerFactory.getLogger("StateContext");

    private final MutableMap<Class<?>, CheckStrategy<?>> strategies = Maps.mutable.empty();

    private final StateContext parent;

    StateContext(StateContext parent) {
        this.parent = parent;
    }

    public @Nullable StateContext parent() {
        return parent;
    }

    //region check strategy

    public <T> StateContext addStrategy(Class<T> type, CheckStrategy<T> strategy) {
        if (strategies.containsKey(type)) {
            log.warn("Trying to add a check strategy for type {} which is already registered", type);
        }
        strategies.put(type, strategy);
        return this;
    }

    public <T> StateContext replaceStrategy(Class<T> type, CheckStrategy<T> strategy) {
        if (strategies.containsKey(type)) {
            strategies.put(type, strategy);
        } else {
            log.warn("Trying to replace a check strategy for type {} which is not registered", type);
        }
        return this;
    }

    public <T> StateContext registerStrategy(Class<T> type, CheckStrategy<T> strategy) {
        if (strategies.containsKey(type)) {
            log.warn("Trying to register a check strategy for type {} which is already registered", type);
        } else {
            strategies.put(type, strategy);
        }
        return this;
    }

    @SafeVarargs
    public final <T> StateContext registerStrategy(CheckStrategy<T> strategy, T... typeCatch) {
        Class<T> type = ClassUtils.getGenericType(typeCatch);
        return registerStrategy(type, strategy);
    }

    @SafeVarargs
    public final <T> StateContext replaceStrategy(CheckStrategy<T> strategy, T... typeCatch) {
        Class<T> type = ClassUtils.getGenericType(typeCatch);
        return replaceStrategy(type, strategy);
    }

    @SafeVarargs
    public final <T> StateContext addStrategy(CheckStrategy<T> strategy, T... typeCatch) {
        Class<T> type = ClassUtils.getGenericType(typeCatch);
        return addStrategy(type, strategy);
    }

    @Contract("_ -> new")
    public StateContext with(Consumer<StateContext> configurator) {
        StateContext stateContext = new StateContext(this);
        configurator.accept(stateContext);
        return stateContext;
    }

    @SuppressWarnings("unchecked")
    public <T> CheckStrategy<T> getStrategy(Class<T> type) {
        CheckStrategy<T> strategy = (CheckStrategy<T>) strategies.get(type);
        if (strategy == null) {
            if (parent == null) {
                throw new IllegalStateException(
                        "No strategy found for type " + type.getName()
                );
            } else return parent.getStrategy(type);
        }
        return strategy;
    }

    //endregion
}
