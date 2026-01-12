package dev.vfyjxf.cloudlib.api.util;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public final class Games {

    //region server

    public static @Nullable MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    //endregion


    //region recipe manager

    public static RecipeManager recipeManager() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            if (!FMLEnvironment.dist.isClient())
                throw new IllegalStateException("Cannot get recipe manager when server is null");
            Minecraft instance = Minecraft.getInstance();
            if (instance.level == null) {
                throw new IllegalStateException("Cannot get recipe manager when client level is null");
            }
            return instance.level.getRecipeManager();
        }
        return server.getRecipeManager();
    }

    //endregion

    public static RegistryAccess registryAccess() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            if (!FMLEnvironment.dist.isClient())
                throw new IllegalStateException("Cannot get registry access when server is null");
            Minecraft instance = Minecraft.getInstance();
            if (instance.level == null) {
                throw new IllegalStateException("Cannot get registry access when client level is null");
            }
            return instance.level.registryAccess();
        }
        return server.registryAccess();
    }

    private Games() {throw new AssertionError("This class should not be instantiated!");}

}
