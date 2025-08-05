package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.state.MutableState;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

final class StateCollector implements StateUsage {

    final ObjectSet<MutableState<?>> states = new ObjectLinkedOpenHashSet<>();

    @Override
    public void use(MutableState<?> state) {
        states.add(state);
    }
}
