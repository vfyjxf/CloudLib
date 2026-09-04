package dev.vfyjxf.cloudlib.integration.moddevmcp.tools;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetPath;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CloudLibUiHitTestTool {

    private static final String OPERATION_ID = "cloudlib.ui.hit_test";

    private CloudLibUiHitTestTool() {
    }

    public static OperationDefinition definition() {
        return new OperationDefinition(
                OPERATION_ID,
                "ui",
                "CloudLib Hit Test",
                "Returns the widget path at the specified coordinates.",
                true,
                Set.of("client"),
                CloudLibUiToolSupport.objectSchema(
                        Map.of(
                                "x", Map.of("type", "number"),
                                "y", Map.of("type", "number"),
                                "screenClass", Map.of("type", "string"),
                                "modId", Map.of("type", "string")
                        ),
                        List.of("x", "y")
                ),
                Map.of(
                        "operationId", OPERATION_ID,
                        "targetSide", "client",
                        "input", Map.of("x", 0, "y", 0)
                )
        );
    }

    public static OperationExecutor executor() {
        return (input, resolvedTargetSide) -> {
            Scene scene = CloudLibUiToolSupport.requireScene(input);
            double x = CloudLibUiToolSupport.doubleArg(input, "x", 0.0d);
            double y = CloudLibUiToolSupport.doubleArg(input, "y", 0.0d);

            WidgetPath path = WidgetTree.hitTestPath(scene.root(), x, y);
            List<Map<String, Object>> hits = new ArrayList<>();
            int depth = 0;
            for (Widget widget : path) {
                hits.add(CloudLibUiToolSupport.nodeSummary(widget, path.subPath(0, depth + 1), depth));
                depth++;
            }

            var payload = new LinkedHashMap<String, Object>();
            payload.put("screenClass", CloudLibUiToolSupport.screenClass(CloudLibUiToolSupport.currentScreen()));
            payload.put("modId", CloudLibUiToolSupport.modId(input));
            payload.put("hitCount", hits.size());
            payload.put("hits", List.copyOf(hits));
            return Map.copyOf(payload);
        };
    }
}
