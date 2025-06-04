package dev.vfyjxf.cloudlib.api.ui.concept;

import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.state.MutableState;
import dev.vfyjxf.cloudlib.api.ui.state.State;
import dev.vfyjxf.cloudlib.api.ui.widget.RootWidget;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;
import org.appliedenergistics.yoga.YogaFlexDirection;

import java.util.Collection;

public final class UIBuilder {
    private final RootWidget rootWidget = new RootWidget();

}

interface InstanceBlueprint<T extends Widget> {

    T construct();

}


interface GroupBlueprint<R extends WidgetGroup<?>, T extends Widget> {
    R construct(GroupScope<T> scope);
}

interface ScopelessGroupBlueprint<R extends WidgetGroup<?>, T extends Widget> {
    R construct();
}

interface GroupScope<T extends Widget> {

    void addStateless(T widget);

    void addStateless(Collection<T> widgets);

    void add(InstanceBlueprint<? extends T> blueprint);

    void addGroup(GroupBlueprint<? extends WidgetGroup<?>, ? extends T> blueprint);

    void addGroup(ScopelessGroupBlueprint<? extends WidgetGroup<?>, ? extends T> blueprint);
}

class TestBuilder {
    static void foo() {
        var textState = State.mutableOf("Hello");
        final var blueprint = new GroupBlueprint<>() {
            final MutableState<String> _textState2 = State.mutableOf("World");

            @Override
            public WidgetGroup<Widget> construct(GroupScope<Widget> scope) {
                {
                    scope.add(Widget::new);

                    scope.add(() -> new Widget() {
                        {

                            withModifier(Modifier
                                    .builder()
                                    .size(100, 100)
                                    .flexDirection(YogaFlexDirection.COLUMN));

                            onRender((graphics, mouseX, mouseY, partialTicks, self, context) ->
                            {
                                graphics.drawString(getContext().getFont(), _textState2.get(), 0, 0, 0xFFFFFF);
                            });
                        }
                    });

                    scope.add(WidgetGroup::new);
                }

                return new WidgetGroup<>() {
                    {
                        withModifier(Modifier.builder()
                                .size(100, 100)
                                .flexDirection(YogaFlexDirection.ROW));

                        onRender((graphics, mouseX, mouseY, partialTicks, self, context) -> {
                            graphics.drawString(getContext().getFont(), textState.get(), 0, 0, 0xFFFFFF);
                        });
                    }

                };
            }
        };
    }
}

