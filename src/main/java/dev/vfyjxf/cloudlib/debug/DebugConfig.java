package dev.vfyjxf.cloudlib.debug;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

public class DebugConfig {

    public static boolean enableDebug() {
        return enableDebug.get();
    }

    public static boolean debugExpose() {
        return debugExpose.get();
    }

    private static final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue enableDebug;
    private static final ModConfigSpec.BooleanValue debugExpose;

    private static final ModConfigSpec SPEC;

    static {
        builder.push("common debug");
        {
            enableDebug = builder
                    .comment("Enable debug mode")
                    .define("enable_debug", !FMLEnvironment.production);
        }
        builder.pop();

        builder.push("network debug");
        {
            debugExpose = builder.comment("Enable expose debug mode")
                    .define("debug_expose", false);
        }
        builder.pop();
        SPEC = builder.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

}
