package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

final class OverlayTestMinecrafts {

    private OverlayTestMinecrafts() {}

    static Minecraft dummyMinecraft() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            return (Minecraft) unsafe.allocateInstance(Minecraft.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to allocate Minecraft instance for test", e);
        }
    }

    static OverlayContext dummyContext(@Nullable Screen screen) {
        int width = screen == null ? 0 : screen.width;
        int height = screen == null ? 0 : screen.height;
        return new OverlayContext(screen, dummyMinecraft(), width, height, 1.0);
    }

    static OverlayManager newManager(OverlayRegisterImpl register) {
        return new OverlayManager(register.entries.values(), OverlayTestMinecrafts::dummyContext);
    }

    static final class TestScreen extends Screen {
        TestScreen() {
            super(Component.empty());
        }

        TestScreen(int width, int height) {
            super(Component.empty());
            this.width = width;
            this.height = height;
        }

        void resizeTo(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
