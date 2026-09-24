package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.util.Maybe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@ApiStatus.Internal
final class StandardReversedExpose<T, S, R> extends BasicDownstreamExpose<T>
        implements
            ReversedExpose<T, S, R>,
            ReversedTranscoder {

    private final SimpleEvent<Consumer<R>> reverseReceiveEvent = SimpleEvent.create();
    private final FlowEncoder<S> reverseEncoder;
    private final FlowDecoder<R> reverseDecoder;
    private Maybe<S> reversedData = Maybe.empty();

    StandardReversedExpose(
        String name,
        short id,
        Snapshot<T> snapshot,
        ValueSupplier<T> supplier,
        FlowEncoder<T> encoder,
        FlowDecoder<T> decoder,
        FlowEncoder<S> reverseEncoder,
        FlowDecoder<R> reverseDecoder
    ) {
        super(name, id, snapshot, supplier, encoder, decoder);
        this.reverseEncoder = reverseEncoder;
        this.reverseDecoder = reverseDecoder;
    }

    @Override
    public void sendToServer(S toSend) {
        if (reversedData.defined()) {
            throw new IllegalStateException(
                "There is already a value going to send,data shouldn't be updated at tick end."
            );
        }
        reversedData = Maybe.ofNonNull(toSend);
    }

    @Override
    public StandardReversedExpose<T, S, R> whenReceiveFromClient(Consumer<R> consumer) {
        reverseReceiveEvent.register(consumer);
        return this;
    }

    @Override
    public boolean hasReversedData() {
        return reversedData.defined();
    }

    @Override
    public void writeToServer(RegistryFriendlyByteBuf byteBuf) {
        @Nullable
        S data = reversedData.orElse(null);
        if (data == null) return;
        reverseEncoder.encode(byteBuf, data);
        reversedData = Maybe.empty();
    }

    @Override
    public void readFromClient(RegistryFriendlyByteBuf byteBuf) {
        R received = reverseDecoder.decode(byteBuf);
        reverseReceiveEvent.invoke(c -> c.accept(received));
    }
}
