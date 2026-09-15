package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.SpacerWidget;
import dev.vfyjxf.cloudlib.ui.Textures;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * DevTools window / dock panel root.
 */
final class DevToolsWindow extends WidgetGroup<Widget> {

    private static final int BUTTON_SIZE = 20;
    private static final int ICON_SIZE = 12;
    private static final int ICON_PAD = (BUTTON_SIZE - ICON_SIZE) / 2;
    private static final int HANDLE_SIZE = 5;

    private final DebugOverlayImpl overlay;
    private final SceneHost host;
    private final DockLayout dockLayout;
    private final WindowFrame windowFrame;
    private final DockMenu dockMenu;
    private final ScaleMenu scaleMenu;
    private final OverflowMenu overflowMenu;
    private final ToolBar toolBar;

    private final List<Tool> tools;
    private Tool pick;
    private Tool highlight;
    private Tool dock;
    private Tool zoom;
    private Tool scale;

    private final WidgetTreeView treeView;
    private final WidgetDetailsView detailsView;
    private final Splitter splitter;
    private final List<ResizeCorner> resizeCorners;
    private final DockResizeHandle dockResizeHandle;
    private final ZoomMenu zoomMenu;

    private float treeRatio = 0.45f;
    private boolean draggingWindow;
    private double grabDX;
    private double grabDY;
    private ResizeCorner.Corner resizingCorner;
    private boolean splitting;
    private boolean dockResizing;

    DevToolsWindow(DebugOverlayImpl overlay, SceneHost host) {
        this.overlay = overlay;
        this.host = host;
        this.dockLayout = new DockLayout(host.width(), host.height());

        useStyle(UIStyle.of(UIStyles.sizeFull()));

        treeView = new WidgetTreeView(overlay);
        detailsView = new WidgetDetailsView(overlay);
        splitter = new Splitter();
        dockResizeHandle = new DockResizeHandle();
        resizeCorners = new ArrayList<>();
        for (ResizeCorner.Corner corner : ResizeCorner.Corner.values()) {
            resizeCorners.add(new ResizeCorner(corner));
        }

        tools = createTools();
        overflowMenu = new OverflowMenu(tools);
        toolBar = new ToolBar(tools, overflowMenu);
        windowFrame = new WindowFrame();
        dockMenu = new DockMenu();
        scaleMenu = new ScaleMenu();
        zoomMenu = new ZoomMenu();

        dockMenu.setVisible(false);
        scaleMenu.setVisible(false);
        zoomMenu.setVisible(false);
        overflowMenu.setVisible(false);

        addWidget(windowFrame);
        addWidget(dockMenu);
        addWidget(scaleMenu);
        addWidget(zoomMenu);
        addWidget(overflowMenu);

        onMouseClick(this::onRootMouseClick, true);
        onMouseDragged(this::onRootMouseDragged);
        onMouseReleased(this::onRootMouseReleased);

        syncButtons();
        applyBounds();
    }

    //region api used by overlay

    DockLayout dockLayout() {
        return dockLayout;
    }

    boolean isOverPanel(double mouseX, double mouseY) {
        return windowFrame.isMouseOver(mouseX, mouseY)
                || (dockMenu.visible() && dockMenu.isMouseOver(mouseX, mouseY))
                || (scaleMenu.visible() && scaleMenu.isMouseOver(mouseX, mouseY))
                || (zoomMenu.visible() && zoomMenu.isMouseOver(mouseX, mouseY))
                || (overflowMenu.visible() && overflowMenu.isMouseOver(mouseX, mouseY));
    }

    void setSelected(@Nullable Widget widget) {
        treeView.setSelected(widget);
        detailsView.setTarget(widget);
    }

    void refreshTree() {
        treeView.refresh();
    }

    void syncButtons() {
        for (Tool tool : tools) tool.sync();
    }

    void clampToScreen() {
        dockLayout.setScreenSize(host.width(), host.height());
        applyBounds();
    }

    Insets contentInsets() {
        if (!dockLayout.mode().isDocked()) return Insets.zero;
        float scale = overlay.devToolsScale();
        float w = dockLayout.width() * scale;
        float h = dockLayout.height() * scale;
        return switch (dockLayout.mode()) {
            case DOCK_RIGHT -> new Insets(0, (int) w, 0, 0);
            case DOCK_LEFT -> new Insets(0, 0, 0, (int) w);
            case DOCK_TOP -> new Insets((int) h, 0, 0, 0);
            case DOCK_BOTTOM -> new Insets(0, 0, (int) h, 0);
            default -> Insets.zero;
        };
    }

    void updateScaleButton() {
        if (scale != null) scale.sync();
    }

    //endregion

    //region window layout

    private void setDockMode(DockMode mode) {
        dockLayout.setMode(mode);
        applyBounds();
        overlay.onDockChanged();
        dockMenu.setVisible(false);
    }

    private void applyBounds() {
        dockLayout.setScreenSize(host.width(), host.height());

        float x = dockLayout.x();
        float y = dockLayout.y();
        float w = dockLayout.width();
        float h = dockLayout.height();

        windowFrame.useStyle(UIStyle.of(
                UIStyles.positionAbsolute(),
                UIStyles.insetLeft(x),
                UIStyles.insetTop(y),
                UIStyles.sizeOf(w, h),
                UIStyles.flexColumn(),
                UIStyles.padding(10)
        ));

        treeView.useStyle(UIStyle.of(
                UIStyles.widthOf(TaffyDimension.percent(1f)),
                UIStyles.heightOf(TaffyDimension.percent(treeRatio)),
                UIStyles.minHeight(0),
                UIStyles.flexShrink(0),
                UIStyles.flexColumn()
        ));

        detailsView.useStyle(UIStyle.of(
                UIStyles.widthOf(TaffyDimension.percent(1f)),
                UIStyles.flexGrow(1),
                UIStyles.minHeight(0),
                UIStyles.flexColumn(),
                UIStyles.padding(4, 0, 0, 0),
                UIStyles.rowGap(2)
        ));

        for (ResizeCorner corner : resizeCorners) {
            corner.setVisible(dockLayout.mode() == DockMode.FLOAT);
        }
        dockResizeHandle.setVisible(dockLayout.mode().isDocked());
        dockResizeHandle.useStyle(dockResizeHandle.handleStyle());

        syncButtons();
    }

    private VisualTexture modeTexture(DockMode mode) {
        return switch (mode) {
            case FLOAT -> DevToolsTextures.DOCK_FLOAT;
            case DOCK_RIGHT -> DevToolsTextures.DOCK_RIGHT;
            case DOCK_LEFT -> DevToolsTextures.DOCK_LEFT;
            case DOCK_TOP -> DevToolsTextures.DOCK_TOP;
            case DOCK_BOTTOM -> DevToolsTextures.DOCK_BOTTOM;
        };
    }

    //endregion

    //region input

    private void beginWindowDrag(double mouseX, double mouseY) {
        draggingWindow = true;
        grabDX = mouseX - dockLayout.x();
        grabDY = mouseY - dockLayout.y();
    }

    private void endDrag() {
        draggingWindow = false;
        resizingCorner = null;
        splitting = false;
        dockResizing = false;
        dockLayout.endResize();
    }

    private EventDispatch onRootMouseClick(InputContext input, int clickCount, BubbleContext context) {
        double mx = input.mouseX(), my = input.mouseY();
        if (isOverOpenMenu(mx, my)) return EventDispatch.pass;
        if (toolBar.isOverMenuToggler(mx, my)) return EventDispatch.pass;
        closeAllMenus();
        return EventDispatch.pass;
    }

    private boolean isOverOpenMenu(double mx, double my) {
        return (dockMenu.visible() && dockMenu.isMouseOver(mx, my))
                || (scaleMenu.visible() && scaleMenu.isMouseOver(mx, my))
                || (zoomMenu.visible() && zoomMenu.isMouseOver(mx, my))
                || (overflowMenu.visible() && overflowMenu.isMouseOver(mx, my));
    }

    private void closeAllMenus() {
        dockMenu.setVisible(false);
        scaleMenu.setVisible(false);
        zoomMenu.setVisible(false);
        toolBar.closeOverflow();
    }

    private EventDispatch onRootMouseDragged(InputContext input, double dragX, double dragY, BubbleContext context) {
        double mx = input.mouseX(), my = input.mouseY();

        if (draggingWindow && dockLayout.mode() == DockMode.FLOAT) {
            float nx = (float) (mx - grabDX);
            float ny = (float) (my - grabDY);
            dockLayout.setWindowBounds(nx, ny, dockLayout.width(), dockLayout.height());
            applyBounds();
            context.consume();
            return EventDispatch.consumed;
        }

        if (resizingCorner != null && dockLayout.mode() == DockMode.FLOAT) {
            float left = dockLayout.x();
            float top = dockLayout.y();
            float right = left + dockLayout.width();
            float bottom = top + dockLayout.height();
            float screenW = host.width();
            float screenH = host.height();
            float newX = left, newY = top, newW = dockLayout.width(), newH = dockLayout.height();

            switch (resizingCorner) {
                case TOP_LEFT -> {
                    newX = (float) Math.max(0, Math.min(mx, right - DebugTheme.MIN_WIDTH));
                    newY = (float) Math.max(0, Math.min(my, bottom - DebugTheme.MIN_HEIGHT));
                    newW = right - newX;
                    newH = bottom - newY;
                }
                case TOP_RIGHT -> {
                    newY = (float) Math.max(0, Math.min(my, bottom - DebugTheme.MIN_HEIGHT));
                    newW = (float) Math.max(DebugTheme.MIN_WIDTH, Math.min(mx, screenW) - left);
                    newH = bottom - newY;
                }
                case BOTTOM_LEFT -> {
                    newX = (float) Math.max(0, Math.min(mx, right - DebugTheme.MIN_WIDTH));
                    newW = right - newX;
                    newH = (float) Math.max(DebugTheme.MIN_HEIGHT, Math.min(my, screenH) - top);
                }
                case BOTTOM_RIGHT -> {
                    newW = (float) Math.max(DebugTheme.MIN_WIDTH, Math.min(mx, screenW) - left);
                    newH = (float) Math.max(DebugTheme.MIN_HEIGHT, Math.min(my, screenH) - top);
                }
            }

            dockLayout.setWindowBounds(newX, newY, newW, newH);
            applyBounds();
            detailsView.refresh();
            context.consume();
            return EventDispatch.consumed;
        }

        if (splitting) {
            float total = treeView.height() + detailsView.height() + DebugTheme.SPLITTER_HEIGHT;
            if (total > 0) {
                float ratio = (float) (my - treeView.absolutePos().y()) / total;
                treeRatio = Math.max(0.15f, Math.min(0.85f, ratio));
                treeView.useStyle(UIStyle.of(
                        UIStyles.widthOf(TaffyDimension.percent(1f)),
                        UIStyles.heightOf(TaffyDimension.percent(treeRatio)),
                        UIStyles.minHeight(0),
                        UIStyles.flexShrink(0),
                        UIStyles.flexColumn()
                ));
            }
            context.consume();
            return EventDispatch.consumed;
        }

        if (dockResizing) {
            dockLayout.updateResize(mx, my);
            applyBounds();
            overlay.onDockChanged();
            context.consume();
            return EventDispatch.consumed;
        }

        return EventDispatch.pass;
    }

    private EventDispatch onRootMouseReleased(InputContext input, BubbleContext context) {
        endDrag();
        return EventDispatch.pass;
    }

    //endregion

    //region tools

    private List<Tool> createTools() {
        UIStyle base = UIStyle.of(UIStyles.sizeOf(BUTTON_SIZE, BUTTON_SIZE), UIStyles.flexShrink(0));
        List<Tool> list = new ArrayList<>();

        pick = new Tool("pick", DevToolsTextures.PICK,
                () -> overlay.setInspectMode(!overlay.inspectMode()),
                () -> overlay.inspectMode(), base,
                () -> {
                    ((IconButton) pick.main).sync();
                    ((IconButton) pick.overflow).sync();
                });

        highlight = new Tool("highlight", DevToolsTextures.HIGHLIGHT,
                () -> overlay.setHighlightEnabled(!overlay.highlightEnabled()),
                () -> overlay.highlightEnabled(), base,
                () -> {
                    ((IconButton) highlight.main).sync();
                    ((IconButton) highlight.overflow).sync();
                });

        Tool refresh = new Tool("refresh", DevToolsTextures.REFRESH,
                () -> {
                    treeView.refresh();
                    detailsView.refresh();
                },
                null, base, null);

        dock = new Tool("dock", DevToolsTextures.DOCK_FLOAT,
                this::toggleDockMenu,
                () -> dockMenu != null && dockMenu.visible(), base,
                () -> {
                    VisualTexture icon = modeTexture(dockLayout.mode());
                    ((IconButton) dock.main).setIcon(icon);
                    ((IconButton) dock.overflow).setIcon(icon);
                    ((IconButton) dock.main).sync();
                    ((IconButton) dock.overflow).sync();
                });

        zoom = new Tool("zoom", overlay::currentZoomLabel,
                this::toggleZoomMenu,
                base,
                () -> {
                    ((TextIconButton) zoom.main).sync();
                    ((TextIconButton) zoom.overflow).sync();
                });

        scale = new Tool("scale", overlay::currentGuiScaleLabel,
                this::toggleScaleMenu,
                base,
                () -> {
                    ((TextIconButton) scale.main).sync();
                    ((TextIconButton) scale.overflow).sync();
                });

        list.add(pick);
        list.add(highlight);
        list.add(refresh);
        list.add(dock);
        list.add(zoom);
        list.add(scale);
        return list;
    }

    private void toggleDockMenu() {
        boolean willOpen = !dockMenu.visible();
        closeAllMenus();
        if (willOpen) {
            Pos p = dock.main.absolutePos();
            dockMenu.useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.insetLeft(p.x()),
                    UIStyles.insetTop(p.y() + BUTTON_SIZE),
                    UIStyles.sizeOf(26, 112),
                    UIStyles.flexColumn(),
                    UIStyles.alignItemsCenter(),
                    UIStyles.padding(2, 0),
                    UIStyles.rowGap(2)
            ));
            dockMenu.setVisible(true);
        }
    }

    private void toggleScaleMenu() {
        boolean willOpen = !scaleMenu.visible();
        closeAllMenus();
        if (willOpen) {
            Pos p = scale.main.absolutePos();
            scaleMenu.useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.insetLeft(p.x()),
                    UIStyles.insetTop(p.y() + BUTTON_SIZE),
                    UIStyles.sizeOf(54, 120),
                    UIStyles.flexColumn(),
                    UIStyles.padding(2, 2),
                    UIStyles.rowGap(2)
            ));
            scaleMenu.setVisible(true);
        }
    }

    private void toggleZoomMenu() {
        boolean willOpen = !zoomMenu.visible();
        closeAllMenus();
        if (willOpen) {
            Pos p = zoom.main.absolutePos();
            zoomMenu.useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.insetLeft(p.x()),
                    UIStyles.insetTop(p.y() + BUTTON_SIZE),
                    UIStyles.sizeOf(54, 134),
                    UIStyles.flexColumn(),
                    UIStyles.padding(2, 2),
                    UIStyles.rowGap(2)
            ));
            zoomMenu.setVisible(true);
        }
    }

    //endregion

    //region inner widgets

    /**
     * Pixel-icon tool button.
     */
    private final class IconButton extends Widget {

        private VisualTexture icon;
        private final Runnable action;
        private final BooleanSupplier toggled;
        private boolean toggledState;

        IconButton(VisualTexture icon, Runnable action, BooleanSupplier toggled) {
            this.icon = icon;
            this.action = action;
            this.toggled = toggled;
            onMouseClick((input, clickCount, context) -> {
                action.run();
                context.consume();
                return EventDispatch.consumed;
            });
        }

        void setIcon(VisualTexture icon) {
            this.icon = icon;
        }

        void setToggled(boolean toggled) {
            this.toggledState = toggled;
        }

        void sync() {
            if (toggled != null) setToggled(toggled.getAsBoolean());
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int w = width();
            int h = height();
            boolean hovered = isMouseOver(mouseX, mouseY);
            if (toggledState) {
                canvas.fill(0, 0, w, h, DebugTheme.BUTTON_ACTIVE_BG);
            } else if (hovered) {
                canvas.fill(0, 0, w, h, DebugTheme.BUTTON_HOVER_BG);
            }
            int color = toggledState ? DebugTheme.ICON_ACTIVE : hovered ? DebugTheme.ICON_HOVER : DebugTheme.ICON;
            if (icon == DevToolsTextures.CLOSE && hovered) color = DebugTheme.ICON_CLOSE_HOVER;
            canvas.color(color)
                  .texture(icon, ICON_PAD, ICON_PAD, ICON_SIZE, ICON_SIZE)
                  .resetColor();
        }

    }

    /**
     * Small text icon button (used for the guiScale value).
     */
    private final class TextIconButton extends Widget {

        private final Runnable action;
        private final Supplier<String> labelSupplier;
        private String label;

        TextIconButton(Runnable action, Supplier<String> labelSupplier) {
            this.action = action;
            this.labelSupplier = labelSupplier;
            sync();
            onMouseClick((input, clickCount, context) -> {
                action.run();
                context.consume();
                return EventDispatch.consumed;
            });
        }

        void sync() {
            this.label = labelSupplier.get();
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int w = width();
            int h = height();
            boolean hovered = isMouseOver(mouseX, mouseY);
            if (hovered) {
                canvas.fill(0, 0, w, h, DebugTheme.BUTTON_HOVER_BG);
            }
            var font = context().font();
            int tw = font.width(label);
            int tx = (w - tw) / 2;
            int ty = (h - font.lineHeight) / 2;
            canvas.drawString(label, tx, ty, DebugTheme.TEXT, false);
        }

    }

    private final class Tool {

        final String id;
        final Widget main;
        final Widget overflow;
        final UIStyle baseStyle;
        final Runnable update;
        boolean mainHidden;

        Tool(String id, VisualTexture icon, Runnable action, BooleanSupplier toggled,
             UIStyle baseStyle, @Nullable Runnable update) {
            this.id = id;
            this.baseStyle = baseStyle;
            this.update = update;
            this.main = new IconButton(icon, action, toggled);
            this.overflow = new IconButton(icon, action, toggled);
            this.main.useStyle(baseStyle);
            this.overflow.useStyle(baseStyle.with(UIStyles.displayNone()));
        }

        Tool(String id, Supplier<String> label, Runnable action, UIStyle baseStyle, @Nullable Runnable update) {
            this.id = id;
            this.baseStyle = baseStyle;
            this.update = update;
            this.main = new TextIconButton(action, label);
            this.overflow = new TextIconButton(action, label);
            this.main.useStyle(baseStyle);
            this.overflow.useStyle(baseStyle.with(UIStyles.displayNone()));
        }

        void show() {
            if (!mainHidden) return;
            mainHidden = false;
            main.useStyle(baseStyle);
            overflow.useStyle(baseStyle.with(UIStyles.displayNone()));
        }

        void hide() {
            if (mainHidden) return;
            mainHidden = true;
            main.useStyle(baseStyle.with(UIStyles.displayNone()));
            overflow.useStyle(baseStyle);
        }

        void sync() {
            if (update != null) update.run();
        }

    }

    /**
     * Title bar with browser-like overflow: if space is tight, extra tools are
     * hidden behind a "..." button.
     */
    private final class ToolBar extends WidgetGroup<Widget> {

        private final List<Tool> tools;
        private final OverflowMenu overflowMenu;
        private final IconButton moreButton;
        private final IconButton closeButton;
        private final UIStyle moreBase;
        private final UIStyle closeBase;
        private List<Tool> hidden = new ArrayList<>();

        ToolBar(List<Tool> tools, OverflowMenu overflowMenu) {
            this.tools = tools;
            this.overflowMenu = overflowMenu;

            useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1f)),
                    UIStyles.heightOf(DebugTheme.TITLE_BAR_HEIGHT),
                    UIStyles.flexShrink(0),
                    UIStyles.flexRow(),
                    UIStyles.alignItemsCenter(),
                    UIStyles.padding(0, 4)
            ));
            setTickable(true);

            for (Tool tool : tools) {
                addWidget(tool.main);
            }

            moreBase = UIStyle.of(UIStyles.sizeOf(BUTTON_SIZE, BUTTON_SIZE), UIStyles.flexShrink(0));
            closeBase = UIStyle.of(UIStyles.sizeOf(BUTTON_SIZE, BUTTON_SIZE), UIStyles.flexShrink(0));

            moreButton = new IconButton(DevToolsTextures.MORE, this::toggleOverflow, null);
            moreButton.useStyle(moreBase.with(UIStyles.displayNone()));
            closeButton = new IconButton(DevToolsTextures.CLOSE, overlay::close, null);
            closeButton.useStyle(closeBase);

            addWidget(SpacerWidget.create());
            addWidget(moreButton);
            addWidget(closeButton);

            onMouseClicked((input, context) -> {
                double mx = input.mouseX(), my = input.mouseY();
                if (dockLayout.mode() != DockMode.FLOAT) return EventDispatch.pass;
                if (isOverAnyButton(mx, my)) return EventDispatch.pass;
                beginWindowDrag(mx, my);
                context.consume();
                return EventDispatch.consumed;
            });
        }

        boolean isOverMenuToggler(double mx, double my) {
            if (moreButton.isMouseOver(mx, my)) return true;
            for (Tool t : tools) {
                if (("dock".equals(t.id) || "scale".equals(t.id) || "zoom".equals(t.id)) && !t.mainHidden && t.main.isMouseOver(mx, my)) {
                    return true;
                }
            }
            return false;
        }

        void closeOverflow() {
            overflowMenu.setVisible(false);
            moreButton.setToggled(false);
        }

        void toggleOverflow() {
            boolean willOpen = !overflowMenu.visible();
            DevToolsWindow.this.closeAllMenus();
            if (willOpen) {
                overflowMenu.setTools(hidden);
                overflowMenu.setPosition(moreButton.absolutePos());
                overflowMenu.setVisible(true);
                moreButton.setToggled(true);
            }
        }

        @Override
        public void tick() {
            if (width() <= 0) return;

            int available = width() - 8; // 4px padding each side, always room for close
            int allFitCount = (available - BUTTON_SIZE) / BUTTON_SIZE;
            boolean overflow = allFitCount < tools.size();
            int visibleCount = overflow
                    ? Math.max(0, (available - 2 * BUTTON_SIZE) / BUTTON_SIZE)
                    : tools.size();
            visibleCount = Math.min(visibleCount, tools.size());

            List<Tool> newHidden = new ArrayList<>();
            for (int i = 0; i < tools.size(); i++) {
                Tool t = tools.get(i);
                if (i < visibleCount) {
                    t.show();
                } else {
                    t.hide();
                    newHidden.add(t);
                }
            }

            hidden = newHidden;
            moreButton.useStyle(overflow ? moreBase : moreBase.with(UIStyles.displayNone()));
            moreButton.setToggled(overflowMenu.visible());
            overflowMenu.setTools(hidden);
        }

        private boolean isOverAnyButton(double mx, double my) {
            for (Tool t : tools) {
                if (!t.mainHidden && t.main.isMouseOver(mx, my)) return true;
            }
            return moreButton.isMouseOver(mx, my) || closeButton.isMouseOver(mx, my);
        }

    }

    /**
     * Overflow menu shown when the title bar is too narrow for all tools.
     */
    private final class OverflowMenu extends WidgetGroup<Widget> {

        private final List<Tool> allTools;
        private Pos pos = Pos.origin;
        private int itemCount;

        OverflowMenu(List<Tool> allTools) {
            this.allTools = allTools;
            for (Tool t : allTools) addWidget(t.overflow);
            setVisible(false);
        }

        void setTools(List<Tool> hidden) {
            int count = 0;
            for (Tool t : allTools) {
                if (hidden.contains(t)) {
                    t.overflow.useStyle(t.baseStyle);
                    count++;
                } else {
                    t.overflow.useStyle(t.baseStyle.with(UIStyles.displayNone()));
                }
            }
            itemCount = count;
            if (count == 0) {
                setVisible(false);
                return;
            }
            if (visible()) applyStyle();
        }

        void setPosition(Pos pos) {
            this.pos = pos;
            applyStyle();
        }

        private void applyStyle() {
            int h = itemCount * BUTTON_SIZE + Math.max(0, itemCount - 1) * 2 + 4;
            useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.insetLeft(pos.x()),
                    UIStyles.insetTop(pos.y() + BUTTON_SIZE),
                    UIStyles.sizeOf(BUTTON_SIZE + 4, h),
                    UIStyles.flexColumn(),
                    UIStyles.padding(2, 0),
                    UIStyles.rowGap(2)
            ));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            canvas.texture(Textures.FLAT, 0, 0, width(), height());
        }

    }

    /**
     * Title bar, body and background of the floating / docked panel.
     */
    private final class WindowFrame extends WidgetGroup<Widget> {

        WindowFrame() {
            addWidget(toolBar);

            WidgetGroup<Widget> body = new WidgetGroup<>();
            body.useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1f)),
                    UIStyles.flexGrow(1),
                    UIStyles.minHeight(0),
                    UIStyles.flexColumn()
            ));
            body.addWidget(treeView);
            body.addWidget(splitter);
            body.addWidget(detailsView);
            addWidget(body);

            for (ResizeCorner corner : resizeCorners) {
                addWidget(corner);
            }
            addWidget(dockResizeHandle);

            // Allow dragging the window from the frame border / padding area
            onMouseClicked((input, context) -> {
                if (dockLayout.mode() != DockMode.FLOAT) return EventDispatch.pass;
                double mx = input.mouseX(), my = input.mouseY();
                // Only start drag if the click is in the border/padding area
                // (not on the toolbar or body which have their own handlers)
                int pad = 10;
                int w = width();
                int h = height();
                boolean inBorder = mx < pad || my < pad || mx > w - pad || my > h - pad;
                if (inBorder && !isOverAnyResizeCorner(mx, my)) {
                    beginWindowDrag(mx, my);
                    context.consume();
                    return EventDispatch.consumed;
                }
                return EventDispatch.pass;
            });
        }

        private boolean isOverAnyResizeCorner(double mx, double my) {
            for (ResizeCorner corner : resizeCorners) {
                if (corner.visible() && corner.isMouseOver(mx, my)) return true;
            }
            return false;
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int w = width();
            int h = height();

            // Draw the NineSlice frame as the window background.
            canvas.texture(Textures.FRAME, 0, 0, w, h);

            // Separator line between the title bar (toolbar) and the body below.
            int innerX = 10;
            int innerW = w - 20;
            int sepY = 10 + DebugTheme.TITLE_BAR_HEIGHT;
            canvas.fill(innerX, sepY, innerW, 1, DebugTheme.TITLE_SEPARATOR);
        }

    }

    private final class Splitter extends Widget {

        private boolean hovered;

        Splitter() {
            useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1f)),
                    UIStyles.heightOf(DebugTheme.SPLITTER_HEIGHT),
                    UIStyles.flexShrink(0)
            ));
            onMouseClicked((input, context) -> {
                splitting = true;
                context.consume();
                return EventDispatch.consumed;
            });
            onMouseEnter((mouseX, mouseY, context) -> {
                hovered = true;
                CursorHelper.set(CursorHelper.VRESIZE);
            });
            onMouseLeave((mouseX, mouseY, context) -> {
                hovered = false;
                CursorHelper.reset();
            });
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int w = width();
            int h = height();
            int lineY = h / 2;
            canvas.fill(0, lineY, w, 1, hovered || splitting ? DebugTheme.ACCENT : DebugTheme.TITLE_SEPARATOR);
        }

    }

    private final class ResizeCorner extends Widget {

        enum Corner {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
        }

        private final Corner corner;

        ResizeCorner(Corner corner) {
            this.corner = corner;
            useStyle(cornerStyle());
            onMouseClicked((input, context) -> {
                resizingCorner = corner;
                context.consume();
                return EventDispatch.consumed;
            });
            onMouseEnter((mouseX, mouseY, context) -> CursorHelper.set(switch (corner) {
                case TOP_LEFT, BOTTOM_RIGHT -> CursorHelper.NWSE;
                case TOP_RIGHT, BOTTOM_LEFT -> CursorHelper.NESW;
            }));
            onMouseLeave((mouseX, mouseY, context) -> CursorHelper.reset());
        }

        private UIStyle cornerStyle() {
            return switch (corner) {
                case TOP_LEFT -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetTop(0),
                        UIStyles.insetLeft(0),
                        UIStyles.sizeOf(HANDLE_SIZE, HANDLE_SIZE)
                );
                case TOP_RIGHT -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetTop(0),
                        UIStyles.insetRight(0),
                        UIStyles.sizeOf(HANDLE_SIZE, HANDLE_SIZE)
                );
                case BOTTOM_LEFT -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetBottom(0),
                        UIStyles.insetLeft(0),
                        UIStyles.sizeOf(HANDLE_SIZE, HANDLE_SIZE)
                );
                case BOTTOM_RIGHT -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetBottom(0),
                        UIStyles.insetRight(0),
                        UIStyles.sizeOf(HANDLE_SIZE, HANDLE_SIZE)
                );
            };
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            // resize corner is invisible; hit testing still handles the drag
        }

    }

    private final class DockResizeHandle extends Widget {

        DockResizeHandle() {
            onMouseClicked((input, context) -> {
                dockResizing = true;
                dockLayout.startResize(input.mouseX(), input.mouseY());
                context.consume();
                return EventDispatch.consumed;
            });
            onMouseEnter((mouseX, mouseY, context) -> CursorHelper.set(switch (dockLayout.mode()) {
                case DOCK_LEFT, DOCK_RIGHT -> CursorHelper.HRESIZE;
                case DOCK_TOP, DOCK_BOTTOM -> CursorHelper.VRESIZE;
                default -> CursorHelper.ARROW;
            }));
            onMouseLeave((mouseX, mouseY, context) -> CursorHelper.reset());
        }

        private UIStyle handleStyle() {
            return switch (dockLayout.mode()) {
                case DOCK_RIGHT -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetLeft(0),
                        UIStyles.insetTop(0),
                        UIStyles.insetBottom(0),
                        UIStyles.widthOf(4),
                        UIStyles.heightOf(TaffyDimension.percent(1f))
                );
                case DOCK_LEFT -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetRight(0),
                        UIStyles.insetTop(0),
                        UIStyles.insetBottom(0),
                        UIStyles.widthOf(4),
                        UIStyles.heightOf(TaffyDimension.percent(1f))
                );
                case DOCK_BOTTOM -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetLeft(0),
                        UIStyles.insetRight(0),
                        UIStyles.insetTop(0),
                        UIStyles.widthOf(TaffyDimension.percent(1f)),
                        UIStyles.heightOf(4)
                );
                case DOCK_TOP -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.insetLeft(0),
                        UIStyles.insetRight(0),
                        UIStyles.insetBottom(0),
                        UIStyles.widthOf(TaffyDimension.percent(1f)),
                        UIStyles.heightOf(4)
                );
                default -> UIStyle.of(
                        UIStyles.positionAbsolute(),
                        UIStyles.sizeOf(0, 0)
                );
            };
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            // dock resize handle is invisible; it only provides the hit area
        }

    }

    private final class DockMenu extends WidgetGroup<Widget> {

        DockMenu() {
            for (DockMode mode : DockMode.values()) {
                IconButton btn = new IconButton(modeTexture(mode), () -> setDockMode(mode), null);
                btn.useStyle(UIStyle.of(
                        UIStyles.sizeOf(BUTTON_SIZE, BUTTON_SIZE),
                        UIStyles.flexShrink(0)
                ));
                addWidget(btn);
            }
            useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.sizeOf(26, 112),
                    UIStyles.flexColumn(),
                    UIStyles.alignItemsCenter(),
                    UIStyles.padding(2, 0),
                    UIStyles.rowGap(2)
            ));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            canvas.texture(Textures.FLAT, 0, 0, width(), height());
        }

    }

    private final class ScaleMenu extends WidgetGroup<Widget> {

        ScaleMenu() {
            String[] labels = {"Auto", "1", "2", "3", "4"};
            int[] values = {0, 1, 2, 3, 4};
            for (int i = 0; i < labels.length; i++) {
                int value = values[i];
                ButtonWidget btn = ButtonWidget.of(labels[i], () -> {
                    overlay.setGuiScale(value);
                    closeAllMenus();
                });
                btn.setTextColor(DebugTheme.TEXT);
                btn.setTextShadow(false);
                btn.setColors(DebugTheme.WINDOW_BG, DebugTheme.BUTTON_HOVER_BG, DebugTheme.ROW_SELECTED_BG);
                btn.useStyle(UIStyle.of(
                        UIStyles.widthOf(TaffyDimension.percent(1f)),
                        UIStyles.heightOf(20),
                        UIStyles.flexShrink(0)
                ));
                addWidget(btn);
            }
            useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.sizeOf(54, 120),
                    UIStyles.flexColumn(),
                    UIStyles.padding(2, 2),
                    UIStyles.rowGap(2)
            ));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            canvas.texture(Textures.FLAT, 0, 0, width(), height());
        }

    }

    private final class ZoomMenu extends WidgetGroup<Widget> {

        ZoomMenu() {
            for (float value : DebugOverlayImpl.ZOOM_LEVELS) {
                String label = Float.toString(value).replaceAll("\\.0$", "") + "x";
                ButtonWidget btn = ButtonWidget.of(label, () -> {
                    overlay.setDevToolsZoom(value);
                    zoom.sync();
                    closeAllMenus();
                });
                btn.setTextColor(DebugTheme.TEXT);
                btn.setTextShadow(false);
                btn.setColors(DebugTheme.WINDOW_BG, DebugTheme.BUTTON_HOVER_BG, DebugTheme.ROW_SELECTED_BG);
                btn.useStyle(UIStyle.of(
                        UIStyles.widthOf(TaffyDimension.percent(1f)),
                        UIStyles.heightOf(20),
                        UIStyles.flexShrink(0)
                ));
                addWidget(btn);
            }
            useStyle(UIStyle.of(
                    UIStyles.positionAbsolute(),
                    UIStyles.sizeOf(54, 134),
                    UIStyles.flexColumn(),
                    UIStyles.padding(2, 2),
                    UIStyles.rowGap(2)
            ));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            canvas.texture(Textures.FLAT, 0, 0, width(), height());
        }

    }

}
