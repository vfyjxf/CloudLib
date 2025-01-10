package dev.vfyjxf.cloudlib.api.sync;


import net.lenni0451.reflect.accessor.FieldAccessor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TypeSyncManager<O extends SyncDataHolder> {
    private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final MutableMap<Class<?>, TypeSyncManager<?>> SYNC_MANAGERS = Maps.mutable.empty();

    private final MutableList<SyncValue<O, ?>> syncValues = Lists.mutable.empty();

    public TypeSyncManager(Class<O> bindType) {
        this.bindType = bindType;
        if (SYNC_MANAGERS.containsKey(bindType)) {
            throw new IllegalStateException("The type has been defined");
        }
        SYNC_MANAGERS.put(bindType, this);
    }

    public static <T extends SyncDataHolder> TypeSyncManager<T> define(Class<T> type) {
        return new TypeSyncManager<>(type);
    }

    private final Class<O> bindType;

    @SafeVarargs
    public final <T> SyncValue<O, T> server(Function<O, T> provider, BiConsumer<O, T> receiver, T... token) {
        return define(Dist.DEDICATED_SERVER, provider, receiver, token);
    }

    @SafeVarargs
    public final <T> SyncValue<O, T> server(String fieldName, T... token) {
        return define(Dist.DEDICATED_SERVER, fieldName, token);
    }

    @SafeVarargs
    public final <T> SyncValue<O, T> client(Function<O, T> provider, BiConsumer<O, T> receiver, T... token) {
        return define(Dist.CLIENT, provider, receiver, token);
    }

    @SafeVarargs
    public final <T> SyncValue<O, T> client(String fieldName, T... token) {
        return define(Dist.CLIENT, fieldName, token);
    }


    @SafeVarargs
    public final <T> SyncValue<O, T> define(Dist dist, Function<O, T> getter, BiConsumer<O, T> setter, T... token) {
        return define(dist, getter, setter, null, token);
    }

    @SafeVarargs
    public final <T> SyncValue<O, T> define(
            Dist dist,
            Function<O, T> getter, BiConsumer<O, T> setter,
            StreamCodec<RegistryFriendlyByteBuf, T> streamCodec,
            T... token
    ) {
        Class<?> callerClass = stackWalker.getCallerClass();
        if (callerClass != bindType) {
            throw new IllegalStateException("The managed value should be defined in the same class as the bind type");
        }
        SyncValue<O, T> syncValue = new SyncValue<>(this, syncValues.size(), bindType, dist, getter, setter, streamCodec, token);
        syncValues.add(syncValue);
        return syncValue;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T> SyncValue<O, T> define(Dist dist, String fieldName, T... token) {
        try {
            Field declaredField = bindType.getDeclaredField(fieldName);
            Function<O, T> provider = FieldAccessor.makeDynamicGetter(Function.class, declaredField);
            BiConsumer<O, T> receiver = FieldAccessor.makeDynamicSetter(BiConsumer.class, declaredField);
            return define(dist, provider, receiver, token);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("The field " + fieldName + " is not found in the class " + bindType);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendAll(O holder) {
        Class<?> currentType = holder.getClass();
        while (currentType != null && currentType != Object.class) {
            TypeSyncManager syncManager = SYNC_MANAGERS.get(currentType);
            if (syncManager != null) {
                syncManager.sendAll(holder);
            }
            currentType = currentType.getSuperclass();
        }
    }

    public void sendDiff(O holder) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Unmodifiable
    MutableList<SyncValue<O, ?>> getSyncValues() {
        return syncValues.asUnmodifiable();
    }

}
