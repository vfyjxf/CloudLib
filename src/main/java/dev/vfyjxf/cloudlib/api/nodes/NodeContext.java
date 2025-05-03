package dev.vfyjxf.cloudlib.api.nodes;

import dev.vfyjxf.cloudlib.api.data.DataAttachable;
import dev.vfyjxf.cloudlib.api.data.DataContainer;
import org.jetbrains.annotations.NotNull;

public class NodeContext implements DataAttachable {
    private final DataContainer container = new DataContainer();

    @Override
    public @NotNull DataContainer dataContainer() {
        return container;
    }
}
