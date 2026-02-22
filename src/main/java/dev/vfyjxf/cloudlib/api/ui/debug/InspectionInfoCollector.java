package dev.vfyjxf.cloudlib.api.ui.debug;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.jetbrains.annotations.Nullable;

/**
 * Collects {@link InspectionProperty} entries from a widget for display in {@link Inspector}.
 * Values are formatted to strings at collection time to prevent recursive toString calls.
 */
public final class InspectionInfoCollector {

    private final MutableList<InspectionProperty> properties = Lists.mutable.empty();
    private final MutableListMultimap<String, InspectionProperty> byCategory = Multimaps.mutable.list.empty();

    public static InspectionInfoCollector create() {
        return new InspectionInfoCollector();
    }

    public static InspectionInfoCollector from(Widget widget) {
        var collector = create();
        widget.collectInspectionInfo(collector);
        return collector;
    }

    private InspectionInfoCollector() {}

    //region add

    public InspectionInfoCollector add(String name, @Nullable Object value) {
        return add(name, value, InspectionProperty.categoryBasic);
    }

    public InspectionInfoCollector add(String name, @Nullable Object value, String category) {
        return addProperty(new InspectionProperty(
            name, InspectionProperty.format(value), null, category
        ));
    }

    /** Adds a property with a pre-formatted string value. */
    public InspectionInfoCollector addFormatted(String name, String value, @Nullable String defaultValue, String category) {
        return addProperty(new InspectionProperty(name, value, defaultValue, category));
    }

    public InspectionInfoCollector addWithDefault(String name, @Nullable Object value, @Nullable Object defaultValue) {
        return addWithDefault(name, value, defaultValue, InspectionProperty.categoryBasic);
    }

    public InspectionInfoCollector addWithDefault(String name, @Nullable Object value, @Nullable Object defaultValue, String category) {
        return addProperty(new InspectionProperty(
            name,
            InspectionProperty.format(value),
            InspectionProperty.format(defaultValue),
            category
        ));
    }

    /** Adds a pre-built property directly. */
    public InspectionInfoCollector addProperty(InspectionProperty property) {
        properties.add(property);
        byCategory.put(property.category(), property);
        return this;
    }

    //endregion

    //region query

    public MutableList<InspectionProperty> getAll() {
        return properties.asUnmodifiable();
    }

    public MutableList<InspectionProperty> getByCategory(String category) {
        return byCategory.get(category).toList().asUnmodifiable();
    }

    public MutableList<String> getCategories() {
        return Lists.mutable.withAll(byCategory.keysView());
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    //endregion
}
