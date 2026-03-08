package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Lifecycle;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.ui.widget.*;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chainable assertions for a single widget.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * assertThat(widget)
 *     .isVisible()
 *     .hasSize(100, 30)
 *     .hasLabel("OK");
 * }</pre>
 */
public final class WidgetAssert {

    private final Widget widget;
    private final String description;

    public WidgetAssert(Widget widget) {
        this(widget, widget.toString());
    }

    public WidgetAssert(Widget widget, String description) {
        this.widget = widget;
        this.description = description;
    }

    public Widget widget() {
        return widget;
    }

    // ==================== Visibility & State ====================

    public WidgetAssert isVisible() {
        assertTrue(widget.visible(), msg("expected visible"));
        return this;
    }

    public WidgetAssert isNotVisible() {
        assertFalse(widget.visible(), msg("expected not visible"));
        return this;
    }

    public WidgetAssert isFocused() {
        assertTrue(widget.focused(), msg("expected focused"));
        return this;
    }

    public WidgetAssert isNotFocused() {
        assertFalse(widget.focused(), msg("expected not focused"));
        return this;
    }

    public WidgetAssert hasKey(Object expectedKey) {
        assertEquals(expectedKey, widget.key(), msg("key mismatch"));
        return this;
    }

    // ==================== Position & Size ====================

    public WidgetAssert hasPos(int x, int y) {
        assertEquals(x, widget.posX(), msg("posX mismatch"));
        assertEquals(y, widget.posY(), msg("posY mismatch"));
        return this;
    }

    public WidgetAssert hasAbsolutePos(int x, int y) {
        var abs = widget.absolutePos();
        assertEquals(x, abs.x(), msg("absolute posX mismatch"));
        assertEquals(y, abs.y(), msg("absolute posY mismatch"));
        return this;
    }

    public WidgetAssert hasSize(int width, int height) {
        assertEquals(width, widget.width(), msg("width mismatch"));
        assertEquals(height, widget.height(), msg("height mismatch"));
        return this;
    }

    public WidgetAssert hasWidth(int width) {
        assertEquals(width, widget.width(), msg("width mismatch"));
        return this;
    }

    public WidgetAssert hasHeight(int height) {
        assertEquals(height, widget.height(), msg("height mismatch"));
        return this;
    }

    // ==================== Type-specific: ButtonWidget ====================

    public WidgetAssert hasLabel(String expected) {
        assertIsType(ButtonWidget.class);
        var btn = (ButtonWidget) widget;
        assertEquals(expected, btn.label().getString(), msg("button label mismatch"));
        return this;
    }

    public WidgetAssert hasLabel(Component expected) {
        assertIsType(ButtonWidget.class);
        var btn = (ButtonWidget) widget;
        assertEquals(expected, btn.label(), msg("button label mismatch"));
        return this;
    }

    public WidgetAssert isEnabled() {
        assertIsType(ButtonWidget.class);
        var btn = (ButtonWidget) widget;
        assertTrue(btn.enabled(), msg("expected enabled"));
        return this;
    }

    public WidgetAssert isDisabled() {
        assertIsType(ButtonWidget.class);
        var btn = (ButtonWidget) widget;
        assertFalse(btn.enabled(), msg("expected disabled"));
        return this;
    }

    // ==================== Type-specific: LabelWidget / TextWidget ====================

    public WidgetAssert hasText(String expected) {
        String actual = WidgetInspector.textOf(widget);
        assertNotNull(actual, msg("widget is not a text-bearing widget"));
        assertEquals(expected, actual, msg("text mismatch"));
        return this;
    }

    public WidgetAssert hasText(Component expected) {
        if (widget instanceof LabelWidget lbl) {
            assertEquals(expected, lbl.text(), msg("text mismatch"));
        } else if (widget instanceof TextWidget txt) {
            assertEquals(expected, txt.text(), msg("text mismatch"));
        } else {
            fail(msg("widget is not a LabelWidget or TextWidget"));
        }
        return this;
    }

    // ==================== Type-specific: TextFieldWidget ====================

    public WidgetAssert hasValue(String expected) {
        assertIsType(TextFieldWidget.class);
        var field = (TextFieldWidget) widget;
        assertEquals(expected, field.text(), msg("text field value mismatch"));
        return this;
    }

    public WidgetAssert isEditable() {
        assertIsType(TextFieldWidget.class);
        var field = (TextFieldWidget) widget;
        assertTrue(field.editable(), msg("expected editable"));
        return this;
    }

    public WidgetAssert isNotEditable() {
        assertIsType(TextFieldWidget.class);
        var field = (TextFieldWidget) widget;
        assertFalse(field.editable(), msg("expected not editable"));
        return this;
    }

    // ==================== Type-specific: ToggleWidget ====================

    public WidgetAssert isToggled() {
        assertIsType(ToggleWidget.class);
        var toggle = (ToggleWidget) widget;
        assertTrue(toggle.toggled(), msg("expected toggled"));
        return this;
    }

    public WidgetAssert isNotToggled() {
        assertIsType(ToggleWidget.class);
        var toggle = (ToggleWidget) widget;
        assertFalse(toggle.toggled(), msg("expected not toggled"));
        return this;
    }

    // ==================== Type-specific: SliderWidget ====================

    public WidgetAssert hasSliderValue(double expected) {
        assertIsType(SliderWidget.class);
        var slider = (SliderWidget) widget;
        assertEquals(expected, slider.value(), msg("slider value mismatch"));
        return this;
    }

    public WidgetAssert hasSliderValue(double expected, double tolerance) {
        assertIsType(SliderWidget.class);
        var slider = (SliderWidget) widget;
        assertEquals(expected, slider.value(), tolerance, msg("slider value mismatch"));
        return this;
    }

    // ==================== Active / Interactive state ====================

    public WidgetAssert isActive() {
        assertTrue(widget.active(), msg("expected active"));
        return this;
    }

    public WidgetAssert isNotActive() {
        assertFalse(widget.active(), msg("expected not active"));
        return this;
    }

    public WidgetAssert isInteractive() {
        assertTrue(widget.interactive(), msg("expected interactive"));
        return this;
    }

    public WidgetAssert isNotInteractive() {
        assertFalse(widget.interactive(), msg("expected not interactive"));
        return this;
    }

    public WidgetAssert isFocusable() {
        assertTrue(widget.focusable(), msg("expected focusable"));
        return this;
    }

    public WidgetAssert isNotFocusable() {
        assertFalse(widget.focusable(), msg("expected not focusable"));
        return this;
    }

    // ==================== Lifecycle ====================

    public WidgetAssert isMounted() {
        assertEquals(Lifecycle.mounted, widget.lifecycle(), msg("expected mounted"));
        return this;
    }

    public WidgetAssert hasLifecycle(Lifecycle expected) {
        assertEquals(expected, widget.lifecycle(), msg("lifecycle mismatch"));
        return this;
    }

    // ==================== Bounds range checks ====================

    public WidgetAssert containsPoint(int x, int y) {
        var abs = widget.absolutePos();
        assertTrue(x >= abs.x() && x <= abs.x() + widget.width()
                        && y >= abs.y() && y <= abs.y() + widget.height(),
                msg("point (" + x + "," + y + ") outside bounds " + boundsString()));
        return this;
    }

    private String boundsString() {
        var abs = widget.absolutePos();
        return "(" + abs.x() + "," + abs.y() + " " + widget.width() + "x" + widget.height() + ")";
    }

    // ==================== Tree structure ====================

    public WidgetAssert hasChildCount(int expected) {
        if (widget instanceof CompositeWidget<?> group) {
            assertEquals(expected, group.children().size(), msg("child count mismatch"));
        } else {
            if (expected != 0) {
                fail(msg("widget is not a CompositeWidget, cannot have children"));
            }
        }
        return this;
    }

    public WidgetAssert hasNoChildren() {
        return hasChildCount(0);
    }

    public WidgetAssert hasChild(WidgetFinder finder) {
        if (widget instanceof CompositeWidget<?> group) {
            var found = finder.findFirst(widget);
            assertNotNull(found, msg("expected child matching " + finder));
        } else {
            fail(msg("widget is not a CompositeWidget"));
        }
        return this;
    }

    public WidgetAssert hasParentOf(Class<? extends Widget> type) {
        assertNotNull(widget.parent(), msg("widget has no parent"));
        assertTrue(type.isInstance(widget.parent()), msg("parent type mismatch"));
        return this;
    }

    // ==================== Custom assertions ====================

    public WidgetAssert satisfies(Consumer<Widget> assertion) {
        assertion.accept(widget);
        return this;
    }

    public <T extends Widget> WidgetAssert satisfies(Class<T> type, Consumer<T> assertion) {
        assertIsType(type);
        assertion.accept(type.cast(widget));
        return this;
    }

    // ==================== Relative position assertions ====================

    /**
     * Asserts this widget's right edge is at or left of another widget's left edge.
     */
    public WidgetAssert isLeftOf(Widget other) {
        var thisAbs = widget.absolutePos();
        var otherAbs = other.absolutePos();
        assertTrue(thisAbs.x() + widget.width() <= otherAbs.x(),
                msg("expected left of " + descOf(other) + " but right edge="
                        + (thisAbs.x() + widget.width()) + " vs other left=" + otherAbs.x()));
        return this;
    }

    /**
     * Asserts this widget's bottom edge is at or above another widget's top edge.
     */
    public WidgetAssert isAbove(Widget other) {
        var thisAbs = widget.absolutePos();
        var otherAbs = other.absolutePos();
        assertTrue(thisAbs.y() + widget.height() <= otherAbs.y(),
                msg("expected above " + descOf(other) + " but bottom edge="
                        + (thisAbs.y() + widget.height()) + " vs other top=" + otherAbs.y()));
        return this;
    }

    /**
     * Asserts this widget is entirely within the bounds of the given parent widget.
     */
    public WidgetAssert isWithin(Widget container) {
        var thisAbs = widget.absolutePos();
        var contAbs = container.absolutePos();
        assertTrue(thisAbs.x() >= contAbs.x()
                        && thisAbs.y() >= contAbs.y()
                        && thisAbs.x() + widget.width() <= contAbs.x() + container.width()
                        && thisAbs.y() + widget.height() <= contAbs.y() + container.height(),
                msg("expected within " + descOf(container) + " bounds "
                        + boundsOf(container) + " but this widget is at " + boundsString()));
        return this;
    }

    /**
     * Asserts this widget's parent has the expected key.
     */
    public WidgetAssert hasParentKey(Object expectedKey) {
        assertNotNull(widget.parent(), msg("widget has no parent"));
        assertEquals(expectedKey, widget.parent().key(),
                msg("parent key mismatch: expected '" + expectedKey + "' but was '" + widget.parent().key() + "'"));
        return this;
    }

    /**
     * Asserts this widget's bounds are entirely within the given rectangular region.
     */
    public WidgetAssert hasBoundsWithin(int x, int y, int w, int h) {
        var abs = widget.absolutePos();
        assertTrue(abs.x() >= x && abs.y() >= y
                        && abs.x() + widget.width() <= x + w
                        && abs.y() + widget.height() <= y + h,
                msg("expected bounds within (" + x + "," + y + " " + w + "x" + h
                        + ") but widget is at " + boundsString()));
        return this;
    }

    // ==================== Type assertion ====================

    public WidgetAssert isOfType(Class<? extends Widget> type) {
        assertIsType(type);
        return this;
    }

    // ==================== Helpers ====================

    private void assertIsType(Class<? extends Widget> type) {
        assertTrue(type.isInstance(widget), msg("expected " + type.getSimpleName() + " but was " + widget.getClass().getSimpleName()));
    }

    private String msg(String detail) {
        return "[" + description + "] " + detail;
    }

    private static String descOf(Widget w) {
        return w.getClass().getSimpleName() + (w.key() != null ? "[" + w.key() + "]" : "");
    }

    private static String boundsOf(Widget w) {
        var abs = w.absolutePos();
        return "(" + abs.x() + "," + abs.y() + " " + w.width() + "x" + w.height() + ")";
    }
}
