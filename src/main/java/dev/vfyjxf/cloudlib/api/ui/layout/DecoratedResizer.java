package dev.vfyjxf.cloudlib.api.ui.layout;

public abstract class DecoratedResizer extends BasicResizer {
    public Resizer resizer;

    public DecoratedResizer(Resizer resizer) {
        this.resizer = resizer;
    }
}