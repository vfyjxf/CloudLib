package dev.vfyjxf.cloudlib.api.search;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The generic immutable search document over a scenario's
 * {@link SearchFieldSet}: per-field text/number/content entries, plus derived
 * unions for the set's virtual (aggregate) fields and an optional default-text
 * channel collecting every searchable value.
 */
public final class SearchDocument implements SearchableDocument {
    private final SearchFieldSet fieldSet;

    private final List<SearchTextValue>[] textEntriesByField;
    private final SearchTextValue[][] textEntryArraysByField;
    private final TextBucket[] textBuckets;
    private final Map<SearchField, List<Double>> numberValues;
    private final Map<SearchField, List<SearchContentValue>> contentValues;

    private SearchDocument(
        SearchFieldSet fieldSet,
        Map<SearchField, List<SearchTextValue>> textEntries,
        Map<SearchField, List<Double>> numberValues,
        Map<SearchField, List<SearchContentValue>> contentValues
    ) {
        this.fieldSet = fieldSet;
        this.numberValues = freeze(numberValues);
        this.contentValues = freeze(contentValues);

        @SuppressWarnings("unchecked")
        List<SearchTextValue>[] entriesByField = new List[fieldSet.size()];
        SearchTextValue[][] arraysByField = new SearchTextValue[fieldSet.size()][];

        BiConsumer<SearchField, List<SearchTextValue>> putField = (field, entries) -> {
            List<SearchTextValue> immutable = List.copyOf(entries);
            entriesByField[field.id()] = immutable;
            arraysByField[field.id()] = immutable.toArray(new SearchTextValue[0]);
        };

        for (SearchField field : fieldSet.fields()) {
            if (field.spec().virtual()) {
                continue; // filled from the union below
            }
            putField.accept(field, textEntries.getOrDefault(field, List.of()));
        }
        for (SearchField virtualField : fieldSet.virtualFields()) {
            ArrayList<SearchTextValue> union = new ArrayList<>();
            for (SearchField source : fieldSet.containsSources()) {
                if (source.id() < fieldSet.size() && entriesByField[source.id()] != null) {
                    union.addAll(entriesByField[source.id()]);
                }
            }
            putField.accept(virtualField, union);
        }

        this.textEntriesByField = entriesByField;
        this.textEntryArraysByField = arraysByField;

        TextBucket[] buckets = new TextBucket[fieldSet.size()];
        for (int i = 0; i < buckets.length; i++) {
            SearchTextValue[] array = arraysByField[i];
            if (array == null || array.length == 0) {
                buckets[i] = EMPTY_BUCKET;
                continue;
            }
            StringBuilder raw = new StringBuilder();
            StringBuilder norm = new StringBuilder();
            for (SearchTextValue entry : array) {
                raw.append('\0').append(entry.value());
                norm.append('\0').append(entry.normalizedValue());
            }
            buckets[i] = new TextBucket(array, raw.toString(), norm.toString());
        }
        this.textBuckets = buckets;
    }

    public static Builder builder(SearchFieldSet fieldSet, @Nullable SearchField defaultTextField) {
        return new Builder(fieldSet, defaultTextField);
    }

    /** The scenario field set this document is built against. */
    public SearchFieldSet fieldSet() {
        return fieldSet;
    }

    @Override
    public List<SearchTextValue> textEntries(SearchField field) {
        int id = field.id();
        return id < textEntriesByField.length && textEntriesByField[id] != null ? textEntriesByField[id] : List.of();
    }

    @Override
    public SearchTextValue[] textEntryArray(SearchField field) {
        int id = field.id();
        return id < textEntryArraysByField.length && textEntryArraysByField[id] != null ? textEntryArraysByField[id] : new SearchTextValue[0];
    }

    TextBucket textBucket(SearchField field) {
        int id = field.id();
        return id < textBuckets.length ? textBuckets[id] : EMPTY_BUCKET;
    }

    @Override
    public List<Double> numberValues(SearchField field) {
        if (field.spec().virtual()) {
            ArrayList<Double> values = new ArrayList<>();
            for (SearchField source : fieldSet.containsSources()) {
                values.addAll(numberValues.getOrDefault(source, List.of()));
            }
            return List.copyOf(values);
        }
        return numberValues.getOrDefault(field, List.of());
    }

    @Override
    public List<SearchContentValue> contentEntries(SearchField field) {
        if (field.spec().virtual()) {
            ArrayList<SearchContentValue> values = new ArrayList<>();
            for (SearchField source : fieldSet.containsSources()) {
                values.addAll(contentValues.getOrDefault(source, List.of()));
            }
            return List.copyOf(values);
        }
        return contentValues.getOrDefault(field, List.of());
    }

    private static <T> Map<SearchField, List<T>> freeze(Map<SearchField, List<T>> source) {
        HashMap<SearchField, List<T>> copy = new HashMap<>();
        for (Map.Entry<SearchField, List<T>> entry : source.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
        }
        return Map.copyOf(copy);
    }

    private static final TextBucket EMPTY_BUCKET = new TextBucket(new SearchTextValue[0], "", "");

    record TextBucket(SearchTextValue[] entries, String rawHaystack, String normalizedHaystack) {}

    public static final class Builder {
        private final SearchFieldSet fieldSet;
        private final @Nullable SearchField defaultTextField;
        private final HashMap<SearchField, ArrayList<SearchTextValue>> textEntries = new HashMap<>();
        private final HashMap<SearchField, ArrayList<Double>> numberValues = new HashMap<>();
        private final HashMap<SearchField, ArrayList<SearchContentValue>> contentValues = new HashMap<>();

        private Builder(SearchFieldSet fieldSet, @Nullable SearchField defaultTextField) {
            if (defaultTextField != null && defaultTextField.spec().virtual()) {
                throw new IllegalArgumentException("The default text field must not be virtual");
            }
            this.fieldSet = fieldSet;
            this.defaultTextField = defaultTextField;
        }

        public Builder text(SearchField field, String value) {
            if (!field.spec().virtual() && value != null && !value.isBlank()) {
                entries(field).add(new SearchTextValue(field, value));
                if (defaultTextField != null && field != defaultTextField) {
                    entries(defaultTextField).add(new SearchTextValue(field, value));
                }
            }
            return this;
        }

        public Builder searchableText(SearchField field, String value) {
            text(field, value);
            return this;
        }

        public Builder searchableContent(SearchField field, String value, double amount) {
            if (!field.spec().virtual()
                && value != null && !value.isBlank()
                && Double.isFinite(amount)) {
                contentValues.computeIfAbsent(field, ignored -> new ArrayList<>())
                    .add(new SearchContentValue(field, value, amount));
                entries(field).add(new SearchTextValue(field, value));
                if (defaultTextField != null && field != defaultTextField && !field.spec().numeric()) {
                    entries(defaultTextField).add(new SearchTextValue(field, value));
                }
                number(field, amount);
            }
            return this;
        }

        public Builder texts(SearchField field, Iterable<String> values) {
            if (values != null) {
                for (String value : values) {
                    text(field, value);
                }
            }
            return this;
        }

        public Builder searchableTexts(SearchField field, Iterable<String> values) {
            return texts(field, values);
        }

        public Builder number(SearchField field, double value) {
            if (!field.spec().virtual() && Double.isFinite(value)) {
                numberValues.computeIfAbsent(field, ignored -> new ArrayList<>()).add(value);
            }
            return this;
        }

        public SearchDocument build() {
            return new SearchDocument(fieldSet, copyOf(textEntries), copyOf(numberValues), copyOf(contentValues));
        }

        private ArrayList<SearchTextValue> entries(SearchField field) {
            return textEntries.computeIfAbsent(field, ignored -> new ArrayList<>());
        }

        private static <T> Map<SearchField, List<T>> copyOf(Map<SearchField, ArrayList<T>> source) {
            HashMap<SearchField, List<T>> copy = new HashMap<>();
            for (Map.Entry<SearchField, ArrayList<T>> entry : source.entrySet()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return copy;
        }
    }
}
