package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.cloudlib.util.Checks;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.jetbrains.annotations.Nullable;

/**
 * Layout property for gap (spacing between flex items).
 * <p>
 * In taffy {@link TaffyStyle#gap}: {@code width = column-gap}, {@code height = row-gap}.
 * <p>
 * This property uses a <b>single</b> {@link StyleType} and can update row/column independently:
 * you may create a row-only gap or column-only gap.
 *
 * @param rowGap    row gap (height) or null to leave unchanged
 * @param columnGap row gap (width) or null to leave unchanged
 * @see LengthPercentage
 * @see UIStyles#gap(float)
 * @see UIStyles#gap(float, float)
 * @see UIStyles#rowGap(float)
 * @see UIStyles#columnGap(float)
 */
public record GapProperty(
    @Nullable LengthPercentage rowGap,
    @Nullable LengthPercentage columnGap
) implements LayoutProperty {

    /**
     * Type-safe gap value.
     * <p>
     * Note: in taffy {@link TaffyStyle#gap}, {@code width = column-gap} and {@code height = row-gap}.
     */
    public static final StyleType<TaffySize<LengthPercentage>> type = StyleType.of(
        "gap",
        () -> TaffySize.all(LengthPercentage.ZERO),
        (context, gap) -> {
            context.layoutStyle().gap.width = gap.width;
            context.layoutStyle().gap.height = gap.height;
        }
    );

    @Override
    public StyleType<?> type() {
        return type;
    }

    public static GapProperty all(float gap) {
        return new GapProperty(LengthPercentage.length(gap), LengthPercentage.length(gap));
    }

    public static GapProperty all(TaffySize<LengthPercentage> gap) {
        Checks.checkNotNull(gap, "gap");
        return new GapProperty(gap.height, gap.width);
    }

    public static GapProperty all(LengthPercentage gap) {
        Checks.checkNotNull(gap, "gap");
        return new GapProperty(gap, gap);
    }

    public static GapProperty both(float rowGap, float columnGap) {
        return new GapProperty(LengthPercentage.length(rowGap), LengthPercentage.length(columnGap));
    }

    public static GapProperty both(LengthPercentage rowGap, LengthPercentage columnGap) {
        Checks.checkNotNull(rowGap, "rowGap");
        Checks.checkNotNull(columnGap, "columnGap");
        return new GapProperty(rowGap, columnGap);
    }

    public static GapProperty rowGap(float gap) {
        Checks.checkArgument(gap >= 0, "gap must be non-negative");
        return new GapProperty(LengthPercentage.length(gap), null);
    }

    public static GapProperty columnGap(float gap) {
        Checks.checkArgument(gap >= 0, "gap must be non-negative");
        return new GapProperty(null, LengthPercentage.length(gap));
    }

    public static GapProperty rowGap(LengthPercentage gap) {
        Checks.checkNotNull(gap, "gap");
        return new GapProperty(gap, null);
    }

    public static GapProperty columnGap(LengthPercentage gap) {
        Checks.checkNotNull(gap, "gap");
        return new GapProperty(null, gap);
    }

    @Override
    public void apply(StyleContext context) {
        TaffySize<LengthPercentage> current = context.get(type);

        LengthPercentage nextCol = columnGap != null ? columnGap : current.width;
        LengthPercentage nextRow = rowGap != null ? rowGap : current.height;

        TaffySize<LengthPercentage> merged = TaffySize.of(nextCol, nextRow);

        context.set(type, merged);
        context.applyGeneric(this);
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        if (columnGap != null) {
            style.gap.width = columnGap;
        }
        if (rowGap != null) {
            style.gap.height = rowGap;
        }
    }

    @Override
    public String toString() {
        String row = rowGap == null ? "<keep>" : rowGap.toString();
        String col = columnGap == null ? "<keep>" : columnGap.toString();
        return "row=" + row + ", col=" + col;
    }
}
