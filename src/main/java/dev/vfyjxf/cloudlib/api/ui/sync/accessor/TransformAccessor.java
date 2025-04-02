package dev.vfyjxf.cloudlib.api.ui.sync.accessor;

import dev.vfyjxf.cloudlib.api.data.EqualsChecker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.Supplier;

public class TransformAccessor<T> implements ValueAccessor<T> {

    private final Supplier<T> transformedSupplier;


    public TransformAccessor(Supplier<T> transformedSupplier) {
        this.transformedSupplier = transformedSupplier;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public short id() {
        return 0;
    }

    @Override
    public EqualsChecker<T> checker() {
        return null;
    }

    @Override
    public T get() {
        return null;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        return null;
    }

    @Override
    public void onMenuAttached(AbstractContainerMenu menu) {

    }
}
