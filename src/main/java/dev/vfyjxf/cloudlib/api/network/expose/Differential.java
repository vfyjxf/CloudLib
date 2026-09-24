package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.util.Maybe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The ability to send a difference to the client.
 *
 * @param <D> the type of the difference
 */
@ApiStatus.Internal
interface Differential<D> {

    /**
     * register difference consumer, the consumer will be called when a difference is received.
     *
     * @param diffConsumer the consumer to receive the difference
     */
    void whenDiffReceive(Consumer<D> diffConsumer);

    Maybe<D> difference();

    static <T extends DiffObservable<D>, D> Maybe<D> getDifference(BasicLayerExpose.LayerSnapshot<T> snapshot) {
        @Nullable
        T current = snapshot.current();
        // no value yet: nothing changed, so there is no difference to send either
        if (current == null) return Maybe.empty();
        return current.changed() ? Maybe.of(current.difference()) : Maybe.empty();
    }

    /**
     * Encode the difference to the buffer.
     *
     * @param byteBuf    the buffer to encode the difference
     * @param difference the difference to encode
     */
    void encodeDifference(RegistryFriendlyByteBuf byteBuf, D difference);

    /**
     * Decode the difference from the buffer.
     *
     * @param byteBuf the buffer to decode the difference
     * @return the difference
     */
    D decodeDifference(RegistryFriendlyByteBuf byteBuf);
}
