package dev.vfyjxf.cloudlib.concept.node.anygroup;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

public class GroupBuilderConceptTest {

    @Test
    void testConcept() {
        final var blueprint = new GroupBlueprint<>() {

            @Override
            public GroupElement<Element> construct(Scope<Element> scope) {
                scope.group(new GroupBlueprint<GroupElement<SpecificElement>, SpecificElement>() {
                    @Override
                    public GroupElement<SpecificElement> construct(Scope<SpecificElement> scope) {
                        scope.element(() -> new SpecificElement("Specific Element A"));
                        scope.element(new SpecificElement("Specific Element B"));
                        scope.elements(List.of(
                                new SpecificElement("Specific Element C"),
                                new SpecificElement("Specific Element D")
                        ));

                        return new GroupElement<>("Specific Group");
                    }
                });

                scope.group(new GroupBlueprint<GroupElement<Element>, Element>() {
                    @Override
                    public GroupElement<Element> construct(Scope<Element> scope) {

                        scope.element(() -> new Element("Element A"));
                        scope.element(new Element("Element B"));
                        scope.elements(List.of(
                                new Element("Element C"),
                                new Element("Element D")
                        ));

                        return new GroupElement<>("Element Group");
                    }
                });

                return new GroupElement<>(
                        "Root"
                );
            }
        };

        GroupElement<Element> groupElement = BuildScope.create(blueprint).down();
        System.out.println(groupElement);
    }

}


//region basic type
//标记接口，密封确保直接实现类只有 GroupElement
//由于java的相交类型必须为类类型和接口类型的混合，所以将Group的约束定义为接口，而不是直接使用GroupElement<E>
sealed interface Group<E> permits GroupElement {
    default GroupElement<E> down() {
        return (GroupElement<E>) this;
    }

    default GroupElement<E> self() {
        return (GroupElement<E>) this;
    }
}

class Element {
    final String description;

    Element(String description) {this.description = description;}

    @Override
    public String toString() {
        return "Element{" +
                "description='" + description + '\'' +
                '}';
    }
}

class SpecificElement extends Element {
    SpecificElement(String description) {super(description);}
}

non-sealed class GroupElement<E> extends Element implements Group<E> {
    final MutableList<E> elements = Lists.mutable.empty();

    GroupElement(String desc) {super("Group:" + desc);}

    @Override
    public String toString() {
        return "GroupElement{" +
                "description='" + description + '\'' +
                ", elements=" + elements +
                '}';
    }
}
//endregion

//region builder

interface ElementBlueprint<E> {
    E construct();
}

interface GroupBlueprint<T extends Group<E>, E extends Element> {

    T construct(Scope<E> scope);
}

sealed interface Scope<E extends Element> {

    void element(ElementBlueprint<? extends E> blueprint);

    void element(E element);

    void elements(Collection<? extends E> elements);

    <T extends Element> void group(GroupBlueprint<? extends E, T> blueprint);

}

sealed interface BuildScope<T extends GroupElement<E>, E extends Element> extends Scope<E> {

    @SuppressWarnings("unchecked")
    static <T extends Group<E>, E extends Element> T create(GroupBlueprint<T, E> blueprint) {
        AnyGroupBuildScope<E> scope = new AnyGroupBuildScope<>();
        return (T) scope.apply((GroupBlueprint<GroupElement<E>, E>) blueprint);
    }

    T apply(GroupBlueprint<T, E> blueprint);
}

final class AnyGroupBuildScope<E extends Element> implements BuildScope<GroupElement<E>, E> {

    private final MutableList<Entry<E>> entries = Lists.mutable.empty();

    @Override
    public GroupElement<E> apply(GroupBlueprint<GroupElement<E>, E> blueprint) {
        var constructed = blueprint.construct(this);
        for (var entry : entries) {
            applyEntry(constructed, entry);
        }
        return constructed;
    }

    void applyEntry(GroupElement<E> group, Entry<E> entry) {
        switch (entry) {
            case ElementEntry<E>(var element) -> group.elements.add(element);
            case ElementBlueprintEntry<E>(var blueprint) -> group.elements.add(blueprint.construct());
            case GroupEntry<E, ?> groupEntry -> {
                E applied = applyGroupEntry(groupEntry);
                group.elements.add(applied);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Element> E applyGroupEntry(GroupEntry<E, T> groupEntry) {
        return BuildScope.create(groupEntry.blueprint);
    }

    @Override
    public void element(ElementBlueprint<? extends E> blueprint) {
        entries.add(new ElementBlueprintEntry<>(blueprint));
    }

    @Override
    public void element(E element) {
        entries.add(new ElementEntry<>(element));
    }

    @Override
    public void elements(Collection<? extends E> elements) {
        for (E element : elements) {
            entries.add(new ElementEntry<>(element));
        }
    }

    @Override
    public <T extends Element> void group(GroupBlueprint<? extends E, T> blueprint) {
        entries.add(new GroupEntry<>(blueprint));
    }

    private sealed interface Entry<E extends Element> {}

    private record ElementEntry<E extends Element>(E element) implements Entry<E> {}

    private record ElementBlueprintEntry<E extends Element>(
            ElementBlueprint<? extends E> blueprint) implements Entry<E> {}

    private record GroupEntry<E extends Element, T extends Element>(
            GroupBlueprint<? extends E, T> blueprint) implements Entry<E> {}

}

//endregion