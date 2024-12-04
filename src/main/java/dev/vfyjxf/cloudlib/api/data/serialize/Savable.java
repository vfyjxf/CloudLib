package dev.vfyjxf.cloudlib.api.data.serialize;

import dev.vfyjxf.cloudlib.data.serialize.SerializerManager;
import org.eclipse.collections.api.list.MutableList;

import java.lang.reflect.Field;

public interface Savable {

    default MutableList<Field> fieldsToSave() {
        return SerializerManager.INSTANCE.getFieldsToSave(getClass()).toList();
    }

}
