package dev.vfyjxf.cloudlib.api.ui.sync.accessor;

import dev.vfyjxf.cloudlib.api.data.EqualsChecker;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.Supplier;

public class ReferenceAccessor<T> implements ValueAccessor<T> {

    private final StreamCodec<? extends ByteBuf, T> streamCodec;
    private final EqualsChecker<T> equalsChecker;
    private final short id;
    private final Supplier<T> getter;
    private final String name;

    public ReferenceAccessor(
            StreamCodec<? extends ByteBuf, T> streamCodec,
            EqualsChecker<T> equalsChecker,
            short id,
            String name,
            Supplier<T> getter
    ) {
        this.streamCodec = streamCodec;
        this.equalsChecker = equalsChecker;
        this.id = id;
        this.name = name;
        this.getter = getter;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public short id() {
        return id;
    }

    @Override
    public EqualsChecker<T> checker() {
        return equalsChecker;
    }

    @Override
    public T get() {
        return getter.get();
    }

    @Override
    public StreamCodec<? extends ByteBuf, T> streamCodec() {
        return streamCodec;
    }

    @Override
    public void onMenuAttached(AbstractContainerMenu menu) {
        //NO-OP
    }
}
