package dev.vfyjxf.cloudlib.api.search;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A scenario's declared field definitions, bound for use: dense ids, name /
 * alias / JEI-prefix resolution and the contains-source channels all live on
 * the set instance — no global registry exists, so two scenarios may define
 * independent field vocabularies (including colliding names) side by side.
 *
 * <p>Build once at scenario init and share the instance:
 * {@snippet :
 * static final SearchFieldSet SET = SearchFieldSet.of(text, mod, out);
 * }
 */
public final class SearchFieldSet {
    private final SearchField[] fields;
    private final Map<String, SearchField> byAlias = new HashMap<>();
    private final List<SearchField> containsSources;
    private final SearchField[] virtualFields;

    private SearchFieldSet(SearchField[] fields) {
        this.fields = fields;
        for (int id = 0; id < fields.length; id++) {
            SearchField field = fields[id];
            String key = field.name();
            SearchField nameOwner = byAlias.putIfAbsent(key, field);
            if (nameOwner != null) {
                throw new IllegalArgumentException("Duplicate search field name/alias: " + key);
            }
            for (String alias : field.spec().aliases()) {
                String aliasKey = alias.toLowerCase(Locale.ROOT);
                SearchField aliasOwner = byAlias.putIfAbsent(aliasKey, field);
                if (aliasOwner != null) {
                    throw new IllegalArgumentException("Duplicate search field alias '" + aliasKey + "' on " + key + ", already on " + aliasOwner.name());
                }
            }
            field.bind(id);
        }
        this.containsSources = java.util.Arrays.stream(fields)
            .filter(field -> field.spec().contentChannel() && !field.spec().virtual())
            .map(field -> (SearchField) field)
            .toList();
        this.virtualFields = java.util.Arrays.stream(fields)
            .filter(field -> field.spec().virtual())
            .map(field -> (SearchField) field)
            .toArray(SearchField[]::new);
    }

    /** Binds the given field definitions, in declaration order. */
    public static SearchFieldSet of(SearchField... fields) {
        Checks.checkNotNull(fields, "fields");
        return new SearchFieldSet(fields.clone());
    }

    public static SearchFieldSet of(List<SearchField> fields) {
        Checks.checkNotNull(fields, "fields");
        return new SearchFieldSet(fields.toArray(new SearchField[0]));
    }

    public int size() {
        return fields.length;
    }

    public SearchField field(int id) {
        return fields[id];
    }

    public List<SearchField> fields() {
        return List.of(fields);
    }

    /** The non-virtual content-channel fields, in declaration order. */
    public List<SearchField> containsSources() {
        return containsSources;
    }

    /** The aggregate fields: each is the union of {@link #containsSources()}. */
    public SearchField[] virtualFields() {
        return virtualFields.clone();
    }

    public @Nullable SearchField byName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        SearchField field = byAlias.get(name.toLowerCase(Locale.ROOT));
        return field != null && field.name().equals(name.toLowerCase(Locale.ROOT)) ? field : null;
    }

    public @Nullable SearchField byAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        return byAlias.get(alias.toLowerCase(Locale.ROOT));
    }

    public boolean isKnownAlias(String alias) {
        return byAlias(alias) != null;
    }

    public @Nullable SearchField byJeiPrefix(char prefix) {
        for (SearchField field : fields) {
            Character jeiPrefix = field.spec().jeiPrefix();
            if (jeiPrefix != null && jeiPrefix == prefix) {
                return field;
            }
        }
        return null;
    }

    /** Field codec scoped to this set (by canonical name). */
    public Codec<SearchField> fieldCodec() {
        return Codec.STRING.xmap(
            name -> {
                SearchField field = byName(name);
                if (field == null) {
                    throw new IllegalArgumentException("Unknown search field: " + name);
                }
                return field;
            },
            SearchField::name
        );
    }
}
