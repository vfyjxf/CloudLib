package dev.vfyjxf.cloudlib.concept.node.typehack;

import dev.vfyjxf.cloudlib.api.data.AttachableDataContainer;
import dev.vfyjxf.cloudlib.api.data.DataAttachable;
import dev.vfyjxf.cloudlib.api.data.DataType;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.concept.node.typehack.NodeBuilderConceptWithTypeHackTest.Group;
import dev.vfyjxf.cloudlib.concept.node.typehack.NodeBuilderConceptWithTypeHackTest.Instance;
import net.minecraft.resources.ResourceLocation;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Function;

public class NodeBuilderConceptWithTypeHackTest {

    private static final DataType<String> TEST_TYPE = DataType.valueOf(ResourceLocation.withDefaultNamespace("test"), "default_value");
    private static final DataType<Integer> TEST_TYPE_2 = DataType.valueOf(ResourceLocation.withDefaultNamespace("test_2"), 42);

    @Test
    void build() {
        final var blueprint = new GroupBlueprint<>() {
            @Override
            public Group<Instance> construct(GroupScope<Instance> scope) {
                class SpecificGroup extends Group<SpecificInstance> {}
//                new GroupSpec<SpecificGroup, SpecificInstance>() {
//                    @Override
//                    public SpecificGroup construct(GroupScope<SpecificInstance> scope) {
//
//                        scope.group(t -> t, new GroupSpec<Group<SpecificInstance>, SpecificInstance>() {
//
//                            @Override
//                            public Group<SpecificInstance> construct(GroupScope<SpecificInstance> scope) {
//                                return null;
//                            }
//                        });
//
//                        return null;
//                    }
//                };

                new GroupBlueprint<Group<Instance>, Instance>() {
                    @Override
                    public Group<Instance> construct(GroupScope<Instance> scope) {
                        scope.group(t -> t, new GroupBlueprint<>() {
                            @Override
                            public Group<Instance> construct(GroupScope<Instance> scope) {
                                return null;
                            }
                        });
                        return null;
                    }
                };
                return null;
            }
        };

        CompositeWidget<Widget> widgetWidgetGroup = new CompositeWidget<>();
    }


    static class Instance implements DataAttachable {
        private final AttachableDataContainer attachableDataContainer = new AttachableDataContainer();

        @Override
        public @NotNull AttachableDataContainer attachableDataContainer() {
            return attachableDataContainer;
        }
    }

    static class SpecificInstance extends Instance {
        // This can be used to create specific instances with additional properties or methods
    }

    static class Group<T extends Instance> extends Instance {
        final MutableList<T> instances = MutableLists.empty();
    }

}


/**
 * @param <R> group type
 * @param <E> element type
 */
abstract class GroupBlueprint<R extends Group<E>, E extends Instance> {

    //TODO:需要运行时检查R的类型，R必须是一个Group的子类型，由于java泛型的限制，我们无法约束R

    abstract R construct(GroupScope<E> scope);

}


interface GroupScope<E extends Instance> {

    void apply(InstanceBlueprint<E> blueprint);

    void apply(E instance);

    void apply(Collection<E> instances);

    //FIXME:mapper实际上不参与任何运算，但是保证了类型安全
    <G extends Group<T>, T extends Instance> void group(Function<G, E> mapper, GroupBlueprint<G, T> blueprint);

}


interface TreeScope<E extends Group<?>> {

}

interface InstanceBlueprint<T> {
    T construct();
}
