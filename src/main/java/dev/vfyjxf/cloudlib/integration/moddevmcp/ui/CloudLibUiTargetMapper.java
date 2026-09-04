package dev.vfyjxf.cloudlib.integration.moddevmcp.ui;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.integration.moddevmcp.CloudLibWidgetPathFormatter;
import dev.vfyjxf.moddev.api.ui.Bounds;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.api.ui.UiTargetState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CloudLibUiTargetMapper {

    private CloudLibUiTargetMapper() {
    }

    public static String targetIdFor(Widget widget) {
        if (widget == null) {
            return null;
        }
        Object key = widget.key();
        if (key != null) {
            String keyText = key.toString();
            if (keyText != null && !keyText.isEmpty()) {
                return keyText;
            }
        }
        try {
            return CloudLibWidgetPathFormatter.format(widget.path());
        } catch (RuntimeException exception) {
            String typeName = widget.inspectionTypeName();
            if (typeName == null || typeName.trim().isEmpty()) {
                typeName = widget.getClass().getSimpleName();
            }
            return typeName + "@" + Integer.toHexString(System.identityHashCode(widget));
        }
    }

    public static UiTarget toTarget(Widget widget, String driverId, String screenClass, String modId) {
        String targetId = targetIdFor(widget);
        String role = widget.inspectionTypeName();
        if (role == null || role.trim().isEmpty()) {
            role = "widget";
        }
        String text = null;
        Object key = widget.key();
        if (key != null) {
            String keyText = key.toString();
            if (keyText != null && !keyText.isEmpty()) {
                text = keyText;
            }
        }
        Rect rect = widget.absoluteBounds();
        Bounds bounds = new Bounds(rect.x(), rect.y(), rect.width(), rect.height());
        UiTargetState state = new UiTargetState(
                widget.visible(),
                widget.active() && widget.interactive(),
                widget.focused(),
                widget.hovered(),
                false,
                widget.dragging()
        );
        List<String> actions = actionsFor(widget);
        Map<String, Object> extensions = extensionsFor(widget, targetId);
        return new UiTarget(
                targetId,
                driverId,
                screenClass,
                modId,
                role,
                text,
                bounds,
                state,
                actions,
                extensions
        );
    }

    private static List<String> actionsFor(Widget widget) {
        List<String> actions = new ArrayList<>();
        if (widget.visible()) {
            actions.add("hover");
        }
        if (widget.active() && widget.interactive()) {
            actions.add("click");
            actions.add("scroll");
        }
        if (widget.focusable()) {
            actions.add("focus");
        }
        if (widget.draggable()) {
            actions.add("drag");
        }
        return List.copyOf(actions);
    }

    private static Map<String, Object> extensionsFor(Widget widget, String widgetPath) {
        Map<String, Object> extensions = new HashMap<>();
        Object key = widget.key();
        if (key != null) {
            extensions.put("widgetKey", key.toString());
        }
        String typeName = widget.inspectionTypeName();
        if (typeName != null && !typeName.isBlank()) {
            extensions.put("widgetType", typeName);
        }
        if (widgetPath != null) {
            extensions.put("widgetPath", widgetPath);
        }
        extensions.put("sceneLayer", widget.sceneLayer().name());
        extensions.put("zIndex", widget.zIndex());
        extensions.put("interactive", widget.interactive());
        return Map.copyOf(extensions);
    }
}
