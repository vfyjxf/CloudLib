package dev.vfyjxf.cloudlib.api.ui.debug;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.function.Predicate;

/**
 * Collector for widget inspection information.
 * <p>
 * This class aggregates {@link InspectionProperty} from widgets and provides
 * filtering and formatting capabilities for display in tools like {@link Inspector}.
 */
public final class InspectionInfoCollector {

    private final MutableList<InspectionProperty> properties = Lists.mutable.empty();
    private final MutableListMultimap<String, InspectionProperty> byCategory = Multimaps.mutable.list.empty();
    private @Nullable String widgetType;
    private @Nullable String widgetId;

    /** Creates a new inspection info collector. */
    public static InspectionInfoCollector create() {
        return new InspectionInfoCollector();
    }

    /** Creates a collector and collects info from the given widget. */
    public static InspectionInfoCollector from(Widget widget) {
        InspectionInfoCollector collector = create();
        collector.setWidgetType(widget.inspectionTypeName());
        if (widget.key() != null) {
            collector.setWidgetId(widget.key().toString());
        }
        widget.collectInspectionInfo(collector);
        return collector;
    }

    private InspectionInfoCollector() {}

    /** Sets the widget type name. */
    public InspectionInfoCollector setWidgetType(String type) {
        this.widgetType = type;
        return this;
    }

    /** Gets the widget type name. */
    public @Nullable String getWidgetType() {
        return widgetType;
    }

    /** Sets the widget ID. */
    public InspectionInfoCollector setWidgetId(@Nullable String id) {
        this.widgetId = id;
        return this;
    }

    /** Gets the widget ID. */
    public @Nullable String getWidgetId() {
        return widgetId;
    }

    /** Adds a simple property. */
    public InspectionInfoCollector add(String name, @Nullable Object value) {
        return add(InspectionProperty.of(name, value));
    }

    /** Adds a property with category. */
    public InspectionInfoCollector add(String name, @Nullable Object value, String category) {
        return add(InspectionProperty.of(name, value, category));
    }

    /** Adds a property with default value comparison. */
    public InspectionInfoCollector addWithDefault(String name, @Nullable Object value, @Nullable Object defaultValue) {
        return add(InspectionProperty.withDefault(name, value, defaultValue));
    }

    /** Adds a property with default value and category. */
    public InspectionInfoCollector addWithDefault(String name, @Nullable Object value, @Nullable Object defaultValue, String category) {
        return add(InspectionProperty.withDefault(name, value, defaultValue, category));
    }

    /** Adds an inspection property. */
    public InspectionInfoCollector add(InspectionProperty property) {
        properties.add(property);
        byCategory.put(property.category(), property);
        return this;
    }

    /** Gets all collected properties. */
    @Unmodifiable
    public MutableList<InspectionProperty> getAll() {
        return properties.asUnmodifiable();
    }

    /** Gets properties filtered by the given predicate. */
    public MutableList<InspectionProperty> getFiltered(Predicate<InspectionProperty> predicate) {
        return properties.select(predicate::test);
    }

    /** Gets all properties that differ from their default values. */
    public MutableList<InspectionProperty> getNonDefaultProperties() {
        return properties.select(InspectionProperty::isNonDefault);
    }

    /** Gets properties by category. */
    @Unmodifiable
    public MutableList<InspectionProperty> getByCategory(String category) {
        return byCategory.get(category).toList().asUnmodifiable();
    }

    /** Gets all category names. */
    @Unmodifiable
    public MutableList<String> getCategories() {
        return Lists.mutable.withAll(byCategory.keysView());
    }

    /** Checks if any properties are collected. */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /** Gets the count of properties. */
    public int size() {
        return properties.size();
    }

    /** Clears all collected properties. */
    public void clear() {
        properties.clear();
        byCategory.clear();
        widgetType = null;
        widgetId = null;
    }

    /** Formats the inspection info as a single-line string. */
    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        sb.append(widgetType != null ? widgetType : "Widget");
        if (widgetId != null) {
            sb.append("[").append(widgetId).append("]");
        }
        sb.append("{");

        MutableList<InspectionProperty> nonDefault = getNonDefaultProperties();
        if (!nonDefault.isEmpty()) {
            sb.append(nonDefault.collect(InspectionProperty::toString).makeString(", "));
        }
        sb.append("}");
        return sb.toString();
    }

    /** Formats the inspection info as a multi-line string for display. */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(widgetType != null ? widgetType : "Widget");
        if (widgetId != null) {
            sb.append(" [id=").append(widgetId).append("]");
        }
        sb.append("\n");

        for (String category : getCategories().toSortedList()) {
            MutableList<InspectionProperty> categoryProps = getByCategory(category);
            MutableList<InspectionProperty> nonDefault = categoryProps.select(InspectionProperty::isNonDefault);

            if (!nonDefault.isEmpty()) {
                sb.append("  ").append(category).append(":\n");
                for (InspectionProperty prop : nonDefault) {
                    sb.append("    ").append(prop.name()).append(": ").append(prop.formattedValue()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /** Formats all properties as a multi-line string for display. */
    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        sb.append(widgetType != null ? widgetType : "Widget");
        if (widgetId != null) {
            sb.append(" [id=").append(widgetId).append("]");
        }
        sb.append("\n");

        for (String category : getCategories().toSortedList()) {
            MutableList<InspectionProperty> categoryProps = getByCategory(category);
            if (!categoryProps.isEmpty()) {
                sb.append("  ").append(category).append(":\n");
                for (InspectionProperty prop : categoryProps) {
                    sb.append("    ").append(prop.name()).append(": ").append(prop.formattedValue());
                    if (!prop.isNonDefault() && prop.defaultValue() != null) {
                        sb.append(" (default)");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toCompactString();
    }
}
