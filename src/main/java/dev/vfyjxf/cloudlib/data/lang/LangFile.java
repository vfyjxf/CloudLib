package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One distributed language file ({@code <name>.yaml}) inside a locale directory.
 * Collects translation entries for a single domain (items, blocks, ui, ...); game objects
 * such as items and blocks contribute their description id and never need to be referenced
 * from the generated key class.
 */
public final class LangFile {

    private final String name;
    private final Map<String, String> entries = new TreeMap<>();

    LangFile(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    Map<String, String> entries() {
        return entries;
    }

    public LangFile add(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        entries.put(key, value);
        return this;
    }

    public LangFile add(LangEntry entry, String value) {
        return add(entry.key(), value);
    }

    /**
     * Adds the display name of an item or block ({@link ItemLike#asItem()} covers both).
     */
    public LangFile add(ItemLike item, String value) {
        return add(item.asItem().getDescriptionId(), value);
    }

    public LangFile add(EntityType<?> entityType, String value) {
        return add(entityType.getDescriptionId(), value);
    }

    public LangFile add(MobEffect effect, String value) {
        return add(effect.getDescriptionId(), value);
    }

}
