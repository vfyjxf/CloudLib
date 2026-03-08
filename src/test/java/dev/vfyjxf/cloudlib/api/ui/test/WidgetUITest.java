package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widget.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the UI test framework.
 * <p>
 * Tests real widgets (ButtonWidget, ToggleWidget, SliderWidget, TextFieldWidget, LabelWidget)
 * through the TestScene facade, exercising finders, assertions, input simulation,
 * tree snapshots and diffs.
 */
class WidgetUITest {

    // ==================== Helper: create a basic scene with standard widgets ====================

    private TestScene createBasicScene() {
        var scene = TestScene.create(800, 600);

        var btn = ButtonWidget.of("OK");
        scene.add(btn, "ok", 10, 10, 80, 30);
        btn.onClick(() -> {});

        var toggle = ToggleWidget.create(false);
        scene.add(toggle, "toggle", 10, 50, 40, 20);

        var slider = SliderWidget.create(0.0, 100.0, 50.0);
        scene.add(slider, "slider", 10, 80, 200, 20);

        var label = LabelWidget.of("Status: Ready");
        scene.add(label, "status", 10, 110, 100, 15);

        return scene;
    }

    // ==================== Lifecycle ====================

    @Nested
    class LifecycleTest {

        @Test
        void setupInitsMountsAndLayoutsWidgets() {
            var scene = createBasicScene();
            scene.setup();

            // After setup, widgets should be findable and visible
            scene.assertExists("ok");
            scene.assertExists("toggle");
            scene.assertExists("slider");
            scene.assertExists("status");
        }

        @Test
        void emptySceneSetup() {
            var scene = TestScene.create(400, 300);
            scene.setup();
            // Root WidgetGroup itself is a Widget, so byType(Widget.class) returns 1 (the root).
            // An empty scene has no children.
            assertEquals(0, scene.root().children().size());
        }

        @Test
        void addWidgetAfterSetup() {
            var scene = TestScene.create(800, 600);
            scene.setup();

            // Add widget after initial setup
            var btn = ButtonWidget.of("Late");
            scene.add(btn, "late", 0, 0, 60, 20);

            // Need to rebuild for the new widget
            scene.rebuild();

            scene.assertExists("late");
        }
    }

    // ==================== Finder ====================

    @Nested
    class FinderTest {

        @Test
        void findByKey() {
            var scene = createBasicScene();
            scene.setup();

            Widget found = scene.find("ok");
            assertNotNull(found);
            assertTrue(found instanceof ButtonWidget);
        }

        @Test
        void findByType() {
            var scene = createBasicScene();
            scene.setup();

            var buttons = scene.findAll(WidgetFinder.byType(ButtonWidget.class));
            assertEquals(1, buttons.size());

            var labels = scene.findAll(WidgetFinder.byType(LabelWidget.class));
            assertEquals(1, labels.size());
        }

        @Test
        void findByText() {
            var scene = createBasicScene();
            scene.setup();

            Widget found = scene.find(WidgetFinder.byText("Status: Ready"));
            assertNotNull(found);
            assertTrue(found instanceof LabelWidget);
        }

        @Test
        void findByPredicate() {
            var scene = createBasicScene();
            scene.setup();

            var visible = scene.findAll(WidgetFinder.where(Widget::visible));
            // All widgets are visible by default
            assertTrue(visible.size() >= 4);
        }

        @Test
        void findComposition_and() {
            var scene = createBasicScene();
            scene.setup();

            // Find ButtonWidget with key "ok"
            Widget found = scene.find(
                    WidgetFinder.byType(ButtonWidget.class).and(WidgetFinder.byKey("ok"))
            );
            assertNotNull(found);
        }

        @Test
        void findComposition_within() {
            var scene = TestScene.create(800, 600);

            // Create a nested structure: root > toolbar > btn1, btn2
            var toolbar = new WidgetGroup<>();
            TestScene.setKey(toolbar, "toolbar");
            TestScene.setBound(toolbar, 0, 0, 400, 40);

            var btn1 = ButtonWidget.of("Add");
            scene.add(btn1, "btn1", 0, 0, 60, 30);

            var btn2 = ButtonWidget.of("Delete");
            scene.add(btn2, "btn2", 70, 0, 60, 30);

            // Add buttons to toolbar, toolbar to root
            toolbar.addWidget(btn1);
            toolbar.addWidget(btn2);
            scene.root().addWidget(toolbar);

            // Also add a button outside toolbar
            var outsideBtn = ButtonWidget.of("Outside");
            scene.add(outsideBtn, "outside", 0, 50, 60, 30);

            scene.setup();

            // Find buttons within toolbar
            var toolbarButtons = scene.findAll(
                    WidgetFinder.byType(ButtonWidget.class).within(WidgetFinder.byKey("toolbar"))
            );
            assertEquals(2, toolbarButtons.size());
        }

        @Test
        void findNotExisting() {
            var scene = createBasicScene();
            scene.setup();

            assertFalse(scene.exists("nonexistent"));
            assertThrows(AssertionError.class, () -> scene.find("nonexistent"));
        }

        @Test
        void countWidgets() {
            var scene = createBasicScene();
            scene.setup();

            assertEquals(1, scene.count(WidgetFinder.byType(ButtonWidget.class)));
            assertEquals(1, scene.count(WidgetFinder.byType(ToggleWidget.class)));
            assertEquals(1, scene.count(WidgetFinder.byType(SliderWidget.class)));
            assertEquals(1, scene.count(WidgetFinder.byType(LabelWidget.class)));
        }
    }

    // ==================== Assertions ====================

    @Nested
    class AssertionTest {

        @Test
        void basicWidgetAssertions() {
            var scene = createBasicScene();
            scene.setup();

            scene.assertThat("ok")
                    .isVisible()
                    .hasPos(10, 10)
                    .hasSize(80, 30);
        }

        @Test
        void buttonLabelAssert() {
            var scene = createBasicScene();
            scene.setup();

            scene.assertThat("ok")
                    .hasLabel("OK");
        }

        @Test
        void toggleStateAssert() {
            var scene = createBasicScene();
            scene.setup();

            scene.assertThat("toggle")
                    .isNotToggled();
        }

        @Test
        void sliderValueAssert() {
            var scene = createBasicScene();
            scene.setup();

            scene.assertThat("slider")
                    .hasSliderValue(50.0);
        }

        @Test
        void labelTextAssert() {
            var scene = createBasicScene();
            scene.setup();

            scene.assertThat("status")
                    .hasText("Status: Ready");
        }

        @Test
        void childrenAssert() {
            var scene = createBasicScene();
            scene.setup();

            // Root should have 4 children
            var rootAssert = new WidgetAssert(scene.root(), "root");
            rootAssert.hasChildCount(4);
        }
    }

    // ==================== Button behavior ====================

    @Nested
    class ButtonBehaviorTest {

        @Test
        void tapButtonTriggersOnClick() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Click Me");
            btn.onClick(() -> clicked.set(true));
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            assertFalse(clicked.get());

            scene.tap("btn");
            assertTrue(clicked.get(), "Button onClick should have been triggered");
        }

        @Test
        void disabledButtonDoesNotClick() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Disabled");
            btn.onClick(() -> clicked.set(true));
            btn.setEnabled(false);
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            scene.tap("btn");

            assertFalse(clicked.get(), "Disabled button should not click");
        }

        @Test
        void multipleButtonClicks() {
            var scene = TestScene.create(800, 600);
            var count = new AtomicInteger(0);

            var btn = ButtonWidget.of("Counter");
            btn.onClick(count::incrementAndGet);
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            scene.tap("btn");
            scene.tap("btn");
            scene.tap("btn");

            assertEquals(3, count.get());
        }
    }

    // ==================== Toggle behavior ====================

    @Nested
    class ToggleBehaviorTest {

        @Test
        void tapToggle() {
            var scene = TestScene.create(800, 600);

            var toggle = ToggleWidget.create(false);
            scene.add(toggle, "toggle", 10, 10, 40, 20);

            scene.setup();
            assertFalse(toggle.toggled());

            scene.tap("toggle");
            assertTrue(toggle.toggled(), "Toggle should be ON after tap");

            scene.tap("toggle");
            assertFalse(toggle.toggled(), "Toggle should be OFF after second tap");
        }

        @Test
        void toggleCallbackFired() {
            var scene = TestScene.create(800, 600);
            var states = new ArrayList<Boolean>();

            var toggle = ToggleWidget.create(false);
            toggle.onToggle(states::add);
            scene.add(toggle, "toggle", 10, 10, 40, 20);

            scene.setup();
            scene.tap("toggle");
            scene.tap("toggle");

            assertEquals(List.of(true, false), states);
        }
    }

    // ==================== Slider behavior ====================

    @Nested
    class SliderBehaviorTest {

        @Test
        void sliderInitialValue() {
            var scene = TestScene.create(800, 600);

            var slider = SliderWidget.create(0, 100, 25);
            scene.add(slider, "slider", 10, 10, 200, 20);

            scene.setup();
            assertEquals(25.0, slider.value());
        }

        @Test
        void sliderSetValue() {
            var scene = TestScene.create(800, 600);
            var values = new ArrayList<Double>();

            var slider = SliderWidget.create(0, 100, 0);
            slider.onValueChanged(values::add);
            scene.add(slider, "slider", 10, 10, 200, 20);

            scene.setup();
            slider.setValue(75);

            scene.assertThat("slider").hasSliderValue(75.0);
        }

        @Test
        void sliderValueClamping() {
            var slider = SliderWidget.create(0, 100, 50);
            slider.setValue(150);
            assertEquals(100.0, slider.value(), "Value should be clamped to max");

            slider.setValue(-50);
            assertEquals(0.0, slider.value(), "Value should be clamped to min");
        }
    }

    // ==================== TextFieldWidget behavior ====================

    @Nested
    class TextFieldBehaviorTest {

        @Test
        void textFieldInitialState() {
            var scene = TestScene.create(800, 600);

            var field = TextFieldWidget.create("Hello");
            scene.add(field, "input", 10, 10, 200, 20);

            scene.setup();
            assertEquals("Hello", field.text());
            assertTrue(field.editable());
        }

        @Test
        void textFieldSetText() {
            var scene = TestScene.create(800, 600);
            var changes = new ArrayList<String>();

            var field = TextFieldWidget.create();
            field.onTextChanged(changes::add);
            scene.add(field, "input", 10, 10, 200, 20);

            scene.setup();
            field.setText("World");
            assertEquals("World", field.text());
        }

        @Test
        void textFieldMaxLength() {
            var field = TextFieldWidget.create();
            field.setMaxLength(5);

            field.setText("Hello World");
            // setText doesn't enforce maxLength, it's enforced only on charTyped
            assertEquals("Hello World", field.text());
        }

        @Test
        void textFieldEditableFlag() {
            var field = TextFieldWidget.create("test");
            assertTrue(field.editable());

            field.setEditable(false);
            assertFalse(field.editable());
        }
    }

    // ==================== Tree structure assertions ====================

    @Nested
    class TreeStructureTest {

        @Test
        void assertSimpleTree() {
            var scene = TestScene.create(800, 600);

            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.add(LabelWidget.of("B"), "b", 60, 0, 50, 15);

            scene.setup();

            scene.assertTree(tree -> tree
                    .widget(ButtonWidget.class, "a")
                    .widget(LabelWidget.class, "b")
            );
        }

        @Test
        void assertNestedTree() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "group1");
            TestScene.setBound(group, 0, 0, 400, 200);

            var inner = ButtonWidget.of("Inner");
            inner.setKey("inner");
            inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(60, 20)));
            group.addWidget(inner);

            scene.root().addWidget(group);
            scene.setup();

            scene.assertTree(tree -> tree
                    .group(WidgetGroup.class, "group1", g -> g
                            .widget(ButtonWidget.class, "inner")
                    )
            );
        }

        @Test
        void wrongChildCountFails() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.setup();

            assertThrows(AssertionError.class, () ->
                    scene.assertTree(tree -> tree
                            .widget(ButtonWidget.class, "a")
                            .widget(ButtonWidget.class, "b")  // doesn't exist
                    )
            );
        }
    }

    // ==================== Tree snapshot & diff ====================

    @Nested
    class TreeSnapshotTest {

        @Test
        void snapshotCapturesTree() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.add(LabelWidget.of("B"), "b", 60, 0, 50, 15);
            scene.setup();

            TreeSnapshot snap = scene.snapshot();
            assertNotNull(snap.root());
            assertEquals("WidgetGroup", snap.root().type());
            assertEquals(2, snap.root().children().size());
        }

        @Test
        void snapshotFindByKey() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "btn-a", 0, 0, 50, 20);
            scene.setup();

            TreeSnapshot snap = scene.snapshot();
            var node = snap.findByKey("btn-a");
            assertNotNull(node);
            assertEquals("ButtonWidget", node.type());
        }

        @Test
        void snapshotPrint() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.setup();

            String tree = scene.snapshot().print();
            assertTrue(tree.contains("WidgetGroup"), "Print should include root type");
            assertTrue(tree.contains("ButtonWidget"), "Print should include child type");
        }

        @Test
        void diffDetectsNoChange() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.setup();

            var before = scene.snapshot();
            // No changes
            scene.assertDiff(before).unchanged();
        }

        @Test
        void diffDetectsPropertyChange() {
            var scene = TestScene.create(800, 600);
            var label = LabelWidget.of("Before");
            scene.add(label, "lbl", 0, 0, 100, 15);
            scene.setup();

            var before = scene.snapshot();

            // Change label text
            label.setText("After");

            scene.assertDiff(before)
                    .updated("lbl");
        }

        @Test
        void diffDetectsAddedWidget() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.setup();

            var before = scene.snapshot();

            // Add a new widget
            var newBtn = ButtonWidget.of("B");
            scene.add(newBtn, "b", 60, 0, 50, 20);
            scene.rebuild();

            scene.assertDiff(before)
                    .reused("a")
                    .created("b");
        }

        @Test
        void diffReport() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.setup();

            var before = scene.snapshot();
            var report = TreeDiffAssert.create(before, scene.snapshot()).report();
            assertNotNull(report);
            assertTrue(report.contains("BEFORE"));
            assertTrue(report.contains("AFTER"));
        }
    }

    // ==================== Input sequence ====================

    @Nested
    class InputSequenceTest {

        @Test
        void sequenceTapButton() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Go");
            btn.onClick(() -> clicked.set(true));
            scene.add(btn, "go", 10, 10, 80, 30);

            scene.setup();

            scene.perform(seq -> seq
                    .moveTo("go")
                    .click()
            );

            assertTrue(clicked.get());
        }

        @Test
        void sequenceWithMidstreamAssertion() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Go");
            btn.onClick(() -> clicked.set(true));
            scene.add(btn, "go", 10, 10, 80, 30);

            scene.setup();

            scene.perform(seq -> seq
                    .then(s -> assertFalse(clicked.get(), "Should not be clicked yet"))
                    .moveTo("go")
                    .click()
                    .then(s -> assertTrue(clicked.get(), "Should be clicked now"))
            );
        }

        @Test
        void sequenceWithTicks() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 0, 0, 50, 20);
            scene.setup();

            // Just verify ticks don't throw
            scene.perform(seq -> seq
                    .tick(5)
                    .moveTo("a")
                    .click()
                    .tick(3)
            );
        }

        @Test
        void sequenceScrollWidget() {
            var scene = TestScene.create(800, 600);
            var scrolled = new AtomicBoolean(false);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "scrollable");
            TestScene.setBound(group, 0, 0, 200, 200);
            group.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
                scrolled.set(true);
                return dev.vfyjxf.cloudlib.api.event.EventDispatch.consumed;
            });
            scene.root().addWidget(group);

            scene.setup();

            scene.perform(seq -> seq
                    .moveTo("scrollable")
                    .scroll(0, -3)
            );

            assertTrue(scrolled.get(), "Scroll event should have been dispatched");
        }
    }

    // ==================== Visibility ====================

    @Nested
    class VisibilityTest {

        @Test
        void hideAndShowWidget() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Hello");
            scene.add(btn, "btn", 0, 0, 80, 30);
            scene.setup();

            scene.assertThat("btn").isVisible();

            btn.setVisible(false);
            scene.assertThat("btn").isNotVisible();

            btn.setVisible(true);
            scene.assertThat("btn").isVisible();
        }

        @Test
        void hiddenWidgetNotClickable() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Hidden");
            btn.onClick(() -> clicked.set(true));
            btn.setVisible(false);
            scene.add(btn, "btn", 10, 10, 80, 30);
            scene.setup();

            // Clicking at the button's location shouldn't trigger onClick
            // since the widget is hidden and hitTest should skip it
            scene.tapAt(50, 25);
            assertFalse(clicked.get());
        }
    }

    // ==================== Complex scene ====================

    @Nested
    class ComplexSceneTest {

        @Test
        void multipleWidgetInteractions() {
            var scene = TestScene.create(800, 600);
            var log = new ArrayList<String>();

            var btn = ButtonWidget.of("Submit");
            btn.onClick(() -> log.add("submit"));
            scene.add(btn, "submit", 10, 10, 80, 30);

            var toggle = ToggleWidget.create(false);
            toggle.onToggle(on -> log.add("toggle:" + on));
            scene.add(toggle, "agree", 10, 50, 40, 20);

            scene.setup();

            scene.tap("agree");
            scene.tap("submit");
            scene.tap("agree");

            assertEquals(List.of("toggle:true", "submit", "toggle:false"), log);
        }

        @Test
        void widgetCountByType() {
            var scene = TestScene.create(800, 600);

            for (int i = 0; i < 5; i++) {
                var btn = ButtonWidget.of("Btn " + i);
                scene.add(btn, "btn" + i, i * 70, 0, 60, 20);
            }
            for (int i = 0; i < 3; i++) {
                var lbl = LabelWidget.of("Label " + i);
                scene.add(lbl, "lbl" + i, i * 100, 30, 80, 15);
            }

            scene.setup();

            scene.assertCount(WidgetFinder.byType(ButtonWidget.class), 5);
            scene.assertCount(WidgetFinder.byType(LabelWidget.class), 3);
        }

        @Test
        void printTree() {
            var scene = createBasicScene();
            scene.setup();

            String tree = scene.printTree();
            assertNotNull(tree);
            assertTrue(tree.contains("WidgetGroup"));
            assertTrue(tree.contains("ButtonWidget"));
        }
    }

    // ==================== PathFinder ====================

    @Nested
    class PathFinderTest {

        @Test
        void findBySimplePath() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "panel");
            TestScene.setBound(group, 0, 0, 400, 200);

            var btn = ButtonWidget.of("OK");
            btn.setKey("ok");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(60, 20)));
            group.addWidget(btn);

            scene.root().addWidget(group);
            scene.setup();

            // Path: WidgetGroup / ButtonWidget
            var found = scene.find(WidgetFinder.path("WidgetGroup / ButtonWidget"));
            assertNotNull(found);
            assertTrue(found instanceof ButtonWidget);
        }

        @Test
        void findByKeyPath() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "panel");
            TestScene.setBound(group, 0, 0, 400, 200);

            var btn = ButtonWidget.of("OK");
            btn.setKey("ok");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(60, 20)));
            group.addWidget(btn);

            scene.root().addWidget(group);
            scene.setup();

            // Path: [key=panel] / [key=ok]
            var found = scene.find(WidgetFinder.path("[key=panel] / [key=ok]"));
            assertNotNull(found);
            assertTrue(found instanceof ButtonWidget);
        }

        @Test
        void findByWildcardPath() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "panel");
            TestScene.setBound(group, 0, 0, 400, 200);

            var btn = ButtonWidget.of("OK");
            btn.setKey("ok");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(60, 20)));
            group.addWidget(btn);

            scene.root().addWidget(group);
            scene.setup();

            // Path: ** / ButtonWidget[key=ok]
            var found = scene.find(WidgetFinder.path("** / ButtonWidget[key=ok]"));
            assertNotNull(found);
        }

        @Test
        void findByIndex() {
            var scene = TestScene.create(800, 600);

            var a = ButtonWidget.of("A");
            a.setKey("a");
            a.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(50, 20)));
            scene.root().addWidget(a);

            var b = ButtonWidget.of("B");
            b.setKey("b");
            b.useStyle(UIStyle.of(positionAbsolute(), insetLeft(60), insetTop(0), sizeOf(50, 20)));
            scene.root().addWidget(b);

            scene.setup();

            // Path: [1] - second child
            var found = scene.find(WidgetFinder.path("[1]"));
            assertNotNull(found);
            assertEquals("b", found.key());
        }
    }

    // ==================== WidgetAssert edge cases ====================

    @Nested
    class WidgetAssertEdgeCaseTest {

        @Test
        void typedAssert() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("X"), "x", 0, 0, 50, 20);
            scene.setup();

            scene.assertWidget("x", a -> a
                    .isVisible()
                    .isOfType(ButtonWidget.class)
            );
        }

        @Test
        void satisfiesCustom() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Custom");
            btn.setEnabled(true);
            scene.add(btn, "custom", 0, 0, 80, 30);
            scene.setup();

            scene.assertThat("custom").satisfies(ButtonWidget.class, b -> {
                assertTrue(b.enabled());
            });
        }
    }
}
