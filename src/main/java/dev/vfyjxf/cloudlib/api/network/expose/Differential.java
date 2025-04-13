package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.utils.Maybe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

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

    static <T extends DiffObservable<D>, D> Maybe<D> getDifference(LayerExpose.LayerSnapshot<T> snapshot) {
        T current = snapshot.current();
        return current.changed() ? Maybe.of(current.difference()) : Maybe.empty();
    }

    void encodeDifference(RegistryFriendlyByteBuf byteBuf, D difference);

    D decodeDifference(RegistryFriendlyByteBuf byteBuf);

}
