package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.utils.Maybe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@ApiStatus.Internal
final class StandardDiffLayerExpose<E, D>
        extends BasicLayerExpose<E>
        implements DiffLayerExpose<E, D> {

    private final SimpleEvent<Consumer<D>> diffReceiveEvent = SimpleEvent.create();
    private final FlowEncoder<D> diffEncoder;
    private final FlowDecoder<D> diffDecoder;

    <T extends DiffObservable<D>> StandardDiffLayerExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            FlowEncoder<D> diffEncoder, FlowDecoder<D> diffDecoder
    ) {
        super(name, id, snapshot, valueSupplier, encoder, decoder);
        this.diffEncoder = diffEncoder;
        this.diffDecoder = diffDecoder;
    }

    @Override
    public void whenDiffReceive(Consumer<D> diffConsumer) {
        diffReceiveEvent.register(diffConsumer);
    }

    @Override
    public Maybe<D> difference() {
        return Differential.getDifference(layerSnapshot());
    }

    @Override
    public void encodeDifference(RegistryFriendlyByteBuf byteBuf, D difference) {
        diffEncoder.encode(byteBuf, difference);
    }

    @Override
    public D decodeDifference(RegistryFriendlyByteBuf byteBuf) {
        D received = diffDecoder.decode(byteBuf);
        diffReceiveEvent.invoke(c -> c.accept(received));
        return received;
    }

}
