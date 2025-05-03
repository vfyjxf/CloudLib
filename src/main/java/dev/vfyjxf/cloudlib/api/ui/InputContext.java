package dev.vfyjxf.cloudlib.api.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.StringUtil;
import net.neoforged.neoforge.client.event.InputEvent;

public record InputContext(
        InputConstants.Key key,
        double mouseX, double mouseY,
        int modifiers,
        KeyAction action
) {

    public static InputContext fromEvent(InputEvent.Key event) {
        var key = InputConstants.getKey(event.getKey(), event.getScanCode());
        FloatPos pos = ScreenUtil.getMousePos();
        return new InputContext(key, pos.x, pos.y, event.getModifiers(), KeyAction.from(event.getAction()));
    }

    public static InputContext fromEvent(InputEvent.MouseButton event) {
        var key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
        FloatPos pos = ScreenUtil.getMousePos();
        return new InputContext(key, pos.x, pos.y, 0, KeyAction.from(event.getAction()));
    }

    public static InputContext fromMouse(double x, double y, int button, boolean isReleased) {
        return new InputContext(InputConstants.Type.MOUSE.getOrCreate(button), x, y, 0, isReleased);
    }

    public static InputContext fromMouse(double x, double y, int button) {
        return new InputContext(InputConstants.Type.MOUSE.getOrCreate(button), x, y, 0, false);
    }

    public static InputContext fromKeyboard(int keyCode, int scanCode, int modifiers, double mouseX, double mouseY, boolean isReleased) {
        return new InputContext(InputConstants.getKey(keyCode, scanCode), mouseX, mouseY, modifiers, isReleased);
    }

    public static InputContext fromKeyboard(int keyCode, int scanCode, int modifiers, double mouseX, double mouseY) {
        return new InputContext(InputConstants.getKey(keyCode, scanCode), mouseX, mouseY, modifiers, false);
    }

    public InputContext(
            InputConstants.Key key,
            double mouseX, double mouseY,
            int modifiers,
            boolean isReleased
    ) {
        this(key, mouseX, mouseY, modifiers, isReleased ? KeyAction.RELEASE : KeyAction.PRESS);
    }

    public boolean isAllowedChatCharacter() {
        return isKeyboard() && StringUtil.isAllowedChatCharacter((char) this.key.getValue());
    }

    public boolean is(KeyMapping keyMapping) {
        return keyMapping.isActiveAndMatches(this.key);
    }

    public boolean released(KeyMapping keyMapping) {
        return keyMapping.isActiveAndMatches(this.key) && this.isRelease();
    }

    public boolean pressed(KeyMapping keyMapping) {
        return keyMapping.isActiveAndMatches(this.key) && this.isPress();
    }

    public boolean is(InputConstants.Key key) {
        return this.key().equals(key);
    }

    public boolean released(InputConstants.Key key) {
        return this.key().equals(key) && this.isRelease();
    }

    public boolean pressed(InputConstants.Key key) {
        return this.key().equals(key) && this.isPress();
    }

    public boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    public boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    public boolean isAltDown() {
        return Screen.hasAltDown();
    }

    public boolean isMouse() {
        return this.key().getType() == InputConstants.Type.MOUSE;
    }

    public Pos mousePos() {
        return new Pos((int) mouseX, (int) mouseY);
    }

    public boolean isLeftClick() {
        return isMouse() && key().getValue() == 0;
    }

    public boolean isRightClick() {
        return isMouse() && key().getValue() == 1;
    }

    public boolean isKeyboard() {
        return this.key().getType() == InputConstants.Type.KEYSYM;
    }

    public boolean isRelease() {
        return this.action() == KeyAction.RELEASE;
    }

    public boolean isPress() {
        return this.action() == KeyAction.PRESS;
    }

    public boolean isRepeat() {
        return this.action() == KeyAction.REPEAT;
    }

    /**
     * @param coordinate the widget to get the mouse position relative to
     * @return the mouse position relative to the widget
     */
    public FloatPos mouseRelative(Widget coordinate) {
        var parent = coordinate.parent();
        var pos = new FloatPos(mouseX, mouseY);
        while (parent != null) {
            pos.translate(-parent.posX(), -parent.posY());
            parent = parent.parent();
        }
        return pos;
    }

    public enum KeyAction {
        PRESS,
        RELEASE,
        REPEAT;

        public static KeyAction from(int action) {
            return switch (action) {
                case 0 -> PRESS;
                case 1 -> RELEASE;
                case 2 -> REPEAT;
                default -> throw new IllegalArgumentException("Unexpected value: " + action);
            };
        }
    }
}
