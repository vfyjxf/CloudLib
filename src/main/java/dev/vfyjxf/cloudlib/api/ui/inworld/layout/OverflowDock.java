package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/**
 * The overflow dock: a screen rect holding folded requests grouped by
 * their {@code overflowGroup}, with paging. {@code externalOnly} marks the
 * page clipped to requests whose group id starts with {@code "ext:"}.
 */
public record OverflowDock(
        GuiRect rect,
        boolean externalOnly,
        List<OverflowGroup> groups,
        int total,
        int page,
        int pageCount,
        List<String> pageItems) {

    public OverflowDock {
        groups = List.copyOf(groups);
        pageItems = List.copyOf(pageItems);
    }
}
