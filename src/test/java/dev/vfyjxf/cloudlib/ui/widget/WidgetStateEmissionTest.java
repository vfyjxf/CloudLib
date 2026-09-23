package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The selector surface the stock interactive widgets emit — the states a theme
 * keys its variants off: hover, press, checked, disabled and focus.
 */
class WidgetStateEmissionTest {

    // region button

    @Test
    void aDisabledButtonSaysSo() {
        ButtonWidget button = ButtonWidget.of("ok");
        assertTrue(button.styleStates().contains("enabled"));
        assertFalse(button.styleStates().contains("disabled"));

        button.setEnabled(false);
        assertTrue(button.styleStates().contains("disabled"), "setEnabled(false) is the button's own gate");
        assertFalse(button.styleStates().contains("enabled"));

        button.setEnabled(true);
        assertTrue(button.styleStates().contains("enabled"));
        assertFalse(button.styleStates().contains("disabled"), "re-enabling clears the state");
    }

    @Test
    void theFrameworkDisabledFlagStillReports() {
        ButtonWidget button = ButtonWidget.of("ok");
        button.setActive(false);
        assertTrue(button.styleStates().contains("disabled"), "!active is the framework's own disabled");
        assertTrue(button.styleStates().contains("inactive"));
    }

    @Test
    void aButtonRunsTheWholeHoverPressReleaseCycle() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            ButtonWidget button = fixture.add(ButtonWidget.of("ok"));
            button.useStyle(UIStyles.sizeOf(40, 20));
            fixture.stabilize();
            assertFalse(button.styleStates().contains("hovered"));

            fixture.scene.mouseMoved(4, 4);
            assertTrue(button.styleStates().contains("hovered"));
            assertTrue(button.styleStates().contains("hover"), "hover and hovered are the same pseudo");

            fixture.scene.mouseClicked(4, 4, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            assertTrue(button.styleStates().contains("pressed"));

            fixture.scene.mouseReleased(4, 4, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            assertFalse(button.styleStates().contains("pressed"), "the release clears the press");

            fixture.scene.mouseMoved(60, 30);
            assertFalse(button.styleStates().contains("hovered"), "the pointer left the button");
        }
    }

    // endregion

    // region toggle

    @Test
    void aToggleReportsCheckedAndPressed() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            ToggleWidget toggle = fixture.add(ToggleWidget.create(false));
            toggle.useStyle(UIStyles.sizeOf(20, 10));
            fixture.stabilize();
            assertFalse(toggle.styleStates().contains("checked"));

            fixture.scene.mouseMoved(4, 4);
            assertTrue(toggle.styleStates().contains("hovered"));
            fixture.scene.mouseClicked(4, 4, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            assertTrue(toggle.styleStates().contains("pressed"), "a press shows before the release");

            fixture.scene.mouseReleased(4, 4, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            assertFalse(toggle.styleStates().contains("pressed"));
            assertTrue(toggle.toggled(), "the click flipped it on");
            assertTrue(toggle.styleStates().contains("checked"));

            toggle.setToggled(false);
            assertFalse(toggle.styleStates().contains("checked"));
        }
    }

    // endregion

    // region text field

    @Test
    void aReadOnlyTextFieldSaysItIsDisabled() {
        TextFieldWidget field = TextFieldWidget.create();
        assertFalse(field.styleStates().contains("disabled"));

        field.setEditable(false);
        assertTrue(field.styleStates().contains("disabled"), "a field nobody can type into is the disabled one");

        field.setEditable(true);
        assertFalse(field.styleStates().contains("disabled"));
    }

    @Test
    void aTextFieldTakesItsCaretColourFromTheTheme() {
        TextFieldWidget field = TextFieldWidget.create();
        assertEquals(0xFF3F3F3F, field.caretColor(), "the code default");

        Theme theme = new Theme(
            ResourceLocation.fromNamespaceAndPath("test", "text-field"),
            CssParser.parse("text-field { cursor-color: #123456 }")
        );
        field.applyThemeStyle(theme.resolve(field));
        assertEquals(0xFF123456, field.caretColor(), "the sheet's cursor-color wins");
    }

    @Test
    void aFocusedTextFieldReportsFocusAndHover() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            TextFieldWidget field = fixture.add(TextFieldWidget.create());
            field.useStyle(UIStyles.sizeOf(60, 14));
            fixture.stabilize();
            assertFalse(field.styleStates().contains("focused"));

            fixture.scene.requestFocus(field);
            assertTrue(field.styleStates().contains("focused"));

            fixture.scene.mouseMoved(4, 4);
            assertTrue(field.styleStates().contains("hovered"));

            fixture.scene.clearFocus();
            assertFalse(field.styleStates().contains("focused"));
        }
    }

    // endregion
}
