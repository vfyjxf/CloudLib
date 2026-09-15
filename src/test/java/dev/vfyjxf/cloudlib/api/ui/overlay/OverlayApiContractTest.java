package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayApiImpl;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayRegisterImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverlayApiContractTest {
    @Test
    void exposesEntriesAndFindsById() {
        var register = new OverlayRegisterImpl();

        OverlayEntry<Widget> entry = OverlayEntry.global("sample", c -> new Widget());
        register.register(entry);

        var api = new OverlayApiImpl(register);
        OverlayApiImpl.attach(api);

        OverlayApi overlay = OverlayApi.instance();
        assertSame(entry, overlay.find("sample"));
        assertEquals(1, overlay.entries().size());
    }
}
