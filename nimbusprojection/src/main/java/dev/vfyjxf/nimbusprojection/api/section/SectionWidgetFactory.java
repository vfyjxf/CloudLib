package dev.vfyjxf.nimbusprojection.api.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jetbrains.annotations.Nullable;

/**
 * Client render half of a container section — builds the widget for one
 * section instance. The widget reads live data through
 * {@link SectionView#data()} every frame; it never caches snapshots.
 * <p>
 * Returning {@code null} skips the section entirely — the config-gated
 * built-ins use this for per-face toggles.
 */
@FunctionalInterface
public interface SectionWidgetFactory<D extends SectionData> {

    @Nullable
    Widget create(SectionView<D> view);
}
