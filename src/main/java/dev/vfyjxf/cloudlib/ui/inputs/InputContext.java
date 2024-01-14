package dev.vfyjxf.cloudlib.ui.inputs;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;

public class InputContext implements IInputContext {
    private final InputConstants.Key key;
    private final double mouseX;
    private final double mouseY;
    private final int modifiers;

    public InputContext(InputConstants.Key key, double mouseX, double mouseY, int modifiers) {
        this.key = key;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.modifiers = modifiers;
    }

    @Override
    public InputConstants.Key getKey() {
        return key;
    }

    @Override
    public double getMouseX() {
        return mouseX;
    }

    @Override
    public double getMouseY() {
        return mouseY;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public boolean isAllowedChatCharacter() {
        return isKeyboard() && SharedConstants.isAllowedChatCharacter((char) this.key.getValue());
    }

    @Override
    public boolean is(KeyMapping keyMapping) {
        return keyMapping.isActiveAndMatches(this.key);
    }
}
