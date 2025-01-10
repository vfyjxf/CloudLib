package dev.vfyjxf.cloudlib.api.sync;

import dev.vfyjxf.cloudlib.api.data.ObservableValue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SyncManager {

    /**
     * When SyncValue does not specify a StreamCodec, the Codec stored here will be used as the default value
     */
    private static final MutableMap<Class<?>, StreamCodec<RegistryFriendlyByteBuf, ?>> DEFAULT_CODECS = Maps.mutable.empty();

    public static <T> void registerDefaultCodec(Class<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        if (DEFAULT_CODECS.containsKey(type)) {
            throw new IllegalStateException("The codec has been defined");
        }
        DEFAULT_CODECS.put(type, codec);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    <T> StreamCodec<RegistryFriendlyByteBuf, T> getCodec(Class<T> type) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) DEFAULT_CODECS.get(type);
    }

    /**
     * @param <T> the data type
     */
    public static <HOLDER extends SyncDataHolder, T> void onReceive(
            HOLDER holder,
            SyncValue<HOLDER, T> syncValue,
            Consumer<T> dataConsumer
    ) {

    }

    /**
     * @param holder    the value holder
     * @param syncValue the managed value
     * @param <HOLDER>  the holder type
     * @param <T>       the data type
     * @return the send function
     */
    @NotNull
    public static <HOLDER extends SyncDataHolder, T> Consumer<T> sender(HOLDER holder, SyncValue<HOLDER, T> syncValue) {
        return null;
    }

    public static <HOLDER extends SyncDataHolder, T> void sendData(HOLDER holder, SyncValue<HOLDER, T> syncValue, T data) {

    }

    /**
     * @param receiver     the receiver to receive data
     * @param tokenCatcher a magic to catch type token automatically
     * @param <W>          the receiver type
     * @param <T>          the data type
     */
    @SafeVarargs
    public static <W, T> void manage(
            W receiver,
            ObservableValue<T> data,
            BiConsumer<W, T> dataConsumer,
            T... tokenCatcher
    ) {
        System.out.println(tokenCatcher.getClass().getComponentType());
    }

}
