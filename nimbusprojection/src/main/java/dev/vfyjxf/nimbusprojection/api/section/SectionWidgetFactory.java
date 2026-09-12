package dev.vfyjxf.nimbusprojection.api.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;

/**
 * Client render half of a container section — builds the widget for one
 * section instance. The widget reads live data through
 * {@link SectionView#data()} every frame; it never caches snapshots.
 */
@FunctionalInterface
public interface SectionWidgetFactory<D extends SectionData> {

    Widget create(SectionView<D> view);
}
