package dev.vfyjxf.nimbusprojection;

import dev.vfyjxf.nimbusprojection.api.presentation.InspectScope;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The client config — every feature's tunables in one place (the spec's
 * "config 收口" point). Registered as a CLIENT spec: all of these steer
 * local presentation and interaction; server-side behavior is untouched.
 */
public final class NimbusConfig {

    private NimbusConfig() {}

    // container feature
    private static ModConfigSpec.BooleanValue containersEnabled;
    private static ModConfigSpec.DoubleValue containerReach;
    private static ModConfigSpec.IntValue containerTopItems;

    // entity feature
    private static ModConfigSpec.BooleanValue entitiesEnabled;
    private static ModConfigSpec.DoubleValue entityReach;
    private static ModConfigSpec.IntValue detailStepTicks;
    private static ModConfigSpec.IntValue maxEffectsShown;

    // machine data faces — per-face toggles (the spec's "按类 config")
    private static ModConfigSpec.BooleanValue machineProgress;
    private static ModConfigSpec.BooleanValue machineText;
    private static ModConfigSpec.BooleanValue machineHive;

    // inventory satellite
    private static ModConfigSpec.BooleanValue autoSummonInventory;

    // interaction
    private static ModConfigSpec.IntValue interactTapTicks;
    private static ModConfigSpec.IntValue engageGraceTicks;

    // inspect layer (hold-R flat projection)
    private static ModConfigSpec.EnumValue<InspectScope> inspectScope;

    private static ModConfigSpec spec;

    static {
        var builder = new ModConfigSpec.Builder();

        builder.push("containers");
        {
            containersEnabled = builder.define("enabled", true);
            containerReach = builder.defineInRange("reach", 6.0, 2.0, 16.0);
            containerTopItems = builder.defineInRange("summary_items", 4, 1, 8);
        }
        builder.pop();

        builder.push("entities");
        {
            entitiesEnabled = builder.define("enabled", true);
            entityReach = builder.defineInRange("reach", 6.0, 2.0, 16.0);
            detailStepTicks = builder.defineInRange("detail_step_ticks", 10, 2, 40);
            maxEffectsShown = builder.defineInRange("max_effects", 3, 1, 10);
        }
        builder.pop();

        builder.push("machines");
        {
            machineProgress = builder.define("progress", true);
            machineText = builder.define("sign_text", true);
            machineHive = builder.define("hive", true);
        }
        builder.pop();

        builder.push("inventory");
        {
            autoSummonInventory = builder.define("auto_summon_on_engage", true);
        }
        builder.pop();

        builder.push("interaction");
        {
            interactTapTicks = builder.defineInRange("interact_tap_ticks", 8, 2, 20);
            engageGraceTicks = builder.defineInRange("engage_grace_ticks", 60, 20, 400);
        }
        builder.pop();

        builder.push("inspect");
        {
            inspectScope = builder.defineEnum("scope", InspectScope.focusAndPinned);
        }
        builder.pop();

        spec = builder.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, spec);
    }

    public static boolean containersEnabled() {
        return containersEnabled.get();
    }

    public static double containerReach() {
        return containerReach.get();
    }

    public static int containerTopItems() {
        return containerTopItems.get();
    }

    public static boolean entitiesEnabled() {
        return entitiesEnabled.get();
    }

    public static double entityReach() {
        return entityReach.get();
    }

    public static int detailStepTicks() {
        return detailStepTicks.get();
    }

    public static int maxEffectsShown() {
        return maxEffectsShown.get();
    }

    public static boolean machineProgress() {
        return machineProgress.get();
    }

    public static boolean machineText() {
        return machineText.get();
    }

    public static boolean machineHive() {
        return machineHive.get();
    }

    public static boolean autoSummonInventory() {
        return autoSummonInventory.get();
    }

    public static int interactTapTicks() {
        return interactTapTicks.get();
    }

    public static int engageGraceTicks() {
        return engageGraceTicks.get();
    }

    public static InspectScope inspectScope() {
        return inspectScope.get();
    }
}
