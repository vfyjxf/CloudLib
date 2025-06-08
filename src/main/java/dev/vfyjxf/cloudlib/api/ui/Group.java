package dev.vfyjxf.cloudlib.api.ui;

/**
 * A marker interface for Intersection Types due to Java's lack of support for intersection types.
 *
 * @see WidgetGroup
 */
public sealed interface Group<T extends Widget> permits WidgetGroup {

    default WidgetGroup<T> narrow() {
        return (WidgetGroup<T>) this;
    }

}
