package dev.vfyjxf.cloudlib.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;

import java.util.function.Supplier;

public class AutoSizeUpdateTrait extends AbstractTrait {

    private final Supplier<Integer> width;
    private final Supplier<Integer> height;

    private IWidgetEvent.OnUpdate listener;

    public AutoSizeUpdateTrait(Supplier<Integer> width, Supplier<Integer> height) {
        this.width = width;
        this.height = height;
    }


    @Override
    public void init() {
        listener = holder.registerListener(IWidgetEvent.onUpdate, self -> self.setSize(width.get(), height.get()));
    }


    @Override
    public void onDelete() {
        holder.events().get(IWidgetEvent.onUpdate).unregister(listener);
    }
}
