package dev.vfyjxf.cloudlib.ui.inputs;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.data.lang.LangBuilder;
import dev.vfyjxf.cloudlib.data.lang.LangProvider;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@LangProvider
public final class KeyMappings {

    private final static LangBuilder builder = LangBuilder.create(Constants.NAMESPACE, "keys");
    private static final String debug = builder.define("debug", "Debug").key();
    private static final String general = builder.define("general", "General").key();
    public static final KeyMapping refreshUI = new KeyMapping(
            builder.defineKey("refresh_ui", "Refresh UI"),
            KeyConflictContext.GUI,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            debug);

}
