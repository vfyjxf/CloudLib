package dev.vfyjxf.cloudlib.api.ui.base;


@FunctionalInterface
public interface ScreenFactory<S extends BasicSpecScreen> {

    S create(PlanFragment<Widget> mainGroup, PlanFragment<Widget> overlay);

}
