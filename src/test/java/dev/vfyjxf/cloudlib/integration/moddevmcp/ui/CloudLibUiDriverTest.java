package dev.vfyjxf.cloudlib.integration.moddevmcp.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CloudLibUiDriver Tests")
class CloudLibUiDriverTest {

    @Test
    @DisplayName("matches() rejects non-BasicScreen contexts")
    void matchesRejectsNonBasicScreen() throws Exception {
        Class<?> driverClass = Class.forName("dev.vfyjxf.cloudlib.integration.moddevmcp.ui.CloudLibUiDriver");
        Object driver = driverClass.getDeclaredConstructor().newInstance();
        Method matches = driverClass.getMethod("matches", UiContext.class);
        UiContext context = new TestContext(new Screen(Component.empty()) {
        }, "custom.UnknownScreen");

        boolean result = (boolean) matches.invoke(driver, context);

        assertFalse(result);
    }

    @Test
    @DisplayName("snapshot() emits unique target ids for repeated sibling widgets")
    void snapshotEmitsUniqueTargetIdsForRepeatedSiblingWidgets() {
        TestWidgetGroup root = new TestWidgetGroup();
        TestWidgetGroup container = root.addChild(new TestWidgetGroup());
        container.addChild(new Widget());
        container.addChild(new Widget());
        container.addChild(new Widget());

        Scene scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(new TestSceneHost()));

        CloudLibUiDriver driver = new CloudLibUiDriver();
        var snapshot = driver.snapshot(
                new TestContext(new TestBasicScreen(scene), TestBasicScreen.class.getName()),
                SnapshotOptions.DEFAULT
        );

        Map<String, Long> duplicateCounts = snapshot.targets().stream()
                .map(target -> target.targetId())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertTrue(duplicateCounts.isEmpty(), "duplicate target ids: " + duplicateCounts);
    }

    private static final class TestContext implements UiContext {
        private final Object screen;
        private final String screenClass;

        private TestContext(Object screen, String screenClass) {
            this.screen = screen;
            this.screenClass = screenClass;
        }

        @Override
        public String screenClass() {
            return screenClass;
        }

        @Override
        public Object screenHandle() {
            return screen;
        }
    }

    private static final class TestBasicScreen extends BasicScreen {
        private final Scene overrideScene;

        private TestBasicScreen(Scene overrideScene) {
            this.overrideScene = overrideScene;
        }

        @Override
        protected Scene scene() {
            return overrideScene;
        }
    }

    private static final class TestWidgetGroup extends WidgetGroup<Widget> {
        private <W extends Widget> W addChild(W child) {
            return addWidget(child);
        }
    }

    private static final class TestSceneHost implements SceneHost {
        @Override
        public Font font() {
            return null;
        }

        @Override
        public int width() {
            return 320;
        }

        @Override
        public int height() {
            return 180;
        }
    }
}
