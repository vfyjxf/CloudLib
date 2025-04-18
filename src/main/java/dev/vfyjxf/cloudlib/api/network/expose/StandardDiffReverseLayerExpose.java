package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.utils.Maybe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@ApiStatus.Internal
final class StandardDiffReverseLayerExpose<E, D, S, R>
        extends BasicLayerExpose<E>
        implements DiffReverseLayerExpose<E, D, S, R>, ReversedTranscoder {

    private final SimpleEvent<Consumer<D>> diffReceiveEvent = SimpleEvent.create();
    private final SimpleEvent<Consumer<R>> reverseReceiveEvent = SimpleEvent.create();
    private final FlowEncoder<D> diffEncoder;
    private final FlowDecoder<D> diffDecoder;
    private final FlowEncoder<S> reverseEncoder;
    private final FlowDecoder<R> reverseDecoder;

    private Maybe<S> reversedData = Maybe.empty();

    <T extends DiffObservable<D>> StandardDiffReverseLayerExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            FlowEncoder<D> diffEncoder, FlowDecoder<D> diffDecoder,
            FlowEncoder<S> reverseEncoder, FlowDecoder<R> reverseDecoder
    ) {
        super(name, id, snapshot, valueSupplier, encoder, decoder);
        this.diffEncoder = diffEncoder;
        this.diffDecoder = diffDecoder;
        this.reverseEncoder = reverseEncoder;
        this.reverseDecoder = reverseDecoder;
    }

    @Override
    public void sendToServer(S toSend) {
        if (this.reversedData.defined()) {
            throw new IllegalStateException("There is already a value going to send,data shouldn't be updated at tick end.");
        }
        this.reversedData = Maybe.of(toSend);
    }

    @Override
    public StandardDiffReverseLayerExpose<E, D, S, R> whenReceiveFromClient(Consumer<R> consumer) {
        reverseReceiveEvent.register(consumer);
        return this;
    }

    @Override
    public boolean hasReversedData() {
        return reversedData.defined();
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
        this.diffReceiveEvent.invoke(c -> c.accept(received));
        return received;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void writeToServer(RegistryFriendlyByteBuf byteBuf) {
        if (this.reversedData.defined()) {
            this.reverseEncoder.encode(byteBuf, this.reversedData.get());
            this.reversedData = Maybe.empty();
        }
    }

    @Override
    public void readFromClient(RegistryFriendlyByteBuf byteBuf) {
        R received = this.reverseDecoder.decode(byteBuf);
        this.reverseReceiveEvent.invoke(c -> c.accept(received));
    }
}
