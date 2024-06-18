package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.network.chat.Component;

public record LangEntry(String key, String value) {

    public Component get() {
        return Component.translatable(this.key);
    }

    public Component get(Object... args) {
        return Component.translatable(this.key, args);
    }

}
