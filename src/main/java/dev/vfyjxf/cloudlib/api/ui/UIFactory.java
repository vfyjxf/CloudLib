package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicSpecScreen;
import dev.vfyjxf.cloudlib.api.ui.base.PlanFragment;
import dev.vfyjxf.cloudlib.api.ui.base.ScreenFactory;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.client.Minecraft;

public final class UIFactory {

    public static <T extends BasicSpecScreen> T createScreen(ScreenFactory<T> factory) {
        PlanFragment<Widget> mainFragment = PlanFragment.create();
        PlanFragment<Widget> overlayFragment = PlanFragment.create();
        return factory.create(mainFragment, overlayFragment);
    }

    public static <T extends BasicSpecScreen> void openScreen(ScreenFactory<T> factory) {
        T screen = createScreen(factory);
        Minecraft.getInstance().setScreen(screen);
    }
}
