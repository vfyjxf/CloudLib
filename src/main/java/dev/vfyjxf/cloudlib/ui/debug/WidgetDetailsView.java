package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.ui.Textures;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.TextFieldWidget;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.tree.Layout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.scrollable;

/**
 * DevTools-style details panel for the selected widget: a header, a filter
 * field, a box-model layout section, and the inspected properties grouped by
 * category. Long values wrap instead of truncating; values refresh once per
 * second while open.
 */
final class WidgetDetailsView extends WidgetGroup<Widget> {

    private static final int PROP_PAD_LEFT = 6;
    private static final int PROP_NAME_WIDTH = 120;
    private static final int PROP_LINE_HEIGHT = 12;
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final DebugOverlayImpl overlay;
    private final LabelWidget header;
    private final TextFieldWidget filterField;
    private final WidgetGroup<Widget> list;
    private @Nullable Widget target;
    private int tickCounter;
    private boolean rebuildQueued;

    WidgetDetailsView(DebugOverlayImpl overlay) {
        this.overlay = overlay;
        useStyle(UIStyle.of(
                UIStyles.flexColumn(),
                UIStyles.padding(4, 0, 0, 0),
                UIStyles.rowGap(2)
        ));

        header = LabelWidget.of("No selection")
                            .setColor(DebugTheme.TEXT_DIM)
                            .setShadow(false);
        header.useStyle(UIStyle.of(
                UIStyles.widthOf(TaffyDimension.percent(1)),
                UIStyles.heightOf(16),
                UIStyles.flexShrink(0)
        ));
        addWidget(header);

        filterField = TextFieldWidget.create()
                                     .setPlaceholder("Filter properties")
                                     .setTextColor(DebugTheme.TEXT)
                                     .setPlaceholderColor(DebugTheme.TEXT_DIM);
        filterField.setBackgroundTexture(Textures.INSET);
        filterField.setBorderTexture(new ColorTexture(0x00000000));
        filterField.useStyle(UIStyle.of(
                UIStyles.widthOf(TaffyDimension.percent(1)),
                UIStyles.heightOf(18),
                UIStyles.flexShrink(0)
        ));
        filterField.onTextChanged(text -> scheduleRebuild());
        addWidget(filterField);

        var listScroll = ScrollState.create(ScrollDirection.vertical)
                                    .scrollSpeed(14)
                                    .smooth(true)
                                    .smoothSpeed(0.4f)
                                    .scrollbarWidth(7)
                                    .viewportInset(0, 7, 0, 0)
                                    .trackTexture(Textures.SCROLL_TRACK)
                                    .thumbTexture(Textures.SCROLLBAR_VERTICAL);
        list = new WidgetGroup<>();
        list.useStyle(UIStyle.of(
                UIStyles.flexColumn(),
                UIStyles.widthOf(TaffyDimension.percent(1)),
                UIStyles.flexGrow(1),
                UIStyles.minHeight(0),
                UIStyles.padding(0, 7, 0, 0)
        ));
        list.useEffect(scrollable(listScroll));
        addWidget(list);

        setTickable(true);
    }

    //region public api

    void setTarget(@Nullable Widget widget) {
        this.target = widget;
        scheduleRebuild();
    }

    void refresh() {
        scheduleRebuild();
    }

    //endregion

    //region internals

    /**
     * Structural mutations are deferred to the scene's deferred-task drain point
     * (start of render) so they never happen during tree traversal or event
     * dispatch.
     */
    private void scheduleRebuild() {
        if (rebuildQueued || !lifecycle().mounted()) return;
        rebuildQueued = true;
        scene().defer(() -> {
            rebuildQueued = false;
            rebuild();
        });
    }

    private void rebuild() {
        list.clear();
        if (target == null || !target.lifecycle().mounted()) {
            header.setText("No selection");
            header.setColor(DebugTheme.TEXT_DIM);
            return;
        }

        String title = target.inspectionTypeName();
        if (target.key() != null) title += "#" + target.key();
        title += "   " + target.width() + " × " + target.height();
        header.setText(title);
        header.setColor(DebugTheme.TEXT);

        var collector = InspectionInfoCollector.from(target);
        String filter = filterField.text().toLowerCase(Locale.ROOT).trim();

        // Box-model layout section — replaces the raw "layout" property category
        if (filter.isEmpty()) {
            list.addWidget(new SectionHeader(InspectionProperty.categoryLayout));
            list.addWidget(new BoxModelView(target));
            Pos abs = target.absolutePos();
            list.addWidget(new PropRow(new InspectionProperty(
                    "position", abs.x() + ", " + abs.y(), null, InspectionProperty.categoryLayout)));
            list.addWidget(new PropRow(new InspectionProperty(
                    "size", target.width() + " × " + target.height(), null, InspectionProperty.categoryLayout)));
        }

        List<String> categories = new ArrayList<>(collector.getCategories().toList());
        categories.sort(Comparator.comparingInt(WidgetDetailsView::categoryRank).thenComparing(Comparator.naturalOrder()));

        for (String category : categories) {
            if (InspectionProperty.categoryLayout.equals(category)) continue;
            MutableList<InspectionProperty> props = collector.getByCategory(category);
            if (!filter.isEmpty()) {
                props = props.select(prop -> matches(prop, filter));
            }
            if (props.isEmpty()) continue;

            list.addWidget(new SectionHeader(category));
            for (var prop : props) {
                list.addWidget(new PropRow(prop));
            }
        }
    }

    private static int categoryRank(String category) {
        return switch (category) {
            case InspectionProperty.categoryBasic -> 0;
            case InspectionProperty.categoryData -> 1;
            case InspectionProperty.categoryState -> 2;
            case InspectionProperty.categoryVisual -> 3;
            default -> category.startsWith("style-") ? 4 : 5;
        };
    }

    private static boolean matches(InspectionProperty prop, String filter) {
        return prop.name().toLowerCase(Locale.ROOT).contains(filter)
                || prop.value().toLowerCase(Locale.ROOT).contains(filter);
    }

    private static int valueColor(InspectionProperty prop) {
        if (!prop.isNonDefault()) return DebugTheme.PROP_VALUE_DEFAULT;
        String value = prop.value();
        if (value.startsWith("\"")) return DebugTheme.PROP_VALUE_STRING;
        if (value.equals("true") || value.equals("false")) return DebugTheme.PROP_VALUE_BOOL;
        if (value.equals("null")) return DebugTheme.PROP_VALUE_DEFAULT;
        if (NUMBER.matcher(value).matches()) return DebugTheme.PROP_VALUE_NUMBER;
        return DebugTheme.TEXT;
    }

    private static List<FormattedCharSequence> wrap(String text, int width) {
        var font = Minecraft.getInstance().font;
        List<FormattedText> parts = font.getSplitter().splitLines(text, Math.max(20, width), Style.EMPTY);
        if (parts.isEmpty()) return List.of(FormattedCharSequence.EMPTY);
        List<FormattedCharSequence> lines = new ArrayList<>(parts.size());
        for (FormattedText part : parts) {
            lines.add(Language.getInstance().getVisualOrder(part));
        }
        return lines;
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCounter >= 20) {
            tickCounter = 0;
            if (target != null) {
                scheduleRebuild();
            }
        }
    }

    //endregion

    /**
     * A category section title: small colored label with a hairline running
     * to the right edge.
     */
    private static final class SectionHeader extends Widget {

        private final String title;

        SectionHeader(String title) {
            this.title = title;
            useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1)),
                    UIStyles.heightOf(DebugTheme.ROW_HEIGHT),
                    UIStyles.flexShrink(0)
            ));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            var font = Minecraft.getInstance().font;
            int textY = (height() - font.lineHeight) / 2 + 1;
            canvas.drawString(title, PROP_PAD_LEFT, textY, DebugTheme.CATEGORY, false);
            int lineX = PROP_PAD_LEFT + font.width(title) + 6;
            if (lineX < width() - 4) {
                canvas.fill(lineX, height() / 2, width() - 4 - lineX, 1, DebugTheme.SECTION_LINE);
            }
        }

    }

    /**
     * A single property line: name column plus a full, soft-wrapped value.
     * The row height is measured from the wrapped line count, so long values
     * expand instead of being truncated.
     */
    private static final class PropRow extends Widget {

        private final InspectionProperty prop;
        private boolean hovered;

        PropRow(InspectionProperty prop) {
            this.prop = prop;
            useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1)),
                    UIStyles.flexShrink(0)
            ));
            onMount((scene, context, handle) -> scene.layoutTree().setMeasureFunc(nodeId(), (known, available) -> {
                float w = available.width.unwrapOr(240);
                int valueWidth = (int) w - PROP_PAD_LEFT - PROP_NAME_WIDTH - 12;
                int lines = wrap(prop.value(), valueWidth).size();
                return new FloatSize(w, lines * PROP_LINE_HEIGHT + 4);
            }));
            onMouseEnter((mouseX, mouseY, context) -> hovered = true);
            onMouseLeave((mouseX, mouseY, context) -> hovered = false);
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            if (hovered) {
                canvas.fill(0, 0, width(), height(), DebugTheme.ROW_HOVER_BG);
            }
            var font = Minecraft.getInstance().font;
            int y = 2;

            String name = prop.name() + ":";
            if (font.width(name) > PROP_NAME_WIDTH) {
                while (name.length() > 1 && font.width(name + "…") > PROP_NAME_WIDTH) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "…";
            }
            canvas.drawString(name, PROP_PAD_LEFT, y, DebugTheme.PROP_NAME, false);

            int valueX = PROP_PAD_LEFT + PROP_NAME_WIDTH + 8;
            int valueWidth = width() - valueX - 4;
            int color = valueColor(prop);
            for (FormattedCharSequence line : wrap(prop.value(), valueWidth)) {
                canvas.text(line, valueX, y, color, false);
                y += PROP_LINE_HEIGHT;
            }
        }

    }

    /**
     * Chrome DevTools-like box-model diagram: nested margin, border, padding
     * and content bands with the computed per-side values, re-read from the
     * layout tree every frame.
     */
    private final class BoxModelView extends Widget {

        private static final int HEIGHT = DebugTheme.BM_BAND * 6 + DebugTheme.BM_CONTENT_MIN;

        private final Widget target;

        BoxModelView(Widget target) {
            this.target = target;
            useStyle(UIStyle.of(
                    UIStyles.widthOf(TaffyDimension.percent(1)),
                    UIStyles.heightOf(HEIGHT),
                    UIStyles.flexShrink(0),
                    UIStyles.margin(0, 3, 0, 3)
            ));
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int w = width();
            int h = height();

            Layout layout = null;
            try {
                if (target.lifecycle().mounted() && target.scene() == overlay.inspected()) {
                    layout = overlay.inspected().layoutTree().getLayout(target.nodeId());
                }
            } catch (RuntimeException ignored) {
                // node gone from the layout tree
            }

            FloatRect margin = layout != null && layout.margin() != null ? layout.margin() : FloatRect.ZERO;
            FloatRect border = layout != null && layout.border() != null ? layout.border() : FloatRect.ZERO;
            FloatRect padding = layout != null && layout.padding() != null ? layout.padding() : FloatRect.ZERO;

            int b = DebugTheme.BM_BAND;
            canvas.fill(0, 0, w, h, DebugTheme.BM_MARGIN);
            canvas.fill(b, b, w - 2 * b, h - 2 * b, DebugTheme.BM_BORDER);
            canvas.fill(2 * b, 2 * b, w - 4 * b, h - 4 * b, DebugTheme.BM_PADDING);
            canvas.fill(3 * b, 3 * b, w - 6 * b, h - 6 * b, DebugTheme.BM_CONTENT);

            var font = Minecraft.getInstance().font;
            // per-side band values; top/bottom centered horizontally, left/right centered in the band
            // Always show values (even 0) so the full box model is visible.
            drawBandValue(canvas, font, margin.top, w / 2, bandCenter(0));
            drawBandValue(canvas, font, margin.bottom, w / 2, bandCenter(h - b));
            drawSideValue(canvas, font, margin.left, 0);
            drawSideValue(canvas, font, margin.right, w - b);

            drawBandValue(canvas, font, border.top, w / 2, bandCenter(b));
            drawBandValue(canvas, font, border.bottom, w / 2, bandCenter(h - 2 * b));
            drawSideValue(canvas, font, border.left, b);
            drawSideValue(canvas, font, border.right, w - 2 * b);

            drawBandValue(canvas, font, padding.top, w / 2, bandCenter(2 * b));
            drawBandValue(canvas, font, padding.bottom, w / 2, bandCenter(h - 3 * b));
            drawSideValue(canvas, font, padding.left, 2 * b);
            drawSideValue(canvas, font, padding.right, w - 3 * b);

            // content size in the center
            String size;
            if (layout != null) {
                size = fmt(layout.contentBoxWidth()) + " × " + fmt(layout.contentBoxHeight());
            } else {
                size = target.width() + " × " + target.height();
            }
            int tw = font.width(size);
            canvas.drawString(size, Math.max(3 * b + 1, (w - tw) / 2), (h - font.lineHeight) / 2 + 1, DebugTheme.BM_TEXT, false);
        }

        private int bandCenter(int bandTop) {
            return bandTop + (DebugTheme.BM_BAND - 9) / 2 + 1;
        }

        /** Top/bottom band value, centered on the given x. */
        private void drawBandValue(SceneCanvas canvas, Font font, float value, int centerX, int y) {
            String text = fmt(value);
            canvas.drawString(text, centerX - font.width(text) / 2, y, DebugTheme.BM_TEXT, false);
        }

        /** Left/right band value, vertically centered, skipped when the band is too narrow. */
        private void drawSideValue(SceneCanvas canvas, Font font, float value, int bandLeft) {
            String text = fmt(value);
            int textW = font.width(text);
            if (textW + 2 > DebugTheme.BM_BAND) return;
            canvas.drawString(text, bandLeft + (DebugTheme.BM_BAND - textW) / 2, (height() - font.lineHeight) / 2 + 1, DebugTheme.BM_TEXT, false);
        }

        private static String fmt(float value) {
            if (value == Mth.floor(value)) return Integer.toString((int) value);
            return String.format(Locale.ROOT, "%.1f", value);
        }

    }

}
