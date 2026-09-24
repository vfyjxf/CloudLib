package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

abstract sealed class BasicLayerExpose<E> implements LayerExpose<E>, Transcoder permits StandardLayerExpose,
        StandardReversedLayerExpose, StandardDiffLayerExpose, StandardDiffReverseLayerExpose {

    private final SimpleEvent<Consumer<E>> receiveEvent = SimpleEvent.create();
    private final String name;
    private final short id;
    private final LayerSnapshot<?> layerSnapshot;
    private final FlowDecoder<E> decoder;

    protected <T> BasicLayerExpose(
        String name,
        short id,
        Snapshot<T> snapshot,
        ValueSupplier<T> valueSupplier,
        FlowEncoder<T> encoder,
        FlowDecoder<E> decoder
    ) {

        this.name = name;
        this.id = id;
        this.layerSnapshot = new LayerSnapshot<>(snapshot, valueSupplier, encoder);
        this.decoder = decoder;
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
    public Snapshot<?> snapshot() {
        return layerSnapshot.snapshot;
    }

    @Override
    public boolean hasValue() {
        return layerSnapshot.current() != null;
    }

    @SuppressWarnings("unchecked")
    protected <T> LayerSnapshot<T> layerSnapshot() {
        return (LayerSnapshot<T>) layerSnapshot;
    }

    @Override
    public <T> boolean changed() {
        LayerSnapshot<T> layerSnapshot = layerSnapshot();
        Snapshot<T> snapshot = layerSnapshot.snapshot();
        return switch (layerSnapshot.currentState()) {
            case unchanged -> false;
            case changed -> true;
            case illegal -> {
                boolean readonly = snapshot instanceof Snapshot.Readonly;
                throw new IllegalStateException(
                    "Illegally modifity a " + (readonly ? "readonly" : "immutable reference") + " snapshot(id:" + id()
                            + " name:" + name() + ")"
                );
            }
        };
    }

    @Override
    public void whenReceive(Consumer<E> consumer) {
        receiveEvent.register(consumer);
    }

    @Override
    public void writeToClient(RegistryFriendlyByteBuf byteBuf) {
        LayerSnapshot<Object> layerSnapshot = layerSnapshot();
        Object value = layerSnapshot.current();
        if (value == null) {
            throw new IllegalStateException("LayerExpose has no value to write: (id:" + id() + " name:" + name() + ")");
        }
        layerSnapshot.encoder().encode(byteBuf, value);
    }

    @Override
    public void readFromServer(RegistryFriendlyByteBuf byteBuf) {
        E received = decoder.decode(byteBuf);
        receiveEvent.invoke(c -> c.accept(received));
    }

    @Override
    public void updateSnapshot() {
        layerSnapshot.updateSnapshot();
    }

    @Override
    public void forceUpdateSnapshot() {
        layerSnapshot.forceUpdateSnapshot();
    }

    @Override
    public String toString() {
        return "BasicLayerExpose{" + "name='" + name + '\'' + ", id=" + id + ", layerSnapshot=" + layerSnapshot + '}';
    }

    @ApiStatus.Internal
    public record LayerSnapshot<T>(Snapshot<T> snapshot, ValueSupplier<T> valueSupplier, FlowEncoder<T> encoder) {
        /**
         * @return the value the underlying supplier currently holds, or null while it holds none
         */
        public @Nullable T current() {
            return valueSupplier.get();
        }

        /** An absent value reports no state change and is never recorded. */
        public Snapshot.State currentState() {
            @Nullable
            T current = current();
            if (current == null) return Snapshot.State.unchanged;
            return snapshot.currentState(current);
        }

        public boolean mutableSnapshot() {
            return snapshot.mutable();
        }

        public void updateSnapshot() {
            if (!snapshot.mutable()) return;
            @Nullable
            T current = current();
            if (current == null) return;
            snapshot.updateState(current);
        }

        public void forceUpdateSnapshot() {
            if (!snapshot.mutable()) return;
            @Nullable
            T current = current();
            if (current == null) return;
            snapshot.forceUpdateState(current);
        }
    }
}
