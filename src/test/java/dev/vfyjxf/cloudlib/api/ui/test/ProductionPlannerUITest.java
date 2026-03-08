package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.GradientTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.RoundedRectTexture;
import dev.vfyjxf.cloudlib.ui.widget.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complex UI test simulating a production planning interface similar to a
 * calculator/planner tool. Tests complex layout, multi-panel interactions,
 * tab switching, layer selection, search, and breadcrumb navigation.
 */
class ProductionPlannerUITest {

    // ==================== Scene Builder ====================

    /**
     * Build a complex production planner scene with:
     * - Status bar (top)
     * - Product selector + config bar
     * - Tab navigation (Tree, Matrix, Milestones, Sankey, Blueprint)
     * - Search bar
     * - Left panel: layer list
     * - Main canvas: node graph area
     * - Bottom panel: path context + trace breadcrumbs
     */
    private TestScene createPlannerScene() {
        var scene = TestScene.create(960, 540);

        // All child positions are RELATIVE to their parent group.
        // Use scene.setTrackedBound() to survive layout re-application.

        // ====== Status Bar (top) ======
        var statusBar = new WidgetGroup<>();
        TestScene.setKey(statusBar, "statusBar");
        scene.setTrackedBound(statusBar, 0, 0, 960, 24);

        var envLabel = LabelWidget.of("Target: HV");
        TestScene.setKey(envLabel, "envLabel");
        scene.setTrackedBound(envLabel, 8, 4, 80, 16);
        statusBar.addWidget(envLabel);

        var coilLabel = LabelWidget.of("Coil: Nichrome");
        TestScene.setKey(coilLabel, "coilLabel");
        scene.setTrackedBound(coilLabel, 100, 4, 100, 16);
        statusBar.addWidget(coilLabel);

        var knowledgeToggle = ToggleWidget.create(true);
        TestScene.setKey(knowledgeToggle, "knowledgeToggle");
        scene.setTrackedBound(knowledgeToggle, 220, 4, 60, 16);
        statusBar.addWidget(knowledgeToggle);

        var sysReadyLabel = LabelWidget.of("SYS.READY");
        TestScene.setKey(sysReadyLabel, "sysReady");
        scene.setTrackedBound(sysReadyLabel, 880, 4, 70, 16);
        statusBar.addWidget(sysReadyLabel);

        scene.root().addWidget(statusBar);

        // ====== Product Selector Bar ======
        var productBar = new WidgetGroup<>();
        TestScene.setKey(productBar, "productBar");
        scene.setTrackedBound(productBar, 0, 24, 960, 36);

        var productName = LabelWidget.of("Stargate Controller (Extreme)");
        TestScene.setKey(productName, "productName");
        scene.setTrackedBound(productName, 30, 6, 300, 20);  // y=30-24=6
        productBar.addWidget(productName);

        var quantityField = TextFieldWidget.create("1");
        TestScene.setKey(quantityField, "quantity");
        scene.setTrackedBound(quantityField, 380, 6, 50, 20);
        productBar.addWidget(quantityField);

        var configBtn = ButtonWidget.of("Config");
        TestScene.setKey(configBtn, "configBtn");
        scene.setTrackedBound(configBtn, 450, 6, 100, 20);
        productBar.addWidget(configBtn);

        var calcBtn = ButtonWidget.of("Calculate");
        TestScene.setKey(calcBtn, "calcBtn");
        scene.setTrackedBound(calcBtn, 780, 6, 130, 24);
        productBar.addWidget(calcBtn);

        scene.root().addWidget(productBar);

        // ====== Tab Navigation ======
        var tabBar = new WidgetGroup<>();
        TestScene.setKey(tabBar, "tabBar");
        scene.setTrackedBound(tabBar, 0, 60, 600, 28);

        String[] tabNames = {"Tree", "Matrix", "Milestones", "Sankey", "Blueprint"};
        int tabX = 8;
        for (String name : tabNames) {
            var tabBtn = ButtonWidget.of(name);
            String key = "tab_" + name.toLowerCase();
            TestScene.setKey(tabBtn, key);
            scene.setTrackedBound(tabBtn, tabX, 4, 90, 22);  // y=64-60=4
            tabBar.addWidget(tabBtn);
            tabX += 96;
        }

        scene.root().addWidget(tabBar);

        // ====== Search Bar ======
        var searchField = TextFieldWidget.create();
        scene.add(searchField, "search", 620, 64, 200, 22);

        // ====== Left Panel: Layers ======
        var layerPanel = new WidgetGroup<>();
        TestScene.setKey(layerPanel, "layerPanel");
        scene.setTrackedBound(layerPanel, 0, 90, 100, 400);

        var layerTitle = LabelWidget.of("LAYERS");
        TestScene.setKey(layerTitle, "layerTitle");
        scene.setTrackedBound(layerTitle, 20, 4, 60, 14);  // y=94-90=4
        layerPanel.addWidget(layerTitle);

        for (int i = 3; i <= 15; i++) {
            var layerBtn = ButtonWidget.of("L" + i);
            String layerKey = "layer_" + i;
            int y = 20 + (i - 3) * 28;  // y=(110-90)+(i-3)*28 = 20+(i-3)*28
            TestScene.setKey(layerBtn, layerKey);
            scene.setTrackedBound(layerBtn, 15, y, 70, 24);
            layerPanel.addWidget(layerBtn);
        }

        scene.root().addWidget(layerPanel);

        // ====== Main Canvas Area ======
        var canvas = new WidgetGroup<>();
        TestScene.setKey(canvas, "canvas");
        scene.setTrackedBound(canvas, 100, 90, 860, 370);

        for (int i = 0; i < 5; i++) {
            var node = new WidgetGroup<>();
            String nodeKey = "node_" + i;
            TestScene.setKey(node, nodeKey);
            int nx = 20 + i * 140;           // x=(120-100)+i*140
            int ny = 50 + (i % 3) * 80;      // y=(140-90)+(i%3)*80
            scene.setTrackedBound(node, nx, ny, 120, 60);

            var nodeLabel = LabelWidget.of("Process " + (i + 1));
            TestScene.setKey(nodeLabel, nodeKey + "_label");
            scene.setTrackedBound(nodeLabel, 4, 4, 112, 12);  // relative to node
            node.addWidget(nodeLabel);

            canvas.addWidget(node);
        }

        scene.root().addWidget(canvas);

        // ====== Bottom Panel: Path Context ======
        var bottomBar = new WidgetGroup<>();
        TestScene.setKey(bottomBar, "bottomBar");
        scene.setTrackedBound(bottomBar, 0, 460, 960, 40);

        var pathLabel = LabelWidget.of("PATH CONTEXT:");
        TestScene.setKey(pathLabel, "pathLabel");
        scene.setTrackedBound(pathLabel, 8, 6, 100, 14);  // y=466-460=6
        bottomBar.addWidget(pathLabel);

        String[] pathParts = {"Assembly Line", "Metal Bender", "Autoclave", "Chemical Reactor"};
        int bx = 120;
        for (int i = 0; i < pathParts.length; i++) {
            var crumb = ButtonWidget.of(pathParts[i]);
            String crumbKey = "crumb_" + i;
            TestScene.setKey(crumb, crumbKey);
            scene.setTrackedBound(crumb, bx, 6, 110, 18);  // y=466-460=6
            bottomBar.addWidget(crumb);
            bx += 120;
        }

        var traceLabel = LabelWidget.of("TRACE:");
        TestScene.setKey(traceLabel, "traceLabel");
        scene.setTrackedBound(traceLabel, 8, 24, 50, 14);  // y=484-460=24
        bottomBar.addWidget(traceLabel);

        var traceCloseBtn = ButtonWidget.of("X");
        TestScene.setKey(traceCloseBtn, "traceClose");
        scene.setTrackedBound(traceCloseBtn, 900, 24, 20, 14);  // y=484-460=24
        bottomBar.addWidget(traceCloseBtn);

        scene.root().addWidget(bottomBar);

        // ====== Bottom Info Cards ======
        var infoBar = new WidgetGroup<>();
        TestScene.setKey(infoBar, "infoBar");
        scene.setTrackedBound(infoBar, 0, 500, 960, 40);

        var machineLabel = LabelWidget.of("Lathe  EV");
        TestScene.setKey(machineLabel, "machineInfo");
        scene.setTrackedBound(machineLabel, 100, 6, 120, 14);  // y=506-500=6
        infoBar.addWidget(machineLabel);

        var comp1 = LabelWidget.of("Micro-Component T-2A9");
        TestScene.setKey(comp1, "comp1");
        scene.setTrackedBound(comp1, 240, 6, 160, 14);
        infoBar.addWidget(comp1);

        var comp2 = LabelWidget.of("Micro-Component R-117");
        TestScene.setKey(comp2, "comp2");
        scene.setTrackedBound(comp2, 420, 6, 160, 14);
        infoBar.addWidget(comp2);

        scene.root().addWidget(infoBar);

        return scene;
    }

    // ==================== Structure Tests ====================

    @Nested
    class StructureTest {

        @Test
        void allPanelsExist() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertExists("statusBar");
            scene.assertExists("productBar");
            scene.assertExists("tabBar");
            scene.assertExists("layerPanel");
            scene.assertExists("canvas");
            scene.assertExists("bottomBar");
            scene.assertExists("infoBar");
        }

        @Test
        void statusBarWidgets() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertThat("envLabel").hasText("Target: HV");
            scene.assertThat("coilLabel").hasText("Coil: Nichrome");
            scene.assertThat("knowledgeToggle").isToggled();
            scene.assertThat("sysReady").hasText("SYS.READY");
        }

        @Test
        void tabBarHasFiveTabs() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertExists("tab_tree");
            scene.assertExists("tab_matrix");
            scene.assertExists("tab_milestones");
            scene.assertExists("tab_sankey");
            scene.assertExists("tab_blueprint");

            var tabs = scene.findAll(
                    WidgetFinder.byType(ButtonWidget.class)
                            .within(WidgetFinder.byKey("tabBar"))
            );
            assertEquals(5, tabs.size());
        }

        @Test
        void layerPanelHas13Layers() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertExists("layerTitle");
            scene.assertThat("layerTitle").hasText("LAYERS");

            var layers = scene.findAll(
                    WidgetFinder.byType(ButtonWidget.class)
                            .within(WidgetFinder.byKey("layerPanel"))
            );
            assertEquals(13, layers.size()); // L3 to L15

            // Check first and last
            scene.assertThat("layer_3").hasLabel("L3");
            scene.assertThat("layer_15").hasLabel("L15");
        }

        @Test
        void canvasHasProcessNodes() {
            var scene = createPlannerScene();
            scene.setup();

            for (int i = 0; i < 5; i++) {
                scene.assertExists("node_" + i);
                scene.assertExists("node_" + i + "_label");
            }
        }

        @Test
        void breadcrumbPath() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertThat("crumb_0").hasLabel("Assembly Line");
            scene.assertThat("crumb_1").hasLabel("Metal Bender");
            scene.assertThat("crumb_2").hasLabel("Autoclave");
            scene.assertThat("crumb_3").hasLabel("Chemical Reactor");
        }

        @Test
        void bottomInfoCards() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertThat("machineInfo").hasText("Lathe  EV");
            scene.assertThat("comp1").hasText("Micro-Component T-2A9");
            scene.assertThat("comp2").hasText("Micro-Component R-117");
        }

        @Test
        void treeStructureVerification() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertTree(tree -> tree
                    .group(WidgetGroup.class, "statusBar", s -> {})
                    .group(WidgetGroup.class, "productBar", p -> {})
                    .group(WidgetGroup.class, "tabBar", t -> {})
                    .widget(TextFieldWidget.class, "search")
                    .group(WidgetGroup.class, "layerPanel", l -> {})
                    .group(WidgetGroup.class, "canvas", c -> {})
                    .group(WidgetGroup.class, "bottomBar", b -> {})
                    .group(WidgetGroup.class, "infoBar", i -> {})
            );
        }
    }

    // ==================== Interaction Tests ====================

    @Nested
    class InteractionTest {

        @Test
        void clickCalcButton() {
            var scene = createPlannerScene();
            var clicked = new AtomicBoolean(false);
            var calcBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("calcBtn"));
            calcBtn.onClick(() -> clicked.set(true));
            scene.setup();

            scene.tap("calcBtn");
            assertTrue(clicked.get(), "Calculate button should have been clicked");
        }

        @Test
        void clickCalcButtonWithEventRecorder() {
            var scene = createPlannerScene();
            var calcBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("calcBtn"));
            var rec = EventRecorder.on(calcBtn);
            scene.setup();

            scene.tap("calcBtn");
            rec.assertFired("click");
            rec.assertFiredTimes("click", 1);
        }

        @Test
        void typeInSearchField() {
            var scene = createPlannerScene();
            scene.setup();

            scene.typeText("search", "Chemical Reactor");
            scene.assertThat("search").hasValue("Chemical Reactor");
        }

        @Test
        void typeInQuantityField() {
            var scene = createPlannerScene();
            scene.setup();

            // Clear existing text and type new
            scene.typeText("quantity", "64");
            scene.assertThat("search").isNotFocused();
        }

        @Test
        void toggleKnowledgeSwitch() {
            var scene = createPlannerScene();
            var toggled = new AtomicReference<Boolean>();
            var toggle = (ToggleWidget) scene.find(WidgetFinder.byKey("knowledgeToggle"));
            toggle.onToggle(toggled::set);
            scene.setup();

            scene.assertThat("knowledgeToggle").isToggled();
            scene.tap("knowledgeToggle");
            scene.assertThat("knowledgeToggle").isNotToggled();
            assertNotNull(toggled.get());
            assertFalse(toggled.get());
        }

        @Test
        void clickTabsSwitchesActiveState() {
            var scene = createPlannerScene();
            var activeTab = new AtomicReference<>("tree");
            String[] tabNames = {"tree", "matrix", "milestones", "sankey", "blueprint"};

            for (String name : tabNames) {
                var btn = (ButtonWidget) scene.find(WidgetFinder.byKey("tab_" + name));
                btn.onClick(() -> activeTab.set(name));
            }

            scene.setup();

            // Click each tab and verify
            for (String name : tabNames) {
                scene.tap("tab_" + name);
                assertEquals(name, activeTab.get(), "Active tab should be " + name);
            }
        }

        @Test
        void clickLayerButtons() {
            var scene = createPlannerScene();
            var selectedLayer = new AtomicInteger(-1);

            for (int i = 3; i <= 15; i++) {
                int layer = i;
                var btn = (ButtonWidget) scene.find(WidgetFinder.byKey("layer_" + i));
                btn.onClick(() -> selectedLayer.set(layer));
            }

            scene.setup();

            scene.tap("layer_5");
            assertEquals(5, selectedLayer.get());

            scene.tap("layer_10");
            assertEquals(10, selectedLayer.get());
        }

        @Test
        void clickBreadcrumbs() {
            var scene = createPlannerScene();
            var navPath = new AtomicReference<>("");

            String[] pathParts = {"Assembly Line", "Metal Bender", "Autoclave", "Chemical Reactor"};
            for (int i = 0; i < pathParts.length; i++) {
                int idx = i;
                var crumb = (ButtonWidget) scene.find(WidgetFinder.byKey("crumb_" + i));
                crumb.onClick(() -> navPath.set(pathParts[idx]));
            }

            scene.setup();

            scene.tap("crumb_2");
            assertEquals("Autoclave", navPath.get());

            scene.tap("crumb_0");
            assertEquals("Assembly Line", navPath.get());
        }

        @Test
        void closeTraceButton() {
            var scene = createPlannerScene();
            var closed = new AtomicBoolean(false);
            var closeBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("traceClose"));
            closeBtn.onClick(() -> closed.set(true));
            scene.setup();

            scene.tap("traceClose");
            assertTrue(closed.get());
        }
    }

    // ==================== Complex Input Sequence Tests ====================

    @Nested
    class ComplexInputTest {

        @Test
        void tabNavigationSequence() {
            var scene = createPlannerScene();
            var tabClicks = new ArrayList<String>();

            String[] tabNames = {"tree", "matrix", "milestones", "sankey", "blueprint"};
            for (String name : tabNames) {
                var btn = (ButtonWidget) scene.find(WidgetFinder.byKey("tab_" + name));
                btn.onClick(() -> tabClicks.add(name));
            }

            scene.setup();

            // Rapid tab switching via InputSequence
            InputSequence.begin()
                    .moveTo("tab_tree").click().tick(2)
                    .moveTo("tab_matrix").click().tick(2)
                    .moveTo("tab_sankey").click().tick(2)
                    .moveTo("tab_blueprint").click().tick(2)
                    .moveTo("tab_tree").click()
                    .executeOn(scene);

            assertEquals(List.of("tree", "matrix", "sankey", "blueprint", "tree"), tabClicks);
        }

        @Test
        void searchAndCalculateWorkflow() {
            var scene = createPlannerScene();
            var calculated = new AtomicBoolean(false);
            var calcBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("calcBtn"));
            calcBtn.onClick(() -> calculated.set(true));
            scene.setup();

            // Type search, then click calculate
            InputSequence.begin()
                    .moveTo("search").click()
                    .typeText("Stargate")
                    .tick(3)
                    .moveTo("quantity").click()
                    .typeText("4")
                    .tick(2)
                    .moveTo("calcBtn").click()
                    .executeOn(scene);

            scene.assertThat("search").hasValue("Stargate");
            assertTrue(calculated.get());
        }

        @Test
        void layerScanSequence() {
            var scene = createPlannerScene();
            var layerClicks = new ArrayList<Integer>();

            for (int i = 3; i <= 15; i++) {
                int layer = i;
                var btn = (ButtonWidget) scene.find(WidgetFinder.byKey("layer_" + i));
                btn.onClick(() -> layerClicks.add(layer));
            }

            scene.setup();

            // Click layers in sequence: 3, 5, 8, 12, 15
            var seq = InputSequence.begin();
            seq.moveTo("layer_3").click().tick(1);
            seq.moveTo("layer_5").click().tick(1);
            seq.moveTo("layer_8").click().tick(1);
            seq.moveTo("layer_12").click().tick(1);
            seq.moveTo("layer_15").click();
            seq.executeOn(scene);

            assertEquals(List.of(3, 5, 8, 12, 15), layerClicks);
        }

        @Test
        void breadcrumbNavigationSequence() {
            var scene = createPlannerScene();
            var navHistory = new ArrayList<String>();

            String[] pathParts = {"Assembly Line", "Metal Bender", "Autoclave", "Chemical Reactor"};
            for (int i = 0; i < pathParts.length; i++) {
                int idx = i;
                var crumb = (ButtonWidget) scene.find(WidgetFinder.byKey("crumb_" + i));
                crumb.onClick(() -> navHistory.add(pathParts[idx]));
            }

            scene.setup();

            // Navigate: Chemical Reactor → Autoclave → Assembly Line
            InputSequence.begin()
                    .moveTo("crumb_3").click().tick(2)
                    .moveTo("crumb_2").click().tick(2)
                    .moveTo("crumb_0").click()
                    .executeOn(scene);

            assertEquals(
                    List.of("Chemical Reactor", "Autoclave", "Assembly Line"),
                    navHistory
            );
        }
    }

    // ==================== Snapshot & Dynamic Tests ====================

    @Nested
    class DynamicTest {

        @Test
        void addNodeToCanvas() {
            var scene = createPlannerScene();
            scene.setup();

            var before = scene.snapshot();

            // Dynamically add a new processing node
            @SuppressWarnings("unchecked")
            var canvas = (WidgetGroup<Widget>) scene.find("canvas");
            var newNode = new WidgetGroup<Widget>();
            TestScene.setKey(newNode, "node_new");
            TestScene.setBound(newNode, 600, 300, 120, 60);
            canvas.addWidget(newNode);
            scene.rebuild();

            scene.assertExists("node_new");
            scene.assertDiff(before).created("node_new");
        }

        @Test
        void removeNodeFromCanvas() {
            var scene = createPlannerScene();
            scene.setup();

            var before = scene.snapshot();

            // Remove a node
            scene.remove("node_0");
            scene.rebuild();

            assertFalse(scene.exists("node_0"));
            scene.assertDiff(before).removed("node_0");
        }

        @Test
        void addNewLayerButton() {
            var scene = createPlannerScene();
            scene.setup();

            var before = scene.snapshot();

            @SuppressWarnings("unchecked")
            var layerPanel = (WidgetGroup<Widget>) scene.find("layerPanel");
            var newLayer = ButtonWidget.of("L16");
            TestScene.setKey(newLayer, "layer_16");
            TestScene.setBound(newLayer, 15, 446, 70, 24);
            layerPanel.addWidget(newLayer);
            scene.rebuild();

            scene.assertExists("layer_16");
            scene.assertThat("layer_16").hasLabel("L16");
            scene.assertDiff(before).created("layer_16");
        }

        @Test
        void addAndRemoveMultipleNodes() {
            var scene = createPlannerScene();
            scene.setup();

            var before = scene.snapshot();

            @SuppressWarnings("unchecked")
            var canvas = (WidgetGroup<Widget>) scene.find("canvas");

            // Add two new nodes
            for (int i = 10; i < 12; i++) {
                var node = new WidgetGroup<Widget>();
                TestScene.setKey(node, "node_" + i);
                TestScene.setBound(node, 100 + i * 60, 200, 100, 50);
                canvas.addWidget(node);
            }

            // Remove one existing node
            scene.remove("node_1");

            scene.rebuild();

            scene.assertExists("node_10");
            scene.assertExists("node_11");
            assertFalse(scene.exists("node_1"));

            var diff = scene.assertDiff(before);
            diff.created("node_10");
            diff.created("node_11");
            diff.removed("node_1");
        }
    }

    // ==================== Focus & Navigation Tests ====================

    @Nested
    class FocusTest {

        @Test
        void tabBetweenInputFields() {
            var scene = createPlannerScene();
            scene.setup();

            // Focus the quantity field
            scene.tap("quantity");
            scene.assertThat("quantity").isFocused();

            // Click another input to move focus
            scene.tap("search");
            scene.assertThat("search").isFocused();
            scene.assertThat("quantity").isNotFocused();
        }

        @Test
        void searchFieldFocusAndType() {
            var scene = createPlannerScene();
            scene.setup();

            // Click on search field
            scene.tap("search");
            scene.assertThat("search").isFocused();

            // Type text
            scene.typeText("search", "Autoclave");
            scene.assertThat("search").hasValue("Autoclave");
        }

        @Test
        void escapeClearsSearchFocus() {
            var scene = createPlannerScene();
            scene.setup();

            scene.tap("search");
            scene.assertThat("search").isFocused();

            // Click a non-focusable widget to clear focus
            scene.tap("calcBtn");
            scene.assertThat("search").isNotFocused();
        }
    }

    // ==================== Visibility Tests ====================

    @Nested
    class VisibilityTest {

        @Test
        void allWidgetsVisible() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertThat("statusBar").isVisible();
            scene.assertThat("productBar").isVisible();
            scene.assertThat("tabBar").isVisible();
            scene.assertThat("search").isVisible();
            scene.assertThat("layerPanel").isVisible();
            scene.assertThat("canvas").isVisible();
            scene.assertThat("bottomBar").isVisible();
            scene.assertThat("infoBar").isVisible();
        }

        @Test
        void widgetPositions() {
            var scene = createPlannerScene();
            scene.setup();

            scene.assertThat("statusBar").hasPos(0, 0).hasSize(960, 24);
            scene.assertThat("productBar").hasPos(0, 24).hasSize(960, 36);
            scene.assertThat("tabBar").hasPos(0, 60).hasSize(600, 28);
            scene.assertThat("layerPanel").hasPos(0, 90).hasSize(100, 400);
            scene.assertThat("canvas").hasPos(100, 90).hasSize(860, 370);
            scene.assertThat("bottomBar").hasPos(0, 460).hasSize(960, 40);
        }
    }

    // ==================== Multi-step Workflow Tests ====================

    @Nested
    class WorkflowTest {

        @Test
        void fullCalculationWorkflow() {
            var scene = createPlannerScene();
            var steps = new ArrayList<String>();

            // Wire callbacks
            var calcBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("calcBtn"));
            calcBtn.onClick(() -> steps.add("calculated"));

            ((ButtonWidget) scene.find(WidgetFinder.byKey("tab_tree")))
                    .onClick(() -> steps.add("tab_tree"));

            ((ButtonWidget) scene.find(WidgetFinder.byKey("layer_5")))
                    .onClick(() -> steps.add("layer_5"));

            ((ButtonWidget) scene.find(WidgetFinder.byKey("crumb_3")))
                    .onClick(() -> steps.add("crumb_reactor"));

            scene.setup();

            // Execute a full workflow
            InputSequence.begin()
                    // 1. Select tree tab
                    .moveTo("tab_tree").click().tick(3)
                    // 2. Select layer 5
                    .moveTo("layer_5").click().tick(3)
                    // 3. Search for an item
                    .moveTo("search").click()
                    .typeText("Reactor")
                    .tick(5)
                    // 4. Click calculate
                    .moveTo("calcBtn").click().tick(3)
                    // 5. Navigate breadcrumb
                    .moveTo("crumb_3").click()
                    .executeOn(scene);

            assertEquals(
                    List.of("tab_tree", "layer_5", "calculated", "crumb_reactor"),
                    steps
            );
            scene.assertThat("search").hasValue("Reactor");
        }

        @Test
        void configChangeAndRecalculate() {
            var scene = createPlannerScene();
            var calcCount = new AtomicInteger(0);
            var configOpened = new AtomicBoolean(false);

            var calcBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("calcBtn"));
            calcBtn.onClick(calcCount::incrementAndGet);

            var configBtn = (ButtonWidget) scene.find(WidgetFinder.byKey("configBtn"));
            configBtn.onClick(() -> configOpened.set(true));

            scene.setup();

            // Open config, change quantity, calculate twice
            InputSequence.begin()
                    .moveTo("configBtn").click().tick(2)
                    .moveTo("quantity").click()
                    .typeText("16")
                    .tick(2)
                    .moveTo("calcBtn").click().tick(5)
                    .moveTo("calcBtn").click()
                    .executeOn(scene);

            assertTrue(configOpened.get());
            assertEquals(2, calcCount.get());
        }
    }
}
