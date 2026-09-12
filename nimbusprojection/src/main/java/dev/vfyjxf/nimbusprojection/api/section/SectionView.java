package dev.vfyjxf.nimbusprojection.api.section;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The client-side handle handed to a {@link SectionWidgetFactory}: panel
 * context, owning {@link SectionTarget} (block or entity), server-
 * addressable section id, kind token, the client-collected initial data,
 * and a live data source that returns the newest server snapshot
 * (falling back to the initial data).
 *
 * @param <D> the snapshot data this section carries
 */
public record SectionView<D extends SectionData>(
        InworldPanelContext panel,
        SectionTarget target,
        String id,
        SectionType<D> type,
        D initial,
        Supplier<D> source) {

    /** Latest data for this section — server snapshot when one has landed, else the initial collect. */
    public D data() {
        D d = source.get();
        return d != null ? d : initial;
    }

    /** The server snapshot only — null until the first reply lands (widgets render "syncing"). */
    public @Nullable D live() {
        return source.get();
    }
}
