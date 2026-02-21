package dev.vfyjxf.cloudlib.api.ui.debug;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvents;
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
 * Display modes:
 * <ul>
 *   <li><b>MINIMIZED</b>: Only a small title bar (20px height)</li>
 *   <li><b>COMPACT</b>: Essential info only (auto-sized)</li>
 *   <li><b>FULL</b>: All details with keyboard scrolling (default 300x400)</li>
 * </ul>
 * <p>
 * Keyboard Controls:
 * <ul>
 *   <li><b>I</b>: Cycle through display modes (MINIMIZED → COMPACT → FULL)</li>
 *   <li><b>↑/↓</b>: Scroll content by one line (in FULL mode)</li>
 *   <li><b>PageUp/PageDown</b>: Scroll by visible page (in FULL mode)</li>
 *   <li><b>Home/End</b>: Jump to top/bottom of content (in FULL mode)</li>
 * </ul>
 * <p>
 * Mouse Controls:
 * <ul>
 *   <li><b>Click title bar</b>: Toggle between display modes (MINIMIZED → COMPACT → FULL)</li>
 * </ul>
 * <p>
 * TODO: Add window dragging and resizing (requires cursor API implementation)
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
 * scene.addToLayer(SceneLayer.debug, inspector);
 * 
 * // Use arrow keys to scroll when content overflows
 * }</pre>
 */
public class Inspector extends Widget {

    /** Display mode enumeration */
    public enum DisplayMode {
        MINIMIZED,  // Only title bar
        COMPACT,    // Essential info
        FULL        // All details with scrolling
    }

    //region configuration
    private boolean showAllProperties = false;
    private boolean showHierarchy = true;
    private boolean showHighlight = true;
    private boolean trackMouse = false;
    private DisplayMode displayMode = DisplayMode.COMPACT;
    //endregion

    //region state
    private @Nullable Widget target;
    private @Nullable Widget trackRoot;
    private @Nullable Supplier<Widget> targetSupplier;
    private double lastMouseX, lastMouseY;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private @Nullable Widget lastRenderedTarget = null;
    //endregion

    //region colors
    private int backgroundColor = 0xCC000000;
    private int borderColor = 0xFF444444;
    private int textColor = 0xFFFFFFFF;
    private int categoryColor = 0xFF88FFFF;
    private int valueColor = 0xFFAAFFAA;
    private int defaultValueColor = 0xFF888888;
    private int highlightColor = 0xCC00FF00;
    private int highlightBorderColor = 0xFF00FF00;
    private int titleBarColor = 0xFF333333;
    private int resizeHandleColor = 0xFF666666;
    //endregion

    //region layout
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int CATEGORY_INDENT = 2;
    private static final int PROPERTY_INDENT = 8;
    private static final int TITLE_BAR_HEIGHT = 12;
    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int MIN_WIDTH = 150;
    private static final int MIN_HEIGHT = 50;
    private static final int DEFAULT_COMPACT_WIDTH = 250;
    private static final int DEFAULT_COMPACT_HEIGHT = 100;
    private static final int DEFAULT_FULL_WIDTH = 300;
    private static final int DEFAULT_FULL_HEIGHT = 400;
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
        // Default size for compact mode
        setSize(DEFAULT_COMPACT_WIDTH, DEFAULT_COMPACT_HEIGHT);
        
        // Enable focus and keyboard input
        setFocusable(true);
        
        // Register keyboard controls
        // Use 'I' key (Inspector) to avoid F3 conflict with Minecraft debug screen
        events().register(InputEvents.onKeyPressed, (input, context) -> {
            // 'I' key: Toggle display mode (avoiding F3 which conflicts with Minecraft)
            if (input.isKey(InputConstants.KEY_I) && !input.isCtrlDown() && !input.isAltDown()) {
                cycleDisplayMode();
                context.consume();
                return EventDispatch.consumed;
            }
            
            // Keyboard scrolling in FULL mode
            if (displayMode == DisplayMode.FULL) {
                int scrollAmount = 0;
                
                // Arrow keys: small scroll (1 line)
                if (input.isKey(InputConstants.KEY_UP)) {
                    scrollAmount = -LINE_HEIGHT;
                } else if (input.isKey(InputConstants.KEY_DOWN)) {
                    scrollAmount = LINE_HEIGHT;
                }
                // Page Up/Down: large scroll (visible area)
                else if (input.isKey(InputConstants.KEY_PAGEUP)) {
                    scrollAmount = -(height() - TITLE_BAR_HEIGHT - PADDING * 2);
                } else if (input.isKey(InputConstants.KEY_PAGEDOWN)) {
                    scrollAmount = height() - TITLE_BAR_HEIGHT - PADDING * 2;
                }
                // Home/End: scroll to top/bottom
                else if (input.isKey(InputConstants.KEY_HOME)) {
                    scrollOffset = 0;
                    context.consume();
                    return EventDispatch.consumed;
                } else if (input.isKey(InputConstants.KEY_END)) {
                    scrollOffset = maxScrollOffset;
                    context.consume();
                    return EventDispatch.consumed;
                }
                
                if (scrollAmount != 0) {
                    scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + scrollAmount));
                    context.consume();
                    return EventDispatch.consumed;
                }
            }
            
            return EventDispatch.pass;
        });
        
        // Register mouse click on title bar to toggle mode
        events().register(InputEvents.onMouseClicked, (input, context) -> {
            double mouseY = input.mouseY();
            
            if (input.key().getType() == InputConstants.Type.MOUSE && input.key().getValue() == 0) { // Left click
                if (mouseY < TITLE_BAR_HEIGHT) {
                    cycleDisplayMode();
                    context.consume();
                    return EventDispatch.consumed;
                }
            }
            return EventDispatch.pass;
        });
        
        // Remove mouse scroll - use keyboard instead to avoid conflicts
        // Keyboard scrolling is more practical since mouse is typically over the target widget
    }

    private void cycleDisplayMode() {
        DisplayMode oldMode = displayMode;
        displayMode = switch (displayMode) {
            case MINIMIZED -> DisplayMode.COMPACT;
            case COMPACT -> DisplayMode.FULL;
            case FULL -> DisplayMode.MINIMIZED;
        };
        
        // Adjust size based on new mode
        switch (displayMode) {
            case MINIMIZED -> setSize(width(), TITLE_BAR_HEIGHT + 4);
            case COMPACT -> {
                if (oldMode == DisplayMode.MINIMIZED) {
                    setSize(DEFAULT_COMPACT_WIDTH, DEFAULT_COMPACT_HEIGHT);
                }
            }
            case FULL -> {
                if (oldMode != DisplayMode.FULL) {
                    setSize(DEFAULT_FULL_WIDTH, DEFAULT_FULL_HEIGHT);
                }
            }
        }
        
        scrollOffset = 0;
        maxScrollOffset = 0;
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

    /** Sets the display mode. */
    public Inspector setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        return this;
    }
    
    /** Gets the current display mode. */
    public DisplayMode getDisplayMode() {
        return displayMode;
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

        // Reset scroll when target changes
        if (currentTarget != lastRenderedTarget) {
            scrollOffset = 0;
            maxScrollOffset = 0;
            lastRenderedTarget = currentTarget;
        }

        // Render background and border
        canvas.fill(0, 0, width(), height(), backgroundColor);
        canvas.border(0, 0, width(), height(), borderColor, 1);

        // Render title bar (always visible)
        int titleBarY = renderTitleBar(canvas, currentTarget);
        
        // Stop here if minimized
        if (displayMode == DisplayMode.MINIMIZED) {
            return;
        }

        if (currentTarget == null) {
            renderNoTarget(canvas, titleBarY);
            return;
        }

        // Render highlight on target (outside our bounds)
        if (showHighlight) {
            renderTargetHighlight(canvas, currentTarget);
        }

        // Render inspection info based on display mode
        switch (displayMode) {
            case COMPACT -> renderCompact(canvas, currentTarget, titleBarY);
            case FULL -> renderFull(canvas, currentTarget, titleBarY);
        }
    }
    
    /**
     * Renders the title bar with mode indicator and target info.
     * @return the Y position after the title bar
     */
    private int renderTitleBar(SceneCanvas canvas, @Nullable Widget target) {
        // Title bar background
        canvas.fill(0, 0, width(), TITLE_BAR_HEIGHT, titleBarColor);
        
        Font font = Minecraft.getInstance().font;
        int x = PADDING;
        int y = 2;
        
        // Mode indicator
        String modeIcon = switch (displayMode) {
            case MINIMIZED -> "[-]";
            case COMPACT -> "[=]";
            case FULL -> "[+]";
        };
        canvas.drawString(modeIcon, x, y, textColor, false);
        x += font.width(modeIcon) + 4;
        
        // Add hint for mode toggle
        String toggleHint = "(I)";
        canvas.drawString(toggleHint, x, y, 0xFF666666, false);
        x += font.width(toggleHint) + 4;
        
        // Target name or "Inspector"
        String title = target != null ? target.inspectionTypeName() : "Inspector";
        if (target != null && target.key() != null) {
            title += "[" + target.key() + "]";
        }
        
        // Truncate if too long
        int maxTitleWidth = width() - x - PADDING;
        if (font.width(title) > maxTitleWidth) {
            title = truncateText(title, font, maxTitleWidth);
        }
        canvas.drawString(title, x, y, categoryColor, false);
        
        // Separator line below title bar
        canvas.hLine(0, width(), TITLE_BAR_HEIGHT, borderColor);
        
        return TITLE_BAR_HEIGHT + 2;
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

    private void renderNoTarget(SceneCanvas canvas, int startY) {
        Font font = Minecraft.getInstance().font;
        String msg = trackMouse ? "Hover over a widget..." : "No target set";
        canvas.drawString(msg, PADDING, startY + 2, defaultValueColor, false);
    }

    private void renderTargetHighlight(SceneCanvas canvas, Widget target) {
        // We need to render in absolute coordinates
        // Calculate offset from this widget to target
        var targetBounds = target.absoluteBounds();
        var ourBounds = absoluteBounds();

        int relX = targetBounds.x() - ourBounds.x();
        int relY = targetBounds.y() - ourBounds.y();
        int relX2 = relX + targetBounds.width();
        int relY2 = relY + targetBounds.height();

        // Use canvas.fill() to avoid corrupting RenderSystem state
        // Fill highlight area
        canvas.fill(relX, relY, targetBounds.width(), targetBounds.height(), highlightColor);
        // Border - top
        canvas.fill(relX, relY, targetBounds.width(), 1, highlightBorderColor);
        // Border - bottom
        canvas.fill(relX, relY2 - 1, targetBounds.width(), 1, highlightBorderColor);
        // Border - left
        canvas.fill(relX, relY, 1, targetBounds.height(), highlightBorderColor);
        // Border - right
        canvas.fill(relX2 - 1, relY, 1, targetBounds.height(), highlightBorderColor);
    }

    private void renderCompact(SceneCanvas canvas, Widget target, int startY) {
        InspectionInfoCollector collector = InspectionInfoCollector.from(target);
        Font font = Minecraft.getInstance().font;

        int x = PADDING;
        int y = startY;

        // Position and size on same line
        var pos = target.pos();
        var size = target.size();
        String posSize = String.format("pos(%d,%d) size(%d,%d)", pos.x(), pos.y(), size.width(), size.height());
        canvas.drawString(posSize, x, y, valueColor, false);
        y += LINE_HEIGHT;
        
        // Group by category and show top categories
        var categories = collector.getCategories().toSortedList();
        int lineCount = 0;
        int maxLines = (height() - y - PADDING) / LINE_HEIGHT;
        
        for (String category : categories) {
            if (lineCount >= maxLines) break;
            
            var catProps = collector.getByCategory(category).select(InspectionProperty::isNonDefault);
            if (catProps.isEmpty()) continue;
            
            // Category name
            canvas.drawString(category + ":", x, y, categoryColor, false);
            y += LINE_HEIGHT;
            lineCount++;
            
            // Show first few properties from this category
            for (var prop : catProps) {
                if (lineCount >= maxLines) break;
                
                String propText = "  " + prop.name() + ": " + prop.formattedValue();
                if (font.width(propText) > width() - PADDING * 2) {
                    propText = truncateText(propText, font, width() - PADDING * 2);
                }
                canvas.drawString(propText, x, y, valueColor, false);
                y += LINE_HEIGHT;
                lineCount++;
            }
        }
    }

    private void renderFull(SceneCanvas canvas, Widget target, int startY) {
        InspectionInfoCollector collector = InspectionInfoCollector.from(target);
        Font font = Minecraft.getInstance().font;

        int x = PADDING;
        int y = startY - scrollOffset; // Apply scroll offset
        int contentStartY = y;
        int maxY = height() - PADDING - (displayMode == DisplayMode.FULL ? RESIZE_HANDLE_SIZE : 0);
        int minY = startY;

        // Track total content height for scrolling
        int totalContentHeight = 0;

        // Hierarchy display
        if (showHierarchy && target.lifecycle().mounted()) {
            int hierarchyEndY = renderHierarchy(canvas, target, x, y, maxY, minY, font);
            int hierarchyHeight = hierarchyEndY - y;
            y = hierarchyEndY;
            totalContentHeight += hierarchyHeight;
        }

        // Separator
        if (y >= minY && y <= maxY) {
            canvas.hLine(x, x + width() - PADDING * 2, y, borderColor);
        }
        y += 4;
        totalContentHeight += 4;

        // Properties by category
        for (String category : collector.getCategories().toSortedList()) {
            MutableList<InspectionProperty> props = showAllProperties
                                               ? collector.getByCategory(category)
                                               : collector.getByCategory(category).select(InspectionProperty::isNonDefault);

            if (props.isEmpty()) continue;

            // Category header
            if (y >= minY && y <= maxY) {
                canvas.drawString(category + ":", x + CATEGORY_INDENT, y, categoryColor, false);
            }
            y += LINE_HEIGHT;
            totalContentHeight += LINE_HEIGHT;

            // Properties
            for (InspectionProperty prop : props) {
                if (y >= minY - LINE_HEIGHT && y <= maxY) {
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
                }
                y += LINE_HEIGHT;
                totalContentHeight += LINE_HEIGHT;
            }
            y += 2;
            totalContentHeight += 2;
        }

        // Update max scroll offset
        int visibleHeight = maxY - startY;
        maxScrollOffset = Math.max(0, totalContentHeight - visibleHeight);
        
        // Render scroll indicator if content is scrollable
        if (maxScrollOffset > 0) {
            renderScrollbar(canvas, startY, maxY, totalContentHeight);
            renderScrollHint(canvas, startY, maxY);
        }
    }
    
    /**
     * Renders a hint for keyboard scrolling at the bottom.
     */
    private void renderScrollHint(SceneCanvas canvas, int contentTop, int contentBottom) {
        Font font = Minecraft.getInstance().font;
        String hint = scrollOffset < maxScrollOffset ? "↓ ↑ PgUp/Dn" : "↑ PgUp/Dn";
        int hintWidth = font.width(hint);
        int hintX = width() - hintWidth - PADDING - 6; // Leave space for scrollbar
        int hintY = height() - RESIZE_HANDLE_SIZE - LINE_HEIGHT - 2;
        
        // Draw semi-transparent background for hint
        canvas.fill(hintX - 2, hintY - 1, hintWidth + 4, LINE_HEIGHT + 1, 0x88000000);
        canvas.drawString(hint, hintX, hintY, defaultValueColor, false);
    }
    
    /**
     * Renders a scrollbar indicating the scroll position.
     */
    private void renderScrollbar(SceneCanvas canvas, int contentTop, int contentBottom, int totalContentHeight) {
        int scrollbarWidth = 2;
        int scrollbarX = width() - scrollbarWidth - 1;
        int trackTop = contentTop + PADDING;
        int trackBottom = contentBottom - PADDING;
        int trackHeight = trackBottom - trackTop;
        
        if (trackHeight <= 0) return;
        
        // Draw track (slot)
        canvas.fill(scrollbarX, trackTop, scrollbarWidth, trackHeight, 0x33FFFFFF);
        
        // Calculate thumb position and size
        float visibleRatio = (float) (contentBottom - contentTop) / totalContentHeight;
        int thumbHeight = Math.max(4, (int) (trackHeight * visibleRatio));
        float scrollPercentage = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * scrollPercentage);
        
        // Draw thumb (simple color block)
        canvas.fill(scrollbarX, thumbY, scrollbarWidth, thumbHeight, 0xAAFFFFFF);
    }

    /**
     * Renders the hierarchy path.
     * <pre>
     * Root/Parent/Child
     * </pre>
     *
     * @param minY minimum Y for clipping
     * @return the new Y position after rendering
     */
    private int renderHierarchy(SceneCanvas canvas, Widget target, int x, int y, int maxY, int minY, Font font) {
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
                    if (currentY >= minY) {
                        canvas.drawString("...", currentX, currentY, defaultValueColor, false);
                    }
                    break;
                }
                text = segment;
                textWidth = font.width(text);
            }

            if (currentY >= minY && currentY <= maxY) {
                canvas.drawString(text, currentX, currentY, color, isTarget);
            }
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
        collector.addWithDefault("trackMouse", trackMouse, false, InspectionProperty.categoryState);
        collector.addWithDefault("showAllProperties", showAllProperties, false, InspectionProperty.categoryState);
        collector.addWithDefault("showHierarchy", showHierarchy, true, InspectionProperty.categoryState);
        collector.addWithDefault("showHighlight", showHighlight, true, InspectionProperty.categoryState);
        collector.add("displayMode", displayMode.name(), InspectionProperty.categoryState);
        collector.add("hasTarget", getTarget() != null, InspectionProperty.categoryState);
        collector.add("scrollOffset", scrollOffset, InspectionProperty.categoryState);
        collector.add("maxScrollOffset", maxScrollOffset, InspectionProperty.categoryState);
    }

    //endregion
}
