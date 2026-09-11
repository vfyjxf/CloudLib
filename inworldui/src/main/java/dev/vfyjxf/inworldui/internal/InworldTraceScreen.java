package dev.vfyjxf.inworldui.internal;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldOverlayScreen;
import dev.vfyjxf.inworldui.InworldKeyMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The transparent capture screen backing a trace session (the Witness-style
 * "solve mode"): while open, mouse motion no longer turns the camera — it is
 * unprojected onto the traced panel's surface instead, and movement keys stay
 * released (the player freezes in place, same as a Witness trace).
 * <p>
 * The session ends when the interact key is released (commit) or the screen
 * is dismissed (cancel via {@link #removed()}). Because {@code RenderGuiEvent}
 * stops while a screen is open, this screen also renders the in-world scene
 * itself so flat panels stay visible mid-trace.
 */
public final class InworldTraceScreen extends Screen implements InworldOverlayScreen {

    private final InworldManager manager;

    public InworldTraceScreen(InworldManager manager) {
        super(Component.empty());
        this.manager = manager;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //region render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        manager.renderTrace(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //trace mode keeps the world fully visible — no dimming, no blur
    }

    //endregion

    //region input → trace session

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        manager.traceMouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //a click mid-trace commits the stroke, mirroring Witness's click-off
        manager.endTrace(true);
        onClose();
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (InworldKeyMappings.interact.isActiveAndMatches(key)) {
            manager.endTrace(true);
            onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    //endregion

    //region lifecycle

    @Override
    public void tick() {
        //belt & suspenders for a missed keyReleased (e.g. focus loss): poll the
        //bound key directly — the stroke commits on release either way
        if (!manager.traceActive() || !manager.traceHeld()) {
            manager.endTrace(true);
            onClose();
            return;
        }
        manager.tickTraceLook();
        super.tick();
    }

    @Override
    public void removed() {
        manager.onTraceScreenRemoved();
    }

    //endregion
}
