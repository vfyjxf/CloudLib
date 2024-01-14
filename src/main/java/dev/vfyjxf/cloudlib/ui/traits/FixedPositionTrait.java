package dev.vfyjxf.cloudlib.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;

public class FixedPositionTrait extends AbstractTrait {

    private IWidgetEvent.OnPositionChanged listener;

    @Override
    public void init() {
        listener = holder.registerListener(IWidgetEvent.onPositionChanged,
                (context, position) -> context.cancel());
    }

    @Override
    public void onDelete() {
        holder.events().get(IWidgetEvent.onPositionChanged).unregister(listener);
    }
}
