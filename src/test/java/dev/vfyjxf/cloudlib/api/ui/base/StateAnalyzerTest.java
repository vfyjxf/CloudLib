package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.state.State;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.api.ui.base.WidgetManageTest.DebugState;
import static dev.vfyjxf.cloudlib.api.ui.base.WidgetManageTest.InternalWidget;

public class StateAnalyzerTest {

    @Test
    void findStateDependency() {
        State state = new DebugState("StateA");
        State anotherState = new DebugState("StateB");
        var rootSpec = GroupSpec.of(plan -> {
            System.out.println(state);
            plan.group(GroupSpec.of((Plan<InternalWidget> viewPlan) -> {
                System.out.println(anotherState);
                viewPlan.include(new InternalWidget());
                viewPlan.include(new InternalWidget());
                return new WidgetGroup<>();
            }));
            return new RootWidget();
        });
        WidgetManager widgetManagement = new WidgetManager(rootSpec);
        System.out.println(widgetManagement);
    }
}
