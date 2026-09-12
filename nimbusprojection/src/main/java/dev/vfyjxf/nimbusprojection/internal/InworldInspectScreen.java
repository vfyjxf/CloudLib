package dev.vfyjxf.nimbusprojection.internal;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldOverlayScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The transparent, non-pausing screen backing inspect presentation.
 * <p>
 * While the inspect key is held this screen is open: the OS cursor is released
 * by vanilla (mouse no longer turns the camera), all input is routed into the
 * in-world scene instead of the game, and every panel is flattened to screen
 * space with leader lines back to its world anchor.
 * <p>
 * Movement keys (walk/jump/sprint/sneak) are forwarded to {@link KeyMapping#set}
 * when the UI doesn't consume them, so the player can keep walking while
 * inspecting — the same behaviour as holding a chat screen open, minus the
 * obstruction.
 * <p>
 * Marked with {@link InworldOverlayScreen} so other mods can recognise it as a
 * UI overlay rather than a menu.
 */
public final class InworldInspectScreen extends Screen implements InworldOverlayScreen {

    private final InworldManager manager;

    public InworldInspectScreen(InworldManager manager) {
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
        manager.renderInspect(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //inspect mode keeps the world fully visible — no dimming, no blur
    }

    //endregion

    //region input → in-world scene

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        manager.inspectMouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return manager.inspectMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return manager.inspectMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return manager.inspectMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return manager.inspectMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (manager.inspectKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        forwardGameplayKey(keyCode, scanCode, true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        manager.inspectKeyReleased(keyCode, scanCode, modifiers);
        forwardGameplayKey(keyCode, scanCode, false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return manager.inspectCharTyped(codePoint, modifiers);
    }

    /**
     * Forwards movement keybinds to the game so walking keeps working while
     * inspecting. Everything else stays owned by the UI.
     */
    private void forwardGameplayKey(int keyCode, int scanCode, boolean down) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        var options = Minecraft.getInstance().options;
        for (KeyMapping mapping : new KeyMapping[]{
                options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keySprint, options.keyShift
        }) {
            if (mapping.isActiveAndMatches(key)) {
                KeyMapping.set(key, down);
                return;
            }
        }
    }

    //endregion

    //region lifecycle

    @Override
    public void tick() {
        if (!manager.inspectHeld()) {
            onClose();
            return;
        }
        super.tick();
    }

    @Override
    public void removed() {
        manager.onInspectScreenRemoved();
    }

    //endregion
}
