package dev.vfyjxf.cloudlib.api.data;

import java.util.function.Function;

public final class DataKeys {

    static class BasicDataKey<T> implements DataKey<T> {

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

    static class DataKeyImpl<T> implements DataKey<T> {
        private final String key;
        private final Function<DataAttachable, T> defaultValue;

        public DataKeyImpl(String key, Function<DataAttachable, T> defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public T defaultValue(DataAttachable holder) {
            return defaultValue.apply(holder);
        }

    }

    private DataKeys() {
    }
}
