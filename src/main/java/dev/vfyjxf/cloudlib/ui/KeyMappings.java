package dev.vfyjxf.cloudlib.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.data.lang.LangBuilder;
import dev.vfyjxf.cloudlib.data.lang.LangProvider;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@LangProvider(Constants.modId)
public final class KeyMappings {

    private final static LangBuilder builder = LangBuilder.create(Constants.namespace, "keys");
    private static final String debug = builder.define("debug", "Debug").key();
    private static final String general = builder.define("general", "General").key();
    public static final KeyMapping refreshUI = new KeyMapping(
            builder.defineKey("rebuild_ui", "Rebuild UI"),
            KeyConflictContext.GUI,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            debug
    );
    public static final KeyMapping openDevTools = new KeyMapping(
            builder.defineKey("open_devtools", "Open DevTools"),
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F12,
            debug
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(openDevTools);
    }

}
