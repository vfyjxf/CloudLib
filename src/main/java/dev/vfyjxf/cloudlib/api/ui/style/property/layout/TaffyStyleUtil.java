package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.JustifyContent;

final class TaffyStyleUtil {

    private TaffyStyleUtil() {
    }

    static <T> void setRectEdge(TaffyRect<T> rect, Edge edge, T value) {
        switch (edge) {
            case TOP -> rect.top = value;
            case RIGHT -> rect.right = value;
            case BOTTOM -> rect.bottom = value;
            case LEFT -> rect.left = value;
        }
    }

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
}
