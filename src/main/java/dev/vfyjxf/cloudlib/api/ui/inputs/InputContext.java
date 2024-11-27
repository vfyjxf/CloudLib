package dev.vfyjxf.cloudlib.api.ui.inputs;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.StringUtil;

public record InputContext(
        InputConstants.Key key,
        double mouseX, double mouseY,
        int modifiers,
        boolean released
) {

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

    public boolean isAllowedChatCharacter() {
        return isKeyboard() && StringUtil.isAllowedChatCharacter((char) this.key.getValue());
    }

    public boolean is(KeyMapping keyMapping) {
        return keyMapping.isActiveAndMatches(this.key);
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

    public boolean isLeftClick() {
        return isMouse() && key().getValue() == 0;
    }

    public boolean isRightClick() {
        return isMouse() && key().getValue() == 1;
    }

    public boolean isKeyboard() {
        return this.key().getType() == InputConstants.Type.KEYSYM;
    }

    public boolean is(InputConstants.Key key) {
        return this.key().equals(key);
    }
}
