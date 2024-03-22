package dev.vfyjxf.cloudlib.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

public abstract class AbstractTrait implements ITrait {

    protected IWidget holder;

    @Override
    public IWidget holder() {
        if (holder == null)
            throw new IllegalStateException("This addTrait is not attached to a widget!");
        return holder;
    }

    @Override
    public ITrait setHolder(IWidget holder) {
        if (this.holder != null)
            throw new IllegalStateException("This addTrait is already attached to a widget!");
        check(holder);

        this.holder = holder;
        return this;
    }

    protected void check(IWidget holder) {

    }

}
