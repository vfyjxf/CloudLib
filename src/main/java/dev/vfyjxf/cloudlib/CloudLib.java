package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.startup.ClientStartup;
import dev.vfyjxf.cloudlib.startup.CommonStartup;
import dev.vfyjxf.cloudlib.startup.ServerStartup;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class CloudLib {
    public static CommonStartup startup;

    public CloudLib() {
        DistExecutor.unsafeRunForDist(() -> () -> {
            startup = new ClientStartup();
            return startup;
        }, () -> () -> {
            startup = new ServerStartup();
            return startup;
        });
        startup.init();
    }

}
