package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A group heading for an info panel: a title with a rule running out to the
 * right of it — {@code Damage}, {@code ————————————}.
 * <p>
 * The title is a rich text leaf with a themed ink ({@code color}) and a themed
 * size ({@code font-size}, scaled against the font's line height); the rule is a
 * themed strip that grows into whatever width the row has left. Parts:
 * {@code ::part(title)} and {@code ::part(rule)}:
 * <pre>
 * section-header              { column-gap: 4px; }
 * section-header::part(title) { color: var(--text); font-size: 10px; }
 * section-header::part(rule)  { background: var(--rule); height: 1px; }
 * </pre>
 * The rule falls back to a 1px neutral line when the sheet paints none, so an
 * unstyled header still reads as a header.
 */
public final class SectionHeaderWidget extends CompositeWidget<Widget> {

    // region parts

    /** {@code ::part(title)} — the heading text. */
    static final String partTitle = "title";
    /** {@code ::part(rule)} — the line running out to the right. */
    static final String partRule = "rule";

    // endregion

    // region fallbacks

    private static final int defaultRuleThickness = 1;
    private static final int defaultRuleMinWidth = 12;
    private static final int defaultGap = 4;
    private static final int defaultTitleColor = 0xFFFFFFFF;

    private @Nullable VisualTexture ruleTexture = new ColorTexture(0xFFAAAAAA);

    // endregion

    // region state

    private final TextPartNode titlePart;
    private final RulePart rulePart;

    private int gap = defaultGap;
    private int ruleThickness = defaultRuleThickness;
    private int ruleMinWidth = defaultRuleMinWidth;

    // endregion

    // region factories

    public static SectionHeaderWidget of(String title) {
        return new SectionHeaderWidget(RichText.of(title));
    }

    public static SectionHeaderWidget of(Component title) {
        return new SectionHeaderWidget(RichText.of(title));
    }

    public static SectionHeaderWidget of(RichText title) {
        return new SectionHeaderWidget(title);
    }

    private SectionHeaderWidget(RichText title) {
        titlePart = addWidget(new TextPartNode(this, partTitle, title, defaultTitleColor).setDefaultShadow(false));
        rulePart = addWidget(new RulePart(this));
        syncDefaults();
    }

    // endregion

    // region configuration

    public RichText title() {
        return titlePart.text();
    }

    public SectionHeaderWidget setTitle(String title) {
        return setTitle(RichText.of(title));
    }

    public SectionHeaderWidget setTitle(Component title) {
        return setTitle(RichText.of(title));
    }

    public SectionHeaderWidget setTitle(RichText title) {
        titlePart.setText(title);
        return this;
    }

    /** The code ink used when the sheet declares no {@code color} for the title. */
    public int titleColor() {
        return titlePart.fallbackColor();
    }

    public SectionHeaderWidget setTitleColor(int color) {
        titlePart.setFallbackColor(color);
        return this;
    }

    /** The space between the title and its rule, below the theme. */
    public int gap() {
        return gap;
    }

    public SectionHeaderWidget setGap(int gap) {
        this.gap = Math.max(0, gap);
        syncDefaults();
        return this;
    }

    /** The rule's thickness in px when the sheet declares no {@code height} for it. */
    public int ruleThickness() {
        return ruleThickness;
    }

    public SectionHeaderWidget setRuleThickness(int thickness) {
        this.ruleThickness = Math.max(1, thickness);
        rulePart.syncDefaults();
        return this;
    }

    public @Nullable VisualTexture ruleTexture() {
        return ruleTexture;
    }

    public SectionHeaderWidget setRuleTexture(@Nullable VisualTexture texture) {
        this.ruleTexture = texture;
        return this;
    }

    /** The shortest the rule may get — a header on a narrow panel still shows a stub of line. */
    public int ruleMinWidth() {
        return ruleMinWidth;
    }

    public SectionHeaderWidget setRuleMinWidth(int width) {
        this.ruleMinWidth = Math.max(0, width);
        rulePart.syncDefaults();
        return this;
    }

    // endregion

    // region layout

    /**
     * The row's own defaults — a flex row with the title centred against the
     * rule, both below the theme so a sheet can re-gap or re-align the header.
     */
    private void syncDefaults() {
        defaultStyle(UIStyle.of(UIStyles.flexRow(), UIStyles.alignItemsCenter(), UIStyles.columnGap(gap)));
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    // endregion

    // region parts

    /**
     * The rule — a strip that grows into the free space of the header row and
     * paints whatever background the sheet resolves for it.
     */
    private static final class RulePart extends PartNode {

        private final SectionHeaderWidget owner;

        RulePart(SectionHeaderWidget owner) {
            super(owner, partRule);
            this.owner = owner;
            syncDefaults();
        }

        /** The strip's own defaults, below the theme — a sheet's {@code height} still wins. */
        void syncDefaults() {
            defaultStyle(
                UIStyle.of(
                    UIStyles.flexGrow(1f),
                    UIStyles.flexShrink(1f),
                    UIStyles.minWidth(owner.ruleMinWidth),
                    UIStyles.heightOf(owner.ruleThickness),
                    UIStyles.alignSelfCenter()
                )
            );
            if (lifecycle().mounted()) {
                scene().layoutTree().markDirty(nodeId());
            }
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            VisualTexture texture = themedBackground(owner.ruleTexture);
            if (texture == null || texture.isEmpty() || width() <= 0 || height() <= 0) {
                return;
            }
            canvas.texture(texture, 0, 0, width(), height());
        }
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        String content = title().isTextual() ? title().toComponent().getString() : title().toString();
        if (content.length() > 30) {
            content = content.substring(0, 27) + "...";
        }
        collector.add("title", content, InspectionProperty.categoryData);
        collector.addWithDefault("gap", gap, defaultGap, InspectionProperty.categoryLayout);
        collector.addWithDefault(
            "ruleThickness",
            ruleThickness,
            defaultRuleThickness,
            InspectionProperty.categoryVisual
        );
        collector.add("rule", rulePart.bounds(), InspectionProperty.categoryLayout);
    }

    // endregion
}
