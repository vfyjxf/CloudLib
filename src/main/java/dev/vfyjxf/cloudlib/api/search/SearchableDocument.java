package dev.vfyjxf.cloudlib.api.search;

import java.util.List;

public interface SearchableDocument {
    List<SearchTextValue> textEntries(SearchField field);

    default List<String> textValues(SearchField field) {
        return textEntries(field).stream()
            .map(SearchTextValue::value)
            .toList();
    }

    default SearchTextValue[] textEntryArray(SearchField field) {
        return textEntries(field).toArray(new SearchTextValue[0]);
    }

    List<Double> numberValues(SearchField field);

    default List<SearchContentValue> contentEntries(SearchField field) {
        return List.of();
    }

    default boolean hasField(SearchField field) {
        return !textEntries(field).isEmpty() || !numberValues(field).isEmpty() || !contentEntries(field).isEmpty();
    }
}
