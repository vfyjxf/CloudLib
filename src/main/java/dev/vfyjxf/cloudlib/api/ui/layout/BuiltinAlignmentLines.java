package dev.vfyjxf.cloudlib.api.ui.layout;

public class BuiltinAlignmentLines {

    public static final AlignmentLine FIRST_BASELINE = new AlignmentLine.VerticalAlignmentLine(Math::min);
    public static final AlignmentLine LAST_BASELINE = new AlignmentLine.VerticalAlignmentLine(Math::max);

}
