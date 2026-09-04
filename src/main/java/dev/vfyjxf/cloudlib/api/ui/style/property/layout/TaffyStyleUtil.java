package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.*;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for working with taffy style types.
 * <p>
 * Provides helper methods for setting rect edge values and converting between
 * taffy style enum types.
 *
 * @see TaffyStyle
 * @see TaffyRect
 */
final class TaffyStyleUtil {

    private TaffyStyleUtil() {
    }

    //region rect operations

    static <T> void setRectEdge(TaffyRect<T> rect, Edge edge, T value) {
        switch (edge) {
            case TOP -> rect.top = value;
            case RIGHT -> rect.right = value;
            case BOTTOM -> rect.bottom = value;
            case LEFT -> rect.left = value;
        }
    }

    //endregion

    //region conversions

    static AlignContent toAlignContent(JustifyContent justify) {
        return switch (justify) {
            case FLEX_START -> AlignContent.FLEX_START;
            case FLEX_END -> AlignContent.FLEX_END;
            case CENTER -> AlignContent.CENTER;
            case SPACE_BETWEEN -> AlignContent.SPACE_BETWEEN;
            case SPACE_AROUND -> AlignContent.SPACE_AROUND;
            case SPACE_EVENLY -> AlignContent.SPACE_EVENLY;
            case START -> AlignContent.START;
            case END -> AlignContent.END;
            case STRETCH -> AlignContent.STRETCH;
        };
    }

    //endregion

    //region formatting

    /**
     * Formats a LengthPercentage value for display.
     */
    static String formatLengthPercentage(@Nullable LengthPercentage value) {
        if (value == null) {
            return "null";
        }
        if (value == LengthPercentage.ZERO) {
            return "0";
        }
        return value.toString();
    }

    /**
     * Formats a LengthPercentageAuto value for display.
     */
    static String formatLengthPercentageAuto(@Nullable LengthPercentageAuto value) {
        if (value == null) {
            return "null";
        }
        if (value == LengthPercentageAuto.ZERO) {
            return "0";
        }
        if (value == LengthPercentageAuto.AUTO) {
            return "auto";
        }
        return value.toString();
    }

    //endregion
}
