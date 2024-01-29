package dev.vfyjxf.cloudlib.data.lang;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;


public class Lang {

    private final String key;
    private final String value;
    private String located;

    public Lang(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Component get() {
        return Component.translatable(this.key);
    }

    public Component get(Object... args) {
        return Component.translatable(this.key, args);
    }

    @CanIgnoreReturnValue
    public String getLocated() {
        if (located == null) {
            located = I18n.get(this.key);
        }
        return located;
    }

    public void reload() {
        located = null;
        getLocated();
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lang lang)) return false;

        if (!key.equals(lang.key)) return false;
        return value.equals(lang.value);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Lang{" +
                "key='" + key + '\'' +
                ", get='" + value + '\'' +
                '}';
    }

}
