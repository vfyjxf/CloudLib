package dev.vfyjxf.nimbusprojection;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.nimbusprojection.data.lang.NimbusLang;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class NimbusKeyMappings {

    /**
     * Hold to enter the inspect presentation: the camera-look mouse is
     * captured by a transparent overlay screen and panels flatten to screen space.
     */
    public static final KeyMapping inspect = new KeyMapping(
            NimbusLang.Keys.inspect.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            NimbusLang.Keys.generalCategory.key());

    /** Cycle the panel focus forward. */
    public static final KeyMapping focusNext = new KeyMapping(
            NimbusLang.Keys.focusNext.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            NimbusLang.Keys.generalCategory.key());

    /** Cycle the panel focus backward. */
    public static final KeyMapping focusPrevious = new KeyMapping(
            NimbusLang.Keys.focusPrevious.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            NimbusLang.Keys.generalCategory.key());

    /**
     * Trigger the focused/soft-focused panel's primary action — the
     * "look roughly at it, press the key" interaction. Held on a
     * {@code InworldTraceable} panel it enters trace mode instead; held on a
     * plain panel it activates the crosshair pointer into panel surfaces.
     */
    public static final KeyMapping interact = new KeyMapping(
            NimbusLang.Keys.interact.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            NimbusLang.Keys.generalCategory.key());

    /**
     * Summon/dismiss the inventory satellite panel manually — independent of
     * the auto-summon that engaging a container triggers.
     */
    public static final KeyMapping inventory = new KeyMapping(
            NimbusLang.Keys.inventory.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            NimbusLang.Keys.generalCategory.key());

    /**
     * Pin/unpin the focused panel — a pinned panel docks to the screen edge
     * and survives focus loss, out-of-range and anchor-offscreen.
     */
    public static final KeyMapping pin = new KeyMapping(
            NimbusLang.Keys.pin.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            NimbusLang.Keys.generalCategory.key());

    /**
     * Hold to escalate the focused panel's detail tier (effects → equipment →
     * debug). Release snaps back to the config-driven base tier.
     */
    public static final KeyMapping detail = new KeyMapping(
            NimbusLang.Keys.detail.key(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            NimbusLang.Keys.generalCategory.key());

    private NimbusKeyMappings() {}
}
