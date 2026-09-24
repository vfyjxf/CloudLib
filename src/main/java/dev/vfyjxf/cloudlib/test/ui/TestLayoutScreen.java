package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.Textures;
import dev.vfyjxf.cloudlib.ui.widget.BoxWidget;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.PanelWidget;
import dev.vfyjxf.cloudlib.ui.widget.ProgressBarWidget;
import dev.vfyjxf.cloudlib.ui.widget.RowWidget;
import dev.vfyjxf.cloudlib.ui.widget.SliderWidget;
import dev.vfyjxf.cloudlib.ui.widget.SpacerWidget;
import dev.vfyjxf.cloudlib.ui.widget.TextFieldWidget;
import dev.vfyjxf.cloudlib.ui.widget.ToggleWidget;
import dev.vfyjxf.taffy.style.TaffyDimension;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.scrollable;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

/**
 * Comprehensive test screen demonstrating all UI components with the Cirrus theme.
 * <p>
 * Showcases: Panel, Button, TextField, Toggle, Slider, ProgressBar, Label,
 * Divider, Spacer, Scroll, and various layout combinations.
 */
@TestScreen
public class TestLayoutScreen extends BasicScreen {

    // State
    private double progressValue = 0.35;
    private double sliderValue = 50;
    private int clickCount = 0;
    private boolean darkMode = false;
    private boolean autoProgress = true;
    private String inputText = "";
    private LabelWidget statusLabel;
    private LabelWidget sliderValueLabel;
    private LabelWidget clickCountLabel;
    private LabelWidget inputEchoLabel;

    public TestLayoutScreen() {
        buildUI();
    }

    private void buildUI() {
        // Root scroll container
        var scrollState = ScrollState.create(ScrollDirection.vertical).scrollSpeed(12).smooth(true).smoothSpeed(0.35f)
                .trackTexture(Textures.scrollTrack).thumbTexture(Textures.scrollbarVertical).scrollbarWidth(7);

        var root = ColumnWidget.create(10);
        root.useStyle(
            UIStyle.of(
                flexColumn(),
                padding(12),
                sizeOf(TaffyDimension.percent(1f), TaffyDimension.percent(1f)),
                background(Textures.frame)
            )
        );
        root.useEffect(scrollable(scrollState));
        root.onMouseScrolled((mx, my, sx, sy, ctx) -> {
            scrollState.scrollBy(0, (float) (-sy * scrollState.scrollSpeed()));
            return EventDispatch.consumed;
        });

        // Header
        root.addWidget(createHeader());

        // Main content: 3-column layout
        var contentRow = RowWidget.create(10);
        contentRow.useStyle(
            UIStyle.of(widthOf(TaffyDimension.percent(1f)), flexGrow(1), minHeight(0), flexWrap(), rowGap(10))
        );

        sliderValueLabel = LabelWidget.of("Value: 50").setColor(0xFF3F3F3F);
        contentRow.addWidget(createControlsPanel());
        inputEchoLabel = LabelWidget.of("Echo: ").setColor(0xFF555555);
        contentRow.addWidget(createPreviewPanel());
        contentRow.addWidget(createListPanel());

        root.addWidget(contentRow);

        // Bottom section
        root.addWidget(createDivider());
        root.addWidget(createColorShowcase());
        root.addWidget(createDivider());
        statusLabel = LabelWidget.of("Status: Ready").setColor(0xFF555555);
        clickCountLabel = LabelWidget.of("Clicks: 0").setColor(0xFF555555);
        root.addWidget(createFooter());

        mainGroup().addWidget(root);
    }

    // ==================== Header ====================

    private Widget createHeader() {
        var header = RowWidget.create(12);
        header.useStyle(
            UIStyle.of(widthOf(TaffyDimension.percent(1f)), alignItemsCenter(), flexShrink(0), padding(4, 0))
        );

        var title = LabelWidget.of("CloudLib UI Showcase").setColor(0xFF3F3F3F).setShadow(false);
        title.useStyle(UIStyle.of(flexShrink(0)));
        header.addWidget(title);

        header.addWidget(SpacerWidget.create());

        // Search field
        var search = TextFieldWidget.create().setPlaceholder("Search components...").onTextChanged(t -> inputText = t);
        search.useStyle(UIStyle.of(sizeOf(140, 20), flexShrink(0)));
        header.addWidget(search);

        // Dark mode toggle
        var darkToggle = ToggleWidget.create(false).onToggle(state -> {
            darkMode = state;
            updateStatus("Dark mode: " + state);
        });
        darkToggle.useStyle(UIStyle.of(sizeOf(36, 18), flexShrink(0)));
        header.addWidget(darkToggle);

        return header;
    }

    // ==================== Controls Panel (Left) ====================

    private Widget createControlsPanel() {
        var panel = PanelWidget.create("Controls");
        panel.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(0.33f)), minWidth(180), flexGrow(1), minHeight(200)));
        panel.setContentPadding(8);

        var content = ColumnWidget.create(8);
        content.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        // Buttons section
        content.addWidget(createSectionLabel("Buttons"));
        content.addWidget(createButtonGrid());

        // Slider section
        content.addWidget(createDivider());
        content.addWidget(createSectionLabel("Slider"));
        content.addWidget(createSliderSection());

        // Progress section
        content.addWidget(createDivider());
        content.addWidget(createSectionLabel("Progress"));
        content.addWidget(createProgressSection());

        panel.addChild(content);
        return panel;
    }

    private Widget createButtonGrid() {
        var grid = ColumnWidget.create(4);

        // Row 1: Primary actions
        var row1 = RowWidget.create(4);
        row1.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        var btn1 = ButtonWidget.of("Click Me", () -> {
            clickCount++;
            updateStatus("Button clicked " + clickCount + " times");
        });
        btn1.useStyle(UIStyle.of(flexGrow(1), heightOf(20)));
        row1.addWidget(btn1);

        var btn2 = ButtonWidget.of("Reset", () -> {
            clickCount = 0;
            progressValue = 0;
            updateStatus("Reset");
        });
        btn2.useStyle(UIStyle.of(flexGrow(1), heightOf(20)));
        row1.addWidget(btn2);

        grid.addWidget(row1);

        // Row 2: Disabled + long text
        var disabled = ButtonWidget.of("Disabled Button").setEnabled(false);
        disabled.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), heightOf(20)));
        grid.addWidget(disabled);

        // Row 3: Small buttons
        var row3 = RowWidget.create(4);
        row3.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        for (int i = 1; i <= 3; i++) {
            int idx = i;
            var btn = ButtonWidget.of("B" + i, () -> updateStatus("Button " + idx + " pressed"));
            btn.useStyle(UIStyle.of(flexGrow(1), heightOf(18)));
            row3.addWidget(btn);
        }
        grid.addWidget(row3);

        return grid;
    }

    private Widget createSliderSection() {
        var section = ColumnWidget.create(4);

        // Slider value label
        section.addWidget(sliderValueLabel);

        // Horizontal slider
        var slider = SliderWidget.create(0, 100, 50).onValueChanged(v -> {
            sliderValue = v;
            sliderValueLabel.setText("Value: " + v.intValue());
        });
        slider.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), heightOf(20)));
        section.addWidget(slider);

        // Vertical sliders row
        var vRow = RowWidget.create(6);
        vRow.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), justifyCenter()));

        for (int i = 0; i < 4; i++) {
            double initial = 20 + i * 20;
            var vs = SliderWidget.create(0, 100, initial).setOrientation(SliderWidget.Orientation.vertical)
                    .onValueChanged(v -> {});
            vs.useStyle(UIStyle.of(widthOf(16), heightOf(50)));
            vRow.addWidget(vs);
        }
        section.addWidget(vRow);

        return section;
    }

    private Widget createProgressSection() {
        var section = ColumnWidget.create(4);

        // Auto-progress toggle
        var autoRow = RowWidget.create(8);
        autoRow.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), alignItemsCenter()));

        var autoLabel = LabelWidget.of("Auto animate").setColor(0xFF3F3F3F);
        autoRow.addWidget(autoLabel);
        autoRow.addWidget(SpacerWidget.create());

        var autoToggle = ToggleWidget.create(true).onToggle(state -> autoProgress = state);
        autoToggle.useStyle(UIStyle.of(sizeOf(36, 18)));
        autoRow.addWidget(autoToggle);
        section.addWidget(autoRow);

        // Horizontal progress bar
        var hProgress = ProgressBarWidget.create(() -> progressValue)
                .setDirection(ProgressBarWidget.Direction.leftToRight).setColors(0xFF555555, 0xFF3A8CFF);
        hProgress.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), heightOf(14)));
        section.addWidget(hProgress);

        // Vertical progress bars
        var vRow = RowWidget.create(4);
        vRow.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), justifyCenter()));

        int[] colors = {0xFF22C55E, 0xFFF97316, 0xFFEF4444, 0xFF8B5CF6};
        for (int i = 0; i < colors.length; i++) {
            double factor = 0.7 - i * 0.15;
            var vp = ProgressBarWidget.create(() -> Math.min(1.0, progressValue * factor + 0.1))
                    .setDirection(ProgressBarWidget.Direction.bottomToTop).setColors(0xFF555555, colors[i]);
            vp.useStyle(UIStyle.of(widthOf(22), heightOf(50)));
            vRow.addWidget(vp);
        }
        section.addWidget(vRow);

        // Progress controls
        var ctrlRow = RowWidget.create(4);
        ctrlRow.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        var dec = ButtonWidget.of("-10%", () -> progressValue = Math.max(0, progressValue - 0.1));
        dec.useStyle(UIStyle.of(flexGrow(1), heightOf(18)));
        ctrlRow.addWidget(dec);

        var inc = ButtonWidget.of("+10%", () -> progressValue = Math.min(1, progressValue + 0.1));
        inc.useStyle(UIStyle.of(flexGrow(1), heightOf(18)));
        ctrlRow.addWidget(inc);

        section.addWidget(ctrlRow);

        return section;
    }

    // ==================== Preview Panel (Center) ====================

    private Widget createPreviewPanel() {
        var panel = PanelWidget.create("Preview");
        panel.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(0.33f)), minWidth(180), flexGrow(1), minHeight(200)));
        panel.setContentPadding(8);

        var content = ColumnWidget.create(8);
        content.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        // Toggle section
        content.addWidget(createSectionLabel("Toggles"));
        content.addWidget(createToggleGrid());

        // Text input section
        content.addWidget(createDivider());
        content.addWidget(createSectionLabel("Text Input"));

        var inputField = TextFieldWidget.create().setPlaceholder("Type something...").onTextChanged(t -> {
            inputText = t;
            inputEchoLabel.setText("Echo: " + t);
        });
        inputField.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), heightOf(20)));
        content.addWidget(inputField);

        content.addWidget(inputEchoLabel);

        // Multi-line text display
        content.addWidget(createDivider());
        content.addWidget(createSectionLabel("Info"));
        content.addWidget(createInfoBox());

        panel.addChild(content);
        return panel;
    }

    private Widget createToggleGrid() {
        var grid = ColumnWidget.create(4);

        String[] labels = {"Enable feature A", "Show debug info", "Auto-save", "Verbose logging"};
        for (int i = 0; i < labels.length; i++) {
            var row = RowWidget.create(8);
            row.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), alignItemsCenter()));

            var label = LabelWidget.of(labels[i]).setColor(0xFF3F3F3F);
            label.useStyle(UIStyle.of(flexGrow(1)));
            row.addWidget(label);

            int idx = i;
            var toggle = ToggleWidget.create(i % 2 == 0).onToggle(state -> updateStatus(labels[idx] + ": " + state));
            toggle.useStyle(UIStyle.of(sizeOf(36, 18), flexShrink(0)));
            row.addWidget(toggle);

            grid.addWidget(row);
        }

        return grid;
    }

    private Widget createInfoBox() {
        var box = BoxWidget.create();
        box.useStyle(
            UIStyle.of(
                widthOf(TaffyDimension.percent(1f)),
                background(Textures.inset),
                padding(8),
                flexColumn(),
                rowGap(4)
            )
        );

        String[] infos = {"Framework: CloudLib UI", "Theme: Cirrus Light", "Layout: Taffy Flexbox",
                "Rendering: SceneCanvas"};

        for (String info : infos) {
            var label = LabelWidget.of(info).setColor(0xFF555555);
            label.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));
            box.addChild(label);
        }

        return box;
    }

    // ==================== List Panel (Right) ====================

    private Widget createListPanel() {
        var panel = PanelWidget.create("Items");
        panel.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(0.33f)), minWidth(180), flexGrow(1), minHeight(200)));
        panel.setContentPadding(6);

        var content = ColumnWidget.create(4);
        content.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        // List items
        String[] items = {"Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape", "Honeydew"};

        for (int i = 0; i < items.length; i++) {
            int idx = i;
            var item = RowWidget.create(8);
            item.useStyle(
                UIStyle.of(
                    widthOf(TaffyDimension.percent(1f)),
                    padding(4, 6),
                    alignItemsCenter(),
                    background(i % 2 == 0 ? Textures.flat : Textures.inset)
                )
            );

            var num = LabelWidget.of(String.valueOf(i + 1)).setColor(0xFF3A8CFF);
            num.useStyle(UIStyle.of(widthOf(16), flexShrink(0)));
            item.addWidget(num);

            var name = LabelWidget.of(items[i]).setColor(0xFF3F3F3F);
            name.useStyle(UIStyle.of(flexGrow(1)));
            item.addWidget(name);

            var selectBtn = ButtonWidget.of(">", () -> updateStatus("Selected: " + items[idx]));
            selectBtn.useStyle(UIStyle.of(sizeOf(20, 16), flexShrink(0)));
            item.addWidget(selectBtn);

            content.addWidget(item);
        }

        // Add item row
        var addRow = RowWidget.create(4);
        addRow.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f))));

        var addBtn = ButtonWidget.of("+ Add Item", () -> updateStatus("Item added"));
        addBtn.useStyle(UIStyle.of(flexGrow(1), heightOf(18)));
        addRow.addWidget(addBtn);

        content.addWidget(addRow);

        panel.addChild(content);
        return panel;
    }

    // ==================== Color Showcase ====================

    private Widget createColorShowcase() {
        var section = ColumnWidget.create(6);
        section.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), flexShrink(0)));

        section.addWidget(createSectionLabel("Texture Showcase"));

        var row = RowWidget.create(4);
        row.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), flexWrap(), rowGap(4)));

        // Texture swatches
        var swatches = new Object[][]{{"FRAME", Textures.frame}, {"FLAT", Textures.flat}, {"INSET", Textures.inset},
                {"DARK", Textures.dark}, {"OUTLINED_FLAT", Textures.outlinedFlat},
                {"OUTLINED_INSET", Textures.outlinedInset}, {"BORDER_DARK", Textures.borderDark},
                {"BORDER_LIGHT", Textures.borderLight}, {"SCROLL_TRACK", Textures.scrollTrack},};

        for (Object[] sw : swatches) {
            var swatch = BoxWidget.create();
            swatch.useStyle(
                UIStyle.of(
                    widthOf(70),
                    heightOf(40),
                    flexShrink(0),
                    background((VisualTexture) sw[1]),
                    alignItemsCenter(),
                    justifyCenter()
                )
            );

            var label = LabelWidget.of((String) sw[0]).setColor(0xFF3F3F3F).setShadow(false);
            label.useStyle(UIStyle.of(heightOf(10)));
            swatch.addChild(label);
            row.addWidget(swatch);
        }

        section.addWidget(row);
        return section;
    }

    // ==================== Footer ====================

    private Widget createFooter() {
        var footer = RowWidget.create(12);
        footer.useStyle(
            UIStyle.of(widthOf(TaffyDimension.percent(1f)), alignItemsCenter(), flexShrink(0), padding(4, 0))
        );

        statusLabel.useStyle(UIStyle.of(flexGrow(1)));
        footer.addWidget(statusLabel);

        clickCountLabel.useStyle(UIStyle.of(flexShrink(0)));
        footer.addWidget(clickCountLabel);

        var closeBtn = ButtonWidget.of("Close", this::onClose);
        closeBtn.useStyle(UIStyle.of(sizeOf(60, 20), flexShrink(0)));
        footer.addWidget(closeBtn);

        return footer;
    }

    // ==================== Helpers ====================

    private Widget createSectionLabel(String text) {
        var label = LabelWidget.of(text).setColor(0xFF2E7D6A).setShadow(false);
        label.useStyle(UIStyle.of(heightOf(12), flexShrink(0)));
        return label;
    }

    private Widget createDivider() {
        var divider = DividerWidget.horizontal().setColor(0xFF8B8B8B);
        divider.useStyle(UIStyle.of(widthOf(TaffyDimension.percent(1f)), heightOf(1), flexShrink(0)));
        return divider;
    }

    private void updateStatus(String message) {
        statusLabel.setText("Status: " + message);
        clickCountLabel.setText("Clicks: " + clickCount);
    }

    @Override
    public void tick() {
        super.tick();
        if (autoProgress) {
            progressValue += 0.008;
            if (progressValue > 1.0) progressValue = 0.0;
        }
    }
}
