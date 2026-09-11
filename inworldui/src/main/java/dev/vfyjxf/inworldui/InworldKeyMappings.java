package dev.vfyjxf.inworldui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class InworldKeyMappings {

    /**
     * Hold to enter the inspect presentation: the camera-look mouse is
     * captured by a transparent overlay screen and panels flatten to screen space.
     */
    public static final KeyMapping inspect = new KeyMapping(
            "inworldui.keys.inspect",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "inworldui.keys.category"
    );

    /** Cycle the panel focus forward. */
    public static final KeyMapping focusNext = new KeyMapping(
            "inworldui.keys.focus_next",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "inworldui.keys.category"
    );

    /** Cycle the panel focus backward. */
    public static final KeyMapping focusPrevious = new KeyMapping(
            "inworldui.keys.focus_previous",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "inworldui.keys.category"
    );

    /**
     * Trigger the focused/soft-focused panel's primary action — the
     * "look roughly at it, press the key" interaction. Held on a
     * {@code InworldTraceable} panel it enters trace mode instead.
     */
    public static final KeyMapping interact = new KeyMapping(
            "inworldui.keys.interact",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "inworldui.keys.category"
    );

    private InworldKeyMappings() {
    }
}
