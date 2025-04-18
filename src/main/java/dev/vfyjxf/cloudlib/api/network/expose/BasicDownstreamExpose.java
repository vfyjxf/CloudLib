package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Consumer;

/**
 * The data from the server down to the client.
 */
sealed abstract class BasicDownstreamExpose<T>
        extends BasicExpose<T>
        implements Transcoder
        permits StandardExpose,
                StandardReversedExpose {

    private final SimpleEvent<Consumer<T>> receiveEvent = SimpleEvent.create();

    protected BasicDownstreamExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> supplier,
            FlowEncoder<T> encoder, FlowDecoder<T> decoder
    ) {
        super(name, id, snapshot, supplier, encoder, decoder);
    }

    @Override
    public void whenReceive(Consumer<T> consumer) {
        receiveEvent.register(consumer);
    }

    @Override
    public void writeToClient(RegistryFriendlyByteBuf byteBuf) {
        encoder.encode(byteBuf, current());
    }

    @Override
    public void readFromServer(RegistryFriendlyByteBuf byteBuf) {
        T received = decoder.decode(byteBuf);
        receiveEvent.invoke(c -> c.accept(received));
    }
}
