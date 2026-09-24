package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.base.host.GlobalSceneHost;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;

// @EventBusSubscriber(modid = Constants.modId, value = Dist.CLIENT)
class SceneManagement {

    // region fallback
    Scene fallbackScene = new Scene(new WidgetGroup<>());

    @SubscribeEvent
    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        SceneManagement management = new SceneManagement();
        management.fallbackScene.init();
        management.fallbackScene.mount(SceneContext.create(new GlobalSceneHost()));
        NeoForge.EVENT_BUS.register(management);
    }

    // endregion
}
