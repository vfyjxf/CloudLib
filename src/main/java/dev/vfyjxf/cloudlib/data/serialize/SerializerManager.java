package dev.vfyjxf.cloudlib.data.serialize;

import com.google.gson.JsonObject;
import dev.vfyjxf.cloudlib.api.data.serialize.Save;
import net.lenni0451.reflect.accessor.FieldAccessor;
import net.minecraft.nbt.CompoundTag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;

import java.lang.reflect.Field;
import java.util.function.Function;

public class SerializerManager {

    public static final SerializerManager INSTANCE = new SerializerManager();

    private final MutableMap<Class<?>, ImmutableList<Field>> fieldsToSave = Maps.mutable.empty();
    /**
     * Should we support primitive types?
     */
    private final MutableMap<Field, Function<Object, Object>> fieldGetters = Maps.mutable.empty();
    private final MutableMap<Class<?>, SavableSerializer<?>> typeSerializers = Maps.mutable.empty();

    public ImmutableList<Field> getFieldsToSave(Class<?> clazz) {
        return fieldsToSave.getIfAbsentPut(clazz, () -> {
            MutableList<Field> fields = Lists.mutable.empty();
            var type = clazz;
            while (type != null) {
                for (Field declaredField : type.getDeclaredFields()) {
                    if (declaredField.isAnnotationPresent(Save.class)) {
                        fields.add(declaredField);
                    }
                }
                type = type.getSuperclass();
            }
            return fields.toImmutable();
        });
    }

    @SuppressWarnings("unchecked")
    private <T> SavableSerializer<T> makeSerializer(Class<T> clazz) {
        var cache = typeSerializers.get(clazz);
        if (cache != null) return (SavableSerializer<T>) cache;

        MutableStack<Class<?>> typeTree = Stacks.mutable.empty();
        Class<?> type = clazz;
        while (type != null && type != Object.class) {
            typeTree.push(type);
            type = type.getSuperclass();
        }
        MutableStack<SavableSerializer<?>> serializers = Stacks.mutable.empty();
        while (!typeTree.isEmpty()) {
            Class<?> currentType = typeTree.pop();
            SavableSerializer<?> serializer = typeSerializers.get(currentType);
            if (serializer == null) {

            }
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private <T> SavableSerializer<T> makeSerializerWithoutParent(Class<T> type) {
        if (type.getSuperclass() != Object.class)
            throw new IllegalArgumentException("Type must not have a parent class");
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @SuppressWarnings("unchecked")
    private Function<Object, Object> getGetter(Field field) {
        return fieldGetters.getIfAbsentPut(field, () -> FieldAccessor.makeDynamicGetter(Function.class, field));
    }

    private interface SavableSerializer<T> {

        T loadJson(JsonObject jsonObject);

        T loadNbt(CompoundTag tag);

        JsonObject saveJson(T instance);

        CompoundTag saveNbt(T instance);

    }

    private interface ArrayLikeSaveSerializer<T> {

    }

}
