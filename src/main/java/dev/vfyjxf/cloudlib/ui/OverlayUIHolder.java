package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.IUIHolder;
import dev.vfyjxf.cloudlib.api.ui.modular.IModularUI;
import net.minecraft.world.entity.player.Player;

public class OverlayUIHolder implements IUIHolder {

    @Override
    public IModularUI createUI(Player entityPlayer) {
        return new ClientModularUI();
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void markAsDirty() {

    }
}
