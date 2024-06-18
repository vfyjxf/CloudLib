package dev.vfyjxf.cloudlib.ui.inputs;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.StringUtil;

public record InputContext(InputConstants.Key key, double mouseX, double mouseY, int modifiers,
                           boolean released) implements IInputContext {

    @Override
    public boolean isAllowedChatCharacter() {
        return isKeyboard() && StringUtil.isAllowedChatCharacter((char) this.key.getValue());
    }

    @Override
    public boolean is(KeyMapping keyMapping) {
        return keyMapping.isActiveAndMatches(this.key);
    }
}
