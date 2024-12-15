package dev.vfyjxf.cloudlib.api.ui.layout;

//TODO:refactor resizer design
public abstract class DecoratedResizer implements Resizer, ParentResizer {
    protected Resizer resizer;

    public DecoratedResizer(Resizer resizer) {
        this.resizer = resizer;
    }
}