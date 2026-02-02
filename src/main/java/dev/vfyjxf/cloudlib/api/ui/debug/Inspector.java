package dev.vfyjxf.cloudlib.api.ui.debug;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.debug.DebugConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * An inspector widget component that can be integrated into the widget system.
 * <p>
 * This widget displays inspection information for a target widget, including:
 * <ul>
 *   <li>Widget type and ID</li>
 *   <li>Widget hierarchy path</li>
 *   <li>Properties organized by category</li>
 *   <li>Visual highlights for the target widget</li>
 * </ul>
 * <p>
 * Usage modes:
 * <ul>
 *   <li><b>Manual target</b>: Set a specific widget via {@link #setTarget(Widget)}</li>
 *   <li><b>Mouse tracking</b>: Automatically track widget under mouse via {@link #setTrackMouse(boolean)}</li>
 *   <li><b>Root tracking</b>: Track widgets within a root via {@link #setTrackRoot(Widget)}</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Create inspector with mouse tracking
 * Inspector inspector = Inspector.create()
 *     .setTrackRoot(rootWidget)
 *     .setTrackMouse(true);
 * panel.addChild(inspector);
 * }</pre>
 */
public class Inspector extends Widget {

    //region configuration
    private boolean showAllProperties = false;
    private boolean showHierarchy = true;
    private boolean showHighlight = true;
    private boolean trackMouse = false;
    private boolean compactMode = false;
    //endregion

    //region state
    private @Nullable Widget target;
    private @Nullable Widget trackRoot;
    private @Nullable Supplier<Widget> targetSupplier;
    private double lastMouseX, lastMouseY;
    //endregion

    //region colors
    private int backgroundColor = 0xCC000000;
    private int borderColor = 0xFF444444;
    private int textColor = 0xFFFFFFFF;
    private int categoryColor = 0xFF88FFFF;
    private int valueColor = 0xFFAAFFAA;
    private int defaultValueColor = 0xFF888888;
    private int highlightColor = 0x8000FF00;
    private int highlightBorderColor = 0xFF00FF00;
    //endregion

    //region layout
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int CATEGORY_INDENT = 2;
    private static final int PROPERTY_INDENT = 8;
    //endregion

    /** Creates a new inspector. */
    public static Inspector create() {
        return new Inspector();
    }

    /** Creates an inspector targeting a specific widget. */
    public static Inspector of(Widget target) {
        return new Inspector().setTarget(target);
    }

    /** Creates an inspector with a dynamic target supplier. */
    public static Inspector of(Supplier<Widget> targetSupplier) {
        return new Inspector().setTargetSupplier(targetSupplier);
    }

    private Inspector() {
        // Default size
        setSize(200, 150);
    }

    //region target configuration

    /** Sets the target widget to display inspection info for. */
    public Inspector setTarget(@Nullable Widget target) {
        this.target = target;
        this.targetSupplier = null;
        return this;
    }

    /** Gets the current target widget. */
    public @Nullable Widget getTarget() {
        if (targetSupplier != null) {
            return targetSupplier.get();
        }
        return target;
    }

    /** Sets a dynamic target supplier. */
    public Inspector setTargetSupplier(@Nullable Supplier<Widget> supplier) {
        this.targetSupplier = supplier;
        this.target = null;
        return this;
    }

    /**
     * Sets the root widget to track mouse within.
     * When trackMouse is enabled, this widget will display inspection info
     * for whatever widget the mouse is hovering over within this root.
     */
    public Inspector setTrackRoot(@Nullable Widget root) {
        this.trackRoot = root;
        return this;
    }

    /**
     * Enables or disables mouse tracking mode.
     * When enabled, the target is automatically updated based on mouse position.
     */
    public Inspector setTrackMouse(boolean trackMouse) {
        this.trackMouse = trackMouse;
        return this;
    }

    //endregion

    //region display configuration

    /** Sets whether to show all properties or only non-default ones. */
    public Inspector setShowAllProperties(boolean showAll) {
        this.showAllProperties = showAll;
        return this;
    }

    /** Sets whether to show the widget hierarchy path. */
    public Inspector setShowHierarchy(boolean show) {
        this.showHierarchy = show;
        return this;
    }

    /** Sets whether to highlight the target widget. */
    public Inspector setShowHighlight(boolean show) {
        this.showHighlight = show;
        return this;
    }

    /** Sets compact mode which shows minimal information. */
    public Inspector setCompactMode(boolean compact) {
        this.compactMode = compact;
        return this;
    }

    //endregion

    //region color configuration

    public Inspector setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public Inspector setBorderColor(int color) {
        this.borderColor = color;
        return this;
    }

    public Inspector setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public Inspector setHighlightColor(int color) {
        this.highlightColor = color;
        return this;
    }

    public Inspector setHighlightBorderColor(int color) {
        this.highlightBorderColor = color;
        return this;
    }

    //endregion

    //region lifecycle

    @Override
    public void tick() {
        super.tick();
        // Update mouse position for tracking
        if (trackMouse && lifecycle().mounted()) {
            var mousePos = getMousePosition();
            lastMouseX = mousePos[0];
            lastMouseY = mousePos[1];
        }
    }

    private double[] getMousePosition() {
        var window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() / scale;
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() / scale;
        return new double[]{mouseX, mouseY};
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        if (!DebugConfig.enableDebug()) {
            return;
        }

        // Resolve target
        Widget currentTarget = resolveTarget();

        // Render background
        canvas.fill(0, 0, width(), height(), backgroundColor);
        canvas.border(0, 0, width(), height(), borderColor, 1);

        // Always render screen info first
        int contentY = renderScreenInfo(canvas);

        if (currentTarget == null) {
            renderNoTarget(canvas, contentY);
            return;
        }

        // Render highlight on target (outside our bounds)
        if (showHighlight) {
            renderTargetHighlight(canvas, currentTarget);
        }

        // Render inspection info
        if (compactMode) {
            renderCompact(canvas, currentTarget, contentY);
        } else {
            renderDetailed(canvas, currentTarget, contentY);
        }
    }

    private @Nullable Widget resolveTarget() {
        // Priority: supplier > manual target > mouse tracking
        if (targetSupplier != null) {
            return targetSupplier.get();
        }
        if (target != null) {
            return target;
        }
        if (trackMouse && trackRoot != null) {
            return WidgetTree.hitTest(trackRoot, lastMouseX, lastMouseY);
        }
        return null;
    }

    /**
     * Renders screen information (size, mouse position).
     *
     * @return the Y position after rendering
     */
    private int renderScreenInfo(SceneCanvas canvas) {
        int x = PADDING;
        int y = PADDING;

        var window = Minecraft.getInstance().getWindow();
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();
        double[] mousePos = getMousePosition();
        String screenInfo = String.format("Screen %dx%d  Mouse %.0f,%.0f", screenW, screenH, mousePos[0], mousePos[1]);
        canvas.drawString(screenInfo, x, y, defaultValueColor, false);
        y += LINE_HEIGHT;

        // Separator
        canvas.hLine(x, x + width() - PADDING * 2, y, borderColor);
        y += 3;

        return y;
    }

    private void renderNoTarget(SceneCanvas canvas, int startY) {
        Font font = Minecraft.getInstance().font;
        String msg = trackMouse ? "Hover over a widget..." : "No target set";
        int textWidth = font.width(msg);
        int x = (width() - textWidth) / 2;
        int y = startY + (height() - startY - font.lineHeight) / 2;
        canvas.drawString(msg, x, y, defaultValueColor, false);
    }

    private void renderTargetHighlight(SceneCanvas canvas, Widget target) {
        // We need to render in absolute coordinates
        // Calculate offset from this widget to target
        var targetBounds = target.absoluteBounds();
        var ourBounds = absoluteBounds();

        int relX = targetBounds.x() - ourBounds.x();
        int relY = targetBounds.y() - ourBounds.y();

        // Use batch to render outside our normal bounds
        canvas.flushBatch();
        canvas.batch(() -> {
            var graphics = canvas.graphics();
            graphics.fill(relX, relY, relX + targetBounds.width(), relY + targetBounds.height(), highlightColor);
            // Border
            graphics.fill(relX, relY, relX + targetBounds.width(), relY + 1, highlightBorderColor);
            graphics.fill(relX, relY + targetBounds.height() - 1, relX + targetBounds.width(), relY + targetBounds.height(), highlightBorderColor);
            graphics.fill(relX, relY, relX + 1, relY + targetBounds.height(), highlightBorderColor);
            graphics.fill(relX + targetBounds.width() - 1, relY, relX + targetBounds.width(), relY + targetBounds.height(), highlightBorderColor);
        });
    }

    private void renderCompact(SceneCanvas canvas, Widget target, int startY) {
        InspectionInfoCollector collector = InspectionInfoCollector.from(target);
        Font font = Minecraft.getInstance().font;

        int x = PADDING;
        int y = startY;

        // Type and ID
        String header = collector.getWidgetType();
        if (collector.getWidgetId() != null) {
            header += "[" + collector.getWidgetId() + "]";
        }
        canvas.drawString(header, x, y, textColor, true);
        y += LINE_HEIGHT;

        // Position and size on same line
        var pos = target.pos();
        var size = target.size();
        String posSize = String.format("pos(%d,%d) size(%d,%d)", pos.x(), pos.y(), size.width(), size.height());
        canvas.drawString(posSize, x, y, valueColor, false);
        y += LINE_HEIGHT;

        // Non-default properties summary
        MutableList<InspectionProperty> nonDefault = collector.getNonDefaultProperties()
                                                         .reject(p -> p.name().equals("position") || p.name().equals("size") || p.name().equals("key") || p.name().equals("lifecycle"));
        if (!nonDefault.isEmpty()) {
            String props = nonDefault.collect(p -> p.name() + "=" + p.formattedValue()).makeString(", ");
            if (props.length() > 40) {
                props = props.substring(0, 37) + "...";
            }
            canvas.drawString(props, x, y, defaultValueColor, false);
        }
    }

    private void renderDetailed(SceneCanvas canvas, Widget target, int startY) {
        InspectionInfoCollector collector = InspectionInfoCollector.from(target);
        Font font = Minecraft.getInstance().font;

        int x = PADDING;
        int y = startY;
        int maxY = height() - PADDING - LINE_HEIGHT;

        // Header
        String header = collector.getWidgetType();
        if (collector.getWidgetId() != null) {
            header += " [" + collector.getWidgetId() + "]";
        }
        canvas.drawString(header, x, y, textColor, true);
        y += LINE_HEIGHT + 2;

        // Hierarchy display
        if (showHierarchy && target.lifecycle().mounted()) {
            y = renderHierarchy(canvas, target, x, y, maxY, font);
        }

        // Separator
        canvas.hLine(x, x + width() - PADDING * 2, y, borderColor);
        y += 4;

        // Properties by category
        for (String category : collector.getCategories().toSortedList()) {
            if (y > maxY) break;

            MutableList<InspectionProperty> props = showAllProperties
                                               ? collector.getByCategory(category)
                                               : collector.getByCategory(category).select(InspectionProperty::isNonDefault);

            if (props.isEmpty()) continue;

            // Category header
            canvas.drawString(category + ":", x + CATEGORY_INDENT, y, categoryColor, false);
            y += LINE_HEIGHT;

            // Properties
            for (InspectionProperty prop : props) {
                if (y > maxY) {
                    canvas.drawString("...", x + PROPERTY_INDENT, y, defaultValueColor, false);
                    break;
                }

                String propText = prop.name() + ": ";
                canvas.drawString(propText, x + PROPERTY_INDENT, y, textColor, false);

                int valueX = x + PROPERTY_INDENT + font.width(propText);
                int valueColorToUse = prop.isNonDefault() ? valueColor : defaultValueColor;

                String valueText = prop.formattedValue();
                int maxValueWidth = width() - valueX - PADDING;
                if (font.width(valueText) > maxValueWidth) {
                    valueText = truncateText(valueText, font, maxValueWidth);
                }
                canvas.drawString(valueText, valueX, y, valueColorToUse, false);
                y += LINE_HEIGHT;
            }
            y += 2;
        }
    }

    /**
     * Renders the hierarchy path.
     * <pre>
     * Root/Parent/Child
     * </pre>
     *
     * @return the new Y position after rendering
     */
    private int renderHierarchy(SceneCanvas canvas, Widget target, int x, int y, int maxY, Font font) {
        // Build ancestor list (from root to target)
        java.util.List<Widget> ancestors = new java.util.ArrayList<>();
        Widget current = target;
        while (current != null) {
            ancestors.add(0, current);
            current = current.parent();
        }

        int availableWidth = width() - PADDING * 2;

        // Build path segments
        java.util.List<String> segments = new java.util.ArrayList<>();
        for (Widget w : ancestors) {
            String name = w.inspectionTypeName();
            if (w.key() != null) {
                name += "[" + w.key() + "]";
            }
            segments.add(name);
        }

        // Render with wrapping if needed
        int currentX = x;
        int currentY = y;
        int maxX = x + availableWidth;

        for (int i = 0; i < segments.size(); i++) {
            if (currentY > maxY) break;

            String segment = segments.get(i);
            boolean isTarget = (i == segments.size() - 1);
            int color = isTarget ? highlightBorderColor : defaultValueColor;

            // Add separator before non-first segments
            String text = i == 0 ? segment : "/" + segment;
            int textWidth = font.width(text);

            // Check if we need to wrap
            if (currentX + textWidth > maxX && currentX > x) {
                currentY += LINE_HEIGHT;
                currentX = x + 4;
                if (currentY > maxY) {
                    canvas.drawString("...", currentX, currentY, defaultValueColor, false);
                    break;
                }
                text = segment;
                textWidth = font.width(text);
            }

            canvas.drawString(text, currentX, currentY, color, isTarget);
            currentX += textWidth;
        }

        return currentY + LINE_HEIGHT + 2;
    }

    private String truncateText(String text, Font font, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;

        String ellipsis = "...";
        int end = text.length();

        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }

        return text.substring(0, end) + ellipsis;
    }

    //endregion

    //region inspection info

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("trackMouse", trackMouse, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("showAllProperties", showAllProperties, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("showHierarchy", showHierarchy, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("showHighlight", showHighlight, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("compactMode", compactMode, false, InspectionProperty.CATEGORY_STATE);
        collector.add("hasTarget", getTarget() != null, InspectionProperty.CATEGORY_STATE);
    }

    //endregion
}
