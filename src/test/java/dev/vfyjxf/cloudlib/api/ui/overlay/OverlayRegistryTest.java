package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayApiImpl;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayRegisterImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverlayRegistryTest {
    @Test
    void registersAndFindsEntriesById() {
        var register = new OverlayRegisterImpl();
        OverlayEntry<Widget> entry = testEntry("test");
        register.register(entry);

        var api = new OverlayApiImpl(register);
        assertSame(entry, api.find("test"));
    }

    @Test
    void rejectsDuplicateIds() {
        var register = new OverlayRegisterImpl();
        register.register(testEntry("dup"));

        assertThrows(IllegalArgumentException.class, () -> register.register(testEntry("dup")));
    }

    @Test
    void findReturnsNullWhenMissing() {
        var register = new OverlayRegisterImpl();
        var api = new OverlayApiImpl(register);

        assertNull(api.find("missing"));
    }

    @Test
    void entriesReturnsRegisteredEntries() {
        var register = new OverlayRegisterImpl();
        register.register(testEntry("entry"));

        var api = new OverlayApiImpl(register);
        assertEquals(1, api.entries().size());
    }

    @Test
    void registerRejectsNull() {
        var register = new OverlayRegisterImpl();

        assertThrows(NullPointerException.class, () -> register.register(null));
    }

    private static OverlayEntry<Widget> testEntry(String id) {
        return OverlayEntry.global(id, context -> new Widget());
    }
}
