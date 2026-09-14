package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.text.ClickAction;
import dev.vfyjxf.cloudlib.api.text.GroupNode;
import dev.vfyjxf.cloudlib.api.text.HoverAction;
import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.text.RichTexts;
import dev.vfyjxf.cloudlib.api.text.StyledNode;
import dev.vfyjxf.cloudlib.api.text.WidgetNode;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
import dev.vfyjxf.cloudlib.api.text.layout.TextAlignment;
import dev.vfyjxf.cloudlib.api.text.layout.TextFragment;
import dev.vfyjxf.cloudlib.api.text.layout.TextLine;
import dev.vfyjxf.cloudlib.api.text.render.RenderOptions;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.PositionTypeProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.SizeProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.TextAlignProperty;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.tree.Layout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Rich text display widget: multi-line styled text with localization, inline
 * images/items/blocks/entities, interactive fragments and embedded child widgets.
 * <p>
 * Sizing integrates with taffy through {@link RichTextMeasure}: the widget
 * auto-measures its content (wrapping when a width is imposed, shrink-to-fit
 * otherwise) like a CSS text node. Horizontal alignment follows the
 * {@code text-align} style property ({@link TextAlignProperty}), or an explicit
 * {@link #setAlignment(TextAlignment)} override.
 * <p>
 * Interactivity: fragments carrying a {@link ClickAction} are clickable, fragments
 * carrying a {@link HoverAction} produce tooltips through the standard
 * {@link #hoverTooltip(int, int)} pipeline. Vanilla click/hover events embedded in
 * component styles are honored with vanilla screen semantics.
 * <p>
 * Embedded widgets ({@link WidgetNode}) become absolute-positioned children whose
 * bounds follow their fragment each layout pass.
 */
public class RichTextWidget extends CompositeWidget<Widget> {

    //region state

    private RichText text;
    private int color = 0xFFFFFFFF;
    private boolean shadow = false;
    private @Nullable TextAlignment alignment;

    private @Nullable RichTextMeasure measure;
    private boolean layoutApplied;
    private int intrinsicMinWidth = -1;
    private int intrinsicMinHeight = -1;

    //endregion

    //region factory

    public static RichTextWidget of(RichText text) {
        return new RichTextWidget(text);
    }

    public static RichTextWidget of(Component text) {
        return new RichTextWidget(RichText.of(text));
    }

    public static RichTextWidget of(String text) {
        return new RichTextWidget(RichText.of(text));
    }

    public static RichTextWidget of(LangEntry entry, Object... args) {
        return new RichTextWidget(RichText.of(entry, args));
    }

    private RichTextWidget(RichText text) {
        this.text = text;
        // Embedded widgets must join the tree before the scene initializes it —
        // adding children during mount would hit the uninitialized-widget check.
        syncEmbeddedWidgets();

        onMount((scene, context, handle) -> {
            measure = RichTexts.measure(this.text);
            measure.withAlignment(currentAlignment());
            scene.layoutTree().setMeasureFunc(nodeId(), measure);
            syncEmbeddedWidgets();
        });
        onUnmount(() -> measure = null);

        style().addChangeListener(TextAlignProperty.type, (oldValue, newValue) -> onAlignmentChanged());

        onMouseClick((input, clickCount, context) -> {
            if (!input.isMouse()) return EventDispatch.pass;
            // Mouse events carry scene coordinates; convert to widget-local first.
            FloatPos local = sceneToLocal(input.mouseX(), input.mouseY());
            float lx = (float) local.x - contentX();
            float ly = (float) local.y - contentY();
            TextFragment fragment = laidOut().interactiveFragmentAt(lx, ly);
            ClickAction action = fragment != null ? fragment.onClick() : null;
            if (action == null) return EventDispatch.pass;
            performClick(action, new ClickAction.Context(lx, ly, input.key().getValue()));
            return EventDispatch.consumed;
        });
    }

    //endregion

    //region configuration

    public RichText text() {
        return text;
    }

    public RichTextWidget setText(RichText text) {
        this.text = text;
        syncEmbeddedWidgets();
        if (measure != null) {
            measure = RichTexts.measure(text);
            measure.withAlignment(currentAlignment());
            scene().layoutTree().setMeasureFunc(nodeId(), measure);
            markLayoutDirty();
        }
        return this;
    }

    public int color() {
        return color;
    }

    public RichTextWidget setColor(int color) {
        this.color = color;
        return this;
    }

    public boolean shadow() {
        return shadow;
    }

    public RichTextWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /**
     * Explicit horizontal alignment override; {@code null} (default) follows the
     * {@code text-align} style property.
     */
    public @Nullable TextAlignment alignment() {
        return alignment;
    }

    public RichTextWidget setAlignment(@Nullable TextAlignment alignment) {
        this.alignment = alignment;
        onAlignmentChanged();
        return this;
    }

    /**
     * Line spacing in pixels (added between lines).
     */
    public RichTextWidget setLineSpacing(int lineSpacing) {
        if (measure != null) {
            measure.withLineSpacing(lineSpacing);
            markLayoutDirty();
        }
        return this;
    }

    //endregion

    //region layout

    private TextAlignment currentAlignment() {
        if (alignment != null) return alignment;
        return switch (style().get(TextAlignProperty.type)) {
            case RIGHT, END -> TextAlignment.RIGHT;
            case CENTER -> TextAlignment.CENTER;
            default -> TextAlignment.LEFT;
        };
    }

    private void onAlignmentChanged() {
        if (measure != null) {
            measure.withAlignment(currentAlignment());
            markLayoutDirty();
        }
    }

    private void markLayoutDirty() {
        if (scene() != null) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    /**
     * The laid-out text at the current content-box width. Cached by the measure;
     * recomputed when the width changed.
     */
    public LaidOutText laidOut() {
        if (measure == null) return LaidOutText.EMPTY;
        return measure.layoutAt(contentWidth());
    }

    private int contentWidth() {
        if (!layoutApplied) return width();
        return Math.max(0, (int) Math.ceil(layout().contentBoxWidth()));
    }

    private float contentX() {
        if (!layoutApplied) return 0;
        Layout layout = layout();
        return layout.padding().left + layout.border().left;
    }

    private float contentY() {
        if (!layoutApplied) return 0;
        Layout layout = layout();
        return layout.padding().top + layout.border().top;
    }

    @Override
    public void applyLayout() {
        super.applyLayout();
        layoutApplied = true;
        syncIntrinsicMinSize();
    }

    /**
     * Embedded widgets make this a taffy <em>container</em> node — and taffy only
     * invokes the leaf measure function on nodes without children, so a
     * RichTextWidget with embedded children gets its size derived from the
     * (absolutely positioned, size-contributing-nothing) children and collapses.
     * Enforce the measured content size as min-size in that case; the next
     * layout pass picks it up.
     */
    private void syncIntrinsicMinSize() {
        if (children().isEmpty()) {
            // Back to a leaf node — the measure func drives sizing again, so
            // drop any previously enforced min-size.
            if (intrinsicMinWidth >= 0 || intrinsicMinHeight >= 0) {
                intrinsicMinWidth = -1;
                intrinsicMinHeight = -1;
                useStyle(UIStyles.minWidth(TaffyDimension.AUTO), UIStyles.minHeight(TaffyDimension.AUTO));
                markLayoutDirty();
            }
            return;
        }
        LaidOutText laidOut = laidOut();
        if (laidOut.isEmpty()) return;
        Layout layout = layout();
        int minW = (int) Math.ceil(laidOut.width()
                + layout.padding().left + layout.padding().right
                + layout.border().left + layout.border().right);
        int minH = (int) Math.ceil(laidOut.height()
                + layout.padding().top + layout.padding().bottom
                + layout.border().top + layout.border().bottom);
        if (minW != intrinsicMinWidth || minH != intrinsicMinHeight) {
            intrinsicMinWidth = minW;
            intrinsicMinHeight = minH;
            useStyle(UIStyles.minWidth(minW), UIStyles.minHeight(minH));
            markLayoutDirty();
        }
    }

    //endregion

    //region embedded widgets

    private void syncEmbeddedWidgets() {
        List<WidgetNode> nodes = new ArrayList<>();
        collectWidgetNodes(text.root(), nodes);

        // Remove children whose node is gone.
        for (Widget child : new ArrayList<>(children())) {
            if (nodes.stream().noneMatch(node -> node.widget() == child)) {
                remove(child);
            }
        }
        // Add children that are new.
        for (WidgetNode node : nodes) {
            Widget widget = node.widget();
            if (widget.parent() != this) {
                widget.useStyle(
                        new PositionTypeProperty(TaffyPosition.ABSOLUTE),
                        new SizeProperty(node.width(), node.height())
                );
                widget.onLayout((self, scope) -> positionEmbedded(node, scope));
                add(widget);
            }
        }
    }

    private void collectWidgetNodes(RichNode node, List<WidgetNode> out) {
        switch (node) {
            case WidgetNode widget -> out.add(widget);
            case GroupNode group -> {
                for (RichNode child : group.children()) {
                    collectWidgetNodes(child, out);
                }
            }
            case StyledNode styled -> collectWidgetNodes(styled.child(), out);
            default -> {
            }
        }
    }

    private void positionEmbedded(WidgetNode node, dev.vfyjxf.cloudlib.api.ui.layout.LayoutScope scope) {
        TextFragment fragment = findFragment(node);
        if (fragment == null) return;
        Insets padding = fragment.style().paddingOr(Insets.zero);
        scope.setBounds(
                contentX() + fragment.x() + padding.left(),
                contentY() + fragment.y() + padding.top(),
                fragment.width() - padding.left() - padding.right(),
                fragment.height() - padding.top() - padding.bottom()
        );
    }

    private @Nullable TextFragment findFragment(RichNode node) {
        for (TextLine line : laidOut().lines()) {
            for (TextFragment fragment : line.fragments()) {
                if (fragment.source() == node) return fragment;
            }
        }
        return null;
    }

    //endregion

    //region interaction

    private void performClick(ClickAction action, ClickAction.Context context) {
        switch (action) {
            case ClickAction.Callback callback -> callback.handler().accept(context);
            case ClickAction.OpenUrl open -> openUrl(open.url());
            case ClickAction.CopyToClipboard copy ->
                    Minecraft.getInstance().keyboardHandler.setClipboard(copy.text());
            case ClickAction.Vanilla vanilla -> handleVanillaClick(vanilla.event());
        }
    }

    private static void openUrl(String url) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            ConfirmLinkScreen.confirmLinkNow(screen, url);
        } else {
            Util.getPlatform().openUri(url);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleVanillaClick(ClickEvent event) {
        switch (event.getAction()) {
            case OPEN_URL -> openUrl(event.getValue());
            case COPY_TO_CLIPBOARD ->
                    Minecraft.getInstance().keyboardHandler.setClipboard(event.getValue());
            case RUN_COMMAND -> {
                var connection = Minecraft.getInstance().getConnection();
                if (connection != null) connection.sendCommand(event.getValue());
            }
            case SUGGEST_COMMAND -> {
                // No chat input to fill outside screens; ignore.
            }
            default -> {
                // CHANGE_PAGE / OPEN_FILE make no sense in widget context.
            }
        }
    }

    @Override
    public @Nullable Tooltip hoverTooltip(int mouseX, int mouseY) {
        float lx = mouseX - contentX();
        float ly = mouseY - contentY();
        TextFragment fragment = laidOut().interactiveFragmentAt(lx, ly);
        if (fragment != null && fragment.onHover() != null) {
            Tooltip tooltip = resolveHover(fragment.onHover(), lx, ly);
            if (tooltip != null) return tooltip;
        }
        return super.hoverTooltip(mouseX, mouseY);
    }

    private @Nullable Tooltip resolveHover(HoverAction action, float mouseX, float mouseY) {
        return switch (action) {
            case HoverAction.ShowTooltip show -> show.tooltip();
            case HoverAction.ShowText text -> Tooltip.create().add(text.text());
            case HoverAction.Callback callback -> {
                callback.handler().accept(new HoverAction.Context(mouseX, mouseY));
                yield null;
            }
            case HoverAction.Vanilla vanilla -> vanillaHoverTooltip(vanilla.event());
        };
    }

    private @Nullable Tooltip vanillaHoverTooltip(HoverEvent event) {
        var action = event.getAction();
        if (action == HoverEvent.Action.SHOW_TEXT) {
            Component value = event.getValue(HoverEvent.Action.SHOW_TEXT);
            return value != null ? Tooltip.create().add(value) : null;
        }
        if (action == HoverEvent.Action.SHOW_ITEM) {
            var info = event.getValue(HoverEvent.Action.SHOW_ITEM);
            if (info != null && !info.getItemStack().isEmpty()) {
                Tooltip tooltip = Tooltip.create();
                tooltip.add(info.getItemStack().getHoverName());
                info.getItemStack().getTooltipLines(
                        net.minecraft.world.item.Item.TooltipContext.of(
                                Minecraft.getInstance().level),
                        Minecraft.getInstance().player,
                        net.minecraft.world.item.TooltipFlag.NORMAL
                ).forEach(tooltip::add);
                return tooltip;
            }
        }
        if (action == HoverEvent.Action.SHOW_ENTITY) {
            var info = event.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (info != null) {
                Tooltip tooltip = Tooltip.create();
                info.name.ifPresent(tooltip::add);
                tooltip.add(Component.literal(info.type.getDescriptionId())
                        .withStyle(ChatFormatting.GRAY));
                return tooltip;
            }
        }
        return null;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        LaidOutText laidOut = laidOut();
        if (laidOut.isEmpty()) return;
        RenderOptions options = RenderOptions.DEFAULT
                .withDefaultColor(color)
                .withShadow(shadow)
                .withMouse(mouseX - contentX(), mouseY - contentY())
                .withPartialTicks(partialTicks);
        RichTexts.renderer().render(canvas, laidOut, contentX(), contentY(), options);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        String content = text.isTextual() ? text.toComponent().getString() : text.toString();
        if (content.length() > 30) {
            content = content.substring(0, 27) + "...";
        }
        collector.add("text", content, InspectionProperty.categoryData);
        collector.addFormatted("color", String.format("#%06X", color & 0xFFFFFF), "#FFFFFF", InspectionProperty.categoryVisual);
        collector.addWithDefault("shadow", shadow, false, InspectionProperty.categoryVisual);
        collector.addWithDefault("alignment", alignment, null, InspectionProperty.categoryLayout);
    }

    //endregion
}
