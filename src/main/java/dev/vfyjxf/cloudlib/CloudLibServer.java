package dev.vfyjxf.cloudlib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = Constants.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class CloudLibServer extends CloudLib {

    public static final Logger logger = LoggerFactory.getLogger("CloudLib Server");

    public CloudLibServer(ModContainer container, IEventBus modBus, Dist dist) {
        super(container, modBus, dist);
    }
}
