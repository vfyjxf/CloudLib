package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/** A named cluster of folded request ids sharing one dock entry. */
public record OverflowGroup(String id, List<String> requestIds) {

    public OverflowGroup {
        requestIds = List.copyOf(requestIds);
    }
}
