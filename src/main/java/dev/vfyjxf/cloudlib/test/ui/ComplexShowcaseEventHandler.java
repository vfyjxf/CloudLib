package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.Constants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Event handler for opening showcase screens.
 * <p>
 * Press ` (grave/backtick) in game to open the DSL Showcase Screen.
 * Press F6 in game to open the Simple Reactive Showcase Screen.
 * Press F7 in game to open the Complex Showcase Screen.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Constants.MOD_ID)
public class ComplexShowcaseEventHandler {

    @SubscribeEvent
    private static void onInputKey(InputEvent.Key event) {
        if (FMLEnvironment.production) return;
        if (Minecraft.getInstance().player == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        
        // ` (grave key) -> DSL Showcase (main demo)
        if (event.getKey() == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            Minecraft.getInstance().setScreen(new DSLShowcaseScreen());
            return;
        }
        
        // F6 -> Simple Reactive Showcase
        if (event.getKey() == GLFW.GLFW_KEY_F6) {
            Minecraft.getInstance().setScreen(new ReactiveShowcaseScreen());
            return;
        }
        
        // F7 -> Complex Showcase
        if (event.getKey() == GLFW.GLFW_KEY_F7) {
            Minecraft.getInstance().setScreen(new ComplexShowcaseScreen());
        }
    }
}
