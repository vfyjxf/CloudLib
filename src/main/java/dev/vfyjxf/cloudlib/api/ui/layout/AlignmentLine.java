package dev.vfyjxf.cloudlib.api.ui.layout;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.IntBinaryOperator;

public sealed class AlignmentLine permits AlignmentLine.VerticalAlignmentLine, AlignmentLine.HorizontalAlignmentLine {

    public static final int UNSPECIFIED = Integer.MIN_VALUE;

    private final IntBinaryOperator merger;

    private AlignmentLine(IntBinaryOperator merger) {
        this.merger = merger;
    }

    @ApiStatus.Internal
    public int merge(int a, int b) {
        return merger.applyAsInt(a, b);
    }

    public static final class VerticalAlignmentLine extends AlignmentLine {
        public VerticalAlignmentLine(IntBinaryOperator merger) {
            super(merger);
        }
    }

    public static final class HorizontalAlignmentLine extends AlignmentLine {
        public HorizontalAlignmentLine(IntBinaryOperator merger) {
            super(merger);
        }
    }

    public static VerticalAlignmentLine vertical(IntBinaryOperator merger) {
        return new VerticalAlignmentLine(merger);
    }

    public static HorizontalAlignmentLine horizontal(IntBinaryOperator merger) {
        return new HorizontalAlignmentLine(merger);
    }

}
