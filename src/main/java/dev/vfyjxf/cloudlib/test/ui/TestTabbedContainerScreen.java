//package dev.vfyjxf.cloudlib.test.ui;
//
//import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
//import dev.vfyjxf.cloudlib.api.ui.base.Widget;
//import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
//import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
//import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
//import dev.vfyjxf.cloudlib.ui.widgets.ButtonWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.ColumnWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.LabelWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.ProgressBarWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.RowWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.ScrollPanelWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.SpacerWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.TabbedContainerWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.TextFieldWidget;
//import dev.vfyjxf.cloudlib.ui.widgets.ToggleWidget;
//import net.minecraft.network.chat.Component;
//
//import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
//
///**
// * Test screen demonstrating the TabbedContainerWidget.
// * <p>
// * This screen showcases:
// * <ul>
// *   <li>Multiple tabs with icons</li>
// *   <li>State preservation when switching tabs</li>
// *   <li>Different content types per tab</li>
// *   <li>Tab change callbacks</li>
// * </ul>
// */
////@TestScreen
//public class TestTabbedContainerScreen extends BasicScreen {
//
//    // State for testing state preservation
//    private int counterValue = 0;
//    private double progressValue = 0.0;
//    private boolean toggleState = false;
//    private String inputText = "";
//
//    // Reference to status label for updates
//    private LabelWidget statusLabel;
//
//    public TestTabbedContainerScreen() {
//        buildUI();
//    }
//
//    private void buildUI() {
//        // Main container
//        var mainContainer = ColumnWidget.create();
//        mainContainer.setSpacing(8);
//        mainContainer.applyStyle(UIStyle.of(
//                padding(16),
//                background(new ColorTexture(0xCC222222))
//        ));
//
//        // Title
//        var title = LabelWidget.of("TabbedContainerWidget Demo")
//                .setColor(0xFFFFAA00)
//                .setShadow(true);
//        title.applyStyle(UIStyle.of(height(16)));
//        mainContainer.addChild(title);
//
//        // Create the tabbed container
//        var tabbedContainer = createTabbedContainer();
//        tabbedContainer.applyStyle(UIStyle.of(
//                flexGrow(1),
//                minHeight(200)
//        ));
//        mainContainer.addChild(tabbedContainer);
//
//        // Status bar
//        var statusBar = createStatusBar();
//        mainContainer.addChild(statusBar);
//
//        mainGroup().addWidget(mainContainer);
//    }
//
//    /**
//     * Creates the main tabbed container with all tabs.
//     */
//    private TabbedContainerWidget createTabbedContainer() {
//        // Create icon textures (using simple colored textures for demo)
//        VisualTexture overviewIcon = new ColorTexture(0xFF4488FF);    // Blue - overview
//        VisualTexture settingsIcon = new ColorTexture(0xFF888888);    // Gray - settings (gear)
//        VisualTexture inventoryIcon = new ColorTexture(0xFFAA8844);   // Brown - inventory
//        VisualTexture statsIcon = new ColorTexture(0xFF44AA44);       // Green - statistics
//
//        return TabbedContainerWidget.create()
//                .setTabWidth(28)
//                .setTabHeight(28)
//                .setTabSpacing(4)
//                .setTabPadding(6)
//                .setContentPadding(8)
//                .setBorderWidth(2)
//                .setBackgroundTexture(new ColorTexture(0xCC1A1A1A))
//                .setTabBarTexture(new ColorTexture(0xFF2A2A2A))
//                .setContentBorderTexture(new ColorTexture(0xFF444444))
//
//                // Tab 1: Overview
//                .addTab(TabbedContainerWidget.Tab.builder("overview")
//                        .icon(overviewIcon)
//                        .tooltip(Component.translatable("tab.overview", "Overview"))
//                        .content(this::createOverviewContent)
//                        .build())
//
//                // Tab 2: Settings
//                .addTab(TabbedContainerWidget.Tab.builder("settings")
//                        .icon(settingsIcon)
//                        .tooltip(Component.translatable("tab.settings", "Settings"))
//                        .content(this::createSettingsContent)
//                        .build())
//
//                // Tab 3: Inventory
//                .addTab(TabbedContainerWidget.Tab.builder("inventory")
//                        .icon(inventoryIcon)
//                        .tooltip(Component.translatable("tab.inventory", "Inventory"))
//                        .content(this::createInventoryContent)
//                        .build())
//
//                // Tab 4: Statistics
//                .addTab(TabbedContainerWidget.Tab.builder("stats")
//                        .icon(statsIcon)
//                        .tooltip(Component.translatable("tab.stats", "Statistics"))
//                        .content(this::createStatsContent)
//                        .build())
//
//                // Tab change callback
//                .onTabChange(event -> {
//                    if (statusLabel != null) {
//                        statusLabel.setText(
//                                "Tab changed: " + (event.previousTab() != null ? event.previousTab().id() : "none") +
//                                " -> " + event.newTab().id()
//                        );
//                    }
//                })
//
//                // Select first tab
//                .selectTab(0);
//    }
//
//    /**
//     * Creates the overview tab content.
//     */
//    private Widget createOverviewContent() {
//        var content = ColumnWidget.create();
//        content.setSpacing(12);
//        content.applyStyle(UIStyle.of(flexGrow(1)));
//
//        // Welcome message
//        var welcomeLabel = LabelWidget.of("Welcome to the Overview Tab")
//                .setColor(0xFFFFFFFF)
//                .setShadow(true);
//        content.addChild(welcomeLabel);
//
//        // Description
//        var description = LabelWidget.of("This demonstrates state preservation across tab switches.")
//                .setColor(0xFFAAAAAA);
//        content.addChild(description);
//
//        // Counter demo (tests state preservation)
//        var counterSection = ColumnWidget.create();
//        counterSection.setSpacing(4);
//
//        var counterLabel = LabelWidget.of("Counter: " + counterValue)
//                .setColor(0xFFFFFF00);
//        counterSection.addChild(counterLabel);
//
//        var counterButtons = RowWidget.create();
//        counterButtons.setSpacing(4);
//
//        var decrementBtn = ButtonWidget.of("-", () -> {
//            counterValue--;
//            counterLabel.setText("Counter: " + counterValue);
//        }).setColors(0xFF555555, 0xFF777777, 0xFF333333);
//        decrementBtn.applyStyle(UIStyle.of(sizeOf(30, 20)));
//        counterButtons.addChild(decrementBtn);
//
//        var incrementBtn = ButtonWidget.of("+", () -> {
//            counterValue++;
//            counterLabel.setText("Counter: " + counterValue);
//        }).setColors(0xFF555555, 0xFF777777, 0xFF333333);
//        incrementBtn.applyStyle(UIStyle.of(sizeOf(30, 20)));
//        counterButtons.addChild(incrementBtn);
//
//        var resetBtn = ButtonWidget.of("Reset", () -> {
//            counterValue = 0;
//            counterLabel.setText("Counter: " + counterValue);
//        }).setColors(0xFF666666, 0xFF888888, 0xFF444444);
//        resetBtn.applyStyle(UIStyle.of(sizeOf(50, 20)));
//        counterButtons.addChild(resetBtn);
//
//        counterSection.addChild(counterButtons);
//        content.addChild(counterSection);
//
//        content.addChild(SpacerWidget.create());
//
//        return content;
//    }
//
//    /**
//     * Creates the settings tab content.
//     */
//    private Widget createSettingsContent() {
//        var content = ColumnWidget.create();
//        content.setSpacing(12);
//        content.applyStyle(UIStyle.of(flexGrow(1)));
//
//        // Title
//        var titleLabel = LabelWidget.of("Settings")
//                .setColor(0xFFFFFFFF)
//                .setShadow(true);
//        content.addChild(titleLabel);
//
//        // Toggle option 1
//        var toggle1Row = createSettingToggle("Enable Feature A", toggleState, state -> {
//            toggleState = state;
//            System.out.println("Feature A: " + state);
//        });
//        content.addChild(toggle1Row);
//
//        // Toggle option 2
//        var toggle2Row = createSettingToggle("Enable Feature B", false, state -> {
//            System.out.println("Feature B: " + state);
//        });
//        content.addChild(toggle2Row);
//
//        // Toggle option 3
//        var toggle3Row = createSettingToggle("Enable Feature C", true, state -> {
//            System.out.println("Feature C: " + state);
//        });
//        content.addChild(toggle3Row);
//
//        // Text input
//        var inputSection = ColumnWidget.create();
//        inputSection.setSpacing(4);
//
//        var inputLabel = LabelWidget.of("Custom Name:")
//                .setColor(0xFFAAAAAA);
//        inputSection.addChild(inputLabel);
//
//        var textField = TextFieldWidget.create()
//                .setPlaceholder("Enter name...")
//                .onTextChanged(text -> inputText = text);
//        textField.applyStyle(UIStyle.of(sizeOf(200, 20)));
//        inputSection.addChild(textField);
//
//        content.addChild(inputSection);
//
//        content.addChild(SpacerWidget.create());
//
//        return content;
//    }
//
//    /**
//     * Creates a setting toggle row.
//     */
//    private Widget createSettingToggle(String label, boolean initialState, java.util.function.Consumer<Boolean> onToggle) {
//        var row = RowWidget.create();
//        row.setSpacing(8);
//        row.applyStyle(UIStyle.of(alignItemsCenter()));
//
//        var labelWidget = LabelWidget.of(label)
//                .setColor(0xFFCCCCCC);
//        labelWidget.applyStyle(UIStyle.of(width(150)));
//        row.addChild(labelWidget);
//
//        row.addChild(SpacerWidget.create());
//
//        var toggle = ToggleWidget.create(initialState)
//                .onToggle(onToggle)
//                .setColors(0xFF555555, 0xFF00CC66);
//        toggle.applyStyle(UIStyle.of(sizeOf(36, 18)));
//        row.addChild(toggle);
//
//        return row;
//    }
//
//    /**
//     * Creates the inventory tab content.
//     */
//    private Widget createInventoryContent() {
//        var content = ColumnWidget.create();
//        content.setSpacing(8);
//        content.applyStyle(UIStyle.of(flexGrow(1)));
//
//        // Title
//        var titleLabel = LabelWidget.of("Inventory")
//                .setColor(0xFFFFFFFF)
//                .setShadow(true);
//        content.addChild(titleLabel);
//
//        // Scrollable item list
//        var scrollPanel = ScrollPanelWidget.vertical();
//        scrollPanel.setContentHeight(300);
//        scrollPanel.setScrollSpeed(15);
//        scrollPanel.setBackgroundTexture(new ColorTexture(0x30000000));
//        scrollPanel.applyStyle(UIStyle.of(flexGrow(1)));
//
//        var itemList = ColumnWidget.create();
//        itemList.setSpacing(2);
//
//        // Add sample items
//        String[] items = {
//                "Iron Ingot", "Gold Ingot", "Diamond", "Emerald",
//                "Redstone", "Lapis Lazuli", "Coal", "Copper Ingot",
//                "Netherite Ingot", "Amethyst Shard", "Quartz", "Glowstone Dust"
//        };
//
//        for (int i = 0; i < items.length; i++) {
//            var itemRow = createInventoryItem(items[i], (int) (Math.random() * 64) + 1, i % 4 == 0);
//            itemList.addChild(itemRow);
//        }
//
//        scrollPanel.addChild(itemList);
//        content.addChild(scrollPanel);
//
//        return content;
//    }
//
//    /**
//     * Creates an inventory item row.
//     */
//    private Widget createInventoryItem(String name, int count, boolean rare) {
//        var row = RowWidget.create();
//        row.setSpacing(8);
//        row.applyStyle(UIStyle.of(
//                padding(4, 8, 4, 8),
//                background(new ColorTexture(rare ? 0x40FFAA00 : 0x20FFFFFF)),
//                alignItemsCenter()
//        ));
//
//        var nameLabel = LabelWidget.of(name)
//                .setColor(rare ? 0xFFFFAA00 : 0xFFCCCCCC);
//        nameLabel.applyStyle(UIStyle.of(width(120)));
//        row.addChild(nameLabel);
//
//        row.addChild(SpacerWidget.create());
//
//        var countLabel = LabelWidget.of("x" + count)
//                .setColor(0xFF88FF88);
//        row.addChild(countLabel);
//
//        var useBtn = ButtonWidget.of("Use", () -> System.out.println("Using: " + name))
//                .setColors(0xFF444444, 0xFF666666, 0xFF333333);
//        useBtn.applyStyle(UIStyle.of(sizeOf(40, 16)));
//        row.addChild(useBtn);
//
//        return row;
//    }
//
//    /**
//     * Creates the statistics tab content.
//     */
//    private Widget createStatsContent() {
//        var content = ColumnWidget.create();
//        content.setSpacing(12);
//        content.applyStyle(UIStyle.of(flexGrow(1)));
//
//        // Title
//        var titleLabel = LabelWidget.of("Statistics")
//                .setColor(0xFFFFFFFF)
//                .setShadow(true);
//        content.addChild(titleLabel);
//
//        // Progress bars section
//        var progressSection = ColumnWidget.create();
//        progressSection.setSpacing(8);
//
//        // Health bar
//        progressSection.addChild(createStatBar("Health", 0.75, 0xFFFF4444));
//
//        // Energy bar
//        progressSection.addChild(createStatBar("Energy", 0.60, 0xFF44FF44));
//
//        // Experience bar
//        progressSection.addChild(createStatBar("Experience", 0.45, 0xFF4444FF));
//
//        // Hunger bar
//        progressSection.addChild(createStatBar("Hunger", 0.90, 0xFFFFAA44));
//
//        content.addChild(progressSection);
//
//        // Interactive progress
//        var interactiveSection = ColumnWidget.create();
//        interactiveSection.setSpacing(4);
//
//        var interactiveLabel = LabelWidget.of("Adjustable Progress")
//                .setColor(0xFFAAAAAA);
//        interactiveSection.addChild(interactiveLabel);
//
//        var progressBar = ProgressBarWidget.create(() -> progressValue)
//                .setDirection(ProgressBarWidget.Direction.LEFT_TO_RIGHT)
//                .setColors(0xFF333333, 0xFF00AAFF);
//        progressBar.applyStyle(UIStyle.of(sizeOf(200, 16)));
//        interactiveSection.addChild(progressBar);
//
//        var progressControls = RowWidget.create();
//        progressControls.setSpacing(4);
//
//        var decreaseBtn = ButtonWidget.of("-10%", () -> {
//            progressValue = Math.max(0, progressValue - 0.1);
//        }).setColors(0xFF555555, 0xFF777777, 0xFF333333);
//        decreaseBtn.applyStyle(UIStyle.of(sizeOf(50, 20)));
//        progressControls.addChild(decreaseBtn);
//
//        var increaseBtn = ButtonWidget.of("+10%", () -> {
//            progressValue = Math.min(1.0, progressValue + 0.1);
//        }).setColors(0xFF555555, 0xFF777777, 0xFF333333);
//        increaseBtn.applyStyle(UIStyle.of(sizeOf(50, 20)));
//        progressControls.addChild(increaseBtn);
//
//        interactiveSection.addChild(progressControls);
//        content.addChild(interactiveSection);
//
//        content.addChild(SpacerWidget.create());
//
//        return content;
//    }
//
//    /**
//     * Creates a stat bar with label.
//     */
//    private Widget createStatBar(String name, double value, int color) {
//        var row = RowWidget.create();
//        row.setSpacing(8);
//        row.applyStyle(UIStyle.of(alignItemsCenter()));
//
//        var label = LabelWidget.of(name + ":")
//                .setColor(0xFFCCCCCC);
//        label.applyStyle(UIStyle.of(width(80)));
//        row.addChild(label);
//
//        var bar = ProgressBarWidget.create(() -> value)
//                .setDirection(ProgressBarWidget.Direction.LEFT_TO_RIGHT)
//                .setColors(0xFF333333, color);
//        bar.applyStyle(UIStyle.of(sizeOf(120, 12)));
//        row.addChild(bar);
//
//        var valueLabel = LabelWidget.of((int) (value * 100) + "%")
//                .setColor(color);
//        row.addChild(valueLabel);
//
//        return row;
//    }
//
//    /**
//     * Creates the status bar at the bottom.
//     */
//    private Widget createStatusBar() {
//        var statusBar = RowWidget.create();
//        statusBar.setSpacing(16);
//        statusBar.applyStyle(UIStyle.of(
//                padding(4, 8, 4, 8),
//                background(new ColorTexture(0x60000000)),
//                alignItemsCenter()
//        ));
//
//        statusLabel = LabelWidget.of("Ready")
//                .setColor(0xFF88FF88);
//        statusBar.addChild(statusLabel);
//
//        statusBar.addChild(SpacerWidget.create());
//
//        var closeBtn = ButtonWidget.of("Close", this::onClose)
//                .setColors(0xFF555555, 0xFF777777, 0xFF333333);
//        closeBtn.applyStyle(UIStyle.of(sizeOf(60, 18)));
//        statusBar.addChild(closeBtn);
//
//        return statusBar;
//    }
//}
