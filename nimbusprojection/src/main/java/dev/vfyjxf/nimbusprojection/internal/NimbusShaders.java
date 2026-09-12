package dev.vfyjxf.nimbusprojection.internal;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.vfyjxf.nimbusprojection.Constants;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Core shaders for the WBOIT pass — see {@link OitTarget} for the pipeline.
 * Registered on the mod bus via annotation, mirroring CloudLib's
 * {@code CloudShaders}.
 */
@ApiStatus.Internal
@EventBusSubscriber(value = Dist.CLIENT, modid = Constants.modId)
public final class NimbusShaders {

    private static final Logger logger = LoggerFactory.getLogger("NimbusProjection Shaders");

    /** Accum pass for textured quads (panel faces) — POSITION_TEX. */
    private static @Nullable ShaderInstance oitAccumTex;
    /** Accum pass for vertex-colored geometry (scan frames, drag trail) — POSITION_COLOR. */
    private static @Nullable ShaderInstance oitAccumColor;
    /** Fullscreen composite into the scene target — POSITION. */
    private static @Nullable ShaderInstance oitResolve;

    private NimbusShaders() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        var provider = event.getResourceProvider();
        event.registerShader(
                new ShaderInstance(provider, location("oit_accum_tex"), DefaultVertexFormat.POSITION_TEX),
                s -> oitAccumTex = s
        );
        event.registerShader(
                new ShaderInstance(provider, location("oit_accum_color"), DefaultVertexFormat.POSITION_COLOR),
                s -> oitAccumColor = s
        );
        event.registerShader(
                new ShaderInstance(provider, location("oit_resolve"), DefaultVertexFormat.POSITION),
                s -> oitResolve = s
        );
        logger.info("Registered NimbusProjection OIT shaders");
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.namespace, path);
    }

    /** All three OIT shaders loaded — the accumulate/resolve pass is usable. */
    public static boolean oitReady() {
        return oitAccumTex != null && oitAccumColor != null && oitResolve != null;
    }

    public static @Nullable ShaderInstance oitAccumTex() {
        return oitAccumTex;
    }

    public static @Nullable ShaderInstance oitAccumColor() {
        return oitAccumColor;
    }

    public static @Nullable ShaderInstance oitResolve() {
        return oitResolve;
    }
}
