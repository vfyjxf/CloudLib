package dev.vfyjxf.cloudlib.api.ui;

import java.util.function.Function;

public interface GroupSpec<R extends Group<T>, T extends Widget> {

    static <R extends Group<T>, T extends Widget> GroupSpec<R, T> of(Function<Scope<T>, R> constructor) {
        return constructor::apply;
    }

    R construct(Scope<T> scope);

}
