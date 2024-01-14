package dev.vfyjxf.cloudlib.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class SoundPlayer {

    private SoundPlayer() {

    }

    public static void play(SoundInstance instance) {
        Minecraft.getInstance().getSoundManager().play(instance);
    }

}
