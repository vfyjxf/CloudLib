package dev.vfyjxf.cloudlib.integration.moddevmcp.tools;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetPath;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.integration.moddevmcp.CloudLibSceneAccessor;
import dev.vfyjxf.cloudlib.integration.moddevmcp.CloudLibWidgetPathFormatter;
import dev.vfyjxf.cloudlib.integration.moddevmcp.ui.CloudLibUiTargetMapper;
import dev.vfyjxf.moddev.service.request.OperationError;
import dev.vfyjxf.moddev.service.request.OperationExecutionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

final class CloudLibUiToolSupport {

    private CloudLibUiToolSupport() {
    }

    static Scene requireScene(Map<String, Object> input) {
        Scene scene = resolveScene(input);
        if (scene == null) {
            throw executionFailure("runtime_unavailable");
        }
        return scene;
    }

    static Scene resolveScene(Map<String, Object> input) {
        try {
            Screen screen = currentScreen();
            if (!(screen instanceof BasicScreen)) {
                return null;
            }
            Scene scene = CloudLibSceneAccessor.tryGetScene(screen);
            if (scene == null) {
                return null;
            }
            String expectedScreenClass = stringArg(input.get("screenClass"));
            if (expectedScreenClass != null && !expectedScreenClass.isBlank()) {
                String actual = screen.getClass().getName();
                if (!expectedScreenClass.equals(actual)) {
                    throw executionFailure("screen_mismatch: expected " + expectedScreenClass + " but was " + actual);
                }
            }
            return scene;
        } catch (NoClassDefFoundError ignored) {
            return null;
        }
    }

    static Screen currentScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        return minecraft.screen;
    }

    static String screenClass(Screen screen) {
        if (screen == null) {
            return "custom.UnknownScreen";
        }
        return screen.getClass().getName();
    }

    static String modId(Map<String, Object> input) {
        String modId = stringArg(input.get("modId"));
        if (modId == null || modId.isBlank()) {
            return Constants.modId;
        }
        return modId;
    }

    static Map<String, Object> boundsMap(Rect rect) {
        if (rect == null) {
            return Map.of();
        }
        return Map.of(
                "x", rect.x(),
                "y", rect.y(),
                "width", rect.width(),
                "height", rect.height()
        );
    }

    static Map<String, Object> nodeSummary(Widget widget, WidgetPath path, int depth) {
        String targetId = CloudLibUiTargetMapper.targetIdFor(widget);
        String role = widget.inspectionTypeName();
        if (role == null || role.isBlank()) {
            role = "widget";
        }
        String widgetKey = null;
        Object key = widget.key();
        if (key != null) {
            widgetKey = key.toString();
        }
        var summary = new LinkedHashMap<String, Object>();
        summary.put("targetId", targetId);
        summary.put("widgetPath", CloudLibWidgetPathFormatter.format(path));
        summary.put("role", role);
        summary.put("depth", depth);
        summary.put("bounds", boundsMap(widget.absoluteBounds()));
        if (widgetKey != null && !widgetKey.isBlank()) {
            summary.put("widgetKey", widgetKey);
        }
        return Map.copyOf(summary);
    }

    static List<Map<String, Object>> collectInspection(
            InspectionInfoCollector collector,
            Predicate<String> categoryFilter
    ) {
        Objects.requireNonNull(collector, "collector");
        List<Map<String, Object>> entries = new ArrayList<>();
        for (String category : collector.getCategories()) {
            if (categoryFilter != null && !categoryFilter.test(category)) {
                continue;
            }
            for (InspectionProperty property : collector.getByCategory(category)) {
                entries.add(propertyMap(property));
            }
        }
        return List.copyOf(entries);
    }

    static Map<String, Object> propertyMap(InspectionProperty property) {
        var map = new LinkedHashMap<String, Object>();
        map.put("name", property.name());
        map.put("value", property.value());
        if (property.defaultValue() != null) {
            map.put("defaultValue", property.defaultValue());
        }
        map.put("category", property.category());
        return Map.copyOf(map);
    }

    static Widget findWidgetByTargetId(Scene scene, String targetId) {
        if (scene == null || targetId == null || targetId.isBlank()) {
            return null;
        }
        return WidgetTree.findFirst(scene.root(), true, -1,
                widget -> targetId.equals(CloudLibUiTargetMapper.targetIdFor(widget)));
    }

    static String stringArg(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static int intArg(Map<String, Object> input, String key, int fallback) {
        Object value = input.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    static double doubleArg(Map<String, Object> input, String key, double fallback) {
        Object value = input.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    static boolean boolArg(Map<String, Object> input, String key, boolean fallback) {
        Object value = input.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    static OperationExecutionException executionFailure(String message) {
        String normalized = message == null || message.isBlank() ? "operation execution failed" : message;
        return new OperationExecutionException(new OperationError("operation_execution_failed", normalized));
    }

    static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return Map.copyOf(schema);
    }
}
