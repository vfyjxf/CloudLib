package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.state.ReadableState;
import dev.vfyjxf.cloudlib.api.ui.state.State;
import org.junit.jupiter.api.Test;

public class WidgetManageTest {

    @Test
    void testWidgetBuild() {
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
        WidgetManager widgetManager = new WidgetManager(rootSpec);
        System.out.println(widgetManager);
    }

    static class InternalWidget extends Widget {
        // Internal widget implementation
    }

    record DebugState(String value) implements ReadableState<String> {

        @Override
        public boolean changed() {
            return false;
        }
    }

}












