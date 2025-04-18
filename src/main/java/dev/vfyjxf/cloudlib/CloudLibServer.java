package dev.vfyjxf.cloudlib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class CloudLibServer extends CloudLib {
    public CloudLibServer(ModContainer container, IEventBus modBus, Dist dist) {
        super(container, modBus, dist);
    }
}
