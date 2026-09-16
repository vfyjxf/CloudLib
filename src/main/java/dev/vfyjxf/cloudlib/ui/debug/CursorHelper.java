package dev.vfyjxf.cloudlib.ui.debug;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches GLFW standard cursors and applies them to the game window.
 * Used by resize handles / splitter to show resize cursors on hover.
 */
final class CursorHelper {

    static final int arrow = GLFW.GLFW_ARROW_CURSOR;
    static final int hresize = GLFW.GLFW_HRESIZE_CURSOR;
    static final int vresize = GLFW.GLFW_VRESIZE_CURSOR;
    static final int nwse = GLFW.GLFW_RESIZE_NWSE_CURSOR;
    static final int nesw = GLFW.GLFW_RESIZE_NESW_CURSOR;

    private static final Map<Integer, Long> cache = new HashMap<>();
    private static int current = arrow;

    static void set(int shape) {
        if (shape == current) return;
        current = shape;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (shape == arrow) {
            GLFW.glfwSetCursor(window, 0L);
            return;
        }
        long cursor = cache.computeIfAbsent(shape, GLFW::glfwCreateStandardCursor);
        GLFW.glfwSetCursor(window, cursor);
    }

    static void reset() {
        set(arrow);
    }
}
