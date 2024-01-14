package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.annotations.CallSuper;
import dev.vfyjxf.cloudlib.api.annotations.Getter;
import dev.vfyjxf.cloudlib.api.ui.widgets.IBuildContext;
import dev.vfyjxf.cloudlib.api.ui.widgets.IStatefulWidget;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface IState<T extends IStatefulWidget> {

    @Getter
    T widget();

    T build();

    @NotNull
    IBuildContext buildContext();

    boolean mounted();

    @CallSuper
    void initState();

    @CallSuper
    void didUpdateWidget(T oldWidget);

    @CallSuper
    default void reassemble() {
    }

    void setState(Runnable function);

    @CallSuper
    default void deactivate() {
    }

    @CallSuper
    default void activate() {
    }

    @CallSuper
    default void dispose() {

    }

    @CallSuper
    default void didChangeDependencies() {
    }

}
