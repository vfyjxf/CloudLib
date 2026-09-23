package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.network.chat.Component;

/**
 * One line of a key/value detail list: a label column and a value column.
 * <p>
 * Parts: {@code ::part(label)} and {@code ::part(value)}. The label's ink falls
 * back to the theme's {@code text-dim} slot, so a sheet that dims its secondary
 * text dims every label without naming the widget, and the value takes the
 * remaining width ({@code flex-grow: 1}) — a sheet's {@code text-align} on
 * {@code ::part(value)} then decides between a left-hugging and a
 * right-hugging column.
 * <pre>
 * kv-row              { column-gap: 6px; }
 * kv-row::part(label) { color: var(--text-dim); }
 * kv-row::part(value) { color: var(--text); text-align: right; }
 * </pre>
 * <b>Element tag</b> — the tag is pinned to {@code kv-row} ({@link #tag}) rather
 * than derived from the class name, which would kebab-case to {@code k-v-row}.
 * <p>
 * <b>Cross-row alignment</b> is the shell's job: measure the widest label it
 * holds and hand every row the same {@link #labelWidth(int)}. The rows then
 * line their value columns up without the stylesheet knowing any of the labels.
 */
public final class KVRowWidget extends CompositeWidget<Widget> {

    // region element

    /**
     * The element tag this widget answers to — {@code kv-row}, not the
     * {@code k-v-row} the automatic kebab-casing of {@code KVRowWidget} would
     * derive (every capital letter becomes a segment). The tag is part of the
     * sheet-facing contract, so it is declared here and inherited by the widget's
     * parts through {@link PartNode}.
     */
    public static final String tag = "kv-row";

    @Override
    public String styleTag() {
        return tag;
    }

    // endregion

    // region parts

    /** {@code ::part(label)} — the key column. */
    static final String partLabel = "label";
    /** {@code ::part(value)} — the detail column. */
    static final String partValue = "value";

    // endregion

    // region fallbacks

    private static final int defaultGap = 4;
    private static final int defaultLabelColor = 0xFFAAAAAA;
    private static final int defaultValueColor = 0xFFFFFFFF;

    // endregion

    // region state

    private final TextPartNode labelPart;
    private final TextPartNode valuePart;

    private int labelWidth;
    private int gap = defaultGap;

    // endregion

    // region factories

    /** A row over plain components — the common case for a tooltip-style detail line. */
    public static KVRowWidget of(String label, Component value) {
        return new KVRowWidget(RichText.of(label), RichText.of(value));
    }

    /** A row over two plain strings. */
    public static KVRowWidget of(String label, String value) {
        return new KVRowWidget(RichText.of(label), RichText.of(value));
    }

    public static KVRowWidget of(Component label, Component value) {
        return new KVRowWidget(RichText.of(label), RichText.of(value));
    }

    /** A row whose value is rich text — inline items, styled fragments, embedded widgets. */
    public static KVRowWidget of(String label, RichText value) {
        return new KVRowWidget(RichText.of(label), value);
    }

    public static KVRowWidget of(Component label, RichText value) {
        return new KVRowWidget(RichText.of(label), value);
    }

    private KVRowWidget(RichText label, RichText value) {
        labelPart = addWidget(
            new TextPartNode(this, partLabel, label, defaultLabelColor).setSlotFallback(Styles.textDim)
        );
        valuePart = addWidget(new TextPartNode(this, partValue, value, defaultValueColor));
        // the value is the column that gives: it takes whatever the label leaves
        valuePart.defaultStyle(UIStyle.of(UIStyles.flexGrow(1f)));
        syncDefaults();
    }

    // endregion

    // region configuration

    public RichText label() {
        return labelPart.text();
    }

    public RichText value() {
        return valuePart.text();
    }

    public KVRowWidget setLabel(String label) {
        return setLabel(RichText.of(label));
    }

    public KVRowWidget setLabel(Component label) {
        return setLabel(RichText.of(label));
    }

    public KVRowWidget setLabel(RichText label) {
        labelPart.setText(label);
        return this;
    }

    public KVRowWidget setValue(String value) {
        return setValue(RichText.of(value));
    }

    public KVRowWidget setValue(Component value) {
        return setValue(RichText.of(value));
    }

    public KVRowWidget setValue(RichText value) {
        valuePart.setText(value);
        return this;
    }

    /**
     * The label column's width in px — {@code 0} (the default) lets the label size
     * to its own text. A shell that holds several rows measures the widest label
     * once and calls this on every row, which is what makes the value columns of
     * a detail list line up.
     */
    public int labelWidth() {
        return labelWidth;
    }

    /** Sets the label column width; {@code 0} hands the column back to its text. */
    public KVRowWidget labelWidth(int width) {
        this.labelWidth = Math.max(0, width);
        labelPart.useStyle(labelWidth == 0 ? UIStyles.widthOf(TaffyDimension.AUTO) : UIStyles.widthOf(labelWidth));
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
        return this;
    }

    /** The space between the two columns, below the theme. */
    public int gap() {
        return gap;
    }

    public KVRowWidget setGap(int gap) {
        this.gap = Math.max(0, gap);
        syncDefaults();
        return this;
    }

    /** The label's code ink, used when the sheet declares neither {@code color} nor {@code text-dim}. */
    public int labelColor() {
        return labelPart.fallbackColor();
    }

    public KVRowWidget setLabelColor(int color) {
        labelPart.setFallbackColor(color);
        return this;
    }

    public int valueColor() {
        return valuePart.fallbackColor();
    }

    public KVRowWidget setValueColor(int color) {
        valuePart.setFallbackColor(color);
        return this;
    }

    // endregion

    // region layout

    /** The row's own defaults — a flex row with the label column centred against the value. */
    private void syncDefaults() {
        defaultStyle(UIStyle.of(UIStyles.flexRow(), UIStyles.alignItemsCenter(), UIStyles.columnGap(gap)));
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("label", plain(label()), InspectionProperty.categoryData);
        collector.add("value", plain(value()), InspectionProperty.categoryData);
        collector.addWithDefault("labelWidth", labelWidth, 0, InspectionProperty.categoryLayout);
        collector.addWithDefault("gap", gap, defaultGap, InspectionProperty.categoryLayout);
    }

    private static String plain(RichText text) {
        String content = text.isTextual() ? text.toComponent().getString() : text.toString();
        return content.length() > 30 ? content.substring(0, 27) + "..." : content;
    }

    // endregion
}
