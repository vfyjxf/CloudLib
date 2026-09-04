package dev.vfyjxf.cloudlib.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.data.lang.CloudLang;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class KeyMappings {

    public static final KeyMapping refreshUI = new KeyMapping(
            CloudLang.Keys.rebuildUi.key(),
            KeyConflictContext.GUI,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CloudLang.Keys.debug.key()
    );

}
