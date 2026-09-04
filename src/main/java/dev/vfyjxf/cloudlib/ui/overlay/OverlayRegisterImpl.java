package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.register.ui.OverlayRegister;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.util.Namespace;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class OverlayRegisterImpl implements OverlayRegister {

    final Map<Namespace, OverlayEntry<?>> entries = new HashMap<>();

    @Override
    public <T extends Widget> void register(OverlayEntry<T> entry) {
        Objects.requireNonNull(entry, "entry");
        if (entries.putIfAbsent(entry.id(), entry) != null) {
            throw new IllegalArgumentException("Duplicate overlay id: " + entry.id());
        }
    }
}
