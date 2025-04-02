package dev.vfyjxf.cloudlib.api.ui.sync.accessor;

import dev.vfyjxf.cloudlib.api.data.EqualsChecker;
import dev.vfyjxf.cloudlib.utils.ClassUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * C direct/wrapped value which provide access from data provider.
 *
 * @param <T>
 */
public interface ValueAccessor<T> {

    //region factory

    @SafeVarargs
    static <T> ValueAccessor<T> reflectionOf(String name, Object holder, T... typeCatch) {
        if (typeCatch.length != 0) {
            throw new IllegalArgumentException("typeCatch must be empty");
        }
        Class<?> holderClass = holder.getClass();
        Field field = ClassUtils.getField(holderClass, name);
        if (field == null) {
            throw new IllegalStateException("Field : " + name + " not found in BlockEntity type: " + holderClass.getName());
        }
        Class<?> exceptedType = typeCatch.getClass().getComponentType();
        if (exceptedType != field.getType()) {
            throw new IllegalStateException("Field : " + name + " in BlockEntity type: " + holderClass.getName() + " is not of type: " + exceptedType.getName());
        }
        return null;//TODO: return new ReflectionValueAccessor<>(holder, field);
    }

    //endregion

    //region identity

    /**
     * @return the name of this accessor,mainly for debugging use.
     */
    String name();

    /**
     * The id of this accessor,it must be the same on both the server and client side
     *
     * @return the id of this accessor
     */
    short id();

    /**
     * @return the equals checker of this accessor
     */
    EqualsChecker<T> checker();

    //endregion

    //region data

    T get();

    default void ifPresent(Consumer<? super T> consumer) {
        T value = get();
        if (value != null) {
            consumer.accept(value);
        }
        Optional<T> optional = Optional.ofNullable(value);
    }

    default void onUpdate(BiConsumer<T, T> consumer) {
    }

    default boolean isNull() {
        return get() == null;

    }
    //endregion

    //region sync and event

    /**
     * Only pass {@link net.minecraft.network.RegistryFriendlyByteBuf} to {@link StreamCodec#encode(Object, Object)} and {@link StreamCodec#decode(Object)}
     *
     * @return the stream codec of this accessor
     */
    StreamCodec<? extends ByteBuf, T> streamCodec();

    void onMenuAttached(AbstractContainerMenu menu);

    //endregion

}
