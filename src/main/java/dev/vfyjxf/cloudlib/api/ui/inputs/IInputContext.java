package dev.vfyjxf.cloudlib.api.ui.inputs;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.math.FloatingPoint;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.inputs.InputContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;

public interface IInputContext {

    static IInputContext fromMouse(double x, double y, int button, boolean isReleased) {
        return new InputContext(InputConstants.Type.MOUSE.getOrCreate(button), x, y, 0, isReleased);
    }

    static IInputContext fromMouse(double x, double y, int button) {
        return new InputContext(InputConstants.Type.MOUSE.getOrCreate(button), x, y, 0, false);
    }

    static IInputContext fromKeyboard(int keyCode, int scanCode, int modifiers, double mouseX, double mouseY, boolean isReleased) {
        return new InputContext(InputConstants.getKey(keyCode, scanCode), mouseX, mouseY, modifiers, isReleased);
    }

    static IInputContext fromKeyboard(int keyCode, int scanCode, int modifiers, double mouseX, double mouseY) {
        return new InputContext(InputConstants.getKey(keyCode, scanCode), mouseX, mouseY, modifiers, false);
    }

    InputConstants.Key key();

    double mouseX();

    double mouseY();

    /**
     * for key to use.
     */
    int modifiers();

    boolean released();

    default boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    default boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    default boolean isAltDown() {
        return Screen.hasAltDown();
    }

    default boolean isMouse() {
        return this.key().getType() == InputConstants.Type.MOUSE;
    }

    default boolean isLeftClick() {
        return isMouse() && key().getValue() == 0;
    }

    default boolean isRightClick() {
        return isMouse() && key().getValue() == 1;
    }

    default boolean isKeyboard() {
        return this.key().getType() == InputConstants.Type.KEYSYM;
    }

    boolean isAllowedChatCharacter();

    boolean is(KeyMapping keyMapping);

    default boolean is(InputConstants.Key key) {
        return this.key().equals(key);
    }

    default FloatingPoint getRelative(IWidget widget) {
        IWidgetGroup<?> parent = widget.parent();
        if (parent == null) return new FloatingPoint(mouseX(), mouseY());
        Point absolute = widget.getAbsolute();
        return new FloatingPoint(mouseX() - absolute.x, mouseY() - absolute.y);
    }

}
