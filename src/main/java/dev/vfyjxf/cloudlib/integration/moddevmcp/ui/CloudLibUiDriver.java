package dev.vfyjxf.cloudlib.integration.moddevmcp.ui;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.integration.moddevmcp.CloudLibSceneAccessor;
import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.api.runtime.DriverDescriptor;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class CloudLibUiDriver implements UiDriver {

    private static final DriverDescriptor DESCRIPTOR = new DriverDescriptor(
            "cloudlib-screen",
            Constants.modId,
            200,
            Set.of("snapshot", "query", "action", "inspect")
    );
    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;

    @Override
    public DriverDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean matches(UiContext context) {
        Object handle = context.screenHandle();
        return handle instanceof BasicScreen;
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        Scene scene = sceneFromContext(context);
        if (scene == null) {
            return new UiSnapshot("screen", context.screenClass(), descriptor().id(), List.of(), List.of(),
                    null, null, null, null, Map.of("closed", true));
        }

        String screenClass = context.screenClass();
        String modId = context.modId();
        List<UiTarget> targets = buildTargets(scene, screenClass, modId);

        String focusedTargetId = CloudLibUiTargetMapper.targetIdFor(scene.focusingWidget());
        String hoveredTargetId = CloudLibUiTargetMapper.targetIdFor(scene.hitTest(context.mouseX(), context.mouseY()));
        String activeTargetId = CloudLibUiTargetMapper.targetIdFor(findDraggingWidget(scene));

        return new UiSnapshot("screen", screenClass, descriptor().id(), targets, List.of(),
                focusedTargetId,
                null,
                hoveredTargetId,
                activeTargetId,
                Map.of());
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets().stream()
                .filter(target -> matchesTarget(selector, target))
                .toList();
    }

    @Override
    public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
        Scene scene = sceneFromContext(context);
        if (scene == null) {
            return OperationResult.rejected("runtime_unavailable");
        }
        UiTarget target = resolveTarget(context, request);
        if (target == null) {
            return OperationResult.rejected("target_not_found");
        }
        Widget widget = resolveWidget(scene, target.targetId());
        if (widget == null) {
            return OperationResult.rejected("widget_not_found");
        }
        return executeAction(scene, widget, target, request);
    }

    @Override
    public OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
        Scene scene = sceneFromContext(context);
        if (scene == null) {
            return OperationResult.rejected("runtime_unavailable");
        }
        var path = WidgetTree.hitTestPath(scene.root(), x, y);
        if (path.isEmpty()) {
            return OperationResult.rejected("hit_test_empty");
        }
        List<UiTarget> targets = new ArrayList<>();
        for (Widget widget : path) {
            targets.add(CloudLibUiTargetMapper.toTarget(widget, descriptor().id(), context.screenClass(), context.modId()));
        }
        return OperationResult.success(targets);
    }

    @Override
    public UiInteractionState interactionState(UiContext context) {
        UiSnapshot snapshot = snapshot(context, SnapshotOptions.DEFAULT);
        return new UiInteractionState(
                findTarget(snapshot.targets(), snapshot.focusedTargetId()),
                null,
                findTarget(snapshot.targets(), snapshot.hoveredTargetId()),
                findTarget(snapshot.targets(), snapshot.activeTargetId()),
                context.mouseX(),
                context.mouseY(),
                false,
                "unknown",
                descriptor().id()
        );
    }

    private Scene sceneFromContext(UiContext context) {
        Object handle = context.screenHandle();
        if (!(handle instanceof Screen screen)) {
            return null;
        }
        return CloudLibSceneAccessor.tryGetScene(screen);
    }

    private List<UiTarget> buildTargets(Scene scene, String screenClass, String modId) {
        List<UiTarget> targets = new ArrayList<>();
        Widget root = scene.root();
        targets.add(screenTarget(root, screenClass, modId));
        WidgetTree.walkPreOrder(root, false, -1, (widget, depth) -> {
            targets.add(CloudLibUiTargetMapper.toTarget(widget, descriptor().id(), screenClass, modId));
            return WidgetTree.TraversalControl.proceed;
        });
        return List.copyOf(targets);
    }

    private UiTarget screenTarget(Widget root, String screenClass, String modId) {
        Bounds bounds;
        if (root == null) {
            bounds = new Bounds(0, 0, 0, 0);
        } else {
            var rect = root.absoluteBounds();
            bounds = new Bounds(rect.x(), rect.y(), rect.width(), rect.height());
        }
        return new UiTarget(
                "screen-root",
                descriptor().id(),
                screenClass,
                modId,
                "screen",
                screenClass,
                bounds,
                UiTargetState.defaultState(),
                List.of("capture", "focus"),
                Map.of()
        );
    }

    private UiTarget resolveTarget(UiContext context, UiActionRequest request) {
        if (request == null || request.target() == null) {
            return snapshot(context, SnapshotOptions.DEFAULT).targets().stream().findFirst().orElse(null);
        }
        return query(context, request.target()).stream().findFirst().orElse(null);
    }

    private Widget resolveWidget(Scene scene, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return null;
        }
        if ("screen-root".equals(targetId)) {
            return scene.root();
        }
        return WidgetTree.findFirst(scene.root(), true, -1,
                widget -> targetId.equals(CloudLibUiTargetMapper.targetIdFor(widget)));
    }

    private OperationResult<Map<String, Object>> executeAction(Scene scene, Widget widget, UiTarget target, UiActionRequest request) {
        if (request == null || request.action() == null) {
            return OperationResult.rejected("invalid_input");
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.isSameThread()) {
                return executeOnClientThread(scene, widget, target, request);
            }
            CompletableFuture<OperationResult<Map<String, Object>>> future = new CompletableFuture<>();
            minecraft.execute(() -> {
                try {
                    future.complete(executeOnClientThread(scene, widget, target, request));
                } catch (RuntimeException exception) {
                    future.completeExceptionally(exception);
                }
            });
            try {
                return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return OperationResult.rejected("interrupted while waiting for ui action execution");
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                return OperationResult.rejected("ui action execution failed: " + String.valueOf(cause));
            } catch (TimeoutException exception) {
                return OperationResult.rejected("timed out waiting for ui action execution");
            }
        } catch (NoClassDefFoundError exception) {
            return OperationResult.rejected("runtime_unavailable");
        }
    }

    private OperationResult<Map<String, Object>> executeOnClientThread(Scene scene, Widget widget, UiTarget target, UiActionRequest request) {
        Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
        String action = request.action();
        double centerX = target.bounds().x() + (target.bounds().width() / 2.0d);
        double centerY = target.bounds().y() + (target.bounds().height() / 2.0d);
        return switch (action) {
            case "hover" -> handleHover(scene, target, centerX, centerY);
            case "click" -> handleClick(scene, target, centerX, centerY, args);
            case "scroll" -> handleScroll(scene, target, centerX, centerY, args);
            case "drag" -> handleDrag(scene, target, centerX, centerY, args);
            case "focus" -> handleFocus(scene, widget, target);
            case "key" -> handleKey(scene, target, args);
            case "char" -> handleChar(scene, target, args);
            default -> OperationResult.rejected("unsupported_action");
        };
    }

    private OperationResult<Map<String, Object>> handleHover(Scene scene, UiTarget target, double x, double y) {
        scene.mouseMoved(x, y);
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "hover",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private OperationResult<Map<String, Object>> handleClick(Scene scene, UiTarget target, double x, double y, Map<String, Object> args) {
        int button = intArg(args, "button", 0);
        scene.mouseMoved(x, y);
        boolean clicked = scene.mouseClicked(x, y, button);
        boolean released = scene.mouseReleased(x, y, button);
        if (!clicked && !released) {
            return OperationResult.rejected("click_not_handled");
        }
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "click",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private OperationResult<Map<String, Object>> handleScroll(Scene scene, UiTarget target, double x, double y, Map<String, Object> args) {
        double scrollX = doubleArg(args, "scrollX", 0.0d);
        double scrollY = doubleArg(args, "scrollY", 0.0d);
        scene.mouseMoved(x, y);
        boolean handled = scene.mouseScrolled(x, y, scrollX, scrollY);
        if (!handled) {
            return OperationResult.rejected("scroll_not_handled");
        }
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "scroll",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private OperationResult<Map<String, Object>> handleDrag(Scene scene, UiTarget target, double x, double y, Map<String, Object> args) {
        int button = intArg(args, "button", 0);
        double dragX = doubleArg(args, "dragX", doubleArg(args, "deltaX", 0.0d));
        double dragY = doubleArg(args, "dragY", doubleArg(args, "deltaY", 0.0d));
        scene.mouseMoved(x, y);
        boolean handled = scene.mouseDragged(x, y, button, dragX, dragY);
        if (!handled) {
            return OperationResult.rejected("drag_not_handled");
        }
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "drag",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private OperationResult<Map<String, Object>> handleFocus(Scene scene, Widget widget, UiTarget target) {
        scene.requestFocus(widget);
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "focus",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private OperationResult<Map<String, Object>> handleKey(Scene scene, UiTarget target, Map<String, Object> args) {
        int keyCode = intArg(args, "keyCode", 0);
        int scanCode = intArg(args, "scanCode", 0);
        int modifiers = intArg(args, "modifiers", 0);
        boolean handled = scene.keyPressed(keyCode, scanCode, modifiers);
        if (!handled) {
            return OperationResult.rejected("key_not_handled");
        }
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "key",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private OperationResult<Map<String, Object>> handleChar(Scene scene, UiTarget target, Map<String, Object> args) {
        Object charArg = args.get("char");
        char codePoint;
        if (charArg instanceof String text && !text.isEmpty()) {
            codePoint = text.charAt(0);
        } else if (charArg instanceof Number number) {
            codePoint = (char) number.intValue();
        } else {
            return OperationResult.rejected("invalid_input");
        }
        int modifiers = intArg(args, "modifiers", 0);
        boolean handled = scene.charTyped(codePoint, modifiers);
        if (!handled) {
            return OperationResult.rejected("char_not_handled");
        }
        return OperationResult.success(Map.of(
                "driverId", descriptor().id(),
                "action", "char",
                "performed", true,
                "targetId", target.targetId()
        ));
    }

    private boolean matchesTarget(TargetSelector selector, UiTarget target) {
        if (selector == null) {
            return true;
        }
        if (selector.scope() != null && !selector.scope().isBlank() && !"element".equals(selector.scope())) {
            if (!selector.scope().equals(target.role())) {
                return false;
            }
        }
        if (selector.role() != null && !selector.role().equals(target.role())) {
            return false;
        }
        if (selector.id() != null && !selector.id().equals(target.targetId())) {
            return false;
        }
        if (selector.text() != null && (target.text() == null || !selector.text().equals(target.text()))) {
            return false;
        }
        if (selector.modId() != null && !selector.modId().equals(target.modId())) {
            return false;
        }
        if (selector.bounds() != null && !intersects(selector.bounds(), target.bounds())) {
            return false;
        }
        return true;
    }

    private boolean intersects(Bounds a, Bounds b) {
        return a.x() < b.x() + b.width()
                && a.x() + a.width() > b.x()
                && a.y() < b.y() + b.height()
                && a.y() + a.height() > b.y();
    }

    private Widget findDraggingWidget(Scene scene) {
        return WidgetTree.findFirst(scene.root(), true, -1, Widget::dragging);
    }

    private UiTarget findTarget(List<UiTarget> targets, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return null;
        }
        return targets.stream()
                .filter(target -> target.targetId().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    private int intArg(Map<String, Object> args, String key, int fallback) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private double doubleArg(Map<String, Object> args, String key, double fallback) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}
