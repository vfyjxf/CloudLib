package dev.vfyjxf.cloudlib.startup;

import dev.vfyjxf.cloudlib.api.registry.ISerializeRegistry;
import dev.vfyjxf.cloudlib.api.ui.UIConfig;
import dev.vfyjxf.cloudlib.data.SerializeRegistry;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonStartup {

    private static final Logger LOGGER = LogManager.getLogger();

    public CommonStartup() {
        MinecraftForge.EVENT_BUS.register(this);
        Singletons.attachInstance(UIConfig.class, new UIConfig());
        Singletons.attachInstance(ISerializeRegistry.class, new SerializeRegistry());
    }

    public void init() {
    }

    public void loadComplete() {

    }

}
