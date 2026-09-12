package dev.vfyjxf.cloudlib.api.ui.inworld;

/**
 * Marker for the transparent input-capture screen used by inspect mode.
 * <p>
 * While the inspect key is held, a screen implementing this interface is open
 * on the client — {@code Minecraft.getInstance().screen != null} even though
 * nothing is visually presented as a screen. Mods and integrations that treat
 * "any open screen" as "the player is in a menu" should special-case this
 * marker.
 */
public interface InworldOverlayScreen {}
