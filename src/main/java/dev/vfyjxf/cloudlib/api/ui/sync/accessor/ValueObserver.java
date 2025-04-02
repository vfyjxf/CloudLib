package dev.vfyjxf.cloudlib.api.ui.sync.accessor;

import dev.vfyjxf.cloudlib.utils.ClassUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * AN observer that observes the value from the data provider,and provide value accessor for outside to access the value(or handled value).
 */
public interface ValueObserver<P> {

    ValueAccessorContainer<P> accessors();

    default MutableList<Field> observing() {
        MutableList<Field> observingFields = Lists.mutable.empty();
        ClassUtils.walkClassTree(
                getClass(), (type) -> {
                    Field[] fields = type.getDeclaredFields();
                    for (Field field : fields) {
                        boolean observing = Modifier.isPublic(field.getModifiers()) &&
                                Modifier.isFinal(field.getModifiers()) &&
                                !Modifier.isStatic(field.getModifiers()) &&
                                field.getType().isAssignableFrom(ValueAccessor.class);
                        observingFields.add(field);
                    }
                    return type != Object.class;
                }
        );
        return observingFields;
    }

    /**
     * @param accessor
     * @param <T>
     * @param <V>
     * @return
     */
    default <T extends ValueAccessor<V>, V> T addAccessor(T accessor) {
        return accessors().add(accessor);
    }

}
