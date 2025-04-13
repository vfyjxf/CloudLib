package dev.vfyjxf.cloudlib.api.network.expose;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Consumer;

public final class StandardUnaryReversed<T> implements UnaryReversed<T>, ReversedTranscoder {

    private final StandardReversed<T, T> reversed;

    StandardUnaryReversed(StandardReversed<T, T> reversed) {
        this.reversed = reversed;
    }

    @Override
    public void sendToServer(T toSend) {
        reversed.sendToServer(toSend);
    }

    @Override
    public UnaryReversed<T> whenReceiveFromClient(Consumer<T> consumer) {
        reversed.whenReceiveFromClient(consumer);
        return this;
    }

    @Override
    public boolean hasReversedData() {
        return reversed.hasReversedData();
    }

    @Override
    public void writeToServer(RegistryFriendlyByteBuf byteBuf) {
        reversed.writeToServer(byteBuf);
    }

    @Override
    public void readFromClient(RegistryFriendlyByteBuf byteBuf) {
        reversed.readFromClient(byteBuf);
    }
}
