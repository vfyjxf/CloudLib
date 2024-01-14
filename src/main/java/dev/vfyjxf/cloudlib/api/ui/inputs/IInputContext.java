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

    static IInputContext fromMouse(double x, double y, int button) {
        return new InputContext(InputConstants.Type.MOUSE.getOrCreate(button), x, y, 0);
    }

    static IInputContext fromKeyboard(int keyCode, int scanCode, int modifiers, double mouseX, double mouseY) {
        return new InputContext(InputConstants.getKey(keyCode, scanCode), mouseX, mouseY, modifiers);
    }

    InputConstants.Key getKey();

    double getMouseX();

    double getMouseY();

    /**
     * for key to use.
     */
    int getModifiers();

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
        return this.getKey().getType() == InputConstants.Type.MOUSE;
    }

    default boolean isLeftClick() {
        return isMouse() && getKey().getValue() == 0;
    }

    default boolean isRightClick() {
        return isMouse() && getKey().getValue() == 1;
    }

    default boolean isKeyboard() {
        return this.getKey().getType() == InputConstants.Type.KEYSYM;
    }

    boolean isAllowedChatCharacter();

    boolean is(KeyMapping keyMapping);

    default boolean is(InputConstants.Key key) {
        return this.getKey().equals(key);
    }

    default FloatingPoint getRelative(IWidget widget) {
        IWidgetGroup<?> parent = widget.getParent();
        if (parent == null) return new FloatingPoint(getMouseX(), getMouseY());
        Point absolute = widget.getAbsolute();
        return new FloatingPoint(getMouseX() - absolute.x, getMouseY() - absolute.y);
    }

}
