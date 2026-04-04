package dev.vfyjxf.cloudlib.api.register.ui;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface OverlayRegister {

    <T extends Widget> void register(OverlayEntry<T> entry);

}
