package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.utils.ClassUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class StateContext {

    private final StateContext parent;

    StateContext(StateContext parent) {
        this.parent = parent;
    }

    public @Nullable StateContext parent() {
        return parent;
    }


    public <T extends State> T findState(Class<? extends T> type, Predicate<T> predicate) {
        return null;
    }


    @SafeVarargs
    public final <T extends State> T findState(Predicate<T> predicate, T... type) {
        Class<T> genericType = ClassUtils.getGenericType(type);
        return findState(genericType, predicate);
    }


}
