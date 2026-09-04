package dev.vfyjxf.cloudlib.integration.moddevmcp.tools;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetPath;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.integration.moddevmcp.CloudLibWidgetPathFormatter;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CloudLibUiWidgetInfoTool {

    private static final String OPERATION_ID = "cloudlib.ui.widget_info";

    private CloudLibUiWidgetInfoTool() {
    }

    public static OperationDefinition definition() {
        return new OperationDefinition(
                OPERATION_ID,
                "ui",
                "CloudLib Widget Info",
                "Returns inspection details for a specific CloudLib widget.",
                true,
                Set.of("client"),
                CloudLibUiToolSupport.objectSchema(
                        Map.of(
                                "targetId", Map.of("type", "string"),
                                "x", Map.of("type", "number"),
                                "y", Map.of("type", "number"),
                                "screenClass", Map.of("type", "string"),
                                "modId", Map.of("type", "string")
                        ),
                        List.of()
                ),
                Map.of(
                        "operationId", OPERATION_ID,
                        "targetSide", "client",
                        "input", Map.of("targetId", "root")
                )
        );
    }

    public static OperationExecutor executor() {
        return (input, resolvedTargetSide) -> {
            Scene scene = CloudLibUiToolSupport.requireScene(input);

            String targetId = CloudLibUiToolSupport.stringArg(input.get("targetId"));
            Widget widget = null;
            WidgetPath path = null;
            if (targetId != null && !targetId.isBlank()) {
                widget = CloudLibUiToolSupport.findWidgetByTargetId(scene, targetId);
                if (widget != null) {
                    path = widget.path();
                }
            } else if (input.containsKey("x") && input.containsKey("y")) {
                double x = CloudLibUiToolSupport.doubleArg(input, "x", 0.0d);
                double y = CloudLibUiToolSupport.doubleArg(input, "y", 0.0d);
                path = WidgetTree.hitTestPath(scene.root(), x, y);
                widget = path.leaf();
            }

            if (widget == null || path == null || path.isEmpty()) {
                throw CloudLibUiToolSupport.executionFailure("target_not_found");
            }

            var collector = InspectionInfoCollector.from(widget);
            var properties = CloudLibUiToolSupport.collectInspection(collector, category -> true);

            var payload = new LinkedHashMap<String, Object>();
            payload.put("screenClass", CloudLibUiToolSupport.screenClass(CloudLibUiToolSupport.currentScreen()));
            payload.put("modId", CloudLibUiToolSupport.modId(input));
            payload.put("targetId", CloudLibUiToolSupport.nodeSummary(widget, path, path.size() - 1).get("targetId"));
            payload.put("widgetPath", CloudLibWidgetPathFormatter.format(path));
            payload.put("bounds", CloudLibUiToolSupport.boundsMap(widget.absoluteBounds()));
            payload.put("properties", properties);
            return Map.copyOf(payload);
        };
    }
}
