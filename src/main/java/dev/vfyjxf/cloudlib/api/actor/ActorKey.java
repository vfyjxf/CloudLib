package dev.vfyjxf.cloudlib.api.actor;

import net.minecraft.resources.ResourceLocation;

public record ActorKey<T>(ResourceLocation id, Class<T> type) {
}
