package dev.vfyjxf.cloudlib.api.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class SyncValue<O extends SyncDataHolder, T> {

    private final TypeSyncManager<O> owner;
    private final Class<O> holderType;
    private final Class<T> valueType;
    private final int id;
    private final Function<O, T> getter;
    private final BiConsumer<O, T> setter;
    private final Dist dist;
    @Nullable
    private StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    private final WeakHashMap<O, MutableList<WeakReference<Consumer<T>>>> receivers = new WeakHashMap<>();

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public SyncValue(TypeSyncManager<O> owner, int id, Class<O> holderType, Dist dist, Function<O, T> getter, BiConsumer<O, T> setter, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, T... token) {
        this.owner = owner;
        if (token.length != 0) {
            throw new IllegalArgumentException("Token catcher should be empty");
        }
        this.holderType = holderType;
        this.valueType = (Class<T>) token.getClass().getComponentType();
        this.id = id;
        this.dist = dist;
        this.getter = getter;
        this.setter = setter;
        this.streamCodec = streamCodec;
    }

    /**
     * Notify the value change of the holder, the value will be sent to another dist.
     *
     * @param holder the data holder
     */
    public void setChange(O holder) {
        if (!holderType.isInstance(holder)) {
            throw new IllegalArgumentException("The holder is not the same type as the bind type");
        }
        Dist currentDist = FMLEnvironment.dist;
        if (dist != currentDist) {
            throw new IllegalStateException("This SyncValue should be used in " + dist + " dist, but now is in " + currentDist + " dist");
        }

    }

    private void onValueSync(O holder, T legacy, T current) {
        if (Objects.equals(legacy, current)) {
            return;
        }
        MutableList<WeakReference<Consumer<T>>> onDataReceive = this.receivers.get(holder);
        for (var iterator = onDataReceive.iterator(); iterator.hasNext(); ) {
            WeakReference<Consumer<T>> reference = iterator.next();
            var consumer = reference.get();
            if (consumer == null) {
                iterator.remove();
            } else {
                consumer.accept(getter.apply(holder));
            }
        }
    }

    /**
     * @param type the value type
     * @param id   the unique id of the managed value, it must be unique in the same class.
     */
    private record TypeSyncValue(Class<?> type, int id) {

    }

}
