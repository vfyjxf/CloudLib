package dev.vfyjxf.cloudlib.api.ui.sync.accessor;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class ValueAccessorContainer<P> {
    private final AbstractContainerMenu owner;
    private final MutableList<ValueAccessor<?>> accessors = Lists.mutable.empty();

    public ValueAccessorContainer(AbstractContainerMenu owner) {
        this.owner = owner;
    }

    public <T extends ValueAccessor<V>, V> T add(T accessor) {
        if (accessor.id() >= accessors.size() || accessors.get(accessor.id()) != null) {
            throw new IllegalArgumentException("Illegal accessor id: " + accessor.id());
        }
        accessors.add(accessor);
        return accessor;
    }

    public short nextUsableId() {
        return (short) accessors.size();
    }
}
