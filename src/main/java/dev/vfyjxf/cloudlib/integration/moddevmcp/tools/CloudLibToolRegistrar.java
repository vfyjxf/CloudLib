package dev.vfyjxf.cloudlib.integration.moddevmcp.tools;

import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CloudLibToolRegistrar {

    private CloudLibToolRegistrar() {
    }

    public static Map<String, OperationDefinition> schemas() {
        var schemas = new LinkedHashMap<String, OperationDefinition>();
        registerSchema(schemas, CloudLibUiTreeTool.definition());
        registerSchema(schemas, CloudLibUiLayoutReportTool.definition());
        registerSchema(schemas, CloudLibUiHitTestTool.definition());
        registerSchema(schemas, CloudLibUiWidgetInfoTool.definition());
        return Map.copyOf(schemas);
    }

    public static void register(RegisterClientOperationsEvent event) {
        Objects.requireNonNull(event, "event");
        register(event, CloudLibUiTreeTool.definition(), CloudLibUiTreeTool.executor());
        register(event, CloudLibUiLayoutReportTool.definition(), CloudLibUiLayoutReportTool.executor());
        register(event, CloudLibUiHitTestTool.definition(), CloudLibUiHitTestTool.executor());
        register(event, CloudLibUiWidgetInfoTool.definition(), CloudLibUiWidgetInfoTool.executor());
    }

    private static void register(RegisterClientOperationsEvent event, OperationDefinition definition, OperationExecutor executor) {
        event.registerOperation(definition, executor);
    }

    private static void registerSchema(Map<String, OperationDefinition> schemas, OperationDefinition definition) {
        schemas.put(definition.operationId(), definition);
    }
}
