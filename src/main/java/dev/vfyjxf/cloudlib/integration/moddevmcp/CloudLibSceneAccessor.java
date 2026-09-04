package dev.vfyjxf.cloudlib.integration.moddevmcp;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CloudLibSceneAccessor {
    private static final String SCENE_METHOD_NAME = "scene";
    private static final String SCENE_FIELD_NAME = "scene";

    private CloudLibSceneAccessor() {
    }

    public static Scene tryGetScene(Screen screen) {
        if (screen == null) {
            return null;
        }

        if (!(screen instanceof BasicScreen)) {
            return null;
        }

        Scene scene = invokeSceneMethod(screen);
        if (scene != null) {
            return scene;
        }
        return readSceneField(screen, BasicScreen.class);
    }

    private static Scene invokeSceneMethod(Screen screen) {
        Method method = findSceneMethod(screen.getClass());
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(screen);
            return value instanceof Scene ? (Scene) value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static Method findSceneMethod(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(SCENE_METHOD_NAME);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (RuntimeException exception) {
                return null;
            }
        }
        return null;
    }

    private static Scene readSceneField(Object screen, Class<?> owner) {
        try {
            Field field = owner.getDeclaredField(SCENE_FIELD_NAME);
            field.setAccessible(true);
            Object value = field.get(screen);
            return value instanceof Scene ? (Scene) value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
