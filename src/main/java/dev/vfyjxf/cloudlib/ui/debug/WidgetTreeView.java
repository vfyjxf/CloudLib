package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.ui.Textures;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.scrollable;

/**
 * DevTools "Elements"-style widget tree panel.
 * <p>
 * Rows are rebuilt from the inspected scene's widget tree; expanded state,
 * scroll position and selection survive rebuilds. The view refreshes itself
 * when the inspected tree's structure changes (checked once per second).
 */
final class WidgetTreeView extends WidgetGroup<Widget> {

    private final DebugOverlayImpl overlay;
    private final ScrollState scrollState;
    private final Set<Widget> expanded = new HashSet<>();
    private @Nullable Widget selected;
    private boolean expandInitialized;
    private int tickCounter;
    private int lastKnownCount = -1;
    private boolean refreshQueued;
    private boolean scrollPending;

    WidgetTreeView(DebugOverlayImpl overlay) {
        this.overlay = overlay;
        useStyle(UIStyle.of(
                UIStyles.flexColumn(),
                UIStyles.padding(4, 7, 0, 0)
        ));
        scrollState = ScrollState.create(ScrollDirection.vertical)
                                 .scrollSpeed(14)
                                 .smooth(true)
                                 .smoothSpeed(0.4f)
                                 .scrollbarWidth(7)
                                 .viewportInset(4, 7, 0, 0)
                                 .trackTexture(Textures.SCROLL_TRACK)
                                 .thumbTexture(Textures.SCROLLBAR_VERTICAL);
        useEffect(scrollable(scrollState));
        setTickable(true);
    }

    //region public api

    void setSelected(@Nullable Widget widget) {
        this.selected = widget;
        if (widget != null) {
            for (Widget p = widget.parent(); p != null; p = p.parent()) {
                expanded.add(p);
            }
        }
        scrollPending = true;
        scheduleRefresh();
    }

    void refresh() {
        scheduleRefresh();
    }

    //endregion

    //region internals

    /**
     * Structural mutations are deferred to the scene's deferred-task drain point
     * (start of render) so they never happen during tree traversal or event
     * dispatch.
     */
    private void scheduleRefresh() {
        if (refreshQueued || !lifecycle().mounted()) return;
        refreshQueued = true;
        scene().defer(() -> {
            refreshQueued = false;
            rebuildRows();
            if (scrollPending) {
                scrollPending = false;
                scrollToSelected();
            }
        });
    }

    private void rebuildRows() {
        Widget root = overlay.inspected().root();
        if (!expandInitialized) {
            expandInitialized = true;
            expanded.add(root);
        }
        clear();
        buildRows(root, 0);
        lastKnownCount = countWidgets(root);
    }

    private void buildRows(Widget node, int depth) {
        boolean hasChildren = node instanceof CompositeWidget<?> composite && !composite.children().isEmpty();
        addWidget(new TreeRow(node, depth, hasChildren, expanded.contains(node)));
        if (hasChildren && expanded.contains(node)) {
            for (Widget child : ((CompositeWidget<?>) node).children()) {
                buildRows(child, depth + 1);
            }
        }
    }

    private void toggleExpanded(Widget node) {
        if (!expanded.remove(node)) {
            expanded.add(node);
        }
        refresh();
    }

    private void scrollToSelected() {
        if (selected == null) return;
        int index = -1;
        for (int i = 0; i < children().size(); i++) {
            if (children().get(i) instanceof TreeRow row && row.node == selected) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        float rowTop = index * (float) DebugTheme.ROW_HEIGHT;
        float viewTop = scrollState.scrollY();
        float viewBottom = viewTop + height();
        if (rowTop < viewTop || rowTop + DebugTheme.ROW_HEIGHT > viewBottom) {
            scrollState.jumpTo(0, Math.max(0, rowTop - height() / 2f));
        }
    }

    private static int countWidgets(Widget widget) {
        int count = 1;
        if (widget instanceof CompositeWidget<?> composite) {
            for (Widget child : composite.children()) {
                count += countWidgets(child);
            }
        }
        return count;
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCounter >= 20) {
            tickCounter = 0;
            int count = countWidgets(overlay.inspected().root());
            if (count != lastKnownCount) {
                refresh();
            }
        }
    }

    //endregion

    /**
     * A single tree row: indent guides + expand arrow + type name + key + size.
     * Clicking the arrow toggles expansion, clicking the rest selects the node.
     */
    private final class TreeRow extends Widget {

        private final Widget node;
        private final int depth;
        private final boolean hasChildren;
        private final boolean isExpanded;
        private final boolean isSelected;
        private boolean hovered;

        TreeRow(Widget node, int depth, boolean hasChildren, boolean isExpanded) {
            this.node = node;
            this.depth = depth;
            this.hasChildren = hasChildren;
            this.isExpanded = isExpanded;
            this.isSelected = node == selected;
            useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1)),
                    UIStyles.heightOf(DebugTheme.ROW_HEIGHT),
                    UIStyles.flexShrink(0)
            ));

            onMouseClicked((input, context) -> {
                double localX = input.mouseRelative(this).x;
                int arrowLeft = depth * DebugTheme.ROW_INDENT;
                if (hasChildren && localX >= arrowLeft && localX <= arrowLeft + DebugTheme.ROW_INDENT) {
                    toggleExpanded(node);
                } else {
                    overlay.select(node);
                }
                context.consume();
                return EventDispatch.consumed;
            });
            onMouseEnter((mouseX, mouseY, context) -> {
                hovered = true;
                overlay.setHoverTarget(node);
            });
            onMouseLeave((mouseX, mouseY, context) -> {
                hovered = false;
                overlay.clearHoverTarget(node);
            });
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int h = height();
            if (isSelected) {
                canvas.fill(0, 0, width(), h, DebugTheme.ROW_SELECTED_BG);
                canvas.fill(0, 0, 2, h, DebugTheme.ACCENT);
            } else if (hovered) {
                canvas.fill(0, 0, width(), h, DebugTheme.ROW_HOVER_BG);
            }

            // indent guides, aligned with the arrow centers of ancestor rows
            for (int d = 1; d <= depth; d++) {
                int guideX = d * DebugTheme.ROW_INDENT - DebugTheme.ROW_INDENT / 2;
                canvas.fill(guideX, 0, 1, h, DebugTheme.INDENT_GUIDE);
            }

            var font = Minecraft.getInstance().font;
            int textY = (h - font.lineHeight) / 2 + 1;
            int x = depth * DebugTheme.ROW_INDENT + 1;
            if (hasChildren) {
                var icon = isExpanded ? Textures.ARROW_DOWN : Textures.ARROW_RIGHT;
                int iconY = (h - 10) / 2;
                canvas.color(DebugTheme.TEXT_DIM)
                      .texture(icon, x, iconY, 10, 10)
                      .resetColor();
            }
            x += DebugTheme.ROW_INDENT;

            int nameColor = node.visible() ? DebugTheme.TEXT : DebugTheme.TEXT_DIM;
            String name = node.inspectionTypeName();
            canvas.drawString(name, x, textY, nameColor, false);
            x += font.width(name);

            Object key = node.key();
            if (key != null) {
                String keyText = "#" + key;
                canvas.drawString(keyText, x, textY, DebugTheme.ACCENT, false);
                x += font.width(keyText);
            }

            String size = node.width() + "×" + node.height();
            canvas.drawString(size, x + 6, textY, DebugTheme.TEXT_DIM, false);
        }

    }

}
