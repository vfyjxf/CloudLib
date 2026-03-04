package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.Inspector;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingEffect;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddleware;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.ui.widget.*;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.floating;
import static dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares.*;

/**
 * Test screen for the floating positioning system.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>All 12 placements (top/bottom/left/right × start/center/end)</li>
 *   <li>Offset middleware for spacing</li>
 *   <li>Flip middleware for automatic boundary avoidance</li>
 *   <li>Shift middleware for keeping elements in view</li>
 *   <li>Combined middleware pipeline (offset + flip + shift)</li>
 *   <li>Dynamic placement switching</li>
 * </ul>
 */
//@TestScreen
public class TestFloatingScreen extends BasicScreen {

    // ========== State ==========
    private FloatingPlacement currentPlacement = FloatingPlacement.bottom;
    private boolean enableFlip = true;
    private boolean enableShift = true;
    private int offsetDistance = 6;

    // ========== Widgets ==========
    private LabelWidget statusLabel;
    private BoxWidget referenceBox;
    private LabelWidget floatingLabel;
    private FloatingEffect activeEffect;

    public TestFloatingScreen() {
        buildUI();
    }

    private void buildUI() {
        // Root layout: vertical column
        var root = ColumnWidget.create(8);
        root.useStyle(UIStyle.of(
                UIStyles.flex(1),
                UIStyles.minSize(0, 0),
                UIStyles.padding(12),
                UIStyles.background(new ColorTexture(0xCC222222))
        ));

        // Title
        root.addWidget(createTitle());
        root.addWidget(createDivider());

        // Controls
        root.addWidget(createPlacementControls());
        root.addWidget(createMiddlewareControls());
        root.addWidget(createDivider());

        // Demo area with reference + floating
        root.addWidget(createDemoArea());
        root.addWidget(createDivider());

        // All-placements showcase
        root.addWidget(createPlacementShowcase());
        root.addWidget(createDivider());

        // Status bar
        root.addWidget(createStatusBar());

        mainGroup().addWidget(
                Inspector.create()
                         .setTrackMouse(true)
                         .setDisplayMode(Inspector.DisplayMode.FULL)
                         .useStyle(UIStyles.positionAbsolute(), UIStyles.sizeOf(280, 180))
                         .setSceneLayer(SceneLayer.debug)
        );

        mainGroup().addWidget(root);
    }

    // ========== Title ==========

    private Widget createTitle() {
        var header = RowWidget.create(8);
        header.useStyle(UIStyle.of(UIStyles.alignItemsCenter()));

        var title = LabelWidget.of("FloatingEffect Test")
                               .setColor(0xFFFFAA00)
                               .setShadow(true);
        title.useStyle(UIStyle.of(UIStyles.sizeOf(200, 12)));
        header.addWidget(title);

        return header;
    }

    // ========== FloatingPlacement Controls ==========

    private Widget createPlacementControls() {
        var panel = ColumnWidget.create(4);
        panel.useStyle(UIStyle.of(
                UIStyles.padding(6),
                UIStyles.background(new ColorTexture(0x40000000))
        ));

        var label = LabelWidget.of("FloatingPlacement:").setColor(0xFFAAAA00);
        label.useStyle(UIStyle.of(UIStyles.heightOf(12)));
        panel.addWidget(label);

        // Row 1: Top placements
        var row1 = RowWidget.create(4);
        row1.useStyle(UIStyle.of(UIStyles.alignItemsCenter(), UIStyles.flexWrap(), UIStyles.rowGap(4)));
        row1.addWidget(createPlacementButton("top", FloatingPlacement.top, 0xFF3366CC));
        row1.addWidget(createPlacementButton("topStart", FloatingPlacement.topStart, 0xFF2255BB));
        row1.addWidget(createPlacementButton("topEnd", FloatingPlacement.topEnd, 0xFF4477DD));
        panel.addWidget(row1);

        // Row 2: Bottom placements
        var row2 = RowWidget.create(4);
        row2.useStyle(UIStyle.of(UIStyles.alignItemsCenter(), UIStyles.flexWrap(), UIStyles.rowGap(4)));
        row2.addWidget(createPlacementButton("bottom", FloatingPlacement.bottom, 0xFF33CC66));
        row2.addWidget(createPlacementButton("bottomStart", FloatingPlacement.bottomStart, 0xFF22BB55));
        row2.addWidget(createPlacementButton("bottomEnd", FloatingPlacement.bottomEnd, 0xFF44DD77));
        panel.addWidget(row2);

        // Row 3: Left/Right placements
        var row3 = RowWidget.create(4);
        row3.useStyle(UIStyle.of(UIStyles.alignItemsCenter(), UIStyles.flexWrap(), UIStyles.rowGap(4)));
        row3.addWidget(createPlacementButton("left", FloatingPlacement.left, 0xFFCC6633));
        row3.addWidget(createPlacementButton("leftStart", FloatingPlacement.leftStart, 0xFFBB5522));
        row3.addWidget(createPlacementButton("leftEnd", FloatingPlacement.leftEnd, 0xFFDD7744));
        row3.addWidget(SpacerWidget.create().useStyle(UIStyle.of(UIStyles.widthOf(8))));
        row3.addWidget(createPlacementButton("right", FloatingPlacement.right, 0xFFCC3366));
        row3.addWidget(createPlacementButton("rightStart", FloatingPlacement.rightStart, 0xFFBB2255));
        row3.addWidget(createPlacementButton("rightEnd", FloatingPlacement.rightEnd, 0xFFDD4477));
        panel.addWidget(row3);

        return panel;
    }

    // ========== FloatingMiddleware Controls ==========

    private Widget createMiddlewareControls() {
        var panel = RowWidget.create(4);
        panel.useStyle(UIStyle.of(
                UIStyles.alignItemsCenter(),
                UIStyles.padding(6),
                UIStyles.flexWrap(),
                UIStyles.rowGap(4),
                UIStyles.background(new ColorTexture(0x40000000))
        ));

        panel.addWidget(LabelWidget.of("FloatingMiddleware:").setColor(0xFF00AAAA));
        panel.addWidget(createButton("Toggle Flip", this::toggleFlip, 0xFF885500));
        panel.addWidget(createButton("Toggle Shift", this::toggleShift, 0xFF558800));
        panel.addWidget(createButton("Offset +2", this::increaseOffset, 0xFF666600));
        panel.addWidget(createButton("Offset -2", this::decreaseOffset, 0xFF666600));
        panel.addWidget(createButton("Apply", this::reapplyEffect, 0xFF008888));

        return panel;
    }

    // ========== Demo Area ==========

    private Widget createDemoArea() {
        var area = BoxWidget.create();
        area.useStyle(UIStyle.of(
                UIStyles.flex(1),
                UIStyles.minSize(0, 60),
                UIStyles.padding(0),
                UIStyles.alignItemsCenter(),
                UIStyles.justifyCenter(),
                UIStyles.background(new ColorTexture(0x20446688))
        ));

        // Reference element — a colored box in the center
        referenceBox = BoxWidget.create();
        referenceBox.useStyle(UIStyle.of(
                UIStyles.sizeOf(80, 32),
                UIStyles.background(new ColorTexture(0xFF4488CC)),
                UIStyles.alignItemsCenter(),
                UIStyles.justifyCenter()
        ));
        var refLabel = LabelWidget.of("Reference")
                                  .setColor(0xFFFFFFFF)
                                  .setShadow(true);
        refLabel.useStyle(UIStyle.of(UIStyles.sizeOf(60, 12)));
        referenceBox.addChild(refLabel);
        area.addChild(referenceBox);

        // Floating element — a tooltip-like label
        floatingLabel = LabelWidget.of("Floating: " + currentPlacement.name())
                                   .setColor(0xFFFFFF88)
                                   .setShadow(true);
        floatingLabel.useStyle(UIStyle.of(
                UIStyles.padding(4, 8),
                UIStyles.background(new ColorTexture(0xEE333333))
        ));

        // Apply the floating effect
        activeEffect = FloatingEffect.create(referenceBox, currentPlacement, buildMiddleware());
        floatingLabel.useEffect(activeEffect);
        area.addChild(floatingLabel);

        return area;
    }

    // ========== FloatingPlacement Showcase ==========

    /**
     * Creates a mini showcase that shows all 12 placements around a small reference box.
     */
    private Widget createPlacementShowcase() {
        var container = ColumnWidget.create(4);
        container.useStyle(UIStyle.of(
                UIStyles.padding(6),
                UIStyles.background(new ColorTexture(0x30000000))
        ));

        var label = LabelWidget.of("All Placements Preview (offset + flip + shift):")
                               .setColor(0xFF88AACC)
                               .setShadow(true);
        label.useStyle(UIStyle.of(UIStyles.heightOf(12)));
        container.addWidget(label);

        var area = BoxWidget.create();
        area.useStyle(UIStyle.of(
                UIStyles.heightOf(100),
                UIStyles.alignItemsCenter(),
                UIStyles.justifyCenter(),
                UIStyles.background(new ColorTexture(0x15FFFFFF))
        ));

        // Small reference
        var smallRef = BoxWidget.create();
        smallRef.useStyle(UIStyle.of(
                UIStyles.sizeOf(48, 24),
                UIStyles.background(new ColorTexture(0xFF558866)),
                UIStyles.alignItemsCenter(),
                UIStyles.justifyCenter()
        ));
        var smallRefLabel = LabelWidget.of("Ref")
                                       .setColor(0xFFFFFFFF);
        smallRefLabel.useStyle(UIStyle.of(UIStyles.sizeOf(24, 10)));
        smallRef.addChild(smallRefLabel);
        area.addChild(smallRef);

        // Create a tiny floating label for each of the 4 main placements
        FloatingPlacement[] showcasePlacements = {
                FloatingPlacement.top, FloatingPlacement.bottom, FloatingPlacement.left, FloatingPlacement.right
        };
        int[] colors = {0xFF6688CC, 0xFF66CC88, 0xFFCC8866, 0xFFCC6688};

        for (int i = 0; i < showcasePlacements.length; i++) {
            var p = showcasePlacements[i];
            var floater = LabelWidget.of(p.name())
                                     .setColor(colors[i])
                                     .setShadow(true);
            floater.useStyle(UIStyle.of(
                    UIStyles.padding(2, 4),
                    UIStyles.background(new ColorTexture(0xCC222222))
            ));
            floater.useEffect(floating(smallRef, p, offset(4), flip(), shift(2)));
            area.addChild(floater);
        }

        container.addWidget(area);
        return container;
    }

    // ========== Status Bar ==========

    private Widget createStatusBar() {
        var bar = RowWidget.create(8);
        bar.useStyle(UIStyle.of(UIStyles.alignItemsCenter()));

        statusLabel = LabelWidget.of(buildStatusText())
                                 .setColor(0xFFAAAAAA);
        statusLabel.useStyle(UIStyle.of(UIStyles.flexGrow(1)));
        bar.addWidget(statusLabel);

        return bar;
    }

    // ========== Actions ==========

    private void setPlacement(FloatingPlacement p) {
        currentPlacement = p;
        reapplyEffect();
    }

    private void toggleFlip() {
        enableFlip = !enableFlip;
        reapplyEffect();
    }

    private void toggleShift() {
        enableShift = !enableShift;
        reapplyEffect();
    }

    private void increaseOffset() {
        offsetDistance += 2;
        reapplyEffect();
    }

    private void decreaseOffset() {
        offsetDistance = Math.max(0, offsetDistance - 2);
        reapplyEffect();
    }

    private void reapplyEffect() {
        if (floatingLabel == null || referenceBox == null) return;
        floatingLabel.setText("Floating: " + currentPlacement.name());
        activeEffect = FloatingEffect.create(referenceBox, currentPlacement, buildMiddleware());
        floatingLabel.useEffect(activeEffect);
        updateStatus();
    }

    private FloatingMiddleware[] buildMiddleware() {
        return new FloatingMiddleware[]{
                offset(offsetDistance),
                enableFlip ? flip() : null,
                enableShift ? shift(4) : null
        };
    }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(buildStatusText());
        }
    }

    private String buildStatusText() {
        return String.format(
                "FloatingPlacement: %s | Offset: %d | Flip: %s | Shift: %s",
                currentPlacement.name(), offsetDistance,
                enableFlip ? "ON" : "OFF",
                enableShift ? "ON" : "OFF"
        );
    }

    // ========== Helpers ==========

    private Widget createDivider() {
        var divider = DividerWidget.horizontal().setColor(0xFF555555);
        divider.useStyle(UIStyle.of(UIStyles.heightOf(2)));
        return divider;
    }

    private ButtonWidget createPlacementButton(String label, FloatingPlacement placement, int color) {
        var btn = ButtonWidget.of(label, () -> setPlacement(placement))
                              .setColors(color, lighten(color), darken(color));
        btn.useStyle(UIStyle.of(UIStyles.minWidth(70), UIStyles.heightOf(14), UIStyles.padding(1, 4)));
        return btn;
    }

    private ButtonWidget createButton(String label, Runnable action, int color) {
        var btn = ButtonWidget.of(label, action)
                              .setColors(color, lighten(color), darken(color));
        btn.useStyle(UIStyle.of(UIStyles.minWidth(70), UIStyles.heightOf(16), UIStyles.padding(2)));
        return btn;
    }

    private static int lighten(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 40);
        int b = Math.min(255, (color & 0xFF) + 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 40);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 40);
        int b = Math.max(0, (color & 0xFF) - 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
