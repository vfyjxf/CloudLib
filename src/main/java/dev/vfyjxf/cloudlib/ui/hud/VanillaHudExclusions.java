package dev.vfyjxf.cloudlib.ui.hud;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.ExclusionContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.ExclusionProvider;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The built-in {@link ExclusionProvider} for the vanilla HUD: empty whenever
 * the HUD is hidden (F1), otherwise the {@link VanillaHudGeometry} rectangles
 * for the frame {@link VanillaHudSampler} measured. The hide-gui flag and the
 * input source are injected so the F1 behavior is testable headless.
 */
public final class VanillaHudExclusions implements ExclusionProvider {

    private final BooleanSupplier hideGui;
    private final Supplier<HudInputs> inputs;

    public VanillaHudExclusions(BooleanSupplier hideGui, Supplier<HudInputs> inputs) {
        this.hideGui = hideGui;
        this.inputs = inputs;
    }

    /** The provider wired to live client state. */
    public static VanillaHudExclusions vanilla(VanillaHudSampler sampler) {
        return new VanillaHudExclusions(() -> Minecraft.getInstance().options.hideGui, sampler::sample);
    }

    @Override
    public List<Rect> exclusionAreas(ExclusionContext context) {
        if (hideGui.getAsBoolean()) {
            return List.of();
        }
        return VanillaHudGeometry.compute(inputs.get());
    }
}
