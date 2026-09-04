package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.util.Locations;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ApiStatus.Internal
@EventBusSubscriber(value = Dist.CLIENT, modid = Constants.modId)
public final class CloudShaders {

    private static final Logger logger = LoggerFactory.getLogger("CloudLib Shaders");

    private static @Nullable ShaderInstance roundedRect;
    private static @Nullable ShaderInstance circle;
    private static @Nullable ShaderInstance bezierCurve;
    private static @Nullable ShaderInstance shadow;

    private CloudShaders() {
    }

    //region registration

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        var provider = event.getResourceProvider();
        event.registerShader(
                new ShaderInstance(provider, Locations.ofMod("rounded_rect"), DefaultVertexFormat.POSITION_TEX),
                s -> roundedRect = s
        );
        event.registerShader(
                new ShaderInstance(provider, Locations.ofMod("circle"), DefaultVertexFormat.POSITION_TEX),
                s -> circle = s
        );
        event.registerShader(
                new ShaderInstance(provider, Locations.ofMod("bezier_curve"), DefaultVertexFormat.POSITION_TEX),
                s -> bezierCurve = s
        );
        event.registerShader(
                new ShaderInstance(provider, Locations.ofMod("shadow"), DefaultVertexFormat.POSITION_TEX),
                s -> shadow = s
        );
        logger.info("Registered CloudLib SDF shaders");
    }

    //endregion

    //region accessors

    public static @Nullable ShaderInstance roundedRect() {
        return roundedRect;
    }

    public static @Nullable ShaderInstance circle() {
        return circle;
    }

    public static @Nullable ShaderInstance bezierCurve() {
        return bezierCurve;
    }

    public static @Nullable ShaderInstance shadow() {
        return shadow;
    }

    //endregion
}
