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

    /**
     * Hold to enter the in-world inspect presentation: the camera-look mouse is
     * captured by a transparent overlay screen and panels flatten to screen space.
     */
    public static final KeyMapping inspect = new KeyMapping(
            "cloudlib.keys.inworld_inspect",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CloudLang.Keys.debug.key()
    );

    /** Cycle the in-world panel focus forward. */
    public static final KeyMapping focusNext = new KeyMapping(
            "cloudlib.keys.inworld_focus_next",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            CloudLang.Keys.debug.key()
    );

    /** Cycle the in-world panel focus backward. */
    public static final KeyMapping focusPrevious = new KeyMapping(
            "cloudlib.keys.inworld_focus_previous",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CloudLang.Keys.debug.key()
    );

    /**
     * Trigger the focused/soft-focused in-world panel's primary action —
     * the Watch-Dogs-style "look roughly at it, press the key" interaction.
     */
    public static final KeyMapping interact = new KeyMapping(
            "cloudlib.keys.inworld_interact",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CloudLang.Keys.debug.key()
    );

}
