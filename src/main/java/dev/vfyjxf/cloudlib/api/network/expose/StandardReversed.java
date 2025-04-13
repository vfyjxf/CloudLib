package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.utils.Maybe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@ApiStatus.Internal
final class StandardReversed<S, R> extends BasicExpose<Void> implements ReversedOnly<S, R>, ReversedTranscoder {

    private final SimpleEvent<Consumer<R>> reverseReceiveEvent = SimpleEvent.create();
    private final FlowEncoder<S> reversedEncoder;
    private final FlowDecoder<R> reversedDecoder;
    private Maybe<S> reversedData = Maybe.empty();

    StandardReversed(
            String name,
            short id,
            FlowEncoder<S> reversedEncoder,
            FlowDecoder<R> reversedDecoder
    ) {
        super(name, id,
              Snapshot.noneOf(),
              () -> {
                  throw new UnsupportedOperationException("StandardReversed doesn't bound to a value");
              },
              (byteBuf, element) -> {
                  throw new UnsupportedOperationException("StandardReversed can't send data to client");
              },
              (byteBuf -> {
                  throw new UnsupportedOperationException("StandardReversed can't receive data from server");
              })
        );
        this.reversedEncoder = reversedEncoder;
        this.reversedDecoder = reversedDecoder;
    }

    @Override
    public boolean changed() {
        return false;
    }

    @Override
    public void sendToServer(S toSend) {
        if (reversedData.defined()) {
            throw new IllegalStateException("There is already a value going to send,data shouldn't be updated at tick end.");
        }
        this.reversedData = Maybe.of(toSend);
    }

    @Override
    public StandardReversed<S, R> whenReceiveFromClient(Consumer<R> consumer) {
        reverseReceiveEvent.register(consumer);
        return this;
    }

    @Override
    public boolean hasReversedData() {
        return reversedData.defined();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void writeToServer(RegistryFriendlyByteBuf byteBuf) {
        if (reversedData.defined()) {
            reversedEncoder.encode(byteBuf, reversedData.get());
            reversedData = Maybe.empty();
        }
    }

    @Override
    public void readFromClient(RegistryFriendlyByteBuf byteBuf) {
        R received = reversedDecoder.decode(byteBuf);
        reverseReceiveEvent.invoke(c -> c.accept(received));
    }

    @Override
    public void whenReceive(Consumer<@Nullable Void> consumer) {
        // No-op, this is a reversed
    }
}