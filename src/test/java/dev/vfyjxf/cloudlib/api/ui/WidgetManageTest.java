package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.state.State;
import org.junit.jupiter.api.Test;

public class WidgetManageTest {

    @Test
    void testWidgetBuild() {
        State state = new DebugState("StateA");
        State anotherState = new DebugState("StateB");
        var rootSpec = GroupSpec.of(scope -> {
            System.out.println(state);
            scope.group(GroupSpec.of((Scope<InternalWidget> scope1) -> {
                System.out.println(anotherState);
                scope1.apply(new InternalWidget());
                scope1.apply(new InternalWidget());
                return new WidgetGroup<>();
            }));
            return new RootWidget();
        });
        WidgetManagement widgetManagement = new WidgetManagement(rootSpec);
        System.out.println(widgetManagement);
    }

    private static class InternalWidget extends Widget {
        // Internal widget implementation
    }

    private record DebugState(String name) implements State {

        @Override
        public boolean changed() {
            return false;
        }


    }

}












