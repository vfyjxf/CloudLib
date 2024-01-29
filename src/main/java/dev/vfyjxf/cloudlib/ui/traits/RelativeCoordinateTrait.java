package dev.vfyjxf.cloudlib.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;

import java.util.function.Supplier;

public class RelativeCoordinateTrait extends AbstractTrait {

    private IWidgetEvent.OnUpdate listener;

    private final Supplier<Integer> x;
    private final Supplier<Integer> y;

    public RelativeCoordinateTrait(Supplier<Integer> x, Supplier<Integer> y) {
        this.x = x;
        this.y = y;
    }


    @Override
    public void init() {
        listener = holder.registerListener(IWidgetEvent.onUpdate, self -> {
            self.setPos(x.get(), y.get());
        });
    }


    @Override
    public void dispose() {
        holder.events().get(IWidgetEvent.onUpdate).unregister(listener);
    }
}
