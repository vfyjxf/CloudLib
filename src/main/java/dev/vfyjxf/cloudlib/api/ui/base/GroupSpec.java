package dev.vfyjxf.cloudlib.api.ui.base;

import java.util.function.Function;

public interface GroupSpec<R extends Group<T>, T extends Widget> {

    static <R extends Group<T>, T extends Widget> GroupSpec<R, T> of(Function<Plan<T>, R> constructor) {
        return constructor::apply;
    }

    R construct(Plan<T> plan);

}
