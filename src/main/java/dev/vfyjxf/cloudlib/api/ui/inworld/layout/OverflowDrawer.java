package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/** The open overflow drawer — the page of folded items the host expanded. */
public record OverflowDrawer(GuiRect rect, List<String> items, int page, int pageCount) {

    public OverflowDrawer {
        items = List.copyOf(items);
    }
}
