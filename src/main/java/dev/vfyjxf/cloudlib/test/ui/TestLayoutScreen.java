package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.debug.Inspector;
import dev.vfyjxf.cloudlib.ui.widgets.BoxWidget;
import dev.vfyjxf.cloudlib.ui.widgets.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widgets.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widgets.DividerWidget;
import dev.vfyjxf.cloudlib.ui.widgets.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widgets.ProgressBarWidget;
import dev.vfyjxf.cloudlib.ui.widgets.RowWidget;
import dev.vfyjxf.cloudlib.ui.widgets.ScrollPanelWidget;
import dev.vfyjxf.cloudlib.ui.widgets.SpacerWidget;
import dev.vfyjxf.cloudlib.ui.widgets.TextFieldWidget;
import dev.vfyjxf.cloudlib.ui.widgets.ToggleWidget;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

/**
 * Test screen demonstrating all basic UI components.
 * <p>
 * This screen showcases the component library:
 * <ul>
 *   <li>Layout: Column, Row, Box, Spacer, Divider, ScrollPanel</li>
 *   <li>Basic: Label, Button, TextField, Toggle, ProgressBar</li>
 *   <li>Styling: backgrounds, colors, padding, gaps</li>
 * </ul>
 */
//@TestScreen
public class TestLayoutScreen extends BasicScreen {

    // State for interactive components
    private double progressValue = 0.0;
    private boolean toggleState = false;
    private String searchText = "";
    private int clickCount = 0;

    public TestLayoutScreen() {
        buildUI();
    }

    private void buildUI() {
        // Main container with padding
        var mainContainer = ColumnWidget.create();
        mainContainer.setSpacing(8);
        mainContainer.applyStyle(UIStyle.of(
            padding(16),
            background(new ColorTexture(0xCC222222))
        ));

        // ==================== Header Section ====================
        var header = createHeader();
        mainContainer.addChild(header);

        // Divider
        var headerDivider = DividerWidget.horizontal()
                                         .setColor(0xFF555555)
                                         .setThickness(2);
        headerDivider.applyStyle(UIStyle.of(height(4)));
        mainContainer.addChild(headerDivider);

        // ==================== Main Content ====================
        var contentRow = RowWidget.create();
        contentRow.setSpacing(16);
        contentRow.applyStyle(UIStyle.of(flexGrow(1)));

        // Left Panel - Controls
        var leftPanel = createLeftPanel();
        contentRow.addChild(leftPanel);

        // Vertical Divider
        var verticalDivider = DividerWidget.vertical()
                                           .setColor(0xFF444444);
        verticalDivider.applyStyle(UIStyle.of(width(2)));
        contentRow.addChild(verticalDivider);

        // Right Panel - Preview
        var rightPanel = createRightPanel();
        contentRow.addChild(rightPanel);

        mainContainer.addChild(contentRow);

        // ==================== Footer Section ====================
        var footerDivider = DividerWidget.horizontal()
                                         .setColor(0xFF555555);
        footerDivider.applyStyle(UIStyle.of(height(2)));
        mainContainer.addChild(footerDivider);

        var footer = createFooter();
        mainContainer.addChild(footer);

        // Add to main group
        mainGroup().addWidget(mainContainer);

        // Add Debug Widget - tracks mouse over mainContainer
        var debugWidget = Inspector.create()
                                   .setTrackRoot(mainContainer)
                                   .setTrackMouse(true)
                                   .setShowHighlight(true)
                                   .setShowHierarchy(true);
        debugWidget.applyStyle(UIStyle.of(
            UIStyles.size(280, 200),
            UIStyles.positionStatic()
        ));
        mainGroup().addWidget(debugWidget);
    }

    /**
     * Creates the header section with title and search.
     */
    private Widget createHeader() {
        var header = RowWidget.create();
        header.setSpacing(16);
        header.applyStyle(UIStyle.of(alignItemsCenter()));

        // Title
        var title = LabelWidget.of("CloudLib UI Components Demo")
                               .setColor(0xFFFFAA00)
                               .setShadow(true);
        title.applyStyle(UIStyle.of(size(200, 12)));
        header.addChild(title);

        // Spacer to push elements apart
        header.addChild(SpacerWidget.create());

        // Search Field
        var searchField = TextFieldWidget.create()
                                         .setPlaceholder("Search...")
                                         .onTextChanged(text -> this.searchText = text);
        searchField.applyStyle(UIStyle.of(
            size(150, 20)
        ));
        header.addChild(searchField);

        // Settings Toggle
        var settingsToggle = ToggleWidget.create(false)
                                         .onToggle(state -> System.out.println("Settings: " + state))
                                         .setColors(0xFF666666, 0xFF00AA00);
        settingsToggle.applyStyle(UIStyle.of(size(30, 16)));
        header.addChild(settingsToggle);

        return header;
    }

    /**
     * Creates the left panel with various controls.
     */
    private Widget createLeftPanel() {
        var panel = ColumnWidget.create();
        panel.setSpacing(12);
        panel.applyStyle(UIStyle.of(
            size(180, -1),
            padding(8),
            background(new ColorTexture(0x40000000))
        ));

        // Section Title
        var controlsTitle = LabelWidget.of("Controls")
                                       .setColor(0xFFFFFF00);
        panel.addChild(controlsTitle);

        // Button Group
        var buttonGroup = createButtonGroup();
        panel.addChild(buttonGroup);

        // Progress Section
        var progressSection = createProgressSection();
        panel.addChild(progressSection);

        // Toggle Section
        var toggleSection = createToggleSection();
        panel.addChild(toggleSection);

        // Spacer at bottom
        panel.addChild(SpacerWidget.create());

        return panel;
    }

    /**
     * Creates a group of buttons.
     */
    private Widget createButtonGroup() {
        var group = ColumnWidget.create();
        group.setSpacing(4);

        // Action Buttons
        var row1 = RowWidget.create();
        row1.setSpacing(4);

        var primaryBtn = ButtonWidget.of("Primary", () -> clickCount++)
                                     .setColors(0xFF0066CC, 0xFF0088FF, 0xFF004499);
        primaryBtn.applyStyle(UIStyle.of(size(80, 20)));
        row1.addChild(primaryBtn);

        var secondaryBtn = ButtonWidget.of("Secondary", () -> System.out.println("Secondary clicked"))
                                       .setColors(0xFF666666, 0xFF888888, 0xFF444444);
        secondaryBtn.applyStyle(UIStyle.of(size(80, 20)));
        row1.addChild(secondaryBtn);

        group.addChild(row1);

        // Danger Button
        var dangerBtn = ButtonWidget.of("Danger Action", () -> System.out.println("Danger!"))
                                    .setColors(0xFFCC0000, 0xFFEE0000, 0xFFAA0000);
        dangerBtn.applyStyle(UIStyle.of(size(164, 20)));
        group.addChild(dangerBtn);

        // Disabled Button
        var disabledBtn = ButtonWidget.of("Disabled")
                                      .setEnabled(false);
        disabledBtn.applyStyle(UIStyle.of(size(164, 20)));
        group.addChild(disabledBtn);

        return group;
    }

    /**
     * Creates the progress bar section.
     */
    private Widget createProgressSection() {
        var section = ColumnWidget.create();
        section.setSpacing(4);

        // Label
        var label = LabelWidget.of("Progress Bars")
                               .setColor(0xFFAAAAAA);
        section.addChild(label);

        // Horizontal Progress
        var horizontalProgress = ProgressBarWidget.create(() -> progressValue)
                                                  .setDirection(ProgressBarWidget.Direction.LEFT_TO_RIGHT)
                                                  .setColors(0xFF333333, 0xFF00AA00);
        horizontalProgress.applyStyle(UIStyle.of(size(164, 12)));
        section.addChild(horizontalProgress);

        // Vertical Progress (in HStack for horizontal layout)
        var verticalRow = RowWidget.create();
        verticalRow.setSpacing(4);

        var verticalProgress1 = ProgressBarWidget.create(() -> progressValue)
                                                 .setDirection(ProgressBarWidget.Direction.BOTTOM_TO_TOP)
                                                 .setColors(0xFF333333, 0xFF0066CC);
        verticalProgress1.applyStyle(UIStyle.of(size(20, 40)));
        verticalRow.addChild(verticalProgress1);

        var verticalProgress2 = ProgressBarWidget.create(() -> Math.min(1.0, progressValue * 1.5))
                                                 .setDirection(ProgressBarWidget.Direction.BOTTOM_TO_TOP)
                                                 .setColors(0xFF333333, 0xFFCC6600);
        verticalProgress2.applyStyle(UIStyle.of(size(20, 40)));
        verticalRow.addChild(verticalProgress2);

        var verticalProgress3 = ProgressBarWidget.create(() -> Math.min(1.0, progressValue * 2.0))
                                                 .setDirection(ProgressBarWidget.Direction.BOTTOM_TO_TOP)
                                                 .setColors(0xFF333333, 0xFFCC0066);
        verticalProgress3.applyStyle(UIStyle.of(size(20, 40)));
        verticalRow.addChild(verticalProgress3);

        section.addChild(verticalRow);

        // Progress Controls
        var controlRow = RowWidget.create();
        controlRow.setSpacing(4);

        var decreaseBtn = ButtonWidget.of("-", () -> progressValue = Math.max(0, progressValue - 0.1))
                                      .setColors(0xFF555555, 0xFF777777, 0xFF333333);
        decreaseBtn.applyStyle(UIStyle.of(size(30, 18)));
        controlRow.addChild(decreaseBtn);

        var increaseBtn = ButtonWidget.of("+", () -> progressValue = Math.min(1, progressValue + 0.1))
                                      .setColors(0xFF555555, 0xFF777777, 0xFF333333);
        increaseBtn.applyStyle(UIStyle.of(size(30, 18)));
        controlRow.addChild(increaseBtn);

        var resetBtn = ButtonWidget.of("Reset", () -> progressValue = 0)
                                   .setColors(0xFF555555, 0xFF777777, 0xFF333333);
        resetBtn.applyStyle(UIStyle.of(size(50, 18)));
        controlRow.addChild(resetBtn);

        section.addChild(controlRow);

        return section;
    }

    /**
     * Creates the toggle section.
     */
    private Widget createToggleSection() {
        var section = ColumnWidget.create();
        section.setSpacing(4);

        var label = LabelWidget.of("Toggles")
                               .setColor(0xFFAAAAAA);
        section.addChild(label);

        // Toggle rows
        for (int i = 1; i <= 3; i++) {
            var row = RowWidget.create();
            row.setSpacing(8);
            row.applyStyle(UIStyle.of(alignItemsCenter()));

            var optionLabel = LabelWidget.of("Option " + i);
            optionLabel.applyStyle(UIStyle.of(width(100)));
            row.addChild(optionLabel);

            row.addChild(SpacerWidget.create());

            int finalI = i;
            var toggle = ToggleWidget.create(i == 1)
                                     .onToggle(state -> System.out.println("Option " + finalI + ": " + state))
                                     .setColors(0xFF555555, 0xFF00CC66);
            toggle.applyStyle(UIStyle.of(size(36, 18)));
            row.addChild(toggle);

            section.addChild(row);
        }

        return section;
    }

    /**
     * Creates the right preview panel.
     */
    private Widget createRightPanel() {
        var panel = ColumnWidget.create();
        panel.setSpacing(8);
        panel.applyStyle(UIStyle.of(
            flexGrow(1),
            padding(8),
            background(new ColorTexture(0x40000000))
        ));

        // Title
        var previewTitle = LabelWidget.of("Preview Panel")
                                      .setColor(0xFFFFFF00);
        panel.addChild(previewTitle);

        // ZStack Demo - Layered content
        var zstackDemo = createZStackDemo();
        panel.addChild(zstackDemo);

        // Scrollable List Demo
        var scrollDemo = createScrollDemo();
        panel.addChild(scrollDemo);

        return panel;
    }

    /**
     * Creates a ZStack demonstration.
     */
    private Widget createZStackDemo() {
        var container = ColumnWidget.create();
        container.setSpacing(4);

        var label = LabelWidget.of("ZStack Layering")
                               .setColor(0xFFAAAAAA);
        container.addChild(label);

        var zstack = BoxWidget.create();
        zstack.applyStyle(UIStyle.of(size(150, 60)));

        // Background layer
        var background = new Widget();
        background.applyStyle(UIStyle.of(
            size(150, 60),
            background(new ColorTexture(0xFF004466))
        ));
        zstack.addChild(background);

        // Middle layer
        var middleLayer = LabelWidget.of("Background")
                                     .setColor(0xFF88CCFF)
                                     .setAlign(LabelWidget.TextAlign.CENTER);
        middleLayer.applyStyle(UIStyle.of(
            size(150, 20),
            margin(20, 0, 0, 0)
        ));
        zstack.addChild(middleLayer);

        // Top layer
        var topLayer = LabelWidget.of("Overlay Text")
                                  .setColor(0xFFFFFFFF)
                                  .setAlign(LabelWidget.TextAlign.CENTER);
        topLayer.applyStyle(UIStyle.of(
            size(150, 20),
            margin(5, 0, 0, 0)
        ));
        zstack.addChild(topLayer);

        container.addChild(zstack);
        return container;
    }

    /**
     * Creates a scrollable list demonstration.
     */
    private Widget createScrollDemo() {
        var container = ColumnWidget.create();
        container.setSpacing(4);
        container.applyStyle(UIStyle.of(flexGrow(1)));

        var label = LabelWidget.of("Scrollable List")
                               .setColor(0xFFAAAAAA);
        container.addChild(label);

        var scrollPanel = ScrollPanelWidget.vertical();
        scrollPanel.setContentHeight(300);
        scrollPanel.setScrollSpeed(15);
        scrollPanel.setBackgroundTexture(new ColorTexture(0x40000000));
        scrollPanel.applyStyle(UIStyle.of(
            flexGrow(1),
            minHeight(100)
        ));

        // Add items to scroll panel
        var listContent = ColumnWidget.create();
        listContent.setSpacing(2);

        for (int i = 1; i <= 20; i++) {
            var listItem = createListItem("Item " + i, i % 3 == 0);
            listContent.addChild(listItem);
        }

        scrollPanel.addChild(listContent);
        container.addChild(scrollPanel);

        return container;
    }

    /**
     * Creates a single list item.
     */
    private Widget createListItem(String text, boolean highlighted) {
        var item = RowWidget.create();
        item.setSpacing(8);
        item.applyStyle(UIStyle.of(
            padding(4, 8, 4, 8),
            background(new ColorTexture(highlighted ? 0x40FFAA00 : 0x20FFFFFF)),
            alignItemsCenter()
        ));

        var itemLabel = LabelWidget.of(text)
                                   .setColor(highlighted ? 0xFFFFAA00 : 0xFFCCCCCC);
        item.addChild(itemLabel);

        item.addChild(SpacerWidget.create());

        var selectBtn = ButtonWidget.of("Select", () -> System.out.println("Selected: " + text))
                                    .setColors(0xFF444444, 0xFF666666, 0xFF333333);
        selectBtn.applyStyle(UIStyle.of(size(50, 16)));
        item.addChild(selectBtn);

        return item;
    }

    /**
     * Creates the footer section.
     */
    private Widget createFooter() {
        var footer = RowWidget.create();
        footer.setSpacing(16);
        footer.applyStyle(UIStyle.of(alignItemsCenter()));

        // Status text
        var statusLabel = LabelWidget.of("Status: Ready")
                                     .setColor(0xFF00FF00);
        footer.addChild(statusLabel);

        footer.addChild(SpacerWidget.create());

        // Click counter
        var clickLabel = LabelWidget.of("Clicks: " + clickCount)
                                    .setColor(0xFFAAAAAA);
        clickLabel.onInit(self -> {
            // This would ideally update dynamically
        });
        footer.addChild(clickLabel);

        // Close button
        var closeBtn = ButtonWidget.of("Close", this::onClose)
                                   .setColors(0xFF555555, 0xFF777777, 0xFF333333);
        closeBtn.applyStyle(UIStyle.of(size(60, 20)));
        footer.addChild(closeBtn);

        return footer;
    }

    @Override
    public void tick() {
        super.tick();
        // Animate progress slowly
        // progressValue = (progressValue + 0.005) % 1.0;
    }
}
