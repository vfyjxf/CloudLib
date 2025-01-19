package dev.vfyjxf.cloudlib.api.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.function.Function;

class BuiltinDataImpl {

    private static abstract class BasicDataKey<T> implements DataKey<T> {

        private final String key;
        private final T defaultValue;

        protected BasicDataKey(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public T defaultValue(DataAttachable holder) {
            return defaultValue;
        }

    }

    static class IntDataKey extends BasicDataKey<Integer> {

        public IntDataKey(String key, int defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public IntTag save(Integer value) {
            return IntTag.valueOf(value);
        }

        @Override
        public Integer load(Tag data) {
            return ((IntTag) data).getAsInt();
        }
    }

    static class EnumDataKey<T extends Enum<T>> extends BasicDataKey<T> {

        private final Class<T> enumClass;

        public EnumDataKey(String key, T defaultValue, Class<T> enumClass) {
            super(key, defaultValue);
            this.enumClass = enumClass;
        }

        @Override
        public Tag save(T value) {
            return IntTag.valueOf(value.ordinal());
        }

        @Override
        public T load(Tag data) {
            return enumClass.getEnumConstants()[((IntTag) data).getAsInt()];
        }
    }

    static class StringDataKey extends BasicDataKey<String> {

        public StringDataKey(String key, String defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Tag save(String value) {
            return StringTag.valueOf(value);
        }

        @Override
        public String load(Tag data) {
            return data.getAsString();
        }
    }

    static class DataKeyImpl<T> implements DataKey<T> {
        private final String key;
        private final Function<DataAttachable, T> defaultValue;
        private final Codec<T> codec;

        public DataKeyImpl(String key, Function<DataAttachable, T> defaultValue, Codec<T> codec) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.codec = codec;

        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public T defaultValue(DataAttachable holder) {
            return defaultValue.apply(holder);
        }

        @Override
        public Tag save(T value) {
            return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        }

        @Override
        public T load(Tag data) {
            return codec.parse(NbtOps.INSTANCE, data).getOrThrow();
        }
    }

}
