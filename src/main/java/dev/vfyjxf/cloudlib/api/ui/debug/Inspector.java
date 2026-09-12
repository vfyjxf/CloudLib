package dev.vfyjxf.cloudlib.api.ui.debug;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvents;
import dev.vfyjxf.cloudlib.debug.DebugConfig;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Debug overlay for inspecting widgets in a {@link Scene}.
 * Resolves its target via {@link Scene#hitTest} when mouse tracking is enabled.
 * The scene root is excluded by default; see {@link #setIncludeRoot}.
 */
public class Inspector extends Widget {

    public enum DisplayMode {
        minimized,
        compact,
        full
    }

    // region constants

    private static final int padding = 4;
    private static final int lineHeight = 10;
    private static final int categoryIndent = 2;
    private static final int propertyIndent = 8;
    private static final int titleBarHeight = 12;
    private static final int scrollbarArea = 8;

    private static final int compactW = 250, compactH = 100;
    private static final int fullW = 300, fullH = 400;

    // endregion

    // region colors

    private static final int bg = 0xCC000000;
    private static final int border = 0xFF444444;
    private static final int text = 0xFFFFFFFF;
    private static final int categoryColor = 0xFF88FFFF;
    private static final int value = 0xFFAAFFAA;
    private static final int dim = 0xFF888888;
    private static final int hlFill = 0x3300CCFF;
    private static final int hlBorder = 0xAA00CCFF;
    private static final int titleBg = 0xFF333333;

    // endregion

    // region configuration

    private boolean showAllProperties;
    private boolean showHierarchy = true;
    private boolean showHighlight = true;
    private boolean includeRoot;
    private boolean trackMouse;
    private DisplayMode displayMode = DisplayMode.compact;

    // endregion

    // region state

    private @Nullable Widget target;
    private @Nullable Supplier<Widget> targetSupplier;
    private double lastMouseX;
    private double lastMouseY;
    private int scrollOffset;
    private int maxScrollOffset;
    private @Nullable Widget lastRenderedTarget;

    // endregion

    public static Inspector create() {
        return new Inspector();
    }

    private Inspector() {
        setTickable(true);
        setSize(compactW, compactH);
        setFocusable(true);
        registerInputHandlers();
    }

    // region configuration

    public Inspector setTarget(@Nullable Widget target) {
        this.target = target;
        this.targetSupplier = null;
        return this;
    }

    public Inspector setTargetSupplier(@Nullable Supplier<Widget> supplier) {
        this.targetSupplier = supplier;
        this.target = null;
        return this;
    }

    public Inspector setTrackMouse(boolean trackMouse) {
        this.trackMouse = trackMouse;
        return this;
    }

    public Inspector setShowAllProperties(boolean showAll) {
        this.showAllProperties = showAll;
        return this;
    }

    public Inspector setShowHierarchy(boolean show) {
        this.showHierarchy = show;
        return this;
    }

    public Inspector setShowHighlight(boolean show) {
        this.showHighlight = show;
        return this;
    }

    public Inspector setIncludeRoot(boolean include) {
        this.includeRoot = include;
        return this;
    }

    public Inspector setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        return this;
    }

    // endregion

    // region lifecycle

    @Override
    public void tick() {
        super.tick();
        if (trackMouse && lifecycle().mounted()) {
            var pos = ScreenUtil.getMousePos();
            lastMouseX = pos.x;
            lastMouseY = pos.y;
        }
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        if (!DebugConfig.enableDebug()) return;

        Widget currentTarget = resolveTarget();
        if (currentTarget != lastRenderedTarget) {
            scrollOffset = 0;
            maxScrollOffset = 0;
            lastRenderedTarget = currentTarget;
        }

        canvas.fill(0, 0, width(), height(), bg);
        canvas.strokeRect(0, 0, width(), height(), border, 1);

        int contentY = renderTitleBar(canvas, currentTarget);
        if (displayMode == DisplayMode.minimized) return;

        if (currentTarget == null) {
            canvas.drawString(
                    trackMouse ? "Hover over a widget..." : "No target set", padding, contentY + 2, dim, false);
            return;
        }

        if (showHighlight) renderHighlight(canvas, currentTarget);

        switch (displayMode) {
            case compact -> renderCompact(canvas, currentTarget, contentY);
            case full -> renderFull(canvas, currentTarget, contentY);
        }
    }

    private @Nullable Widget resolveTarget() {
        if (targetSupplier != null) return targetSupplier.get();
        if (target != null) return target;
        if (!trackMouse || !lifecycle().mounted()) return null;

        Scene scene = scene();
        Widget hit = scene.hitTest(lastMouseX, lastMouseY);
        if (hit == null || hit == this) return null;
        if (!includeRoot && hit == scene.root()) return null;
        return hit;
    }

    private int renderTitleBar(SceneCanvas canvas, @Nullable Widget target) {
        canvas.fill(0, 0, width(), titleBarHeight, titleBg);
        Font font = Minecraft.getInstance().font;
        int x = padding, y = 2;

        String modeIcon =
                switch (displayMode) {
                    case minimized -> "[-]";
                    case compact -> "[=]";
                    case full -> "[+]";
                };
        canvas.drawString(modeIcon, x, y, text, false);
        x += font.width(modeIcon) + 4;

        canvas.drawString("(I)", x, y, dim, false);
        x += font.width("(I)") + 4;

        String title = target != null ? target.inspectionTypeName() : "Inspector";
        if (target != null && target.key() != null) title += "[" + target.key() + "]";
        int maxW = width() - x - padding;
        if (font.width(title) > maxW) title = truncate(title, font, maxW);
        canvas.drawString(title, x, y, categoryColor, false);

        canvas.line(0, titleBarHeight, width(), titleBarHeight, 1f, border);
        return titleBarHeight + 2;
    }

    private void renderHighlight(SceneCanvas canvas, Widget target) {
        FloatPos scenePos = target.localToScene(0, 0);
        FloatPos local = sceneToLocal(scenePos.x, scenePos.y);
        int rx = (int) local.x, ry = (int) local.y;
        int rw = target.width(), rh = target.height();

        canvas.fill(rx, ry, rw, rh, hlFill);
        canvas.fill(rx, ry, rw, 1, hlBorder);
        canvas.fill(rx, ry + rh - 1, rw, 1, hlBorder);
        canvas.fill(rx, ry, 1, rh, hlBorder);
        canvas.fill(rx + rw - 1, ry, 1, rh, hlBorder);
    }

    private void renderCompact(SceneCanvas canvas, Widget target, int startY) {
        var collector = InspectionInfoCollector.from(target);
        Font font = Minecraft.getInstance().font;
        int x = padding, y = startY;

        var pos = target.pos();
        var size = target.size();
        canvas.drawString(
                String.format("pos(%d,%d) size(%d,%d)", pos.x(), pos.y(), size.width(), size.height()),
                x,
                y,
                value,
                false);
        y += lineHeight;

        int maxLines = (height() - y - padding) / lineHeight;
        int lineCount = 0;

        for (String category : collector.getCategories().toSortedList()) {
            if (lineCount >= maxLines) break;
            var props = collector.getByCategory(category).select(InspectionProperty::isNonDefault);
            if (props.isEmpty()) continue;

            canvas.drawString(category + ":", x, y, categoryColor, false);
            y += lineHeight;
            lineCount++;

            for (var prop : props) {
                if (lineCount >= maxLines) break;
                String text = "  " + prop.name() + ": " + prop.value();
                if (font.width(text) > width() - padding * 2) {
                    text = truncate(text, font, width() - padding * 2);
                }
                canvas.drawString(text, x, y, value, false);
                y += lineHeight;
                lineCount++;
            }
        }
    }

    private void renderFull(SceneCanvas canvas, Widget target, int startY) {
        var collector = InspectionInfoCollector.from(target);
        Font font = Minecraft.getInstance().font;
        int x = padding;
        int y = startY - scrollOffset;
        int maxY = height() - padding - scrollbarArea;
        int minY = startY;
        int totalH = 0;

        if (showHierarchy && target.lifecycle().mounted()) {
            int endY = renderHierarchy(canvas, target, x, y, maxY, minY, font);
            totalH += endY - y;
            y = endY;
        }

        if (y >= minY && y <= maxY) canvas.line(x, y, x + width() - padding * 2, y, 1f, border);
        y += 4;
        totalH += 4;

        for (String category : collector.getCategories().toSortedList()) {
            MutableList<InspectionProperty> props = showAllProperties
                    ? collector.getByCategory(category)
                    : collector.getByCategory(category).select(InspectionProperty::isNonDefault);
            if (props.isEmpty()) continue;

            if (y >= minY && y <= maxY) {
                canvas.drawString(category + ":", x + categoryIndent, y, categoryColor, false);
            }
            y += lineHeight;
            totalH += lineHeight;

            for (var prop : props) {
                if (y >= minY - lineHeight && y <= maxY) {
                    String label = prop.name() + ": ";
                    canvas.drawString(label, x + propertyIndent, y, text, false);
                    int valueX = x + propertyIndent + font.width(label);
                    int color = prop.isNonDefault() ? value : dim;
                    String value = prop.value();
                    int maxW = width() - valueX - padding;
                    if (font.width(value) > maxW) value = truncate(value, font, maxW);
                    canvas.drawString(value, valueX, y, color, false);
                }
                y += lineHeight;
                totalH += lineHeight;
            }
            y += 2;
            totalH += 2;
        }

        int visibleH = maxY - startY;
        maxScrollOffset = Math.max(0, totalH - visibleH);
        if (maxScrollOffset > 0) renderScrollbar(canvas, startY, maxY, totalH);
    }

    private int renderHierarchy(SceneCanvas canvas, Widget target, int x, int y, int maxY, int minY, Font font) {
        List<Widget> ancestors = new ArrayList<>();
        for (Widget w = target; w != null; w = w.parent()) ancestors.addFirst(w);

        int maxX = x + width() - padding * 2;
        List<String> segments = new ArrayList<>(ancestors.size());
        for (Widget w : ancestors) {
            String name = w.inspectionTypeName();
            if (w.key() != null) name += "[" + w.key() + "]";
            segments.add(name);
        }

        int cx = x, cy = y;
        for (int i = 0; i < segments.size(); i++) {
            if (cy > maxY) break;
            String segment = segments.get(i);
            boolean last = i == segments.size() - 1;
            int color = last ? hlBorder : dim;

            String text = i == 0 ? segment : "/" + segment;
            int tw = font.width(text);

            if (cx + tw > maxX && cx > x) {
                cy += lineHeight;
                cx = x + 4;
                if (cy > maxY) {
                    if (cy >= minY) canvas.drawString("...", cx, cy, dim, false);
                    break;
                }
                text = segment;
                tw = font.width(text);
            }

            if (cy >= minY) canvas.drawString(text, cx, cy, color, last);
            cx += tw;
        }

        return cy + lineHeight + 2;
    }

    private void renderScrollbar(SceneCanvas canvas, int top, int bottom, int totalHeight) {
        int barX = width() - 3;
        int trackTop = top + padding;
        int trackH = bottom - padding - trackTop;
        if (trackH <= 0) return;

        canvas.fill(barX, trackTop, 2, trackH, 0x33FFFFFF);
        float ratio = (float) (bottom - top) / totalHeight;
        int thumbH = Math.max(4, (int) (trackH * ratio));
        float pct = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int thumbY = trackTop + (int) ((trackH - thumbH) * pct);
        canvas.fill(barX, thumbY, 2, thumbH, 0xAAFFFFFF);
    }

    private static String truncate(String text, Font font, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "...") > maxWidth) end--;
        return text.substring(0, end) + "...";
    }

    // endregion

    // region input

    private void registerInputHandlers() {
        events().register(InputEvents.onKeyPressed, (input, context) -> {
            if (input.isKey(InputConstants.KEY_I) && !input.isCtrlDown() && !input.isAltDown()) {
                cycleDisplayMode();
                context.consume();
                return EventDispatch.consumed;
            }
            return displayMode == DisplayMode.full ? handleScrollKey(input, context) : EventDispatch.pass;
        });

        events().register(InputEvents.onMouseClicked, (input, context) -> {
            if (input.key().getType() == InputConstants.Type.MOUSE
                    && input.key().getValue() == 0
                    && input.mouseY() < titleBarHeight) {
                cycleDisplayMode();
                context.consume();
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });
    }

    private EventDispatch handleScrollKey(InputContext input, BubbleContext context) {
        int amount = 0;
        if (input.isKey(InputConstants.KEY_UP)) amount = -lineHeight;
        else if (input.isKey(InputConstants.KEY_DOWN)) amount = lineHeight;
        else if (input.isKey(InputConstants.KEY_PAGEUP)) amount = -(height() - titleBarHeight - padding * 2);
        else if (input.isKey(InputConstants.KEY_PAGEDOWN)) amount = height() - titleBarHeight - padding * 2;
        else if (input.isKey(InputConstants.KEY_HOME)) {
            scrollOffset = 0;
            context.consume();
            return EventDispatch.consumed;
        } else if (input.isKey(InputConstants.KEY_END)) {
            scrollOffset = maxScrollOffset;
            context.consume();
            return EventDispatch.consumed;
        }

        if (amount != 0) {
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + amount));
            context.consume();
            return EventDispatch.consumed;
        }
        return EventDispatch.pass;
    }

    private void cycleDisplayMode() {
        DisplayMode prev = displayMode;
        displayMode = switch (displayMode) {
            case minimized -> DisplayMode.compact;
            case compact -> DisplayMode.full;
            case full -> DisplayMode.minimized;
        };
        switch (displayMode) {
            case minimized -> setSize(width(), titleBarHeight + 4);
            case compact -> setSize(compactW, compactH);
            case full -> setSize(fullW, fullH);
        }
        scrollOffset = 0;
        maxScrollOffset = 0;
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("trackMouse", trackMouse, false, InspectionProperty.categoryState);
        collector.addWithDefault("includeRoot", includeRoot, false, InspectionProperty.categoryState);
        collector.addWithDefault("showAllProperties", showAllProperties, false, InspectionProperty.categoryState);
        collector.addWithDefault("showHierarchy", showHierarchy, true, InspectionProperty.categoryState);
        collector.addWithDefault("showHighlight", showHighlight, true, InspectionProperty.categoryState);
        collector.add("displayMode", displayMode, InspectionProperty.categoryState);
        collector.add("hasTarget", resolveTarget() != null, InspectionProperty.categoryState);
    }

    // endregion
}
