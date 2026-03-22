package dev.vfyjxf.cloudlib.integration.moddevmcp.tools;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CloudLibUiTreeTool {

    private static final String OPERATION_ID = "cloudlib.ui.tree";

    private CloudLibUiTreeTool() {
    }

    public static OperationDefinition definition() {
        return new OperationDefinition(
                OPERATION_ID,
                "ui",
                "CloudLib UI Tree",
                "Returns the widget tree for the active CloudLib screen.",
                true,
                Set.of("client"),
                CloudLibUiToolSupport.objectSchema(
                        Map.of(
                                "includeRoot", Map.of("type", "boolean"),
                                "maxDepth", Map.of("type", "integer"),
                                "screenClass", Map.of("type", "string"),
                                "modId", Map.of("type", "string")
                        ),
                        List.of()
                ),
                Map.of(
                        "operationId", OPERATION_ID,
                        "targetSide", "client",
                        "input", Map.of()
                )
        );
    }

    public static OperationExecutor executor() {
        return (input, resolvedTargetSide) -> {
            Scene scene = CloudLibUiToolSupport.requireScene(input);
            boolean includeRoot = CloudLibUiToolSupport.boolArg(input, "includeRoot", true);
            int maxDepth = CloudLibUiToolSupport.intArg(input, "maxDepth", -1);

            List<Map<String, Object>> nodes = new ArrayList<>();
            Widget root = scene.root();
            WidgetTree.walkPreOrderWithPath(root, true, maxDepth, (widget, depth, ancestry) -> {
                if (depth == 0 && !includeRoot) {
                    return WidgetTree.TraversalControl.proceed;
                }
                nodes.add(CloudLibUiToolSupport.nodeSummary(widget, ancestry.toPath(), depth));
                return WidgetTree.TraversalControl.proceed;
            });

            var payload = new LinkedHashMap<String, Object>();
            payload.put("screenClass", CloudLibUiToolSupport.screenClass(CloudLibUiToolSupport.currentScreen()));
            payload.put("modId", CloudLibUiToolSupport.modId(input));
            payload.put("nodeCount", nodes.size());
            payload.put("nodes", List.copyOf(nodes));
            return Map.copyOf(payload);
        };
    }
}
