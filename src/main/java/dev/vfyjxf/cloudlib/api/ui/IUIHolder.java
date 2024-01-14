package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.modular.IModularUI;
import net.minecraft.world.entity.player.Player;

public interface IUIHolder {

    IModularUI createUI(Player entityPlayer);

    boolean isInvalid();

    boolean isClient();

    void markAsDirty();
}
