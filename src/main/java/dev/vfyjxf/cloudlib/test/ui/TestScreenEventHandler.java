package dev.vfyjxf.cloudlib.test.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Type;

import java.util.function.Supplier;

@EventBusSubscriber(value = Dist.CLIENT, modid = Constants.modId)
public class TestScreenEventHandler {

    public static final KeyMapping openTestScreen = new KeyMapping(
            "Open Test Screen",
            KeyConflictContext.GUI,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "debug"
    );
    public static Supplier<Screen> testScreenSupplier;

    static {
        Type annotationType = Type.getType(TestScreen.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (annotation.annotationType().equals(annotationType)) {
                    String memberName = annotation.memberName();
                    testScreenSupplier = () -> {
                        try {
                            return (Screen) Class.forName(memberName).getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    };
                }
            }
        }
    }

    @SubscribeEvent
    private static void onInputKey(InputEvent.Key event) {
        if (FMLEnvironment.production || testScreenSupplier == null) return;
        InputContext inputContext = InputContext.fromEvent(event);
        if (inputContext.released(openTestScreen)) {
            Minecraft.getInstance().setScreen(testScreenSupplier.get());
        }
    }
}
