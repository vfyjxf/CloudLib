package dev.vfyjxf.cloudlib.integration.moddevmcp;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("CloudLibSceneAccessor Tests")
class CloudLibSceneAccessorTest {

    @Test
    @DisplayName("BasicScreen uses scene() when available")
    void basicScreenUsesSceneMethod() throws Exception {
        Scene overrideScene = new Scene(new WidgetGroup<>());
        Scene fallbackScene = new Scene(new WidgetGroup<>());
        BasicScreen screen = new TestBasicScreen(overrideScene);
        setField(BasicScreen.class, "scene", screen, fallbackScene);

        Scene result = CloudLibSceneAccessor.tryGetScene(screen);

        assertSame(overrideScene, result);
    }

    @Test
    @DisplayName("BasicScreen falls back to field when scene() fails")
    void basicScreenFallsBackToField() throws Exception {
        Scene expected = new Scene(new WidgetGroup<>());
        BasicScreen screen = new TestBasicScreen(null);
        setField(BasicScreen.class, "scene", screen, expected);

        Scene result = CloudLibSceneAccessor.tryGetScene(screen);

        assertSame(expected, result);
    }

    private static void setField(Class<?> owner, String name, Object target, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestBasicScreen extends BasicScreen {
        private final Scene overrideScene;

        private TestBasicScreen(Scene overrideScene) {
            super();
            this.overrideScene = overrideScene;
        }

        @Override
        protected Scene scene() {
            return overrideScene;
        }
    }
}
