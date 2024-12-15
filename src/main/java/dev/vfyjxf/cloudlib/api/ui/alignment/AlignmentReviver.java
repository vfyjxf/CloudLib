package dev.vfyjxf.cloudlib.api.ui.alignment;

public interface AlignmentReviver<THIS extends AlignmentReviver<THIS>> {

    THIS alignment(Alignment alignment);

    THIS horizontalAlignment(Alignment.Horizontal alignment);

    THIS verticalAlignment(Alignment.Vertical alignment);

}
