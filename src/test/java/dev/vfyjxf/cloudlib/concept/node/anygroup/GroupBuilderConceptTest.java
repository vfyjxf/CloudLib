package dev.vfyjxf.cloudlib.concept.node.anygroup;

public class GroupBuilderConceptTest {


}


//region basic type
//标记接口，密封确保直接实现类只有 GroupElement
//由于java的相交类型必须为类类型和接口类型的混合，所以将Group的约束定义为接口，而不是直接使用GroupElement<E>
sealed interface Group<E> permits GroupElement {}

class Element {}

non-sealed class GroupElement<E> extends Element implements Group<E> {}
//endregion

//region builder

interface ElementBlueprint<E> {
    E construct();
}

interface GroupBlueprint<T extends Group<E>, E extends Element> {
    T construct(GroupScope<E> scope);
}

interface GroupScope<E extends Element> {
    <T extends Element> void group(GroupBlueprint<? extends E, T> blueprint);

}

//endregion